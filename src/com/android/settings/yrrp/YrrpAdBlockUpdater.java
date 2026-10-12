/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.LongSupplier;

/**
 * Turns the enabled lists into hosts.blocked, then bumps the apply property so init copies it
 * over the live file. If an enabled list has nothing usable, or anything fails, hosts.blocked
 * keeps its last good content and init is not asked to copy.
 *
 * <p>Two locks. The update lock lets one update run at a time. The state lock ({@code this})
 * guards each load-change-save of state.tsv and is never held across a download, so a page
 * edit waits at most for a rebuild from the caches, not for the network. Downloads land in a
 * staging file and are promoted to the cache under the state lock, only if the list still
 * exists.
 */
final class YrrpAdBlockUpdater {
    /** Status of a list that answered OK with no block entries while it had some before. */
    static final String STATUS_EMPTY = "EMPTY";

    enum Result { UPDATED, NO_DATA, FAILED }

    /** What one run did. */
    static final class Outcome {
        final Result result;
        final int entries;
        final int failedSources;

        Outcome(Result result, int entries, int failedSources) {
            this.result = result;
            this.entries = entries;
            this.failedSources = failedSources;
        }
    }

    /** One list's download, decided outside the state lock. */
    private static final class Download {
        final String url;
        final File staging;
        final String status;
        final YrrpAdBlockFetcher.Result result;

        Download(String url, File staging, String status, YrrpAdBlockFetcher.Result result) {
            this.url = url;
            this.staging = staging;
            this.status = status;
            this.result = result;
        }

        boolean succeeded() {
            return YrrpAdBlockFetcher.Status.OK.name().equals(status)
                    || YrrpAdBlockFetcher.Status.NOT_MODIFIED.name().equals(status);
        }
    }

    private final Object mUpdateLock = new Object();
    private final YrrpAdBlockStore mStore;
    private final YrrpAdBlockFetcher mFetcher;
    private final YrrpAdBlockState mState;
    private final YrrpAdBlockEventLog mLog;
    private final File mBlockedFile;
    private final LongSupplier mClock;
    /** The merged entry cap; lowered by tests. */
    int mMaxEntries = YrrpAdBlockHostsBuilder.MAX_ENTRIES;

    YrrpAdBlockUpdater(YrrpAdBlockStore store, YrrpAdBlockFetcher fetcher, YrrpAdBlockState state,
            YrrpAdBlockEventLog log, File blockedFile, LongSupplier clock) {
        mStore = store;
        mFetcher = fetcher;
        mState = state;
        mLog = log;
        mBlockedFile = blockedFile;
        mClock = clock;
    }

    /**
     * Runs one update; {@code download} false rebuilds from the caches only. Never throws:
     * any failure, out-of-memory included, ends as FAILED with hosts.blocked unchanged.
     */
    Outcome update(boolean download) {
        synchronized (mUpdateLock) {
            mLog.record("update start download=" + download);
            Outcome outcome;
            try {
                outcome = runUpdate(download);
            } catch (IOException | RuntimeException | OutOfMemoryError e) {
                mLog.record("update failed error=" + e.getClass().getSimpleName());
                outcome = new Outcome(Result.FAILED, 0, 0);
                saveResultQuietly(Result.FAILED);
            }
            mLog.record("update end result=" + outcome.result + " entries=" + outcome.entries
                    + " failedSources=" + outcome.failedSources);
            return outcome;
        }
    }

    /**
     * Loads, changes, and saves the state under the state lock; never waits for a download, so
     * an edit made while an update runs is kept when that update saves.
     */
    synchronized <T> T edit(Function<YrrpAdBlockStore.State, T> change) throws IOException {
        final YrrpAdBlockStore.State state = mStore.load();
        final T result = change.apply(state);
        mStore.save(state);
        return result;
    }

    /** Removes a list and its cache under the state lock. */
    synchronized boolean removeSource(String url) throws IOException {
        final YrrpAdBlockStore.State state = mStore.load();
        final boolean removed = mStore.removeSource(state, url);
        mStore.save(state);
        return removed;
    }

    private Outcome runUpdate(boolean download) throws IOException {
        final List<Download> downloads = download ? downloadAll(enabledSnapshot()) : List.of();
        synchronized (this) {
            final YrrpAdBlockStore.State state = mStore.load();
            final int failed = promote(downloads, state);
            final Outcome outcome = build(state, failed);
            state.lastUpdateMillis = mClock.getAsLong();
            state.lastResult = outcome.result.name();
            if (outcome.result == Result.UPDATED) {
                state.lastEntries = outcome.entries;
            }
            mStore.save(state);
            return outcome;
        }
    }

