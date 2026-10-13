/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Plain JUnit; no Robolectric. Scripted connections stand in for the network. */
public class YrrpAdBlockUpdaterTest {
    private static final String A = YrrpAdBlockStore.DEFAULT_URLS.get(0);
    private static final String B = YrrpAdBlockStore.DEFAULT_URLS.get(1);
    private static final String C = YrrpAdBlockStore.DEFAULT_URLS.get(2);

    private final Map<String, YrrpAdBlockFetcherTest.Reply> mReplies = new HashMap<>();
    private final List<String> mOpened = new ArrayList<>();
    private final YrrpRecordingPropertyBackend mProps = new YrrpRecordingPropertyBackend();
    private File mDir;
    private File mBlocked;
    private YrrpAdBlockStore mStore;
    private YrrpAdBlockEventLog mLog;
    private YrrpAdBlockUpdater mUpdater;
    private long mNow = 5000;
    /** Runs inside every connection open, to block or fail a download. */
    private Runnable mOnOpen = () -> { };

    @Before
    public void setUp() throws IOException {
        mDir = YrrpAdBlockTestFiles.newDir();
        mBlocked = new File(mDir, "misc/hosts.blocked");
        mBlocked.getParentFile().mkdirs();
        mStore = new YrrpAdBlockStore(new File(mDir, "store"));
        mLog = new YrrpAdBlockEventLog(new File(mDir, "store/events.log"), () -> 77);
        final YrrpAdBlockFetcher fetcher = new YrrpAdBlockFetcher(this::open);
        mUpdater = new YrrpAdBlockUpdater(mStore, fetcher, new YrrpAdBlockState(mProps), mLog,
                mBlocked, () -> mNow);
    }

    @After
    public void tearDown() throws IOException {
        YrrpAdBlockTestFiles.delete(mDir);
    }

    private HttpURLConnection open(URL url) throws IOException {
        mOpened.add(url.toString());
        mOnOpen.run();
        final YrrpAdBlockFetcherTest.Reply reply = mReplies.get(url.toString());
        if (reply == null) {
            throw new IOException("offline");
        }
        return new YrrpAdBlockFetcherTest.FakeConnection(url, reply);
    }

    private void serve(String url, String body) {
        final YrrpAdBlockFetcherTest.Reply reply = new YrrpAdBlockFetcherTest.Reply();
        reply.body = body.getBytes(StandardCharsets.UTF_8);
        reply.headers.put("ETag", "\"" + body.length() + "\"");
        mReplies.put(url, reply);
    }

    private String blocked() throws IOException {
        return YrrpAdBlockTestFiles.read(mBlocked);
    }

    @Test
    public void allSourcesOkWritesFileBumpsOnceAndSavesCounts() throws IOException {
        serve(A, "0.0.0.0 a.example\n");
        serve(B, "0.0.0.0 b.example\n0.0.0.0 a.example\n");
        serve(C, "127.0.0.1 c.example\n");

        final YrrpAdBlockUpdater.Outcome outcome = mUpdater.update(true);

        assertEquals(YrrpAdBlockUpdater.Result.UPDATED, outcome.result);
        assertEquals(3, outcome.entries);
        assertEquals(0, outcome.failedSources);
        assertEquals(YrrpAdBlockHostsBuilder.STOCK_HEADER
                + "0.0.0.0 a.example\n0.0.0.0 b.example\n0.0.0.0 c.example\n", blocked());
        assertEquals(List.of("sys.yrrp.adblock.apply=1"), mProps.writes);
        assertTrue(mBlocked.canRead());

        final YrrpAdBlockStore.State saved = mStore.load();
        assertEquals(5000, saved.lastUpdateMillis);
        assertEquals("UPDATED", saved.lastResult);
        assertEquals(3, saved.lastEntries);
        final YrrpAdBlockSource b = saved.findSource(B);
        assertEquals("OK", b.status);
        assertEquals(2, b.entries);
        assertEquals("\"" + "0.0.0.0 b.example\n0.0.0.0 a.example\n".length() + "\"", b.etag);
        assertEquals(5000, b.lastSuccessMillis);
        assertFalse(mLog.lines().isEmpty());
    }

