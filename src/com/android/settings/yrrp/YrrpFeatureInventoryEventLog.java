/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import com.android.settings.yrrp.YrrpFeatureCatalog.Id;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

/** Process-lifetime log of the newest 50 bounded feature availability transitions. */
final class YrrpFeatureInventoryEventLog {
    static final int CAPACITY = 50;
    static final YrrpFeatureInventoryEventLog INSTANCE =
            new YrrpFeatureInventoryEventLog(System::currentTimeMillis);

    private final ArrayDeque<String> mLines = new ArrayDeque<>();
    private final LongSupplier mClock;

    YrrpFeatureInventoryEventLog(LongSupplier clock) {
        mClock = clock;
    }

    synchronized void record(Id id, YrrpFeatureAvailability availability) {
        mLines.addLast(mClock.getAsLong()
                + " feature=" + id.key
                + " available=" + availability.available
                + " reasonCode=" + availability.diagnosticReasonCode()
                + " blockedBy=" + availability.diagnosticBlockedBy()
                + " probeStatus=" + availability.diagnosticProbeStatus());
        while (mLines.size() > CAPACITY) {
            mLines.removeFirst();
        }
    }

    /** Oldest first, copied so callers cannot mutate process state. */
    synchronized List<String> lines() {
        return new ArrayList<>(mLines);
    }
}
