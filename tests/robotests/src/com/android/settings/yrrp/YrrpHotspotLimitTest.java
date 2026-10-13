/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.android.settings.yrrp.YrrpHotspotLimit.Source;

import org.junit.Test;

/** Plain JUnit; no Robolectric. */
public class YrrpHotspotLimitTest {
    private static YrrpHotspotLimit limit(int cap, int user, int carrier) {
        return new YrrpHotspotLimit(cap, user, carrier, /* forceDisconnect= */ true);
    }

    @Test
    public void userValueBelowCapIsEffectiveAndAttributedToUser() {
        final YrrpHotspotLimit l = limit(32, 2, 0);
        assertEquals(2, l.effectiveMax());
        assertEquals(Source.USER, l.source());
        assertEquals(2, l.sliderPosition());
    }

    @Test
    public void zeroUserValueMeansDeviceMaximum() {
        final YrrpHotspotLimit l = limit(32, 0, 0);
        assertEquals(32, l.effectiveMax());
        assertEquals(Source.DEVICE, l.source());
        assertEquals(32, l.sliderPosition());
    }

    @Test
    public void userValueAtOrAboveCapDoesNotBind() {
        final YrrpHotspotLimit l = limit(10, 16, 0);
        assertEquals(10, l.effectiveMax());
        assertEquals(Source.DEVICE, l.source());
        assertEquals(10, l.sliderPosition());
    }

    @Test
    public void carrierValueEqualToCapIsAttributedToCarrier() {
        assertEquals(Source.CARRIER, limit(5, 0, 5).source());
        assertEquals(Source.DEVICE, limit(32, 0, 40).source());
    }

    @Test
    public void userBeatsCarrierWhenLower() {
        assertEquals(Source.USER, limit(5, 3, 5).source());
    }

    @Test
    public void topSliderPositionStoresZero() {
        final YrrpHotspotLimit l = limit(32, 2, 0);
        assertEquals(0, l.storedValueFor(32));
        assertEquals(0, l.storedValueFor(40));
        assertEquals(7, l.storedValueFor(7));
        assertEquals(1, l.storedValueFor(0));
    }

    @Test
    public void controlNeedsForceDisconnectAndRoomForAChoice() {
        assertTrue(limit(32, 0, 0).controlAvailable());
        assertFalse(new YrrpHotspotLimit(32, 0, 0, false).controlAvailable());
        assertFalse(limit(1, 0, 0).controlAvailable());
        assertFalse(limit(0, 0, 0).controlAvailable());
    }
}
