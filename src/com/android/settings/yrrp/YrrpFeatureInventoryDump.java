/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import com.android.settings.yrrp.YrrpFeatureAvailability.HotspotControls;
import com.android.settings.yrrp.YrrpFeatureCatalog.Id;

import java.io.PrintWriter;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** Process-wide, privacy-bounded current inventory state for dumpsys. */
final class YrrpFeatureInventoryDump {
    static final YrrpFeatureInventoryDump INSTANCE =
            new YrrpFeatureInventoryDump(YrrpFeatureInventoryEventLog.INSTANCE);

    private final YrrpFeatureInventoryEventLog mLog;
    private final Map<Id, YrrpFeatureAvailability> mResults = new EnumMap<>(Id.class);

    private boolean mStarted;
    private long mResolutionEpoch;
    private HotspotControls mHotspotControls = HotspotControls.UNDETERMINED;
    private boolean mCallbackRegistered;

    YrrpFeatureInventoryDump(YrrpFeatureInventoryEventLog log) {
        mLog = log;
    }

    synchronized void update(
            Map<Id, YrrpFeatureAvailability> results,
            boolean started,
            long resolutionEpoch,
            HotspotControls hotspotControls,
            boolean callbackRegistered) {
        mResults.clear();
        mResults.putAll(results);
        mStarted = started;
        mResolutionEpoch = resolutionEpoch;
        mHotspotControls = hotspotControls;
        mCallbackRegistered = callbackRegistered;
    }

    synchronized long resolutionEpoch() {
        return mResolutionEpoch;
    }

    void recordTransition(Id id, YrrpFeatureAvailability availability) {
        mLog.record(id, availability);
    }

    synchronized void format(PrintWriter writer) {
        writer.println("started=" + mStarted
                + " resolutionEpoch=" + mResolutionEpoch
                + " hotspotCapabilityKnown="
                + (mHotspotControls != HotspotControls.UNDETERMINED)
                + " hotspotControls=" + mHotspotControls.name().toLowerCase(Locale.ROOT)
                + " callbackRegistered=" + mCallbackRegistered);
        for (Id id : YrrpFeatureCatalog.ids()) {
            final YrrpFeatureAvailability availability = mResults.get(id);
            if (availability == null) {
                writer.println("feature=" + id.key
                        + " available=false reasonCode=probe_unknown"
                        + " blockedBy=none probeStatus=pending");
            } else {
                writer.println("feature=" + id.key
                        + " " + availability.diagnosticFields());
            }
        }
        writer.println("events:");
        for (String event : mLog.lines()) {
            writer.println("  " + event);
        }
        writer.flush();
    }
}
