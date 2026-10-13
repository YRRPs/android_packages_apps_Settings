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
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.List;

/**
 * The blocked MACs on {@link YrrpHotspotBlockedSettings}. Tapping one asks to unblock it; the
 * client can then reconnect at once. Row keys come from {@link YrrpHotspotRowKeys}: indexes, never
 * MACs, resolved by key on tap, and rebuilt only when the list changes.
 */
public class YrrpHotspotBlockedListPreferenceController extends BasePreferenceController
        implements YrrpHotspotMonitor.Listener, DefaultLifecycleObserver {
    private static final String ROW_PREFIX = "yrrp_hotspot_blocked_mac_";
    private static final String EMPTY_KEY = "yrrp_hotspot_blocked_empty";

    private final YrrpHotspotRowKeys<String> mKeys = new YrrpHotspotRowKeys<>(ROW_PREFIX);
    private Fragment mHost;
    private YrrpHotspotMonitor mMonitor;
    private PreferenceCategory mCategory;
    private AlertDialog mDialog;

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
        mKeys.reset();
        onHotspotChanged();
    }

    /** Closes the unblock dialog, so it neither leaks nor acts on a stopped page. */
    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    @Override
    public void onHotspotChanged() {
        if (mCategory == null || mMonitor == null) {
            return;
        }
        final List<String> blocked = mMonitor.blocked();
        if (!mKeys.update(blocked)) {
            return;
        }
        mCategory.removeAll();
        for (int i = 0; i < blocked.size(); i++) {
            final Preference row = new Preference(mContext);
            row.setKey(mKeys.key(i));
            row.setTitle(blocked.get(i));
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
        // By key only: the tapped object can be a detached row whose view was never rebound.
        final String mac = mKeys.get(preference.getKey());
        if (mac == null) {
            return false;
        }
        confirmUnblock(mac);
        return true;
    }

    private void confirmUnblock(String mac) {
        if (mHost == null || !mHost.isAdded() || mMonitor == null || mDialog != null) {
            return;
        }
        mDialog = new AlertDialog.Builder(mHost.requireContext())
                .setTitle(mContext.getString(R.string.yrrp_hotspot_unblock_title, mac))
                .setPositiveButton(R.string.yrrp_hotspot_unblock, (dialog, which) -> {
                    if (!mMonitor.writer().unblock(mac)) {
                        Toast.makeText(mContext, R.string.yrrp_hotspot_write_failed,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, /* listener= */ null)
                .setOnDismissListener(dialog -> mDialog = null)
                .show();
    }
}
