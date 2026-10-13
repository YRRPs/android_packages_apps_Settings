/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

/**
 * The Client controls category on the Wi-Fi hotspot page. Hidden while neither of its rows is
 * shown, so the page never shows an empty header.
 */
public class YrrpHotspotControlsCategoryPreferenceController extends BasePreferenceController
        implements YrrpHotspotMonitor.Listener {
    private YrrpHotspotMonitor mMonitor;
    private @Nullable Preference mPreference;

    public YrrpHotspotControlsCategoryPreferenceController(
            @NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    /** Binds the page's monitor; call from the page's {@code onAttach()}. */
    void init(@NonNull YrrpHotspotMonitor monitor) {
        mMonitor = monitor;
        monitor.addListener(this);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        onHotspotChanged();
    }

    @Override
    public void onHotspotChanged() {
        if (mPreference != null && mMonitor != null) {
            mPreference.setVisible(YrrpHotspotDump.controlsVisible(mMonitor.snapshot()));
        }
    }
}
