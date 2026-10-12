/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** Plain JUnit; no Robolectric. */
public class YrrpAdBlockEventLogTest {
    private File mDir;
    private long mNow = 1000;
    private YrrpAdBlockEventLog mLog;

    @Before
    public void setUp() throws IOException {
        mDir = YrrpAdBlockTestFiles.newDir();
        mLog = new YrrpAdBlockEventLog(new File(mDir, "events.log"), () -> mNow);
    }

    @After
    public void tearDown() {
        YrrpAdBlockTestFiles.delete(mDir);
    }

    @Test
    public void missingFileReadsAsEmpty() {
        assertTrue(mLog.lines().isEmpty());
    }

    @Test
    public void recordsWithTimestampsOldestFirst() {
        mLog.record("toggle enabled=true");
        mNow = 2000;
        mLog.record("update result=UPDATED");
        assertEquals(List.of("1000 toggle enabled=true", "2000 update result=UPDATED"),
                mLog.lines());
    }

    @Test
    public void keepsOnlyTheLastFifty() {
        for (int i = 0; i < 60; i++) {
            mNow = i;
            mLog.record("e" + i);
        }
        final List<String> lines =
                new YrrpAdBlockEventLog(new File(mDir, "events.log"), () -> 0).lines();
        assertEquals(YrrpAdBlockEventLog.CAPACITY, lines.size());
        assertEquals("10 e10", lines.get(0));
        assertEquals("59 e59", lines.get(49));
    }

    @Test
    public void lineBreaksInEventsCannotSplitRecords() {
        mLog.record("a\nb");
        assertEquals(List.of("1000 a b"), mLog.lines());
    }
}
