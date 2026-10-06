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
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.SliderPreferenceController;
import com.android.settingslib.widget.SliderPreference;

/**
 * Sets the Pulse bar height in 4 dp steps between 8 and 96 dp.
 *
 * <p>The text shows the stored height clamped to that range; the thumb shows the nearest step.
 * Showing the page never writes. The row stays visible but disabled while Pulse is off.
 */
public class YrrpPulseHeightPreferenceController extends SliderPreferenceController
        implements DefaultLifecycleObserver {

    private final YrrpSettingsStore mStore;
    private final YrrpSecureSettingObserver mObserver;
    private @Nullable Preference mPreference;

    public YrrpPulseHeightPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mStore = new YrrpSettingsStore(context);
        mObserver =
                new YrrpSecureSettingObserver(
                        context,
                        this,
                        mStore,
                        YrrpSettingsStore.PULSE_HEIGHT_DP,
                        YrrpSettingsStore.PULSE_ENABLED);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference instanceof SliderPreference) {
            configure((SliderPreference) mPreference);
        }
        mObserver.displayPreference(screen);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        mObserver.onStart(owner);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        mObserver.onStop(owner);
    }

    /** Places the thumb, then shows the exact clamped height in the summary and to TalkBack. */
    @Override
    public void updateState(@Nullable Preference preference) {
        if (preference == null) {
            return;
        }
        super.updateState(preference);
        preference.setEnabled(mStore.isPulseEnabled());
        showHeightText(preference);
    }

    /**
     * The thumb snaps to the nearest step for display only: with a step size above zero, the
     * material Slider throws IllegalStateException for a value off the valueFrom + n * stepSize
     * grid (BaseSlider.validateValues), and the stored value may be off the grid.
     */
    @Override
    public int getSliderPosition() {
        return YrrpSettingsStore.normalizeHeightForWrite(mStore.getPulseHeightDp());
    }

    /**
     * Writes the user's choice, snapped by the store, and refreshes only the text. The thumb is
     * left to SliderPreference and the settings observer: calling updateState() here would rebind
     * the row twice per drag step.
     */
    @Override
    public boolean setSliderPosition(int position) {
        final boolean written = mStore.setPulseHeightDp(position);
        if (mPreference != null) {
            showHeightText(mPreference);
        }
        return written;
    }

    @Override
    public int getMax() {
        return YrrpSettingsStore.PULSE_HEIGHT_MAX_DP;
    }

    @Override
    public int getMin() {
        return YrrpSettingsStore.PULSE_HEIGHT_MIN_DP;
    }

    /** Not exposed as a slice, so the setting stays reachable only through this page. */
    @Override
    public boolean isSliceable() {
        return false;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.yrrp_menu_key;
    }

    private void configure(@NonNull SliderPreference preference) {
        preference.setMax(getMax());
        preference.setMin(getMin());
        preference.setSliderIncrement(YrrpSettingsStore.PULSE_HEIGHT_STEP_DP);
        preference.setUpdatesContinuously(true);
        preference.setShowSliderValue(true);
        preference.setLabelFormater(value -> formatHeight(Math.round(value)));
        // Start on the grid; the default 0 is below the minimum.
        preference.setValue(getSliderPosition());
    }

    private void showHeightText(@NonNull Preference preference) {
        final String height = formatHeight(mStore.getPulseHeightDp());
        preference.setSummary(height);
        if (preference instanceof SliderPreference) {
            ((SliderPreference) preference).setSliderStateDescription(height);
        }
    }

    @NonNull
    private String formatHeight(int heightDp) {
        return mContext.getString(R.string.yrrp_pulse_height_value, heightDp);
    }
}
