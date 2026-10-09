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
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

/**
 * The Solid color mode row. Its summary shows the stored color as {@code #RRGGBB}, and its gear
 * opens the Pulse RGB picker in every color mode: the color applies only in Solid, but may be
 * chosen before switching to it. Confirming the picker writes only the color.
 */
public class YrrpPulseSolidColorModePreferenceController
        extends YrrpPulseColorModePreferenceController {

    private final YrrpPulseColorPicker mPicker;

    public YrrpPulseSolidColorModePreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        this(context, preferenceKey, new YrrpSettingsStore(context));
    }

    /** Injects the store, so tests can count writes and make them fail. */
    @VisibleForTesting
    YrrpPulseSolidColorModePreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey,
            @NonNull YrrpSettingsStore store) {
        super(context, preferenceKey, store, YrrpSettingsStore.PULSE_COLOR);
        mPicker = new YrrpPulseColorPicker(store, this::refresh);
    }

    /** Binds the hosting page; see {@link YrrpPulseColorPicker#init(Fragment)}. */
    public void init(@NonNull Fragment host) {
        mPicker.init(host);
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        final SelectorWithWidgetPreference preference = screen.findPreference(getPreferenceKey());
        if (preference != null) {
            preference.setExtraWidgetContentDescription(
                    mContext.getString(R.string.yrrp_pulse_color_title));
            preference.setExtraWidgetOnClickListener(v -> onGearClicked());
        }
    }

    @Override
    public void updateState(@Nullable Preference preference) {
        super.updateState(preference);
        if (preference != null) {
            preference.setSummary(YrrpSettingsStore.formatRgb(mStore.getPulseColor()));
        }
    }

    /** A disabled row also disables the gear view; this guard covers a click already queued. */
    @VisibleForTesting
    void onGearClicked() {
        if (mStore.isPulseEnabled()) {
            mPicker.show();
        }
    }
}
