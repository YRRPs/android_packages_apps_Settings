/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static com.google.common.truth.Truth.assertThat;

import com.android.settings.R;
import com.android.settings.yrrp.YrrpFeatureAvailability.HotspotControls;
import com.android.settings.yrrp.YrrpFeatureAvailability.ProbeStatus;
import com.android.settings.yrrp.YrrpFeatureCatalog.Id;

import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EnumMap;
import java.util.Map;

public class YrrpFeatureInventoryDumpTest {
    @Test
    public void format_printsEveryResultAndLifecycleField() {
        final YrrpFeatureInventoryEventLog log = new YrrpFeatureInventoryEventLog(() -> 9L);
        final YrrpFeatureInventoryDump dump = new YrrpFeatureInventoryDump(log);
        final Map<Id, YrrpFeatureAvailability> results = allAvailable();
        results.put(Id.FOLD_ANIMATIONS, new YrrpFeatureAvailability(
                false,
                R.string.yrrp_feature_status_fold_sensor,
                "fold_sensor_missing",
                "hingeSensorPresent",
                ProbeStatus.KNOWN));
        log.record(Id.FOLD_ANIMATIONS, results.get(Id.FOLD_ANIMATIONS));
        dump.update(results, true, 3L, HotspotControls.UNDETERMINED, true);

        final String text = format(dump);

        for (Id id : YrrpFeatureCatalog.ids()) {
            assertThat(text).contains("feature=" + id.key);
        }
        assertThat(text).contains(
                "feature=yrrp_feature_fold_animations available=false"
                        + " reasonCode=fold_sensor_missing"
                        + " blockedBy=hingeSensorPresent probeStatus=known");
        assertThat(text).contains("started=true");
        assertThat(text).contains("resolutionEpoch=3");
        assertThat(text).contains("hotspotCapabilityKnown=false");
        assertThat(text).contains("callbackRegistered=true");
        assertThat(text).contains("events:");
    }

    @Test
    public void format_excludesUnboundedDiagnosticValues() {
        final YrrpFeatureInventoryDump dump =
                new YrrpFeatureInventoryDump(new YrrpFeatureInventoryEventLog(() -> 1L));
        final Map<Id, YrrpFeatureAvailability> results = allAvailable();
        results.put(Id.ONEPLUS_CAMERA, new YrrpFeatureAvailability(
                false,
                R.string.yrrp_feature_status_unknown,
                "com.oplus.camera\nro.yrrp.build.type=gapps",
                "AA:BB:CC:DD:EE:FF 192.0.2.1 private-host",
                ProbeStatus.FAILED));
        dump.update(results, false, 1L, HotspotControls.SUPPORTED, false);

        final String text = format(dump);

        assertThat(text).contains(
                "feature=yrrp_feature_oneplus_camera available=false"
                        + " reasonCode=invalid blockedBy=invalid probeStatus=failed");
        assertThat(text).doesNotContain("com.oplus.camera");
        assertThat(text).doesNotContain("ro.yrrp.build.type");
        assertThat(text).doesNotContain("AA:BB:CC:DD:EE:FF");
        assertThat(text).doesNotContain("192.0.2.1");
        assertThat(text).doesNotContain("private-host");
    }

    private static Map<Id, YrrpFeatureAvailability> allAvailable() {
        final Map<Id, YrrpFeatureAvailability> results = new EnumMap<>(Id.class);
        for (Id id : YrrpFeatureCatalog.ids()) {
            results.put(id, new YrrpFeatureAvailability(
                    true,
                    R.string.yrrp_feature_status_available,
                    "available",
                    "none",
                    ProbeStatus.KNOWN));
        }
        return results;
    }

    private static String format(YrrpFeatureInventoryDump dump) {
        final StringWriter output = new StringWriter();
        dump.format(new PrintWriter(output));
        return output.toString();
    }
}
