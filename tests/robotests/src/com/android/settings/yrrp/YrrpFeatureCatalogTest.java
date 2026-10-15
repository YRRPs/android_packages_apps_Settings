/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static com.google.common.truth.Truth.assertThat;

import com.android.settings.R;
import com.android.settings.yrrp.YrrpFeatureAvailability.HotspotControls;
import com.android.settings.yrrp.YrrpFeatureAvailability.Probe;
import com.android.settings.yrrp.YrrpFeatureCatalog.Group;
import com.android.settings.yrrp.YrrpFeatureCatalog.Id;

import org.junit.Before;
import org.junit.Test;

/** Pure availability policy for every shipped YRRP feature family. */
public class YrrpFeatureCatalogTest {
    private FakeEnvironment mEnvironment;

    @Before
    public void setUp() {
        mEnvironment = new FakeEnvironment();
    }

    @Test
    public void ids_haveStableUniqueOrder() {
        assertThat(YrrpFeatureCatalog.ids()).containsExactly(
                Id.PULSE,
                Id.SCREEN_OFF,
                Id.MATRIX_BOOT_SHUTDOWN,
                Id.FOLD_ANIMATIONS,
                Id.FULL_THEMING,
                Id.AD_BLOCKING,
                Id.HOTSPOT_CLIENT_MANAGEMENT,
                Id.GAPPS,
                Id.INDOOR_AUTO_BRIGHTNESS,
                Id.ONEPLUS_CAMERA,
                Id.ONEPLUS_GALLERY).inOrder();

        assertThat(YrrpFeatureCatalog.ids().stream().map(id -> id.key).distinct().count())
                .isEqualTo(11);
    }

    @Test
    public void groups_matchApprovedPageOrder() {
        assertThat(YrrpFeatureCatalog.ids(Group.CUSTOMIZATION)).containsExactly(
                Id.PULSE,
                Id.SCREEN_OFF,
                Id.MATRIX_BOOT_SHUTDOWN,
                Id.FOLD_ANIMATIONS,
                Id.FULL_THEMING).inOrder();
        assertThat(YrrpFeatureCatalog.ids(Group.SYSTEM_SERVICES)).containsExactly(
                Id.AD_BLOCKING,
                Id.HOTSPOT_CLIENT_MANAGEMENT,
                Id.GAPPS).inOrder();
        assertThat(YrrpFeatureCatalog.ids(Group.DEVICE_INTEGRATIONS)).containsExactly(
                Id.INDOOR_AUTO_BRIGHTNESS,
                Id.ONEPLUS_CAMERA,
                Id.ONEPLUS_GALLERY).inOrder();
    }

    @Test
    public void universalFeatures_ignoreUnknownEnvironment() {
        mEnvironment.setEveryProbe(Probe.UNKNOWN);

        for (Id id : new Id[] {
                Id.PULSE,
                Id.SCREEN_OFF,
                Id.MATRIX_BOOT_SHUTDOWN,
                Id.FULL_THEMING,
        }) {
            assertResult(id, true, R.string.yrrp_feature_status_available, "available", "none");
        }
    }

    @Test
    public void foldReportsFirstFalseGate() {
        mEnvironment.fold = fold(Probe.NO, Probe.NO, Probe.NO);
        assertResult(Id.FOLD_ANIMATIONS, false,
                R.string.yrrp_feature_status_fold_framework,
                "fold_framework_disabled", "unfoldEnabled");

        mEnvironment.fold = fold(Probe.YES, Probe.NO, Probe.NO);
        assertResult(Id.FOLD_ANIMATIONS, false,
                R.string.yrrp_feature_status_fold_hinge_config,
                "fold_hinge_config_disabled", "hingeAngleEnabled");

        mEnvironment.fold = fold(Probe.YES, Probe.YES, Probe.NO);
        assertResult(Id.FOLD_ANIMATIONS, false,
                R.string.yrrp_feature_status_fold_sensor,
                "fold_sensor_missing", "hingeSensorPresent");

        mEnvironment.fold = fold(Probe.YES, Probe.YES, Probe.YES);
        assertResult(Id.FOLD_ANIMATIONS, true,
                R.string.yrrp_feature_status_available, "available", "none");
    }

