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
import androidx.annotation.StringRes;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/** Opens the screen-off animation page and summarizes the current user's animation. */
public class YrrpScreenOffAnimationEntryPreferenceController extends BasePreferenceController
        implements DefaultLifecycleObserver {

    private final YrrpSettingsStore mStore;
    private final YrrpSecureSettingObserver mObserver;

    public YrrpScreenOffAnimationEntryPreferenceController(
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

    /** Summarizes the normalized stored mode; an unknown stored value shows Stock. */
    @Override
    public CharSequence getSummary() {
        return mContext.getText(labelFor(mStore.getScreenOffAnimation()));
    }

    /** The row label for a normalized effect value. */
    @StringRes
    static int labelFor(int mode) {
        switch (mode) {
            case YrrpSettingsStore.SCREEN_OFF_CRT:
                return R.string.yrrp_screen_off_animation_crt;
            case YrrpSettingsStore.SCREEN_OFF_TEAR:
                return R.string.yrrp_screen_off_animation_tear;
            case YrrpSettingsStore.SCREEN_OFF_CORRUPT:
                return R.string.yrrp_screen_off_animation_corrupt;
            case YrrpSettingsStore.SCREEN_OFF_SIGNAL_LOSS:
                return R.string.yrrp_screen_off_animation_signal_loss;
            default:
                return R.string.yrrp_screen_off_animation_stock;
        }
    }
}
