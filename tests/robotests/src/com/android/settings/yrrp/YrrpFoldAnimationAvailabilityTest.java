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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** The fold animation is offered only on a foldable that drives the unfold framework by hinge. */
@RunWith(RobolectricTestRunner.class)
public class YrrpFoldAnimationAvailabilityTest {
    private static final int UNFOLD_ID = 1;
    private static final int HINGE_ANGLE_ID = 2;

    @Test
    public void supported_whenUnfoldOnHingeAngleOnAndHingeSensorPresent() {
        assertThat(YrrpFoldAnimationAvailability.isSupported(
                resources(true, true), sensors(true))).isTrue();
    }

    @Test
    public void unsupported_whenUnfoldFrameworkOff() {
        assertThat(YrrpFoldAnimationAvailability.isSupported(
                resources(false, true), sensors(true))).isFalse();
    }

    @Test
    public void unsupported_whenHingeAngleConfigOff() {
        assertThat(YrrpFoldAnimationAvailability.isSupported(
                resources(true, false), sensors(true))).isFalse();
    }

    @Test
    public void unsupported_whenNoHingeAngleSensor() {
        assertThat(YrrpFoldAnimationAvailability.isSupported(
                resources(true, true), sensors(false))).isFalse();
    }

    @Test
    public void unsupported_whenNoSensorService() {
        assertThat(YrrpFoldAnimationAvailability.isSupported(
                resources(true, true), null)).isFalse();
    }

    @Test
    public void unsupported_whenResourcesMissing() {
        Resources res = mock(Resources.class);
        when(res.getIdentifier("config_unfoldTransitionEnabled", "bool", "android"))
                .thenReturn(0);
        when(res.getIdentifier("config_unfoldTransitionHingeAngle", "bool", "android"))
                .thenReturn(0);

        assertThat(YrrpFoldAnimationAvailability.isSupported(res, sensors(true))).isFalse();
    }

    private static Resources resources(boolean unfold, boolean hingeAngle) {
        Resources res = mock(Resources.class);
        when(res.getIdentifier("config_unfoldTransitionEnabled", "bool", "android"))
                .thenReturn(UNFOLD_ID);
        when(res.getIdentifier("config_unfoldTransitionHingeAngle", "bool", "android"))
                .thenReturn(HINGE_ANGLE_ID);
        when(res.getBoolean(UNFOLD_ID)).thenReturn(unfold);
        when(res.getBoolean(HINGE_ANGLE_ID)).thenReturn(hingeAngle);
        return res;
    }

    private static SensorManager sensors(boolean hinge) {
        SensorManager sensors = mock(SensorManager.class);
        when(sensors.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE))
                .thenReturn(hinge ? mock(Sensor.class) : null);
        return sensors;
    }
}