    @Test
    public void foldUnknownNamesTheProbeThatFailed() {
        mEnvironment.fold = fold(Probe.UNKNOWN, Probe.YES, Probe.YES);
        assertUnknown(Id.FOLD_ANIMATIONS, "unfoldEnabled");

        mEnvironment.fold = fold(Probe.YES, Probe.UNKNOWN, Probe.YES);
        assertUnknown(Id.FOLD_ANIMATIONS, "hingeAngleEnabled");

        mEnvironment.fold = fold(Probe.YES, Probe.YES, Probe.UNKNOWN);
        assertUnknown(Id.FOLD_ANIMATIONS, "hingeSensorPresent");
    }

    @Test
    public void adBlockingRequiresPrimaryUser() {
        mEnvironment.primaryUser = Probe.NO;
        assertResult(Id.AD_BLOCKING, false,
                R.string.yrrp_feature_status_primary_user,
                "primary_user_only", "primaryUser");

        mEnvironment.primaryUser = Probe.UNKNOWN;
        assertUnknown(Id.AD_BLOCKING, "primaryUser");

        mEnvironment.primaryUser = Probe.YES;
        assertAvailable(Id.AD_BLOCKING);
    }

    @Test
    public void hotspotDetailsRemainAvailableForEveryControlState() {
        mEnvironment.hotspotControls = HotspotControls.SUPPORTED;
        assertAvailable(Id.HOTSPOT_CLIENT_MANAGEMENT);

        mEnvironment.hotspotControls = HotspotControls.UNSUPPORTED;
        assertResult(Id.HOTSPOT_CLIENT_MANAGEMENT, true,
                R.string.yrrp_feature_status_hotspot_details_only,
                "hotspot_controls_unavailable", "forceDisconnectSupported");

        mEnvironment.hotspotControls = HotspotControls.UNDETERMINED;
        assertResult(Id.HOTSPOT_CLIENT_MANAGEMENT, true,
                R.string.yrrp_feature_status_hotspot_unknown,
                "hotspot_controls_unknown", "capabilityKnown");
    }

    @Test
    public void autoBrightnessRequiresSalami() {
        mEnvironment.salamiDevice = Probe.NO;
        assertResult(Id.INDOOR_AUTO_BRIGHTNESS, false,
                R.string.yrrp_feature_status_salami_only,
                "salami_only", "salamiDevice");

        mEnvironment.salamiDevice = Probe.UNKNOWN;
        assertUnknown(Id.INDOOR_AUTO_BRIGHTNESS, "salamiDevice");

        mEnvironment.salamiDevice = Probe.YES;
        assertAvailable(Id.INDOOR_AUTO_BRIGHTNESS);
    }

    @Test
    public void cameraRequiresIntegrationPackage() {
        mEnvironment.cameraPackage = Probe.NO;
        assertResult(Id.ONEPLUS_CAMERA, false,
                R.string.yrrp_feature_status_camera_missing,
                "camera_missing", "cameraPackage");

        mEnvironment.cameraPackage = Probe.UNKNOWN;
        assertUnknown(Id.ONEPLUS_CAMERA, "cameraPackage");

        mEnvironment.cameraPackage = Probe.YES;
        assertAvailable(Id.ONEPLUS_CAMERA);
    }