    private synchronized List<YrrpAdBlockSource> enabledSnapshot() throws IOException {
        final List<YrrpAdBlockSource> enabled = new ArrayList<>();
        for (YrrpAdBlockSource source : mStore.load().sources) {
            if (source.enabled) {
                enabled.add(source);
            }
        }
        return enabled;
    }

    private List<Download> downloadAll(List<YrrpAdBlockSource> sources) {
        final List<Download> downloads = new ArrayList<>();
        for (YrrpAdBlockSource source : sources) {
            downloads.add(download(source));
        }
        return downloads;
    }

    /**
     * Fetches one list into its staging file. A 200 that parses to nothing while the list had
     * entries before (an error page, an empty body) counts as a failure, so the cache stays.
     */
    private Download download(YrrpAdBlockSource source) {
        final File staging = new File(mStore.cacheFile(source).getPath() + ".download");
        final YrrpAdBlockFetcher.Result result =
                mFetcher.fetch(source.url, source.etag, source.lastModified, staging);
        String status = result.status.name();
        if (result.status == YrrpAdBlockFetcher.Status.OK && source.entries > 0
                && parseCount(staging) == 0) {
            status = STATUS_EMPTY;
            staging.delete();
        }
        mLog.record("source host=" + source.host() + " status=" + status
                + " http=" + result.httpCode);
        return new Download(source.url, staging, status, result);
    }

    /** Applies downloads to the lists that still exist; returns how many failed. */
    private int promote(List<Download> downloads, YrrpAdBlockStore.State state)
            throws IOException {
        int failed = 0;
        for (Download d : downloads) {
            final YrrpAdBlockSource source = state.findSource(d.url);
            if (source == null) {
                d.staging.delete();
                continue;
            }
            source.status = d.status;
            if (!d.succeeded()) {
                failed++;
                continue;
            }
            if (d.result.status == YrrpAdBlockFetcher.Status.OK) {
                if (!d.staging.renameTo(mStore.cacheFile(source))) {
                    throw new IOException("cache rename failed");
                }
                source.etag = nullToEmpty(d.result.etag);
                source.lastModified = nullToEmpty(d.result.lastModified);
            }
            source.lastSuccessMillis = mClock.getAsLong();
        }
        return failed;
    }

    private Outcome build(YrrpAdBlockStore.State state, int failed) throws IOException {
        int enabled = 0;
        int cached = 0;
        final TreeSet<String> merged = new TreeSet<>();
        for (YrrpAdBlockSource source : state.sources) {
            if (!source.enabled) {
                continue;
            }
            enabled++;
            if (mergeCache(source, merged)) {
                cached++;
            }
        }
        if (enabled > 0 && cached == 0) {
            return new Outcome(Result.NO_DATA, 0, failed);
        }
        // With every list off this writes only the localhost lines: a deliberate empty list.
        merged.removeAll(state.allowlist);
        YrrpAdBlockFiles.writeAtomically(mBlockedFile,
                out -> YrrpAdBlockHostsBuilder.writeSorted(out, merged));
        // init and netd run as other users; the file holds only public list data.
        mBlockedFile.setReadable(true, false);
        mLog.record("apply bumped=" + mState.bumpApply());
        return new Outcome(Result.UPDATED, merged.size(), failed);
    }

    /**
     * Parses one list's cache into {@code merged}, recording its counts. Returns false when it
     * has no cache. Throws once the merged set passes the cap, before it grows further.
     */
    private boolean mergeCache(YrrpAdBlockSource source, TreeSet<String> merged)
            throws IOException {
        final File cache = mStore.cacheFile(source);
        if (!cache.isFile()) {
            return false;
        }
        final YrrpAdBlockHostsParser.Result result = parse(cache);
        source.entries = result.hosts.size();
        source.redirectsDropped = result.redirectsDropped;
        merged.addAll(result.hosts);
        if (merged.size() > mMaxEntries) {
            throw new IllegalStateException("entries over " + mMaxEntries);
        }
        return true;
    }

    private static YrrpAdBlockHostsParser.Result parse(File file) throws IOException {
        try (Reader in = new InputStreamReader(new FileInputStream(file),
                StandardCharsets.UTF_8)) {
            return YrrpAdBlockHostsParser.parse(in);
        }
    }

    private static int parseCount(File file) {
        try {
            return parse(file).hosts.size();
        } catch (IOException e) {
            return 0;
        }
    }

    private synchronized void saveResultQuietly(Result result) {
        try {
            final YrrpAdBlockStore.State state = mStore.load();
            state.lastUpdateMillis = mClock.getAsLong();
            state.lastResult = result.name();
            mStore.save(state);
        } catch (IOException | RuntimeException e) {
            mLog.record("save failed error=" + e.getClass().getSimpleName());
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
