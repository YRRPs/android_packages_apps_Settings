/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static com.google.common.truth.Truth.assertThat;

import com.android.settings.R;
import com.android.settings.yrrp.YrrpFeatureAvailability.ProbeStatus;
import com.android.settings.yrrp.YrrpFeatureCatalog.Id;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class YrrpFeatureInventoryEventLogTest {
    @Test
    public void record_keepsNewestFiftyOldestFirst() {
        final AtomicLong clock = new AtomicLong();
        final YrrpFeatureInventoryEventLog log =
                new YrrpFeatureInventoryEventLog(clock::getAndIncrement);

        for (int i = 0; i < 51; i++) {
            log.record(Id.PULSE, available());
        }

        assertThat(log.lines()).hasSize(50);
        assertThat(log.lines().get(0)).startsWith("1 feature=yrrp_feature_pulse");
        assertThat(log.lines().get(49)).startsWith("50 feature=yrrp_feature_pulse");
    }

    @Test
    public void record_flattensNewlinesAndRejectsUnboundedFields() {
        final YrrpFeatureInventoryEventLog log = new YrrpFeatureInventoryEventLog(() -> 7L);
        final YrrpFeatureAvailability malicious = new YrrpFeatureAvailability(
                false,
                R.string.yrrp_feature_status_unknown,
                "private\nreason",
                "AA:BB:CC:DD:EE:FF\rhost",
                ProbeStatus.FAILED);

        log.record(Id.ONEPLUS_CAMERA, malicious);

        assertThat(log.lines()).containsExactly(
                "7 feature=yrrp_feature_oneplus_camera available=false"
                        + " reasonCode=invalid blockedBy=invalid probeStatus=failed");
    }

    @Test
    public void lines_returnsCopy() {
        final YrrpFeatureInventoryEventLog log = new YrrpFeatureInventoryEventLog(() -> 1L);
        log.record(Id.PULSE, available());
        final List<String> copy = log.lines();

        copy.clear();

        assertThat(log.lines()).hasSize(1);
    }

    private static YrrpFeatureAvailability available() {
        return new YrrpFeatureAvailability(
                true,
                R.string.yrrp_feature_status_available,
                "available",
                "none",
                ProbeStatus.KNOWN);
    }
}
