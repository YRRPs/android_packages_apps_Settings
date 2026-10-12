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

import java.util.function.BooleanSupplier;

/**
 * One fold animation choice (Stock, Pixel, or Duo) for the current user. Each radio row on the
 * fold animation page binds its own instance by key.
 */
public class YrrpFoldAnimationPreferenceController extends BasePreferenceController
        implements SelectorWithWidgetPreference.OnClickListener, DefaultLifecycleObserver {

    // Must match the radio keys in R.xml.yrrp_fold_animation_settings.
    static final String KEY_STOCK = "yrrp_fold_animation_stock";
    static final String KEY_PIXEL = "yrrp_fold_animation_pixel";
    static final String KEY_DUO = "yrrp_fold_animation_duo";

    private final int mMode;
    private final YrrpSettingsStore mStore;
    private final YrrpSecureSettingObserver mObserver;
    private final BooleanSupplier mSupported;

    public YrrpFoldAnimationPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        this(
                context,
                preferenceKey,
                new YrrpSettingsStore(context),
                () -> YrrpFoldAnimationAvailability.isAvailable(context));
    }

    /** Injects the store and the device check, so tests can count writes and flip availability. */
    @VisibleForTesting
    YrrpFoldAnimationPreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey,
            @NonNull YrrpSettingsStore store,
            @NonNull BooleanSupplier supported) {
        super(context, preferenceKey);
        mMode = keyToMode(preferenceKey);
        mStore = store;
        mSupported = supported;
        mObserver =
                new YrrpSecureSettingObserver(
                        context, this, mStore, YrrpSettingsStore.FOLD_ANIMATION);
    }

    @Override
    public int getAvailabilityStatus() {
        return mSupported.getAsBoolean() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
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

    /** Checks this row when it matches the normalized stored style; an unknown value is kept. */
    @Override
    public void updateState(@Nullable Preference preference) {
        if (preference instanceof SelectorWithWidgetPreference) {
            ((SelectorWithWidgetPreference) preference)
                    .setChecked(mStore.getFoldAnimation() == mMode);
        }
    }

    /**
     * Writes this row's style; every row, this one included, refreshes from its observer. A failed
     * write is logged by the store and leaves the selection unchanged.
     */
    @Override
    public void onRadioButtonClicked(@NonNull SelectorWithWidgetPreference preference) {
        mStore.setFoldAnimation(mMode);
    }

    private static int keyToMode(@NonNull String key) {
        switch (key) {
            case KEY_STOCK:
                return YrrpSettingsStore.FOLD_STOCK;
            case KEY_PIXEL:
                return YrrpSettingsStore.FOLD_PIXEL;
            case KEY_DUO:
                return YrrpSettingsStore.FOLD_DUO;
            default:
                throw new IllegalArgumentException("unknown fold animation key");
        }
    }
}
