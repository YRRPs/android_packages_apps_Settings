/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.junit.Test;

/** Plain JUnit; no Robolectric. */
public class YrrpHotspotDumpTest {
    private static YrrpHotspotDump.Snapshot ready() {
        final YrrpHotspotDump.Snapshot s = new YrrpHotspotDump.Snapshot();
        s.apEnabled = true;
        s.capabilityKnown = true;
        s.forceDisconnectSupported = true;
        s.limit = new YrrpHotspotLimit(32, 2, 0, true);
        s.connected = 2;
        s.withDetails = 1;
        s.blocked = 1;
        return s;
    }

    private static String format(YrrpHotspotDump.Snapshot s) {
        final StringWriter out = new StringWriter();
        YrrpHotspotDump.format(new PrintWriter(out), s);
        return out.toString();
    }

    @Test
    public void listsEveryGateAndValue() {
        final String dump = format(ready());
        for (String field : List.of("apEnabled=true", "capabilityKnown=true",
                "forceDisconnectSupported=true", "connected=2", "withDetails=1", "blocked=1",
                "maxSupported=32", "userMax=2", "carrierMax=0", "effectiveMax=2",
                "limitSource=USER", "blockAllowed=true", "blockBlockedBy=none",
                "limitAllowed=true", "limitBlockedBy=none", "events:")) {
            assertTrue(field + " missing from\n" + dump, dump.contains(field));
        }
    }

    @Test
    public void blockGateNamesFirstFalseInput() {
        final YrrpHotspotDump.Snapshot s = ready();
        s.forceDisconnectSupported = false;
        s.apEnabled = false;
        assertEquals("apEnabled", YrrpHotspotDump.blockBlockedBy(s));
        s.apEnabled = true;
        assertEquals("forceDisconnectSupported", YrrpHotspotDump.blockBlockedBy(s));
        assertTrue(format(s).contains("blockAllowed=false blockBlockedBy=forceDisconnectSupported"));
    }

    @Test
    public void limitGateWaitsForCapability() {
        final YrrpHotspotDump.Snapshot s = ready();
        s.capabilityKnown = false;
        assertEquals("capabilityKnown", YrrpHotspotDump.limitBlockedBy(s));
        assertEquals("capabilityKnown", YrrpHotspotDump.blockBlockedBy(s));
    }

    @Test
    public void limitGateNeedsTwoSliderPositions() {
        final YrrpHotspotDump.Snapshot s = ready();
        s.limit = new YrrpHotspotLimit(1, 0, 0, true);
        assertEquals("maxSupported", YrrpHotspotDump.limitBlockedBy(s));
        assertFalse(YrrpHotspotDump.limitAllowed(s));
    }

    @Test
    public void limitGateIgnoresHotspotState() {
        final YrrpHotspotDump.Snapshot s = ready();
        s.apEnabled = false;
        assertTrue(YrrpHotspotDump.limitAllowed(s));
    }

    @Test
    public void printsEventsAfterState() {
        final YrrpHotspotDump.Snapshot s = ready();
        s.events = List.of("1 apEnabled=true");
        assertTrue(format(s).endsWith("events:\n  1 apEnabled=true\n"));
    }
}
