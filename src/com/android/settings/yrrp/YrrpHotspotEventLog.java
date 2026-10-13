/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * In-memory transitions of the hotspot client controls, printed by the hotspot page's dump. It
 * lives as long as the Settings process, so it outlasts logcat rotation while the page is reused.
 * Callers pass counts, booleans, and enum names only, never a MAC, hostname, or address.
 */
final class YrrpHotspotEventLog {
    static final int CAPACITY = 50;

    /** The process-wide log the hotspot page writes to. */
    static final YrrpHotspotEventLog INSTANCE = new YrrpHotspotEventLog(System::currentTimeMillis);

    private final ArrayDeque<String> mLines = new ArrayDeque<>();
    private final LongSupplier mClock;

    YrrpHotspotEventLog(LongSupplier clock) {
        mClock = clock;
    }

    synchronized void record(String event) {
        mLines.addLast(mClock.getAsLong() + " " + event.replaceAll("[\\r\\n]+", " "));
        while (mLines.size() > CAPACITY) {
            mLines.removeFirst();
        }
    }

    /** Oldest first, copied. */
    synchronized List<String> lines() {
        return new ArrayList<>(mLines);
    }
}