    @Test
    public void galleryChecksProductBeforePackage() {
        mEnvironment.salamiDevice = Probe.NO;
        mEnvironment.galleryPackage = Probe.YES;
        assertResult(Id.ONEPLUS_GALLERY, false,
                R.string.yrrp_feature_status_gallery_device,
                "gallery_wrong_device", "salamiDevice");

        mEnvironment.salamiDevice = Probe.YES;
        mEnvironment.galleryPackage = Probe.NO;
        assertResult(Id.ONEPLUS_GALLERY, false,
                R.string.yrrp_feature_status_gallery_missing,
                "gallery_missing", "galleryPackage");

        mEnvironment.galleryPackage = Probe.UNKNOWN;
        assertUnknown(Id.ONEPLUS_GALLERY, "galleryPackage");

        mEnvironment.galleryPackage = Probe.YES;
        assertAvailable(Id.ONEPLUS_GALLERY);
    }

    @Test
    public void galleryUnknownProductDoesNotProbePackageResult() {
        mEnvironment.salamiDevice = Probe.UNKNOWN;
        mEnvironment.galleryPackage = Probe.NO;

        assertUnknown(Id.ONEPLUS_GALLERY, "salamiDevice");
    }

    @Test
    public void gappsReflectsBuildFlavor() {
        mEnvironment.gappsBuild = Probe.NO;
        assertResult(Id.GAPPS, false,
                R.string.yrrp_feature_status_vanilla,
                "vanilla_build", "gappsBuild");

        mEnvironment.gappsBuild = Probe.UNKNOWN;
        assertUnknown(Id.GAPPS, "gappsBuild");

        mEnvironment.gappsBuild = Probe.YES;
        assertAvailable(Id.GAPPS);
    }

    private void assertAvailable(Id id) {
        assertResult(id, true, R.string.yrrp_feature_status_available, "available", "none");
    }

    private void assertUnknown(Id id, String blockedBy) {
        assertResult(id, false, R.string.yrrp_feature_status_unknown,
                "probe_unknown", blockedBy);
    }

    private void assertResult(
            Id id, boolean available, int statusRes, String reasonCode, String blockedBy) {
        final YrrpFeatureAvailability result = YrrpFeatureCatalog.resolve(id, mEnvironment);
        assertThat(result.available).isEqualTo(available);
        assertThat(result.statusRes).isEqualTo(statusRes);
        assertThat(result.reasonCode).isEqualTo(reasonCode);
        assertThat(result.blockedBy).isEqualTo(blockedBy);
    }

    private static YrrpFoldAnimationAvailability.Snapshot fold(
            Probe unfold, Probe hingeConfig, Probe sensor) {
        return new YrrpFoldAnimationAvailability.Snapshot(unfold, hingeConfig, sensor);
    }

    private static final class FakeEnvironment implements YrrpFeatureEnvironment {
        YrrpFoldAnimationAvailability.Snapshot fold = fold(Probe.YES, Probe.YES, Probe.YES);
        Probe primaryUser = Probe.YES;
        Probe salamiDevice = Probe.YES;
        Probe cameraPackage = Probe.YES;
        Probe galleryPackage = Probe.YES;
        Probe gappsBuild = Probe.YES;
        HotspotControls hotspotControls = HotspotControls.SUPPORTED;

        void setEveryProbe(Probe value) {
            fold = fold(value, value, value);
            primaryUser = value;
            salamiDevice = value;
            cameraPackage = value;
            galleryPackage = value;
            gappsBuild = value;
            hotspotControls = HotspotControls.UNDETERMINED;
        }

        @Override
        public YrrpFoldAnimationAvailability.Snapshot fold() {
            return fold;
        }

        @Override
        public Probe primaryUser() {
            return primaryUser;
        }

        @Override
        public Probe salamiDevice() {
            return salamiDevice;
        }

        @Override
        public Probe cameraPackage() {
            return cameraPackage;
        }

        @Override
        public Probe galleryPackage() {
            return galleryPackage;
        }

        @Override
        public Probe gappsBuild() {
            return gappsBuild;
        }

        @Override
        public HotspotControls hotspotControls() {
            return hotspotControls;
        }
    }
}
