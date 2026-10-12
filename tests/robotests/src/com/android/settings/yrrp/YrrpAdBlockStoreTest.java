/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/** Plain JUnit; no Robolectric. */
public class YrrpAdBlockStoreTest {
    private File mDir;
    private YrrpAdBlockStore mStore;

    @Before
    public void setUp() throws IOException {
        mDir = YrrpAdBlockTestFiles.newDir();
        mStore = new YrrpAdBlockStore(mDir);
    }

    @After
    public void tearDown() throws IOException {
        YrrpAdBlockTestFiles.delete(mDir);
    }

    @Test
    public void emptyDirectoryLoadsDefaults() throws IOException {
        final YrrpAdBlockStore.State state = mStore.load();
        assertEquals(YrrpAdBlockStore.DEFAULT_URLS, urls(state));
        for (YrrpAdBlockSource source : state.sources) {
            assertTrue(source.enabled);
            assertEquals(YrrpAdBlockSource.STATUS_NEVER, source.status);
        }
        assertTrue(state.autoUpdate);
        assertTrue(state.allowlist.isEmpty());
        assertEquals(0, state.lastUpdateMillis);
    }

    @Test
    public void roundTripsEveryField() throws IOException {
        final YrrpAdBlockStore.State state = mStore.load();
        final YrrpAdBlockSource source = state.sources.get(1);
        source.enabled = false;
        source.etag = "\"abc\"";
        source.lastModified = "Wed, 21 Oct 2026 07:28:00 GMT";
        source.status = "OK";
        source.entries = 1234;
        source.redirectsDropped = 5;
        source.lastSuccessMillis = 42;
        state.allowlist.add("good.example");
        state.autoUpdate = false;
        state.lastUpdateMillis = 99;
        state.lastResult = "UPDATED";
        state.lastEntries = 321;
        mStore.save(state);

        final YrrpAdBlockStore.State read = new YrrpAdBlockStore(mDir).load();
        final YrrpAdBlockSource back = read.sources.get(1);
        assertFalse(back.enabled);
        assertEquals("\"abc\"", back.etag);
        assertEquals("Wed, 21 Oct 2026 07:28:00 GMT", back.lastModified);
        assertEquals("OK", back.status);
        assertEquals(1234, back.entries);
        assertEquals(5, back.redirectsDropped);
        assertEquals(42, back.lastSuccessMillis);
        assertEquals(Set.of("good.example"), read.allowlist);
        assertFalse(read.autoUpdate);
        assertEquals(99, read.lastUpdateMillis);
        assertEquals("UPDATED", read.lastResult);
        assertEquals(321, read.lastEntries);
        assertFalse(new File(mDir, "state.tsv.tmp").exists());
    }

    @Test
    public void controlCharactersInHeadersAreNotStored() throws IOException {
        final YrrpAdBlockStore.State state = mStore.load();
        state.sources.get(0).etag = "a\tb";
        state.sources.get(0).lastModified = "x\ny";
        mStore.save(state);
        final YrrpAdBlockStore.State read = mStore.load();
        assertEquals("", read.sources.get(0).etag);
        assertEquals("", read.sources.get(0).lastModified);
        assertEquals(3, read.sources.size());
    }

    @Test
    public void addSourceAcceptsOnlyHttpsUrls() throws IOException {
        final YrrpAdBlockStore.State state = mStore.load();
        assertEquals(YrrpAdBlockStore.AddResult.NOT_HTTPS,
                state.addSource("http://lists.example/hosts"));
        assertEquals(YrrpAdBlockStore.AddResult.NOT_HTTPS,
                state.addSource("ftp://lists.example/hosts"));
        assertEquals(YrrpAdBlockStore.AddResult.MALFORMED, state.addSource("not a url"));
        assertEquals(YrrpAdBlockStore.AddResult.MALFORMED,
                state.addSource("https://user@lists.example/hosts"));
        assertEquals(YrrpAdBlockStore.AddResult.MALFORMED, state.addSource("https:///hosts"));
        assertEquals(YrrpAdBlockStore.AddResult.DUPLICATE,
                state.addSource(YrrpAdBlockStore.DEFAULT_URLS.get(0)));
        assertEquals(YrrpAdBlockStore.AddResult.OK,
                state.addSource(" https://lists.example/hosts "));
        assertEquals(4, state.sources.size());
        assertEquals("https://lists.example/hosts", state.sources.get(3).url);
        assertTrue(state.sources.get(3).enabled);
    }

