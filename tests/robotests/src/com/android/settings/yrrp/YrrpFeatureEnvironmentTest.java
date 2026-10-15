/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static com.google.common.truth.Truth.assertThat;

import android.content.pm.PackageManager.NameNotFoundException;

import com.android.settings.yrrp.YrrpFeatureAvailability.HotspotControls;
import com.android.settings.yrrp.YrrpFeatureAvailability.Probe;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Android probe parsing keeps absence separate from failures and never logs probed values. */
@RunWith(RobolectricTestRunner.class)
public class YrrpFeatureEnvironmentTest {
    private static final String CAMERA = "com.oplus.camera";
    private static final String GALLERY = "com.oneplus.gallery";

    private final AtomicReference<String> mBuildType = new AtomicReference<>("gapps");
    private final AtomicReference<HotspotControls> mHotspot =
            new AtomicReference<>(HotspotControls.UNDETERMINED);
    private final List<String> mPackages = new ArrayList<>();

    private boolean mPrimaryUser = true;
    private boolean mSalami = true;
    private YrrpFeatureEnvironment.AndroidEnvironment mEnvironment;

    @Before
    public void setUp() {
        ShadowLog.clear();
        mEnvironment = environment(packageName -> mPackages.add(packageName));
    }

    @Test
    public void booleanProbesMapTrueAndFalse() {
        assertThat(mEnvironment.primaryUser()).isEqualTo(Probe.YES);
        assertThat(mEnvironment.salamiDevice()).isEqualTo(Probe.YES);

        mPrimaryUser = false;
        mSalami = false;

        assertThat(mEnvironment.primaryUser()).isEqualTo(Probe.NO);
        assertThat(mEnvironment.salamiDevice()).isEqualTo(Probe.NO);
    }

    @Test
    public void packageProbesUseOnlyFixedIntegrationPackages() {
        assertThat(mEnvironment.cameraPackage()).isEqualTo(Probe.YES);
        assertThat(mEnvironment.galleryPackage()).isEqualTo(Probe.YES);

        assertThat(mPackages).containsExactly(CAMERA, GALLERY).inOrder();
    }

    @Test
    public void packageNotFoundMeansAbsent() {
        mEnvironment = environment(packageName -> {
            if (GALLERY.equals(packageName)) {
                throw new NameNotFoundException(packageName);
            }
        });

        assertThat(mEnvironment.cameraPackage()).isEqualTo(Probe.YES);
        assertThat(mEnvironment.galleryPackage()).isEqualTo(Probe.NO);
    }

    @Test
    public void packageRuntimeFailureMeansUnknown() {
        mEnvironment = environment(packageName -> {
            throw new IllegalStateException("private-package-value");
        });

        assertThat(mEnvironment.cameraPackage()).isEqualTo(Probe.UNKNOWN);

        final String logs = logs();
        assertThat(logs).contains("probe=cameraPackage");
        assertThat(logs).contains("error=IllegalStateException");
        assertThat(logs).doesNotContain("private-package-value");
        assertThat(logs).doesNotContain(CAMERA);
    }

    @Test
    public void booleanRuntimeFailureMeansUnknown() {
        mEnvironment = new YrrpFeatureEnvironment.AndroidEnvironment(
                this::availableFold,
                () -> { throw new SecurityException("private-user-value"); },
                () -> true,
                packageName -> {},
                mBuildType::get,
                mHotspot::get);

        assertThat(mEnvironment.primaryUser()).isEqualTo(Probe.UNKNOWN);

        final String logs = logs();
        assertThat(logs).contains("probe=primaryUser");
        assertThat(logs).contains("error=SecurityException");
        assertThat(logs).doesNotContain("private-user-value");
    }

    @Test
    public void buildTypeMapsOnlyApprovedValues() {
        assertThat(mEnvironment.gappsBuild()).isEqualTo(Probe.YES);

        mBuildType.set("vanilla");
        assertThat(mEnvironment.gappsBuild()).isEqualTo(Probe.NO);

        mBuildType.set("");
        assertThat(mEnvironment.gappsBuild()).isEqualTo(Probe.UNKNOWN);

        mBuildType.set("private-unsupported-value");
        assertThat(mEnvironment.gappsBuild()).isEqualTo(Probe.UNKNOWN);
        assertThat(logs()).doesNotContain("private-unsupported-value");
    }

    @Test
    public void buildTypeFailureMeansUnknownWithoutRawValue() {
        mEnvironment = new YrrpFeatureEnvironment.AndroidEnvironment(
                this::availableFold,
                () -> true,
                () -> true,
                packageName -> {},
                () -> { throw new IllegalStateException("private-property-value"); },
                mHotspot::get);

        assertThat(mEnvironment.gappsBuild()).isEqualTo(Probe.UNKNOWN);

        final String logs = logs();
        assertThat(logs).contains("probe=gappsBuild");
        assertThat(logs).contains("error=IllegalStateException");
        assertThat(logs).doesNotContain("private-property-value");
    }

    @Test
    public void hotspotControlStateIsReadAtCallTime() {
        assertThat(mEnvironment.hotspotControls()).isEqualTo(HotspotControls.UNDETERMINED);

        mHotspot.set(HotspotControls.SUPPORTED);
        assertThat(mEnvironment.hotspotControls()).isEqualTo(HotspotControls.SUPPORTED);

        mHotspot.set(HotspotControls.UNSUPPORTED);
        assertThat(mEnvironment.hotspotControls()).isEqualTo(HotspotControls.UNSUPPORTED);
    }

    @Test
    public void foldSnapshotIsReadAtCallTime() {
        assertThat(mEnvironment.fold().available()).isTrue();
    }

    private YrrpFeatureEnvironment.AndroidEnvironment environment(
            YrrpFeatureEnvironment.PackageLookup packages) {
        return new YrrpFeatureEnvironment.AndroidEnvironment(
                this::availableFold,
                () -> mPrimaryUser,
                () -> mSalami,
                packages,
                mBuildType::get,
                mHotspot::get);
    }

    private YrrpFoldAnimationAvailability.Snapshot availableFold() {
        return new YrrpFoldAnimationAvailability.Snapshot(Probe.YES, Probe.YES, Probe.YES);
    }

    private static String logs() {
        final StringBuilder text = new StringBuilder();
        for (ShadowLog.LogItem item : ShadowLog.getLogsForTag("YrrpFeatureInventory")) {
            text.append(item.msg).append('\n');
        }
        return text.toString();
    }
}
