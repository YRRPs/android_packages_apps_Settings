/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import java.io.PrintWriter;
import java.util.List;

/**
 * The ad-block dump: every input that decides whether a lookup is blocked, one per line, so a
 * single dumpsys shows which link is broken. Sources print by host only; the allowlist prints
 * as a count.
 */
final class YrrpAdBlockDump {
    private static final String HOSTS_PATH = "/system/etc/hosts";

    /** Everything the dump prints, gathered by the caller. */
    static final class Snapshot {
        boolean enabled;
        boolean autoUpdate;
        boolean primaryUser;
        boolean jobScheduled;
        /** -1 when hosts.blocked is missing. */
        long blockedBytes = -1;
        /** Block lines in hosts.blocked; -1 when it is missing. */
        long blockedEntries = -1;
        long liveHostsBytes = -1;
        boolean bindMountPresent;
        YrrpAdBlockStore.State state = new YrrpAdBlockStore.State();
        List<String> events = List.of();
    }

    private YrrpAdBlockDump() {}

    static void format(PrintWriter pw, Snapshot s) {
        pw.println("enabled=" + s.enabled + " autoUpdate=" + s.autoUpdate
                + " isPrimaryUser=" + s.primaryUser + " jobScheduled=" + s.jobScheduled);
        pw.println("hostsBlockedBytes=" + s.blockedBytes + " blockedEntries=" + s.blockedEntries
                + " sourceEntries=" + enabledEntries(s.state)
                + " liveHostsBytes=" + s.liveHostsBytes
                + " bindMountPresent=" + s.bindMountPresent);
        pw.println("lastUpdate=" + s.state.lastUpdateMillis + " lastResult=" + s.state.lastResult
                + " allowlistSize=" + s.state.allowlist.size());
        for (int i = 0; i < s.state.sources.size(); i++) {
            final YrrpAdBlockSource src = s.state.sources.get(i);
            pw.println("source[" + i + "] host=" + src.host() + " enabled=" + src.enabled
                    + " status=" + src.status + " entries=" + src.entries
                    + " droppedRedirects=" + src.redirectsDropped
                    + " etagPresent=" + !src.etag.isEmpty()
                    + " lastSuccess=" + src.lastSuccessMillis);
        }
        pw.println("events:");
        for (String event : s.events) {
            pw.println("  " + event);
        }
        pw.flush();
    }

    /** True when a /proc/<pid>/mountinfo line mounts something on /system/etc/hosts. */
    static boolean bindMountPresent(List<String> mountinfoLines) {
        for (String line : mountinfoLines) {
            final String[] fields = line.split(" ");
            if (fields.length > 4 && HOSTS_PATH.equals(fields[4])) {
                return true;
            }
        }
        return false;
    }

    /** Per-source counts before merging; duplicates across sources count once per source. */
    private static int enabledEntries(YrrpAdBlockStore.State state) {
        int total = 0;
        for (YrrpAdBlockSource source : state.sources) {
            if (source.enabled) {
                total += source.entries;
            }
        }
        return total;
    }
}
