/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

/** Immutable availability decision for one YRRP feature family. */
final class YrrpFeatureAvailability {
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
}
