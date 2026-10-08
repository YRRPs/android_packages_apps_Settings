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
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.accessibility.ColorPreference;
import com.android.settings.core.BasePreferenceController;

/**
 * Shows the Pulse RGB color and opens the picker; writes only a value confirmed in it.
 *
 * <p>The color applies only in the Solid color mode, so the row stays visible but is disabled while
 * Pulse is off or another color mode is selected. Opacity has its own row.
 */
public class YrrpPulseColorPreferenceController extends BasePreferenceController
        implements DefaultLifecycleObserver {

    private static final String TAG = "YrrpPulseColorPref";
    private static final int OPAQUE = 0xFF000000;

    private final YrrpSettingsStore mStore;
    private final YrrpSecureSettingObserver mObserver;
    private @Nullable Fragment mHost;

    public YrrpPulseColorPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        this(context, preferenceKey, new YrrpSettingsStore(context));
    }

    /** Injects the store, so tests can count writes and make them fail. */
    @VisibleForTesting
    YrrpPulseColorPreferenceController(
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
                        YrrpSettingsStore.PULSE_COLOR,
                        YrrpSettingsStore.PULSE_ENABLED,
                        YrrpSettingsStore.PULSE_COLOR_MODE);
    }

    /**
     * Binds the hosting page, which shows the picker and delivers its result. Call from the host's
     * {@code onAttach()} so the listener is registered again after recreation and a pending
     * confirmed result still arrives.
     */
    public void init(@NonNull Fragment host) {
        mHost = host;
        host.getChildFragmentManager()
                .setFragmentResultListener(
                        YrrpColorPickerDialogFragment.RESULT_KEY,
                        host,
                        (requestKey, result) -> onColorConfirmed(result));
    }

    @Override
    public int getAvailabilityStatus() {
        // Not DISABLED_DEPENDENT_SETTING: that is applied once in displayPreference, while the
        // Pulse switch and color mode can change on this page. updateState() toggles enabled.
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

    @Override
    public void updateState(@Nullable Preference preference) {
        if (preference == null) {
            return;
        }
        preference.setEnabled(
                mStore.isPulseEnabled()
                        && mStore.getPulseColorMode() == YrrpSettingsStore.PULSE_COLOR_MODE_SOLID);
        if (preference instanceof ColorPreference) {
            showColor((ColorPreference) preference, mStore.getPulseColor());
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(@NonNull Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        showPicker();
        return true;
    }

    /** Not exposed as a slice, so the setting stays reachable only through this page. */
    @Override
    public boolean isSliceable() {
        return false;
    }

    /**
     * The row's swatch and summary come from a one-entry list, so the summary, which is also the
     * swatch's content description, is {@code #RRGGBB} and the swatch is the opaque color. The
     * preference is not persistent, so nothing is stored.
     */
    private static void showColor(@NonNull ColorPreference preference, int rgb) {
        final int opaque = OPAQUE | rgb;
        preference.setValues(new int[] {opaque});
        preference.setTitles(new CharSequence[] {YrrpSettingsStore.formatRgb(rgb)});
        preference.setValue(opaque);
    }

    private void showPicker() {
        if (mHost == null) {
            Log.w(TAG, "init(Fragment) not called");
            return;
        }
        if (!mHost.isAdded()) {
            return;
        }
        final FragmentManager manager = mHost.getChildFragmentManager();
        if (manager.isStateSaved()
                || manager.findFragmentByTag(YrrpColorPickerDialogFragment.TAG) != null) {
            return;
        }
        YrrpColorPickerDialogFragment.newInstance(mStore.getPulseColor())
                .showNow(manager, YrrpColorPickerDialogFragment.TAG);
    }

    /** The result bundle is untrusted input: the RGB key is required and its bits are masked. */
    private void onColorConfirmed(@NonNull Bundle result) {
        if (result.containsKey(YrrpColorPickerDialogFragment.RESULT_RGB)) {
            mStore.setPulseColor(
                    YrrpSettingsStore.normalizeColor(
                            result.getInt(YrrpColorPickerDialogFragment.RESULT_RGB)));
        }
        // On failure the store logs the key; either way the row re-reads the persisted value.
        mObserver.refresh();
    }
}
