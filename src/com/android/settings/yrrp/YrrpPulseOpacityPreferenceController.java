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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.android.settingslib.Utils;

/**
 * Sets the Pulse bar opacity as the stored alpha, 26-255, shown as a percentage of 255.
 *
 * <p>Opacity applies in every color mode, so the row is disabled only while Pulse is off. Every
 * integer in the range is a valid slider value, so the thumb always shows the clamped stored value.
 */
public class YrrpPulseOpacityPreferenceController extends YrrpPulseSliderPreferenceController {

    public YrrpPulseOpacityPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        this(context, preferenceKey, new YrrpSettingsStore(context));
    }

    /** Injects the store, so tests can count writes and make them fail. */
    @VisibleForTesting
    YrrpPulseOpacityPreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey,
            @NonNull YrrpSettingsStore store) {
        super(context, preferenceKey, store, YrrpSettingsStore.PULSE_ALPHA);
    }

    @Override
    public int getMax() {
        return YrrpSettingsStore.PULSE_ALPHA_MAX;
    }

    @Override
    public int getMin() {
        return YrrpSettingsStore.PULSE_ALPHA_MIN;
    }

    @Override
    protected int getStoredValue() {
        return mStore.getPulseAlpha();
    }

    @Override
    protected boolean storeValue(int position) {
        return mStore.setPulseAlpha(position);
    }

    @Override
    protected int getSliderStep() {
        return 1;
    }

    /** The alpha as a rounded, locale-formatted percentage of 255, for example "85%". */
    @NonNull
    @Override
    protected String formatValue(int alpha) {
        return Utils.formatPercentage(
                Math.round(alpha * 100f / YrrpSettingsStore.PULSE_ALPHA_MAX));
    }
}
