/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import androidx.annotation.Nullable;

import com.android.settings.yrrp.YrrpFeatureAvailability.Probe;
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

    @Nullable private YrrpFeatureEnvironment.Snapshot mGates;
    private boolean mStarted;
    private long mResolutionEpoch;
    private boolean mCallbackRegistered;

    YrrpFeatureInventoryDump(YrrpFeatureInventoryEventLog log) {
        mLog = log;
    }

    synchronized void update(
            Map<Id, YrrpFeatureAvailability> results,
            YrrpFeatureEnvironment.Snapshot gates,
            boolean started,
            boolean callbackRegistered) {
        boolean changed = false;
        for (Id id : YrrpFeatureCatalog.ids()) {
            final YrrpFeatureAvailability next = results.get(id);
            if (next != null && !sameDecision(mResults.get(id), next)) {
                mLog.record(id, next);
                changed = true;
            }
        }
        if (changed) {
            mResolutionEpoch++;
        }
        mResults.clear();
        mResults.putAll(results);
        mGates = gates;
        mStarted = started;
        mCallbackRegistered = callbackRegistered;
    }

    synchronized long resolutionEpoch() {
        return mResolutionEpoch;
    }

    synchronized void format(PrintWriter writer) {
        final YrrpFeatureEnvironment.Snapshot gates = mGates;
        writer.println("started=" + mStarted
                + " resolutionEpoch=" + mResolutionEpoch
                + " hotspotCapabilityKnown="
                + (gates != null
                        && gates.hotspotControls()
                                != YrrpFeatureAvailability.HotspotControls.UNDETERMINED)
                + " callbackRegistered=" + mCallbackRegistered);
        printGates(writer, gates);
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

    private static void printGates(
            PrintWriter writer, @Nullable YrrpFeatureEnvironment.Snapshot gates) {
        if (gates == null) {
            for (String name : new String[] {
                    "unfoldEnabled",
                    "hingeAngleEnabled",
                    "hingeSensorPresent",
                    "primaryUser",
                    "salamiDevice",
                    "cameraPackage",
                    "galleryPackage",
                    "gappsBuild",
                    "hotspotControls",
            }) {
                writer.println("gate=" + name + " value=unknown");
            }
            return;
        }
        writer.println(gate("unfoldEnabled", gates.fold().unfoldEnabled));
        writer.println(gate("hingeAngleEnabled", gates.fold().hingeAngleEnabled));
        writer.println(gate("hingeSensorPresent", gates.fold().hingeSensorPresent));
        writer.println(gate("primaryUser", gates.primaryUser()));
        writer.println(gate("salamiDevice", gates.salamiDevice()));
        writer.println(gate("cameraPackage", gates.cameraPackage()));
        writer.println(gate("galleryPackage", gates.galleryPackage()));
        writer.println(gate("gappsBuild", gates.gappsBuild()));
        writer.println("gate=hotspotControls value="
                + gates.hotspotControls().name().toLowerCase(Locale.ROOT));
    }

    private static String gate(String name, Probe value) {
        return "gate=" + name + " value=" + value.name().toLowerCase(Locale.ROOT);
    }

    private static boolean sameDecision(
            @Nullable YrrpFeatureAvailability left,
            YrrpFeatureAvailability right) {
        return left != null
                && left.statusRes == right.statusRes
                && left.diagnosticFields().equals(right.diagnosticFields());
    }
}