    @Test
    public void failingSourceKeepsItsPreviousCache() throws IOException {
        serve(A, "0.0.0.0 a.example\n");
        serve(B, "0.0.0.0 b.example\n");
        serve(C, "0.0.0.0 c.example\n");
        mUpdater.update(true);
        mReplies.remove(B);
        mNow = 6000;

        final YrrpAdBlockUpdater.Outcome outcome = mUpdater.update(true);

        assertEquals(YrrpAdBlockUpdater.Result.UPDATED, outcome.result);
        assertEquals(1, outcome.failedSources);
        assertTrue(blocked().contains("0.0.0.0 b.example\n"));
        assertEquals("IO_ERROR", mStore.load().findSource(B).status);
        assertEquals(5000, mStore.load().findSource(B).lastSuccessMillis);
        assertEquals(1, mStore.load().failedSources());
    }

    @Test
    public void everyListFailingWithCachesSavesTheFailureCountForTheNextVisit()
            throws IOException {
        serve(A, "0.0.0.0 a.example\n");
        serve(B, "0.0.0.0 b.example\n");
        serve(C, "0.0.0.0 c.example\n");
        mUpdater.update(true);
        mReplies.clear();

        final YrrpAdBlockUpdater.Outcome outcome = mUpdater.update(true);

        // The last good copies still build a list, so the result alone reads as success.
        assertEquals(YrrpAdBlockUpdater.Result.UPDATED, outcome.result);
        final YrrpAdBlockStore.State saved = mStore.load();
        assertEquals(outcome.failedSources, saved.failedSources());
        assertEquals(3, saved.enabledSources());
    }

    @Test
    public void nothingCachedAndAllFailingLeavesFileAbsentAndDoesNotBump() throws IOException {
        final YrrpAdBlockUpdater.Outcome outcome = mUpdater.update(true);

        assertEquals(YrrpAdBlockUpdater.Result.NO_DATA, outcome.result);
        assertEquals(3, outcome.failedSources);
        assertFalse(mBlocked.exists());
        assertTrue(mProps.writes.isEmpty());
        assertEquals("NO_DATA", mStore.load().lastResult);
    }

    @Test
    public void overTheCapFailsAndKeepsTheLastGoodFile() throws IOException {
        YrrpAdBlockTestFiles.write(mBlocked, "last good\n");
        serve(A, "0.0.0.0 a.example\n0.0.0.0 b.example\n");
        mUpdater.mMaxEntries = 1;

        final YrrpAdBlockUpdater.Outcome outcome = mUpdater.update(true);

        assertEquals(YrrpAdBlockUpdater.Result.FAILED, outcome.result);
        assertEquals("last good\n", blocked());
        assertTrue(mProps.writes.isEmpty());
        assertFalse(new File(mBlocked.getPath() + ".tmp").exists());
    }

    @Test
    public void allowlistRemovesNames() throws IOException {
        serve(A, "0.0.0.0 a.example\n0.0.0.0 keep.example\n");
        final YrrpAdBlockStore.State state = mStore.load();
        state.addAllowed("keep.example");
        mStore.save(state);

        mUpdater.update(true);

        assertFalse(blocked().contains("keep.example"));
        assertTrue(blocked().contains("0.0.0.0 a.example\n"));
    }

    @Test
    public void disabledSourceIsNeitherFetchedNorMerged() throws IOException {
        serve(A, "0.0.0.0 a.example\n");
        serve(B, "0.0.0.0 b.example\n");
        mUpdater.update(true);
        final YrrpAdBlockStore.State state = mStore.load();
        state.findSource(B).enabled = false;
        mStore.save(state);
        mOpened.clear();

        mUpdater.update(true);

        assertFalse(mOpened.contains(B));
        assertFalse(blocked().contains("b.example"));
    }

    @Test
    public void rebuildWithoutDownloadOpensNoConnection() throws IOException {
        serve(A, "0.0.0.0 a.example\n");
        mUpdater.update(true);
        mOpened.clear();
        final YrrpAdBlockStore.State state = mStore.load();
        state.addAllowed("a.example");
        mStore.save(state);

        final YrrpAdBlockUpdater.Outcome outcome = mUpdater.update(false);

        assertTrue(mOpened.isEmpty());
        assertEquals(YrrpAdBlockUpdater.Result.UPDATED, outcome.result);
        assertEquals(0, outcome.entries);
        assertEquals(YrrpAdBlockHostsBuilder.STOCK_HEADER, blocked());
        assertEquals(List.of("sys.yrrp.adblock.apply=1", "sys.yrrp.adblock.apply=2"),
                mProps.writes);
    }

    @Test
    public void editSavesTheChangeAndReturnsItsResult() throws IOException {
        final YrrpAdBlockStore.AddResult result =
                mUpdater.edit(state -> state.addAllowed("ok.example"));

        assertEquals(YrrpAdBlockStore.AddResult.OK, result);
        assertTrue(mStore.load().allowlist.contains("ok.example"));
    }

