/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

/**
 * The hotspot client limit as the Wi-Fi module enforces it: the capability cap (hardware, lowered
 * by carrier config) and, when it is lower, the user's stored value. A stored 0 means no user
 * limit. The slider's top position is "No limit" and stores 0, so the device maximum keeps
 * following the capability if it changes later.
 */
final class YrrpHotspotLimit {
    /** Which input decides the effective limit. */
    enum Source { DEVICE, CARRIER, USER }

    private final int mMaxSupported;
    private final int mUserMax;
    private final int mCarrierMax;
    private final boolean mForceDisconnect;

    /**
     * @param maxSupported {@code SoftApCapability.getMaxSupportedClients()}
     * @param userMax {@code SoftApConfiguration.getMaxNumberOfClients()}, 0 for none
     * @param carrierMax carrier {@code KEY_HOTSPOT_MAX_CLIENT_COUNT}, 0 for unset
     * @param forceDisconnect whether the capability has {@code CLIENT_FORCE_DISCONNECT}
     */
    YrrpHotspotLimit(int maxSupported, int userMax, int carrierMax, boolean forceDisconnect) {
        mMaxSupported = maxSupported;
        mUserMax = userMax;
        mCarrierMax = carrierMax;
        mForceDisconnect = forceDisconnect;
    }

    int maxSupported() {
        return mMaxSupported;
    }

    int userMax() {
        return mUserMax;
    }

    int carrierMax() {
        return mCarrierMax;
    }

    int effectiveMax() {
        return userBinds() ? mUserMax : mMaxSupported;
    }

    Source source() {
        if (userBinds()) {
            return Source.USER;
        }
        return mCarrierMax > 0 && mCarrierMax == mMaxSupported ? Source.CARRIER : Source.DEVICE;
    }

    /** The thumb position; the top position stands for "No limit". */
    int sliderPosition() {
        return effectiveMax();
    }

    /** The value to store for a slider position: 0 at or past the top, otherwise at least 1. */
    int storedValueFor(int position) {
        if (position >= mMaxSupported) {
            return 0;
        }
        return Math.max(1, position);
    }

    /**
     * The module rejects a non-zero limit without force-disconnect support, and a slider needs at
     * least two positions.
     */
    boolean controlAvailable() {
        return mForceDisconnect && mMaxSupported > 1;
    }

    private boolean userBinds() {
        return mUserMax > 0 && mUserMax < mMaxSupported;
    }
}
