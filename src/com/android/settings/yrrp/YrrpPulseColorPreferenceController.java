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
 * Shows the Pulse color and opacity and opens the picker; writes only values confirmed in it.
 *
 * <p>The row stays visible but disabled while Pulse is off.
 */
public class YrrpPulseColorPreferenceController extends BasePreferenceController
        implements DefaultLifecycleObserver {

    private static final String TAG = "YrrpPulseColorPref";

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
                        YrrpSettingsStore.PULSE_ALPHA,
                        YrrpSettingsStore.PULSE_ENABLED);
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
        // Pulse switch can change on this page. updateState() toggles enabled instead.
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
        preference.setEnabled(mStore.isPulseEnabled());
        if (preference instanceof ColorPreference) {
            showColor(
                    (ColorPreference) preference,
                    YrrpSettingsStore.toArgb(mStore.getPulseColor(), mStore.getPulseAlpha()));
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
     * swatch's content description, is {@code #AARRGGBB} and the swatch is the translucent color
     * over ColorPreference's checkerboard. The preference is not persistent, so nothing is stored.
     */
    private static void showColor(@NonNull ColorPreference preference, int argb) {
        preference.setValues(new int[] {argb});
        preference.setTitles(new CharSequence[] {YrrpSettingsStore.formatArgb(argb)});
        preference.setValue(argb);
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
        YrrpColorPickerDialogFragment.newInstance(mStore.getPulseColor(), mStore.getPulseAlpha())
                .showNow(manager, YrrpColorPickerDialogFragment.TAG);
    }

    /**
     * The result bundle is untrusted input: both keys are required, the RGB bits are masked and the
     * alpha is clamped. A result missing either key writes nothing.
     */
    private void onColorConfirmed(@NonNull Bundle result) {
        if (result.containsKey(YrrpColorPickerDialogFragment.RESULT_RGB)
                && result.containsKey(YrrpColorPickerDialogFragment.RESULT_ALPHA)) {
            writeColor(
                    YrrpSettingsStore.normalizeColor(
                            result.getInt(YrrpColorPickerDialogFragment.RESULT_RGB)),
                    YrrpSettingsStore.normalizeAlpha(
                            result.getInt(YrrpColorPickerDialogFragment.RESULT_ALPHA)));
        }
        // On failure the store logs the key; either way the row re-reads both persisted values.
        mObserver.refresh();
    }

    /**
     * Writes the color, then the opacity only if the color was stored. The two secure settings
     * writes are separate, so an observer can briefly see the new color with the old opacity.
     */
    private void writeColor(int rgb, int alpha) {
        if (mStore.setPulseColor(rgb)) {
            mStore.setPulseAlpha(alpha);
        }
    }
}