    @Test
    public void addSourceStopsAtTheLimit() throws IOException {
        final YrrpAdBlockStore.State state = mStore.load();
        for (int i = state.sources.size(); i < YrrpAdBlockStore.MAX_SOURCES; i++) {
            assertEquals(YrrpAdBlockStore.AddResult.OK,
                    state.addSource("https://l" + i + ".example/hosts"));
        }
        assertEquals(YrrpAdBlockStore.AddResult.LIMIT,
                state.addSource("https://one-more.example/hosts"));
    }

    @Test
    public void removeSourceDeletesItsCache() throws IOException {
        final YrrpAdBlockStore.State state = mStore.load();
        final YrrpAdBlockSource source = state.sources.get(0);
        final File cache = mStore.cacheFile(source);
        YrrpAdBlockTestFiles.write(cache, "0.0.0.0 a.example\n");
        assertTrue(mStore.removeSource(state, source.url));
        assertFalse(cache.exists());
        assertEquals(2, state.sources.size());
        assertFalse(mStore.removeSource(state, source.url));
    }

    @Test
    public void cacheNamesAreStableAndDistinct() throws IOException {
        final List<YrrpAdBlockSource> sources = mStore.load().sources;
        assertEquals(mStore.cacheFile(sources.get(0)), mStore.cacheFile(sources.get(0)));
        assertNotEquals(mStore.cacheFile(sources.get(0)), mStore.cacheFile(sources.get(1)));
        assertTrue(mStore.cacheFile(sources.get(0)).getName()
                .matches("src-[0-9a-f]{16}\\.hosts"));
    }

    @Test
    public void allowlistNormalizesAndRejects() throws IOException {
        final YrrpAdBlockStore.State state = mStore.load();
        assertEquals(YrrpAdBlockStore.AddResult.OK, state.addAllowed(" Good.Example "));
        assertEquals(YrrpAdBlockStore.AddResult.DUPLICATE, state.addAllowed("good.example"));
        assertEquals(YrrpAdBlockStore.AddResult.INVALID_HOST, state.addAllowed("localhost"));
        assertEquals(YrrpAdBlockStore.AddResult.INVALID_HOST, state.addAllowed("a b"));
        assertEquals(Set.of("good.example"), state.allowlist);
        assertTrue(state.removeAllowed("good.example"));
        assertTrue(state.allowlist.isEmpty());
    }

    @Test
    public void corruptLinesAreSkipped() throws IOException {
        YrrpAdBlockTestFiles.write(new File(mDir, "state.tsv"),
                "V\t1\nA\t0\t7\tUPDATED\nS\tgarbage\n"
                        + "S\thttps://one.example/h\t1\t\t\tOK\t3\t0\t5\n"
                        + "S\thttp://plain.example/h\t1\t\t\tOK\t3\t0\t5\n"
                        + "H\tok.example\nH\tlocalhost\nZ\twhat\n");
        final YrrpAdBlockStore.State state = mStore.load();
        assertEquals(List.of("https://one.example/h"), urls(state));
        assertEquals(Set.of("ok.example"), state.allowlist);
        assertFalse(state.autoUpdate);
        assertEquals(7, state.lastUpdateMillis);
    }

    @Test
    public void unreadableStateFailsInsteadOfResettingToDefaults() throws IOException {
        new File(mDir, "state.tsv").mkdirs();
        try {
            mStore.load();
            throw new AssertionError("load must fail");
        } catch (IOException expected) {
            // A save after a silent reset would replace the user's lists.
        }
        assertEquals(YrrpAdBlockStore.DEFAULT_URLS, urls(mStore.loadOrDefaults()));
    }

    private static List<String> urls(YrrpAdBlockStore.State state) {
        return state.sources.stream().map(s -> s.url).toList();
    }
}
