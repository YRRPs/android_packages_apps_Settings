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
 * Sets the Pulse low-level boost: the strength k of SystemUI's logarithmic bar-height curve, 0 to
 * 100 in steps of 1. At 0 the curve is off and bars are drawn linearly.
 */
public class YrrpPulseBoostPreferenceController extends YrrpPulseSliderPreferenceController {

    public YrrpPulseBoostPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        this(context, preferenceKey, new YrrpSettingsStore(context));
    }

    /** Injects the store, so tests can count writes and make them fail. */
    @VisibleForTesting
    YrrpPulseBoostPreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey,
            @NonNull YrrpSettingsStore store) {
        super(context, preferenceKey, store, YrrpSettingsStore.PULSE_BOOST);
    }

    @Override
    public int getMax() {
        return YrrpSettingsStore.PULSE_BOOST_MAX;
    }

    @Override
    public int getMin() {
        return YrrpSettingsStore.PULSE_BOOST_MIN;
    }

    @Override
    protected int getStoredValue() {
        return mStore.getPulseBoost();
    }

    @Override
    protected boolean storeValue(int position) {
        return mStore.setPulseBoost(position);
    }

    @Override
    protected int getSliderStep() {
        return 1;
    }

    @NonNull
    @Override
    protected String formatValue(int boost) {
        return boost == YrrpSettingsStore.PULSE_BOOST_MIN
                ? mContext.getString(R.string.yrrp_pulse_boost_off)
                : mContext.getString(R.string.yrrp_pulse_boost_value, boost);
    }
}
