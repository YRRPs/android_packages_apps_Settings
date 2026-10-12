/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/** Plain JUnit; no Robolectric. */
public class YrrpAdBlockDumpTest {
    private static final String MOUNT_LINE = "123 45 259:3 /misc/yrrp/hosts /system/etc/hosts "
            + "rw,relatime shared:1 - ext4 /dev/block/dm-48 rw,seclabel";

    private static String dump(YrrpAdBlockDump.Snapshot snapshot) {
        final StringWriter text = new StringWriter();
        YrrpAdBlockDump.format(new PrintWriter(text), snapshot);
        return text.toString();
    }

    private static YrrpAdBlockDump.Snapshot sample() {
        final YrrpAdBlockDump.Snapshot s = new YrrpAdBlockDump.Snapshot();
        s.enabled = true;
        s.autoUpdate = true;
        s.primaryUser = true;
        s.jobScheduled = true;
        s.blockedBytes = 2048;
        s.blockedEntries = 6;
        s.liveHostsBytes = 2048;
        s.bindMountPresent = true;
        s.state = new YrrpAdBlockStore.State();
        s.state.lastUpdateMillis = 1234;
        s.state.lastResult = "UPDATED";
        s.state.allowlist.add("secret-allowed.example");
        final YrrpAdBlockSource source =
                new YrrpAdBlockSource("https://lists.example/path/hosts?token=SECRET");
        source.status = "OK";
        source.entries = 7;
        source.etag = "\"x\"";
        s.state.sources.add(source);
        s.events = List.of("1 toggle enabled=true");
        return s;
    }

    @Test
    public void printsEveryField() {
        final String text = dump(sample());
        for (String field : new String[] {"enabled=true", "autoUpdate=true", "isPrimaryUser=true",
                "jobScheduled=true", "hostsBlockedBytes=2048", "blockedEntries=6",
                "sourceEntries=7",
                "liveHostsBytes=2048", "bindMountPresent=true", "lastUpdate=1234",
                "lastResult=UPDATED", "allowlistSize=1", "source[0]", "host=lists.example",
                "status=OK", "droppedRedirects=0", "etagPresent=true", "lastSuccess=0",
                "events:", "1 toggle enabled=true"}) {
            assertTrue(field + " in\n" + text, text.contains(field));
        }
    }

    @Test
    public void neverPrintsPathsQueriesOrAllowlistEntries() {
        final String text = dump(sample());
        assertFalse(text.contains("SECRET"));
        assertFalse(text.contains("/path/"));
        assertFalse(text.contains("secret-allowed"));
    }

    @Test
    public void entriesSumOnlyEnabledSources() {
        final YrrpAdBlockDump.Snapshot s = sample();
        final YrrpAdBlockSource off = new YrrpAdBlockSource("https://off.example/h");
        off.enabled = false;
        off.entries = 100;
        s.state.sources.add(off);
        assertTrue(dump(s).contains("sourceEntries=7 "));
    }

    @Test
    public void detectsTheBindMount() {
        assertTrue(YrrpAdBlockDump.bindMountPresent(List.of("1 2 3:4 / / rw - ext4 x rw",
                MOUNT_LINE)));
        assertFalse(YrrpAdBlockDump.bindMountPresent(List.of("1 2 3:4 / /system ro - ext4 x ro")));
        assertFalse(YrrpAdBlockDump.bindMountPresent(List.of("short")));
    }
}
