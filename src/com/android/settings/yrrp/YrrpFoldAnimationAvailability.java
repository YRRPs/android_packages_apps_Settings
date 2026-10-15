/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.yrrp;

import static com.android.settings.yrrp.YrrpFeatureAvailability.Probe.NO;
import static com.android.settings.yrrp.YrrpFeatureAvailability.Probe.UNKNOWN;
import static com.android.settings.yrrp.YrrpFeatureAvailability.Probe.YES;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.yrrp.YrrpFeatureAvailability.Probe;

import java.util.function.Predicate;

/**
 * Whether this device can play the fold animation: SystemUI's unfold framework is on, it is
 * driven by the hinge angle, and the device reports a hinge-angle sensor. LineageOS device trees
 * opt in through the {@code config_unfoldTransition*} overlays; the sensor check keeps the page
 * hidden if an overlay is installed on hardware without a hinge.
 */
final class YrrpFoldAnimationAvailability {
    private YrrpFoldAnimationAvailability() {}

    /** The production check. */
    @VisibleForTesting
    static final Predicate<Context> READ_DEVICE = context -> snapshot(context).available();

    /** Where {@link #isAvailable} reads; tests replace it and must restore {@link #READ_DEVICE}. */
    @VisibleForTesting static Predicate<Context> sSource = READ_DEVICE;

    /** One answer for the hub row, the page controllers and the search provider. */
    static boolean isAvailable(@NonNull Context context) {
        return sSource.test(context);
    }

    static Snapshot snapshot(@NonNull Context context) {
        return snapshot(Resources.getSystem(), context.getSystemService(SensorManager.class));
    }

    /** Reads the same resources as SystemUI's ResourceUnfoldTransitionConfig. */
    @VisibleForTesting
    static boolean isSupported(@NonNull Resources res, @Nullable SensorManager sensors) {
        return snapshot(res, sensors).available();
    }

    @VisibleForTesting
    static Snapshot snapshot(@NonNull Resources res, @Nullable SensorManager sensors) {
        return new Snapshot(
                readBool(res, "config_unfoldTransitionEnabled"),
                readBool(res, "config_unfoldTransitionHingeAngle"),
                readHingeSensor(sensors));
    }

    private static Probe readBool(Resources res, String name) {
        try {
            final int id = res.getIdentifier(name, "bool", "android");
            return id != 0 && res.getBoolean(id) ? YES : NO;
        } catch (RuntimeException e) {
            return UNKNOWN;
        }
    }

    private static Probe readHingeSensor(@Nullable SensorManager sensors) {
        if (sensors == null) {
            return UNKNOWN;
        }
        try {
            return sensors.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE) == null ? NO : YES;
        } catch (RuntimeException e) {
            return UNKNOWN;
        }
    }

    static final class Snapshot {
        final Probe unfoldEnabled;
        final Probe hingeAngleEnabled;
        final Probe hingeSensorPresent;

        Snapshot(Probe unfoldEnabled, Probe hingeAngleEnabled, Probe hingeSensorPresent) {
            this.unfoldEnabled = unfoldEnabled;
            this.hingeAngleEnabled = hingeAngleEnabled;
            this.hingeSensorPresent = hingeSensorPresent;
        }

        boolean available() {
            return unfoldEnabled == YES
                    && hingeAngleEnabled == YES
                    && hingeSensorPresent == YES;
        }
    }
}
