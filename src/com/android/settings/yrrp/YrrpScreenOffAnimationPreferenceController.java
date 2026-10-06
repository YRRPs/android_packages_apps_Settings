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
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

/** Chooses the screen-off animation (Stock or CRT) for the current user. */
public class YrrpScreenOffAnimationPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener, DefaultLifecycleObserver {

    // Must match R.array.yrrp_screen_off_animation_values.
    private static final String VALUE_STOCK = modeToValue(YrrpSettingsStore.SCREEN_OFF_STOCK);
    private static final String VALUE_CRT = modeToValue(YrrpSettingsStore.SCREEN_OFF_CRT);

    private final YrrpSettingsStore mStore;
    private final YrrpSecureSettingObserver mObserver;
    private @Nullable ListPreference mPreference;

    public YrrpScreenOffAnimationPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mStore = new YrrpSettingsStore(context);
        mObserver =
                new YrrpSecureSettingObserver(
                        context, this, mStore, YrrpSettingsStore.SCREEN_OFF_ANIMATION);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
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

    /** Shows the normalized stored mode; an unknown stored value shows Stock and is kept. */
    @Override
    public void updateState(@Nullable Preference preference) {
        if (preference instanceof ListPreference) {
            ((ListPreference) preference).setValue(modeToValue(mStore.getScreenOffAnimation()));
        }
    }

    /** Accepts only the exact entry values; the store logs a failed write. */
    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, @Nullable Object newValue) {
        final int mode;
        if (VALUE_STOCK.equals(newValue)) {
            mode = YrrpSettingsStore.SCREEN_OFF_STOCK;
        } else if (VALUE_CRT.equals(newValue)) {
            mode = YrrpSettingsStore.SCREEN_OFF_CRT;
        } else {
            return false;
        }
        if (!mStore.setScreenOffAnimation(mode)) {
            updateState(mPreference);
            return false;
        }
        return true;
    }

    private static String modeToValue(int mode) {
        return Integer.toString(YrrpSettingsStore.normalizeScreenOffAnimation(mode));
    }
}
