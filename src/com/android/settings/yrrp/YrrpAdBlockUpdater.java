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
import java.util.Set;
import java.util.function.Function;
import java.util.function.LongSupplier;

/**
 * Turns the enabled sources into hosts.blocked: optionally downloads each one into its cache,
 * parses every cache, merges, and replaces hosts.blocked atomically. Then it bumps the apply
 * property so init copies the new file over the live one. If nothing usable is cached, or the
 * build fails, hosts.blocked keeps its last good content and init is not asked to copy.
 */
final class YrrpAdBlockUpdater {
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

    private final YrrpAdBlockStore mStore;
    private final YrrpAdBlockFetcher mFetcher;
    private final YrrpAdBlockState mState;
    private final YrrpAdBlockEventLog mLog;
    private final File mBlockedFile;
    private final LongSupplier mClock;
    /** The builder's entry cap; lowered by tests. */
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

    /** Runs one update; {@code download} false rebuilds from the caches only. */
    synchronized Outcome update(boolean download) {
        final YrrpAdBlockStore.State state = mStore.load();
        mLog.record("update start download=" + download);
        final int failed = download ? downloadAll(state) : 0;
        final List<Set<String>> parsed = parseCaches(state);
        final Outcome outcome = parsed.isEmpty()
                ? new Outcome(Result.NO_DATA, 0, failed)
                : build(parsed, state.allowlist, failed);
        state.lastUpdateMillis = mClock.getAsLong();
        state.lastResult = outcome.result.name();
        if (outcome.result == Result.UPDATED) {
            state.lastEntries = outcome.entries;
        }
        save(state);
        mLog.record("update end result=" + outcome.result + " entries=" + outcome.entries
                + " failedSources=" + failed);
        return outcome;
    }

    /**
     * Loads, changes, and saves the state under the update lock, so an edit made while an
     * update runs is not overwritten when that update saves.
     */
    synchronized <T> T edit(Function<YrrpAdBlockStore.State, T> change) throws IOException {
        final YrrpAdBlockStore.State state = mStore.load();
        final T result = change.apply(state);
        mStore.save(state);
        return result;
    }

    /** Removes a source and its cache under the update lock. */
    synchronized boolean removeSource(String url) throws IOException {
        final YrrpAdBlockStore.State state = mStore.load();
        final boolean removed = mStore.removeSource(state, url);
        mStore.save(state);
        return removed;
    }

    /** The saved state, read under the update lock. */
    synchronized YrrpAdBlockStore.State load() {
        return mStore.load();
    }

    private int downloadAll(YrrpAdBlockStore.State state) {
        int failed = 0;
        for (YrrpAdBlockSource source : state.sources) {
            if (source.enabled && !download(source)) {
                failed++;
            }
        }
        return failed;
    }

    /** Fetches one source into its cache; returns false on any failure. */
    private boolean download(YrrpAdBlockSource source) {
        final YrrpAdBlockFetcher.Result result = mFetcher.fetch(
                source.url, source.etag, source.lastModified, mStore.cacheFile(source));
        source.status = result.status.name();
        mLog.record("source host=" + source.host() + " status=" + result.status
                + " http=" + result.httpCode);
        if (result.status == YrrpAdBlockFetcher.Status.OK) {
            source.etag = nullToEmpty(result.etag);
            source.lastModified = nullToEmpty(result.lastModified);
        } else if (result.status != YrrpAdBlockFetcher.Status.NOT_MODIFIED) {
            return false;
        }
        source.lastSuccessMillis = mClock.getAsLong();
        return true;
    }

    /** Parses the cache of every enabled source that has one, recording its counts. */
    private List<Set<String>> parseCaches(YrrpAdBlockStore.State state) {
        final List<Set<String>> parsed = new ArrayList<>();
        for (YrrpAdBlockSource source : state.sources) {
            final File cache = mStore.cacheFile(source);
            if (!source.enabled || !cache.isFile()) {
                continue;
            }
            try (Reader in = new InputStreamReader(new FileInputStream(cache),
                    StandardCharsets.UTF_8)) {
                final YrrpAdBlockHostsParser.Result result = YrrpAdBlockHostsParser.parse(in);
                source.entries = result.hosts.size();
                source.redirectsDropped = result.redirectsDropped;
                parsed.add(result.hosts);
            } catch (IOException e) {
                source.status = YrrpAdBlockFetcher.Status.IO_ERROR.name();
            }
        }
        return parsed;
    }

    private Outcome build(List<Set<String>> parsed, Set<String> allowlist, int failed) {
        final int[] entries = new int[1];
        try {
            YrrpAdBlockFiles.writeAtomically(mBlockedFile, out ->
                    entries[0] = YrrpAdBlockHostsBuilder.write(
                            out, parsed, allowlist, mMaxEntries));
        } catch (IOException | IllegalStateException e) {
            mLog.record("build failed error=" + e.getClass().getSimpleName());
            return new Outcome(Result.FAILED, 0, failed);
        }
        // init and netd run as other users; the file holds only public list data.
        mBlockedFile.setReadable(true, false);
        mLog.record("apply bumped=" + mState.bumpApply());
        return new Outcome(Result.UPDATED, entries[0], failed);
    }

    private void save(YrrpAdBlockStore.State state) {
        try {
            mStore.save(state);
        } catch (IOException e) {
            mLog.record("save failed error=" + e.getClass().getSimpleName());
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
