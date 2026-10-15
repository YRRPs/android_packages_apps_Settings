/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.Locale;
import java.util.Set;

/** Immutable availability decision for one YRRP feature family. */
final class YrrpFeatureAvailability {
    private static final Set<String> REASON_CODES = Set.of(
            "available",
            "fold_framework_disabled",
            "fold_hinge_config_disabled",
            "fold_sensor_missing",
            "primary_user_only",
            "hotspot_controls_unavailable",
            "hotspot_controls_unknown",
            "salami_only",
            "camera_missing",
            "gallery_wrong_device",
            "gallery_missing",
            "vanilla_build",
            "probe_unknown");
    private static final Set<String> GATE_NAMES = Set.of(
            "none",
            "unfoldEnabled",
            "hingeAngleEnabled",
            "hingeSensorPresent",
            "primaryUser",
            "forceDisconnectSupported",
            "capabilityKnown",
            "salamiDevice",
            "cameraPackage",
            "galleryPackage",
            "gappsBuild");

    enum Probe { YES, NO, UNKNOWN }
    enum HotspotControls { SUPPORTED, UNSUPPORTED, UNDETERMINED }
    enum ProbeStatus { KNOWN, FAILED, PENDING }

    final boolean available;
    @StringRes final int statusRes;
    @NonNull final String reasonCode;
    @NonNull final String blockedBy;
    @NonNull final ProbeStatus probeStatus;

    YrrpFeatureAvailability(
            boolean available,
            @StringRes int statusRes,
            @NonNull String reasonCode,
            @NonNull String blockedBy,
            @NonNull ProbeStatus probeStatus) {
        this.available = available;
        this.statusRes = statusRes;
        this.reasonCode = reasonCode;
        this.blockedBy = blockedBy;
        this.probeStatus = probeStatus;
    }

    String diagnosticReasonCode() {
        return REASON_CODES.contains(reasonCode) ? reasonCode : "invalid";
    }

    String diagnosticBlockedBy() {
        return GATE_NAMES.contains(blockedBy) ? blockedBy : "invalid";
    }

    String diagnosticProbeStatus() {
        return probeStatus.name().toLowerCase(Locale.ROOT);
    }
}
