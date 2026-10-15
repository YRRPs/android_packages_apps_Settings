/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static com.android.settings.yrrp.YrrpFeatureAvailability.HotspotControls.SUPPORTED;
import static com.android.settings.yrrp.YrrpFeatureAvailability.HotspotControls.UNDETERMINED;
import static com.android.settings.yrrp.YrrpFeatureAvailability.Probe.NO;
import static com.android.settings.yrrp.YrrpFeatureAvailability.Probe.UNKNOWN;
import static com.android.settings.yrrp.YrrpFeatureAvailability.ProbeStatus.FAILED;
import static com.android.settings.yrrp.YrrpFeatureAvailability.ProbeStatus.KNOWN;
import static com.android.settings.yrrp.YrrpFeatureAvailability.ProbeStatus.PENDING;

import com.android.settings.R;
import com.android.settings.yrrp.YrrpFeatureAvailability.HotspotControls;
import com.android.settings.yrrp.YrrpFeatureAvailability.Probe;
import com.android.settings.yrrp.YrrpFeatureAvailability.ProbeStatus;

import java.util.ArrayList;
import java.util.List;

/** Ordered feature definitions and pure availability policy. */
final class YrrpFeatureCatalog {
    enum Group { CUSTOMIZATION, SYSTEM_SERVICES, DEVICE_INTEGRATIONS }

    enum Id {
        PULSE("yrrp_feature_pulse", Group.CUSTOMIZATION),
        SCREEN_OFF("yrrp_feature_screen_off", Group.CUSTOMIZATION),
        MATRIX_BOOT_SHUTDOWN("yrrp_feature_matrix_boot_shutdown", Group.CUSTOMIZATION),
        FOLD_ANIMATIONS("yrrp_feature_fold_animations", Group.CUSTOMIZATION),
        FULL_THEMING("yrrp_feature_full_theming", Group.CUSTOMIZATION),
        AD_BLOCKING("yrrp_feature_ad_blocking", Group.SYSTEM_SERVICES),
        HOTSPOT_CLIENT_MANAGEMENT(
                "yrrp_feature_hotspot_client_management", Group.SYSTEM_SERVICES),
        GAPPS("yrrp_feature_gapps", Group.SYSTEM_SERVICES),
        INDOOR_AUTO_BRIGHTNESS(
                "yrrp_feature_indoor_auto_brightness", Group.DEVICE_INTEGRATIONS),
        ONEPLUS_CAMERA("yrrp_feature_oneplus_camera", Group.DEVICE_INTEGRATIONS),
        ONEPLUS_GALLERY("yrrp_feature_oneplus_gallery", Group.DEVICE_INTEGRATIONS);

        final String key;
        final Group group;

        Id(String key, Group group) {
            this.key = key;
            this.group = group;
        }
    }

    private static final List<Id> IDS = List.of(Id.values());

    private YrrpFeatureCatalog() {}

    static List<Id> ids() {
        return IDS;
    }

    static List<Id> ids(Group group) {
        final List<Id> result = new ArrayList<>();
        for (Id id : IDS) {
            if (id.group == group) {
                result.add(id);
            }
        }
        return List.copyOf(result);
    }

    static YrrpFeatureAvailability resolve(Id id, YrrpFeatureEnvironment environment) {
        switch (id) {
            case PULSE:
            case SCREEN_OFF:
            case MATRIX_BOOT_SHUTDOWN:
            case FULL_THEMING:
                return available();
            case FOLD_ANIMATIONS:
                return resolveFold(environment.fold());
            case AD_BLOCKING:
                return resolveProbe(environment.primaryUser(),
                        R.string.yrrp_feature_status_primary_user,
                        "primary_user_only", "primaryUser");
            case HOTSPOT_CLIENT_MANAGEMENT:
                return resolveHotspot(environment.hotspotControls());
            case GAPPS:
                return resolveProbe(environment.gappsBuild(),
                        R.string.yrrp_feature_status_vanilla,
                        "vanilla_build", "gappsBuild");
            case INDOOR_AUTO_BRIGHTNESS:
                return resolveProbe(environment.salamiDevice(),
                        R.string.yrrp_feature_status_salami_only,
                        "salami_only", "salamiDevice");
            case ONEPLUS_CAMERA:
                return resolveProbe(environment.cameraPackage(),
                        R.string.yrrp_feature_status_camera_missing,
                        "camera_missing", "cameraPackage");
            case ONEPLUS_GALLERY:
                return resolveGallery(environment);
        }
        throw new AssertionError("Unhandled feature " + id);
    }

