/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import java.io.PrintWriter;
import java.util.List;

/**
 * The hotspot client controls' gates and inputs, printed by {@code WifiTetherSettings.dump}. Read
 * with {@code adb shell dumpsys activity top} while the Wi-Fi hotspot page is open. Prints counts
 * and booleans only: client MACs, hostnames, and addresses belong to other people's devices.
 */
final class YrrpHotspotDump {
    private static final String NONE = "none";

    /** Everything the dump prints, gathered by the caller. */
    static final class Snapshot {
        boolean apEnabled;
        boolean capabilityKnown;
        boolean forceDisconnectSupported;
        YrrpHotspotLimit limit = new YrrpHotspotLimit(0, 0, 0, false);
        int connected;
        /** Connected clients for which tethering reported a hostname or IPv4 address. */
        int withDetails;
        int blocked;
        List<String> events = List.of();
    }

    private YrrpHotspotDump() {}

    /** Block needs a running hotspot whose capability supports force-disconnect. */
    static String blockBlockedBy(Snapshot s) {
        if (!s.capabilityKnown) {
            return "capabilityKnown";
        }
        if (!s.apEnabled) {
            return "apEnabled";
        }
        return s.forceDisconnectSupported ? NONE : "forceDisconnectSupported";
    }

    /** The limit slider works while the hotspot is off, because the limit is stored config. */
    static String limitBlockedBy(Snapshot s) {
        if (!s.capabilityKnown) {
            return "capabilityKnown";
        }
        if (!s.forceDisconnectSupported) {
            return "forceDisconnectSupported";
        }
        return s.limit.controlAvailable() ? NONE : "maxSupported";
    }

    static boolean blockAllowed(Snapshot s) {
        return NONE.equals(blockBlockedBy(s));
    }

    static boolean limitAllowed(Snapshot s) {
        return NONE.equals(limitBlockedBy(s));
    }

    static void format(PrintWriter pw, Snapshot s) {
        pw.println("YrrpHotspot:");
        pw.println("apEnabled=" + s.apEnabled + " capabilityKnown=" + s.capabilityKnown
                + " forceDisconnectSupported=" + s.forceDisconnectSupported);
        pw.println("connected=" + s.connected + " withDetails=" + s.withDetails
                + " blocked=" + s.blocked);
        pw.println("maxSupported=" + s.limit.maxSupported() + " userMax=" + s.limit.userMax()
                + " carrierMax=" + s.limit.carrierMax() + " effectiveMax=" + s.limit.effectiveMax()
                + " limitSource=" + s.limit.source());
        pw.println("blockAllowed=" + blockAllowed(s) + " blockBlockedBy=" + blockBlockedBy(s));
        pw.println("limitAllowed=" + limitAllowed(s) + " limitBlockedBy=" + limitBlockedBy(s));
        pw.println("events:");
        for (String event : s.events) {
            pw.println("  " + event);
        }
        pw.flush();
    }
}
