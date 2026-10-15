/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static com.google.common.truth.Truth.assertThat;

import com.android.settings.yrrp.YrrpFeatureAvailability.HotspotControls;
import com.android.settings.yrrp.YrrpFeatureAvailability.Probe;

import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

public class YrrpFeatureInventoryStatusServiceTest {
    @Test
    public void coldResolver_populatesEveryFeatureWithoutOpeningPage() {
        final YrrpFeatureInventoryDump dump =
                new YrrpFeatureInventoryDump(new YrrpFeatureInventoryEventLog(() -> 1L));

        YrrpFeatureInventoryStatusService.resolveForDump(new FakeEnvironment(), dump);

        final StringWriter output = new StringWriter();
        dump.format(new PrintWriter(output));
        final String text = output.toString();
        assertThat(text).contains(
                "feature=yrrp_feature_pulse available=true"
                        + " reasonCode=available blockedBy=none probeStatus=known");
        assertThat(text).contains(
                "feature=yrrp_feature_fold_animations available=false"
                        + " reasonCode=fold_sensor_missing"
                        + " blockedBy=hingeSensorPresent probeStatus=known");
        assertThat(text).contains("gate=hingeSensorPresent value=no");
        assertThat(text).doesNotContain("probeStatus=pending");
    }

    private static final class FakeEnvironment implements YrrpFeatureEnvironment {
        @Override
        public YrrpFoldAnimationAvailability.Snapshot fold() {
            return new YrrpFoldAnimationAvailability.Snapshot(
                    Probe.NO, Probe.NO, Probe.NO);
        }

        @Override
        public Probe primaryUser() {
            return Probe.YES;
        }

        @Override
        public Probe salamiDevice() {
            return Probe.YES;
        }

        @Override
        public Probe cameraPackage() {
            return Probe.YES;
        }

        @Override
        public Probe galleryPackage() {
            return Probe.YES;
        }

        @Override
        public Probe gappsBuild() {
            return Probe.YES;
        }

        @Override
        public HotspotControls hotspotControls() {
            return HotspotControls.UNDETERMINED;
        }
    }
}