    private static YrrpFeatureAvailability resolveFold(
            YrrpFoldAnimationAvailability.Snapshot fold) {
        if (fold.hingeSensorPresent == NO) {
            return unavailable(R.string.yrrp_feature_status_fold_sensor,
                    "fold_sensor_missing", "hingeSensorPresent", KNOWN);
        }
        YrrpFeatureAvailability result = foldGate(fold.unfoldEnabled,
                R.string.yrrp_feature_status_fold_framework,
                "fold_framework_disabled", "unfoldEnabled");
        if (result != null) {
            return result;
        }
        result = foldGate(fold.hingeAngleEnabled,
                R.string.yrrp_feature_status_fold_hinge_config,
                "fold_hinge_config_disabled", "hingeAngleEnabled");
        if (result != null) {
            return result;
        }
        result = foldGate(fold.hingeSensorPresent,
                R.string.yrrp_feature_status_fold_sensor,
                "fold_sensor_missing", "hingeSensorPresent");
        return result == null ? available() : result;
    }

    private static YrrpFeatureAvailability foldGate(
            Probe probe, int statusRes, String reasonCode, String blockedBy) {
        if (probe == NO) {
            return unavailable(statusRes, reasonCode, blockedBy, KNOWN);
        }
        if (probe == UNKNOWN) {
            return unknown(blockedBy);
        }
        return null;
    }

    private static YrrpFeatureAvailability resolveProbe(
            Probe probe, int unavailableStatus, String reasonCode, String blockedBy) {
        if (probe == NO) {
            return unavailable(unavailableStatus, reasonCode, blockedBy, KNOWN);
        }
        if (probe == UNKNOWN) {
            return unknown(blockedBy);
        }
        return available();
    }

    private static YrrpFeatureAvailability resolveGallery(YrrpFeatureEnvironment environment) {
        final Probe salami = environment.salamiDevice();
        if (salami == NO) {
            return unavailable(R.string.yrrp_feature_status_gallery_device,
                    "gallery_wrong_device", "salamiDevice", KNOWN);
        }
        if (salami == UNKNOWN) {
            return unknown("salamiDevice");
        }
        return resolveProbe(environment.galleryPackage(),
                R.string.yrrp_feature_status_gallery_missing,
                "gallery_missing", "galleryPackage");
    }

    private static YrrpFeatureAvailability resolveHotspot(HotspotControls controls) {
        if (controls == SUPPORTED) {
            return available();
        }
        if (controls == UNDETERMINED) {
            return result(true, R.string.yrrp_feature_status_hotspot_unknown,
                    "hotspot_controls_unknown", "capabilityKnown", PENDING);
        }
        return result(true, R.string.yrrp_feature_status_hotspot_details_only,
                "hotspot_controls_unavailable", "forceDisconnectSupported", KNOWN);
    }

    private static YrrpFeatureAvailability available() {
        return result(true, R.string.yrrp_feature_status_available,
                "available", "none", KNOWN);
    }

    private static YrrpFeatureAvailability unknown(String blockedBy) {
        return unavailable(R.string.yrrp_feature_status_unknown,
                "probe_unknown", blockedBy, FAILED);
    }

    private static YrrpFeatureAvailability unavailable(
            int statusRes, String reasonCode, String blockedBy, ProbeStatus probeStatus) {
        return result(false, statusRes, reasonCode, blockedBy, probeStatus);
    }

    private static YrrpFeatureAvailability result(
            boolean available, int statusRes, String reasonCode, String blockedBy,
            ProbeStatus probeStatus) {
        return new YrrpFeatureAvailability(
                available, statusRes, reasonCode, blockedBy, probeStatus);
    }
}