    @Test
    public void removeSourceThroughEditDeletesItsCache() throws IOException {
        serve(A, "0.0.0.0 a.example\n");
        mUpdater.update(true);
        final File cache = mStore.cacheFile(mStore.load().findSource(A));
        assertTrue(cache.exists());

        assertTrue(mUpdater.removeSource(A));

        assertFalse(cache.exists());
        assertEquals(null, mStore.load().findSource(A));
    }

    @Test
    public void everyListOffWritesOnlyLocalhostAndBumps() throws IOException {
        serve(A, "0.0.0.0 a.example\n");
        mUpdater.update(true);
        mUpdater.edit(state -> {
            state.sources.forEach(source -> source.enabled = false);
            return null;
        });

        final YrrpAdBlockUpdater.Outcome outcome = mUpdater.update(false);

        assertEquals(YrrpAdBlockUpdater.Result.UPDATED, outcome.result);
        assertEquals(YrrpAdBlockHostsBuilder.STOCK_HEADER, blocked());
        assertEquals(List.of("sys.yrrp.adblock.apply=1", "sys.yrrp.adblock.apply=2"),
                mProps.writes);
    }

    @Test
    public void emptyReplyKeepsTheLastGoodCache() throws IOException {
        serve(A, "0.0.0.0 a.example\n");
        mUpdater.update(true);
        serve(A, "<html>maintenance</html>\n");

        final YrrpAdBlockUpdater.Outcome outcome = mUpdater.update(true);

        // A, plus B and C, which are offline in this test.
        assertEquals(3, outcome.failedSources);
        assertTrue(blocked().contains("0.0.0.0 a.example\n"));
        final YrrpAdBlockSource a = mStore.load().findSource(A);
        assertEquals(YrrpAdBlockUpdater.STATUS_EMPTY, a.status);
        assertEquals(outcome.failedSources, mStore.load().failedSources());
        assertEquals(1, a.entries);
        assertEquals("0.0.0.0 a.example\n", YrrpAdBlockTestFiles.read(mStore.cacheFile(a)));
    }

    @Test
    public void unexpectedErrorsBecomeFailedAndKeepTheFile() throws IOException {
        YrrpAdBlockTestFiles.write(mBlocked, "last good\n");
        mOnOpen = () -> {
            throw new IllegalStateException("boom");
        };

        final YrrpAdBlockUpdater.Outcome outcome = mUpdater.update(true);

        assertEquals(YrrpAdBlockUpdater.Result.FAILED, outcome.result);
        assertEquals("last good\n", blocked());
        assertTrue(mProps.writes.isEmpty());
    }

    @Test
    public void editsDoNotWaitForDownloadsAndSurviveTheUpdate() throws Exception {
        serve(A, "0.0.0.0 a.example\n");
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        mOnOpen = () -> {
            entered.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        final ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            final Future<YrrpAdBlockUpdater.Outcome> update =
                    pool.submit(() -> mUpdater.update(true));
            assertTrue(entered.await(5, TimeUnit.SECONDS));

            final Future<YrrpAdBlockStore.AddResult> edit =
                    pool.submit(() -> mUpdater.edit(state -> state.addAllowed("kept.example")));
            assertEquals(YrrpAdBlockStore.AddResult.OK, edit.get(2, TimeUnit.SECONDS));

            release.countDown();
            assertEquals(YrrpAdBlockUpdater.Result.UPDATED,
                    update.get(5, TimeUnit.SECONDS).result);
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
        assertTrue(mStore.load().allowlist.contains("kept.example"));
    }

    @Test
    public void listRemovedDuringDownloadIsDroppedWithItsCache() throws Exception {
        serve(A, "0.0.0.0 a.example\n");
        mOnOpen = () -> {
            try {
                mUpdater.removeSource(A);
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        };

        final YrrpAdBlockUpdater.Outcome outcome = mUpdater.update(true);

        // B and C are offline and have no cache, so nothing is written.
        assertEquals(YrrpAdBlockUpdater.Result.NO_DATA, outcome.result);
        assertEquals(null, mStore.load().findSource(A));
        assertFalse(mBlocked.exists());
        assertFalse(mStore.cacheFile(new YrrpAdBlockSource(A)).exists());
    }

    @Test
    public void redirectEntriesAreCountedAndNeverWritten() throws IOException {
        serve(A, "0.0.0.0 a.example\n6.6.6.6 bank.example\n");

        mUpdater.update(true);

        assertFalse(blocked().contains("bank.example"));
        assertEquals(1, mStore.load().findSource(A).redirectsDropped);
    }
}
