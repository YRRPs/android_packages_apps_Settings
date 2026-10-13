/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.List;

/**
 * The blocked MACs on {@link YrrpHotspotBlockedSettings}. Tapping one asks to unblock it; the
 * client can then reconnect at once.
 */
public class YrrpHotspotBlockedListPreferenceController extends BasePreferenceController
        implements YrrpHotspotMonitor.Listener {
    private static final String ROW_PREFIX = "yrrp_hotspot_blocked_mac_";
    private static final String EMPTY_KEY = "yrrp_hotspot_blocked_empty";

    private Fragment mHost;
    private YrrpHotspotMonitor mMonitor;
    private PreferenceCategory mCategory;

    public YrrpHotspotBlockedListPreferenceController(
            @NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    /** Binds the page and its monitor; call from the page's {@code onAttach()}. */
    void init(@NonNull Fragment host, @NonNull YrrpHotspotMonitor monitor) {
        mHost = host;
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
        mCategory = screen.findPreference(getPreferenceKey());
        onHotspotChanged();
    }

    @Override
    public void onHotspotChanged() {
        if (mCategory == null || mMonitor == null) {
            return;
        }
        mCategory.removeAll();
        final List<String> blocked = mMonitor.blocked();
        for (String mac : blocked) {
            final Preference row = new Preference(mContext);
            row.setKey(ROW_PREFIX + mac);
            row.setTitle(mac);
            mCategory.addPreference(row);
        }
        if (blocked.isEmpty()) {
            final Preference empty = new Preference(mContext);
            empty.setKey(EMPTY_KEY);
            empty.setTitle(R.string.yrrp_hotspot_blocked_empty);
            empty.setSelectable(false);
            mCategory.addPreference(empty);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(@NonNull Preference preference) {
        final String key = preference.getKey();
        if (key == null || !key.startsWith(ROW_PREFIX) || mCategory == null
                || mCategory.findPreference(key) != preference) {
            return false;
        }
        confirmUnblock(key.substring(ROW_PREFIX.length()));
        return true;
    }

    private void confirmUnblock(String mac) {
        if (mHost == null || !mHost.isAdded() || mMonitor == null) {
            return;
        }
        new AlertDialog.Builder(mHost.requireContext())
                .setTitle(mContext.getString(R.string.yrrp_hotspot_unblock_title, mac))
                .setPositiveButton(R.string.yrrp_hotspot_unblock, (dialog, which) -> {
                    if (!mMonitor.writer().unblock(mac)) {
                        Toast.makeText(mContext, R.string.yrrp_hotspot_write_failed,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, /* listener= */ null)
                .show();
    }
}
