/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.SystemProperties;
import android.os.UserManager;
import android.util.Log;

import com.android.settings.yrrp.YrrpFeatureAvailability.HotspotControls;
import com.android.settings.yrrp.YrrpFeatureAvailability.Probe;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Runtime inputs used by the pure feature catalog. */
interface YrrpFeatureEnvironment {
    YrrpFoldAnimationAvailability.Snapshot fold();
    Probe primaryUser();
    Probe salamiDevice();
    Probe cameraPackage();
    Probe galleryPackage();
    Probe gappsBuild();
    HotspotControls hotspotControls();

    @FunctionalInterface
    interface FoldLookup {
        YrrpFoldAnimationAvailability.Snapshot get();
    }

    @FunctionalInterface
    interface PackageLookup {
        void requireInstalled(String packageName) throws NameNotFoundException;
    }

    /** Immutable, privacy-safe values captured together for resolution and diagnostics. */
    final class Snapshot implements YrrpFeatureEnvironment {
        private final YrrpFoldAnimationAvailability.Snapshot mFold;
        private final Probe mPrimaryUser;
        private final Probe mSalamiDevice;
        private final Probe mCameraPackage;
        private final Probe mGalleryPackage;
        private final Probe mGappsBuild;
        private final HotspotControls mHotspotControls;

        Snapshot(
                YrrpFoldAnimationAvailability.Snapshot fold,
                Probe primaryUser,
                Probe salamiDevice,
                Probe cameraPackage,
                Probe galleryPackage,
                Probe gappsBuild,
                HotspotControls hotspotControls) {
            mFold = fold;
            mPrimaryUser = primaryUser;
            mSalamiDevice = salamiDevice;
            mCameraPackage = cameraPackage;
            mGalleryPackage = galleryPackage;
            mGappsBuild = gappsBuild;
            mHotspotControls = hotspotControls;
        }

        static Snapshot capture(YrrpFeatureEnvironment environment) {
            return new Snapshot(
                    environment.fold(),
                    environment.primaryUser(),
                    environment.salamiDevice(),
                    environment.cameraPackage(),
                    environment.galleryPackage(),
                    environment.gappsBuild(),
                    environment.hotspotControls());
        }

        Snapshot withHotspot(HotspotControls hotspotControls) {
            return new Snapshot(
                    mFold,
                    mPrimaryUser,
                    mSalamiDevice,
                    mCameraPackage,
                    mGalleryPackage,
                    mGappsBuild,
                    hotspotControls);
        }

        @Override
        public YrrpFoldAnimationAvailability.Snapshot fold() {
            return mFold;
        }

        @Override
        public Probe primaryUser() {
            return mPrimaryUser;
        }

        @Override
        public Probe salamiDevice() {
            return mSalamiDevice;
        }

        @Override
        public Probe cameraPackage() {
            return mCameraPackage;
        }

        @Override
        public Probe galleryPackage() {
            return mGalleryPackage;
        }

        @Override
        public Probe gappsBuild() {
            return mGappsBuild;
        }

        @Override
        public HotspotControls hotspotControls() {
            return mHotspotControls;
        }
    }

    /** Android-backed environment with injectable probe boundaries for tests. */
    final class AndroidEnvironment implements YrrpFeatureEnvironment {
        private static final String TAG = "YrrpFeatureInventory";
        private static final String CAMERA_PACKAGE = "com.oplus.camera";
        private static final String GALLERY_PACKAGE = "com.oneplus.gallery";

        private final FoldLookup mFold;
        private final BooleanSupplier mPrimaryUser;
        private final BooleanSupplier mSalamiDevice;
        private final PackageLookup mPackages;
        private final Supplier<String> mBuildType;
        private final Supplier<HotspotControls> mHotspotControls;

        AndroidEnvironment(
                FoldLookup fold,
                BooleanSupplier primaryUser,
                BooleanSupplier salamiDevice,
                PackageLookup packages,
                Supplier<String> buildType,
                Supplier<HotspotControls> hotspotControls) {
            mFold = fold;
            mPrimaryUser = primaryUser;
            mSalamiDevice = salamiDevice;
            mPackages = packages;
            mBuildType = buildType;
            mHotspotControls = hotspotControls;
        }

        static AndroidEnvironment create(
                Context context, Supplier<HotspotControls> hotspotControls) {
            return new AndroidEnvironment(
                    () -> YrrpFoldAnimationAvailability.snapshot(context),
                    () -> context.getSystemService(UserManager.class).isSystemUser(),
                    // Product properties can be spoofed; Lineage preserves the target codename.
                    () -> "salami".equals(SystemProperties.get("ro.lineage.device", "")),
                    packageName -> context.getPackageManager().getPackageInfo(packageName, 0),
                    () -> SystemProperties.get("ro.yrrp.build.type", ""),
                    hotspotControls);
        }

        @Override
        public YrrpFoldAnimationAvailability.Snapshot fold() {
            try {
                return mFold.get();
            } catch (RuntimeException e) {
                logFailure("fold", e);
                return new YrrpFoldAnimationAvailability.Snapshot(
                        Probe.UNKNOWN, Probe.UNKNOWN, Probe.UNKNOWN);
            }
        }

        @Override
        public Probe primaryUser() {
            return booleanProbe("primaryUser", mPrimaryUser);
        }

        @Override
        public Probe salamiDevice() {
            return booleanProbe("salamiDevice", mSalamiDevice);
        }

        @Override
        public Probe cameraPackage() {
            return packageProbe("cameraPackage", CAMERA_PACKAGE);
        }

        @Override
        public Probe galleryPackage() {
            return packageProbe("galleryPackage", GALLERY_PACKAGE);
        }

        @Override
        public Probe gappsBuild() {
            try {
                final String value = mBuildType.get();
                if ("gapps".equals(value)) {
                    return Probe.YES;
                }
                if ("vanilla".equals(value)) {
                    return Probe.NO;
                }
                return Probe.UNKNOWN;
            } catch (RuntimeException e) {
                logFailure("gappsBuild", e);
                return Probe.UNKNOWN;
            }
        }

        @Override
        public HotspotControls hotspotControls() {
            try {
                final HotspotControls controls = mHotspotControls.get();
                return controls == null ? HotspotControls.UNDETERMINED : controls;
            } catch (RuntimeException e) {
                logFailure("hotspotControls", e);
                return HotspotControls.UNDETERMINED;
            }
        }

        private Probe packageProbe(String probeName, String packageName) {
            try {
                mPackages.requireInstalled(packageName);
                return Probe.YES;
            } catch (NameNotFoundException e) {
                return Probe.NO;
            } catch (RuntimeException e) {
                logFailure(probeName, e);
                return Probe.UNKNOWN;
            }
        }

        private static Probe booleanProbe(String probeName, BooleanSupplier supplier) {
            try {
                return supplier.getAsBoolean() ? Probe.YES : Probe.NO;
            } catch (RuntimeException e) {
                logFailure(probeName, e);
                return Probe.UNKNOWN;
            }
        }

        private static void logFailure(String probeName, RuntimeException error) {
            Log.w(TAG, "probe=" + probeName + " error=" + error.getClass().getSimpleName());
        }
    }
}
