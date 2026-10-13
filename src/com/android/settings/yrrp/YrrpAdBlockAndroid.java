/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.content.Context;
import android.os.UserManager;

import com.android.settingslib.utils.ThreadUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Consumer;

/**
 * Production wiring for ad blocking, shared by the page, the tile, the job, and the status
 * service, so they all act through one updater and one switch path. Ad blocking is device-wide,
 * so only the primary (system) user controls it.
 */
final class YrrpAdBlockAndroid {
    static final File BLOCKED_FILE = new File("/data/misc/yrrp/hosts.blocked");
    static final File LIVE_HOSTS = new File("/system/etc/hosts");
    private static final File MOUNTINFO = new File("/proc/self/mountinfo");
    private static final String DIR_NAME = "yrrp_adblock";

    private static final YrrpAdBlockObservers sEnabledObservers = new YrrpAdBlockObservers();
    private static final YrrpAdBlockObservers sUpdateObservers = new YrrpAdBlockObservers();

    private static YrrpAdBlockUpdater sUpdater;
    private static YrrpAdBlockEventLog sLog;

    private YrrpAdBlockAndroid() {}

    static boolean isPrimaryUser(Context context) {
        return context.getSystemService(UserManager.class).isSystemUser();
    }

    static YrrpAdBlockState state() {
        return YrrpAdBlockState.create();
    }

    static synchronized YrrpAdBlockUpdater updater(Context context) {
        if (sUpdater == null) {
            final File dir = dir(context);
            sUpdater = new YrrpAdBlockUpdater(new YrrpAdBlockStore(dir),
                    YrrpAdBlockFetcher.create(), state(), log(context), BLOCKED_FILE,
                    System::currentTimeMillis);
        }
        return sUpdater;
    }

    /**
     * The saved state without taking the update lock, so the main thread never waits for a
     * download. state.tsv is replaced by rename, so a reader sees a whole file.
     */
    static YrrpAdBlockStore.State readState(Context context) {
        return new YrrpAdBlockStore(dir(context)).loadOrDefaults();
    }

    /** One shared log, so its lock serializes every writer. Record off the main thread. */
    static synchronized YrrpAdBlockEventLog log(Context context) {
        if (sLog == null) {
            sLog = new YrrpAdBlockEventLog(new File(dir(context), "events.log"),
                    System::currentTimeMillis);
        }
        return sLog;
    }

    /** Records an event on a background thread; safe to call from the main thread. */
    static void record(Context context, String event) {
        final Context app = context.getApplicationContext();
        ThreadUtils.postOnBackgroundThread(() -> log(app).record(event));
    }

    /** Run on the main thread after every switch write, from the page or the tile. */
    static YrrpAdBlockObservers enabledObservers() {
        return sEnabledObservers;
    }

    /** Run on the main thread after every {@link #runUpdate}, from the page, the job, or init. */
    static YrrpAdBlockObservers updateObservers() {
        return sUpdateObservers;
    }

    /**
     * The one switch path for the page and the tile. Turning on with no hosts.blocked yet
     * starts a download, because init has nothing to copy until one finishes. Observers run
     * after the caller returns, so the page switch that made the change is not re-entered.
     */
    static boolean setEnabled(Context context, boolean enabled) {
        final Context app = context.getApplicationContext();
        final boolean written = state().setEnabled(enabled);
        record(app, "toggle enabled=" + enabled + " written=" + written);
        ThreadUtils.postOnMainThread(sEnabledObservers::notifyChanged);
        if (written && enabled && !BLOCKED_FILE.exists()) {
            runUpdate(app, true, null);
        }
        YrrpAdBlockUpdateJobService.sync(app);
        return written;
    }

    /**
     * Runs an update off the main thread. {@code onDone}, then the update observers, always
     * run on the main thread, even if the update throws, so a waiting row or job is never left
     * hanging and an open page redraws its list statuses.
     */
    static void runUpdate(Context context, boolean download,
            Consumer<YrrpAdBlockUpdater.Outcome> onDone) {
        final Context app = context.getApplicationContext();
        ThreadUtils.postOnBackgroundThread(() -> {
            YrrpAdBlockUpdater.Outcome outcome =
                    new YrrpAdBlockUpdater.Outcome(YrrpAdBlockUpdater.Result.FAILED, 0, 0);
            try {
                outcome = updater(app).update(download);
            } finally {
                final YrrpAdBlockUpdater.Outcome done = outcome;
                ThreadUtils.postOnMainThread(() -> {
                    if (onDone != null) {
                        onDone.accept(done);
                    }
                    sUpdateObservers.notifyChanged();
                });
            }
        });
    }

    /** Gathers the dump inputs. Reads files, so call it off the main thread or from dump(). */
    static YrrpAdBlockDump.Snapshot snapshot(Context context) {
        final YrrpAdBlockDump.Snapshot s = new YrrpAdBlockDump.Snapshot();
        s.enabled = state().isEnabled();
        s.state = readState(context);
        s.autoUpdate = s.state.autoUpdate;
        s.primaryUser = isPrimaryUser(context);
        s.jobScheduled = YrrpAdBlockUpdateJobService.isScheduled(context);
        s.blockedBytes = BLOCKED_FILE.exists() ? BLOCKED_FILE.length() : -1;
        s.blockedEntries = countBlockLines(BLOCKED_FILE);
        s.liveHostsBytes = LIVE_HOSTS.length();
        s.bindMountPresent = YrrpAdBlockDump.bindMountPresent(readLines(MOUNTINFO));
        s.events = log(context).lines();
        return s;
    }

    private static File dir(Context context) {
        return new File(context.createDeviceProtectedStorageContext().getFilesDir(), DIR_NAME);
    }

    private static long countBlockLines(File file) {
        if (!file.exists()) {
            return -1;
        }
        long count = 0;
        try (BufferedReader in = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                if (line.startsWith("0.0.0.0 ")) {
                    count++;
                }
            }
        } catch (IOException e) {
            return -1;
        }
        return count;
    }

    private static List<String> readLines(File file) {
        try {
            return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return List.of();
        }
    }
}
