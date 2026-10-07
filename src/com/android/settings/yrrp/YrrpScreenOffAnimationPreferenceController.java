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

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

/**
 * One screen-off animation choice (Stock or CRT) for the current user. Each radio row on the
 * screen-off animation page binds its own instance by key.
 */
public class YrrpScreenOffAnimationPreferenceController extends BasePreferenceController
        implements SelectorWithWidgetPreference.OnClickListener, DefaultLifecycleObserver {

    // Must match the radio keys in R.xml.yrrp_screen_off_animation_settings.
    static final String KEY_STOCK = "yrrp_screen_off_animation_stock";
    static final String KEY_CRT = "yrrp_screen_off_animation_crt";

    private final int mMode;
    private final YrrpSettingsStore mStore;
    private final YrrpSecureSettingObserver mObserver;

    public YrrpScreenOffAnimationPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        this(context, preferenceKey, new YrrpSettingsStore(context));
    }

    /** Injects the store, so tests can count writes and make them fail. */
    @VisibleForTesting
    YrrpScreenOffAnimationPreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey,
            @NonNull YrrpSettingsStore store) {
        super(context, preferenceKey);
        mMode = keyToMode(preferenceKey);
        mStore = store;
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
        final SelectorWithWidgetPreference preference = screen.findPreference(getPreferenceKey());
        if (preference != null) {
            preference.setOnClickListener(this);
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

    /** Checks this row when it matches the normalized stored mode; an unknown value is kept. */
    @Override
    public void updateState(@Nullable Preference preference) {
        if (preference instanceof SelectorWithWidgetPreference) {
            ((SelectorWithWidgetPreference) preference)
                    .setChecked(mStore.getScreenOffAnimation() == mMode);
        }
    }

    /**
     * Writes this row's mode; every row, this one included, refreshes from its observer. A failed
     * write is logged by the store and leaves the selection unchanged.
     */
    @Override
    public void onRadioButtonClicked(@NonNull SelectorWithWidgetPreference preference) {
        mStore.setScreenOffAnimation(mMode);
    }

    private static int keyToMode(@NonNull String key) {
        switch (key) {
            case KEY_STOCK:
                return YrrpSettingsStore.SCREEN_OFF_STOCK;
            case KEY_CRT:
                return YrrpSettingsStore.SCREEN_OFF_CRT;
            default:
                throw new IllegalArgumentException("unknown screen-off animation key");
        }
    }
}
