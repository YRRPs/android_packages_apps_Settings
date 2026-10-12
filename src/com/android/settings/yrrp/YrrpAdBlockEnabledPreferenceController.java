/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/**
 * The Use ad blocking switch. Writes go through the same path as the Quick Settings tile; the
 * switch re-reads on start, so a change made from the tile shows when the page returns.
 */
public class YrrpAdBlockEnabledPreferenceController extends TogglePreferenceController
        implements DefaultLifecycleObserver {
    private Preference mPreference;

    public YrrpAdBlockEnabledPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return YrrpAdBlockAndroid.isPrimaryUser(mContext) ? AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

    @Override
    public boolean isChecked() {
        return YrrpAdBlockAndroid.state().isEnabled();
    }

    /** Returns false on a refused write, so the switch stays unchanged. */
    @Override
    public boolean setChecked(boolean isChecked) {
        return YrrpAdBlockAndroid.setEnabled(mContext, isChecked);
    }

    @Override
    public boolean isSliceable() {
        return false;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.yrrp_menu_key;
    }
}
