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
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.function.BooleanSupplier;

/**
 * Opens the fold animation page and summarizes the current user's style. Hidden unless the
 * device is a supported foldable, see {@link YrrpFoldAnimationAvailability}.
 */
public class YrrpFoldAnimationEntryPreferenceController extends BasePreferenceController
        implements DefaultLifecycleObserver {

    private final YrrpSettingsStore mStore;
    private final YrrpSecureSettingObserver mObserver;
    private final BooleanSupplier mSupported;

    public YrrpFoldAnimationEntryPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        this(context, preferenceKey, () -> YrrpFoldAnimationAvailability.isAvailable(context));
    }

    /** Injects the device check, so tests can flip availability. */
    @VisibleForTesting
    YrrpFoldAnimationEntryPreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey,
            @NonNull BooleanSupplier supported) {
        super(context, preferenceKey);
        mSupported = supported;
        mStore = new YrrpSettingsStore(context);
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

    /** Summarizes the normalized stored style; an unknown stored value shows Stock. */
    @Override
    public CharSequence getSummary() {
        return mContext.getText(labelFor(mStore.getFoldAnimation()));
    }

    /** The row label for a normalized style. */
    @StringRes
    static int labelFor(int style) {
        switch (style) {
            case YrrpSettingsStore.FOLD_PIXEL:
                return R.string.yrrp_fold_animation_pixel;
            case YrrpSettingsStore.FOLD_DUO:
                return R.string.yrrp_fold_animation_duo;
            default:
                return R.string.yrrp_fold_animation_stock;
        }
    }
}
