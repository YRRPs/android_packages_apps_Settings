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
 * Sets the empty share of each Pulse bar's slot, 0 to 80 percent in steps of 5. The default 30
 * reproduces the gap Pulse drew before the setting. SystemUI shrinks the gap further if a bar
 * would be narrower than 1 px.
 *
 * <p>The text shows the stored gap clamped to that range; the thumb shows the nearest step.
 */
public class YrrpPulseBarGapPreferenceController extends YrrpPulseSliderPreferenceController {

    public YrrpPulseBarGapPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        this(context, preferenceKey, new YrrpSettingsStore(context));
    }

    /** Injects the store, so tests can count writes and make them fail. */
    @VisibleForTesting
    YrrpPulseBarGapPreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey,
            @NonNull YrrpSettingsStore store) {
        super(context, preferenceKey, store, YrrpSettingsStore.PULSE_BAR_GAP_PERCENT);
    }

    /** Snaps the thumb onto the step grid for display only, as the height slider does. */
    @Override
    public int getSliderPosition() {
        return YrrpSettingsStore.normalizeBarGapForWrite(getStoredValue());
    }

    @Override
    public int getMax() {
        return YrrpSettingsStore.PULSE_BAR_GAP_PERCENT_MAX;
    }

    @Override
    public int getMin() {
        return YrrpSettingsStore.PULSE_BAR_GAP_PERCENT_MIN;
    }

    @Override
    protected int getStoredValue() {
        return mStore.getPulseBarGapPercent();
    }

    @Override
    protected boolean storeValue(int position) {
        return mStore.setPulseBarGapPercent(position);
    }

    @Override
    protected int getSliderStep() {
        return YrrpSettingsStore.PULSE_BAR_GAP_PERCENT_STEP;
    }

    @NonNull
    @Override
    protected String formatValue(int percent) {
        return Utils.formatPercentage(percent);
    }
}
