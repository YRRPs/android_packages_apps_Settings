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
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.SliderPreferenceController;
import com.android.settingslib.Utils;
import com.android.settingslib.widget.SliderPreference;

/**
 * Sets the Pulse bar opacity as the stored alpha, 26-255, shown as a percentage of 255.
 *
 * <p>Opacity applies in every color mode, so the row is disabled only while Pulse is off. Every
 * integer in the range is a valid slider value, so the thumb always shows the clamped stored value.
 * Showing the page never writes.
 */
public class YrrpPulseOpacityPreferenceController extends SliderPreferenceController
        implements DefaultLifecycleObserver {

    private final YrrpSettingsStore mStore;
    private final YrrpSecureSettingObserver mObserver;
    private @Nullable Preference mPreference;

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
        super(context, preferenceKey);
        mStore = store;
        mObserver =
                new YrrpSecureSettingObserver(
                        context,
                        this,
                        mStore,
                        YrrpSettingsStore.PULSE_ALPHA,
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

    @Override
    public void updateState(@Nullable Preference preference) {
        if (preference == null) {
            return;
        }
        super.updateState(preference);
        preference.setEnabled(mStore.isPulseEnabled());
        showOpacityText(preference);
    }

    @Override
    public int getSliderPosition() {
        return mStore.getPulseAlpha();
    }

    /**
     * Writes the user's choice, clamped by the store, and refreshes only the text. Calling
     * updateState() here would rebind the row on every drag step.
     */
    @Override
    public boolean setSliderPosition(int position) {
        final boolean written = mStore.setPulseAlpha(position);
        if (mPreference != null) {
            showOpacityText(mPreference);
        }
        return written;
    }

    @Override
    public int getMax() {
        return YrrpSettingsStore.PULSE_ALPHA_MAX;
    }

    @Override
    public int getMin() {
        return YrrpSettingsStore.PULSE_ALPHA_MIN;
    }

    /** Not exposed as a slice, so the setting stays reachable only through this page. */
    @Override
    public boolean isSliceable() {
        return false;
    }

    private void configure(@NonNull SliderPreference preference) {
        preference.setMax(getMax());
        preference.setMin(getMin());
        preference.setSliderIncrement(1);
        preference.setUpdatesContinuously(true);
        preference.setShowSliderValue(true);
        preference.setLabelFormater(value -> formatOpacity(Math.round(value)));
        // Start inside the range; the default 0 is below the minimum.
        preference.setValue(getSliderPosition());
    }

    private void showOpacityText(@NonNull Preference preference) {
        final String opacity = formatOpacity(mStore.getPulseAlpha());
        preference.setSummary(opacity);
        if (preference instanceof SliderPreference) {
            ((SliderPreference) preference).setSliderStateDescription(opacity);
        }
    }

    /** The alpha as a rounded, locale-formatted percentage of 255, for example "85%". */
    @NonNull
    private static String formatOpacity(int alpha) {
        return Utils.formatPercentage(
                Math.round(alpha * 100f / YrrpSettingsStore.PULSE_ALPHA_MAX));
    }
}
