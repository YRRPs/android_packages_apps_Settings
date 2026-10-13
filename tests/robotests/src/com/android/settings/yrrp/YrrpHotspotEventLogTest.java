/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

/** Plain JUnit; no Robolectric. */
public class YrrpHotspotEventLogTest {
    @Test
    public void prefixesClockAndFlattensNewlines() {
        final YrrpHotspotEventLog log = new YrrpHotspotEventLog(() -> 42L);
        log.record("write field=blocked\nresult=ok");
        assertEquals(List.of("42 write field=blocked result=ok"), log.lines());
    }

    @Test
    public void keepsNewestCapacityEntries() {
        final AtomicLong clock = new AtomicLong();
        final YrrpHotspotEventLog log = new YrrpHotspotEventLog(clock::incrementAndGet);
        for (int i = 0; i < YrrpHotspotEventLog.CAPACITY + 5; i++) {
            log.record("e");
        }
        final List<String> lines = log.lines();
        assertEquals(YrrpHotspotEventLog.CAPACITY, lines.size());
        assertEquals("6 e", lines.get(0));
    }

    @Test
    public void linesIsASnapshot() {
        final YrrpHotspotEventLog log = new YrrpHotspotEventLog(() -> 1L);
        final List<String> before = log.lines();
        log.record("e");
        assertEquals(0, before.size());
    }
}
