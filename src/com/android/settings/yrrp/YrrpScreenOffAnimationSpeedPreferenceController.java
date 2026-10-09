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
 * One speed choice for custom screen-off effects (CRT) for the current user. Each radio row on
 * the Screen-off animation page binds its own instance by key. The rows stay visible but
 * disabled while Stock is selected, because Stock ignores the speed.
 */
public class YrrpScreenOffAnimationSpeedPreferenceController extends BasePreferenceController
        implements SelectorWithWidgetPreference.OnClickListener, DefaultLifecycleObserver {

    // Must match the speed radio keys in R.xml.yrrp_screen_off_animation_settings.
    static final String KEY_50 = "yrrp_screen_off_animation_speed_50";
    static final String KEY_75 = "yrrp_screen_off_animation_speed_75";
    static final String KEY_100 = "yrrp_screen_off_animation_speed_100";
    static final String KEY_150 = "yrrp_screen_off_animation_speed_150";
    static final String KEY_200 = "yrrp_screen_off_animation_speed_200";

    private final int mSpeed;
    private final YrrpSettingsStore mStore;
    private final YrrpSecureSettingObserver mObserver;

    public YrrpScreenOffAnimationSpeedPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        this(context, preferenceKey, new YrrpSettingsStore(context));
    }

    /** Injects the store, so tests can count writes and make them fail. */
    @VisibleForTesting
    YrrpScreenOffAnimationSpeedPreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey,
            @NonNull YrrpSettingsStore store) {
        super(context, preferenceKey);
        mSpeed = keyToSpeed(preferenceKey);
        mStore = store;
        mObserver =
                new YrrpSecureSettingObserver(
                        context,
                        this,
                        mStore,
                        YrrpSettingsStore.SCREEN_OFF_ANIMATION_SPEED,
                        YrrpSettingsStore.SCREEN_OFF_ANIMATION);
    }

    @Override
    public int getAvailabilityStatus() {
        // Not DISABLED_DEPENDENT_SETTING: the effect can change on this page, so updateState()
        // toggles enabled instead.
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

    /**
     * Checks this row when it matches the clamped stored speed; the stored value is kept. A
     * clamped value between two listed speeds checks no row, which only an external write can
     * cause, because this page writes listed speeds only.
     */
    @Override
    public void updateState(@Nullable Preference preference) {
        if (preference == null) {
            return;
        }
        preference.setEnabled(
                mStore.getScreenOffAnimation() != YrrpSettingsStore.SCREEN_OFF_STOCK);
        if (preference instanceof SelectorWithWidgetPreference) {
            ((SelectorWithWidgetPreference) preference)
                    .setChecked(mStore.getScreenOffAnimationSpeed() == mSpeed);
        }
    }

    /**
     * Writes this row's speed; every row, this one included, refreshes from its observer. A
     * failed write is logged by the store and leaves the selection unchanged.
     */
    @Override
    public void onRadioButtonClicked(@NonNull SelectorWithWidgetPreference preference) {
        mStore.setScreenOffAnimationSpeed(mSpeed);
    }

    /** Not exposed as a slice, so the setting stays reachable only through this page. */
    @Override
    public boolean isSliceable() {
        return false;
    }

    private static int keyToSpeed(@NonNull String key) {
        switch (key) {
            case KEY_50:
                return 50;
            case KEY_75:
                return 75;
            case KEY_100:
                return 100;
            case KEY_150:
                return 150;
            case KEY_200:
                return 200;
            default:
                throw new IllegalArgumentException("unknown screen-off animation speed key");
        }
    }
}
