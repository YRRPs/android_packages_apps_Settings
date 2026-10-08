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

import com.android.settings.R;

/**
 * Sets how many Pulse bars span the screen, 16 to 64 in steps of 4. SystemUI keeps analysing 32
 * bands and resamples them onto this count.
 *
 * <p>The text shows the stored count clamped to that range; the thumb shows the nearest step.
 */
public class YrrpPulseBarCountPreferenceController extends YrrpPulseSliderPreferenceController {

    public YrrpPulseBarCountPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        this(context, preferenceKey, new YrrpSettingsStore(context));
    }

    /** Injects the store, so tests can count writes and make them fail. */
    @VisibleForTesting
    YrrpPulseBarCountPreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey,
            @NonNull YrrpSettingsStore store) {
        super(context, preferenceKey, store, YrrpSettingsStore.PULSE_BAR_COUNT);
    }

    /** Snaps the thumb onto the step grid for display only, as the height slider does. */
    @Override
    public int getSliderPosition() {
        return YrrpSettingsStore.normalizeBarCountForWrite(getStoredValue());
    }

    @Override
    public int getMax() {
        return YrrpSettingsStore.PULSE_BAR_COUNT_MAX;
    }

    @Override
    public int getMin() {
        return YrrpSettingsStore.PULSE_BAR_COUNT_MIN;
    }

    @Override
    protected int getStoredValue() {
        return mStore.getPulseBarCount();
    }

    @Override
    protected boolean storeValue(int position) {
        return mStore.setPulseBarCount(position);
    }

    @Override
    protected int getSliderStep() {
        return YrrpSettingsStore.PULSE_BAR_COUNT_STEP;
    }

    @NonNull
    @Override
    protected String formatValue(int count) {
        return mContext.getString(R.string.yrrp_pulse_bar_count_value, count);
    }
}
