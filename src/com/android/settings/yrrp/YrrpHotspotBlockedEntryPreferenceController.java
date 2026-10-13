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

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/**
 * The Blocked devices row on the Wi-Fi hotspot page, opening {@link YrrpHotspotBlockedSettings}.
 * Shown when the module can force-disconnect, and also whenever the list is non-empty, so a
 * restored list can always be cleared.
 */
public class YrrpHotspotBlockedEntryPreferenceController extends BasePreferenceController
        implements YrrpHotspotMonitor.Listener {
    private YrrpHotspotMonitor mMonitor;
    private @Nullable Preference mPreference;

    public YrrpHotspotBlockedEntryPreferenceController(
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
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

    @Override
    public void updateState(@Nullable Preference preference) {
        if (preference == null || mMonitor == null) {
            return;
        }
        final YrrpHotspotDump.Snapshot snapshot = mMonitor.snapshot();
        preference.setVisible(snapshot.forceDisconnectSupported || snapshot.blocked > 0);
        preference.setSummary(snapshot.blocked == 0
                ? mContext.getString(R.string.yrrp_hotspot_blocked_none)
                : mContext.getResources().getQuantityString(
                        R.plurals.yrrp_hotspot_devices, snapshot.blocked, snapshot.blocked));
    }
}
