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
 * Shared behavior of the Pulse page's integer sliders, each backed by one secure setting.
 *
 * <p>The text shows the stored value as the store normalizes it for display. Showing the page
 * never writes. The row stays visible but disabled while Pulse is off, and refreshes when its
 * setting or the Pulse switch changes elsewhere.
 */
public abstract class YrrpPulseSliderPreferenceController extends SliderPreferenceController
        implements DefaultLifecycleObserver {

    protected final YrrpSettingsStore mStore;
    private final YrrpSecureSettingObserver mObserver;
    private @Nullable Preference mPreference;

    protected YrrpPulseSliderPreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey,
            @NonNull YrrpSettingsStore store,
            @NonNull String settingKey) {
        super(context, preferenceKey);
        mStore = store;
        mObserver =
                new YrrpSecureSettingObserver(
                        context, this, mStore, settingKey, YrrpSettingsStore.PULSE_ENABLED);
    }

    /** Returns the stored value, normalized for display. */
    protected abstract int getStoredValue();

    /** Writes {@code position}, normalized by the store. Returns false if the write failed. */
    protected abstract boolean storeValue(int position);

    /** Returns the distance between two slider positions. */
    protected abstract int getSliderStep();

    /** Formats {@code value} for the slider label and the state description. */
    @NonNull
    protected abstract String formatValue(int value);

    /** Formats {@code value} for the summary only; defaults to {@link #formatValue(int)}. */
    @NonNull
    protected String formatSummary(int value) {
        return formatValue(value);
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

    /** Places the thumb, then shows the exact stored value in the summary and to TalkBack. */
    @Override
    public void updateState(@Nullable Preference preference) {
        if (preference == null) {
            return;
        }
        super.updateState(preference);
        preference.setEnabled(mStore.isPulseEnabled());
        showValueText(preference);
    }

    @Override
    public int getSliderPosition() {
        return getStoredValue();
    }

    /**
     * Writes the user's choice, normalized by the store, and refreshes only the text. The thumb is
     * left to SliderPreference and the settings observer: calling updateState() here would rebind
     * the row twice per drag step.
     */
    @Override
    public boolean setSliderPosition(int position) {
        final boolean written = storeValue(position);
        if (mPreference != null) {
            showValueText(mPreference);
        }
        return written;
    }

    /** Not exposed as a slice, so the setting stays reachable only through this page. */
    @Override
    public boolean isSliceable() {
        return false;
    }

    // Kept deliberately: the plan pins yrrp_menu_key as the highlight if slices are ever enabled.
    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.yrrp_menu_key;
    }

    private void configure(@NonNull SliderPreference preference) {
        preference.setMax(getMax());
        preference.setMin(getMin());
        preference.setSliderIncrement(getSliderStep());
        preference.setUpdatesContinuously(true);
        preference.setShowSliderValue(true);
        preference.setLabelFormater(value -> formatValue(Math.round(value)));
        // Start on the grid; the default 0 may be below the minimum.
        preference.setValue(getSliderPosition());
    }

    private void showValueText(@NonNull Preference preference) {
        final int value = getStoredValue();
        preference.setSummary(formatSummary(value));
        if (preference instanceof SliderPreference) {
            ((SliderPreference) preference).setSliderStateDescription(formatValue(value));
        }
    }
}
