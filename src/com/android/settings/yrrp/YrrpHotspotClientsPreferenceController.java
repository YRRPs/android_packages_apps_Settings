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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Connected devices category on the Wi-Fi hotspot page: one row per client while the hotspot
 * is on, hidden while it is off. Tapping a row shows its details and, when the module can
 * force-disconnect, a Block button.
 *
 * <p>Row keys are indexes, never MACs: Settings logs the key of every clicked preference to the
 * events buffer and statsd.
 */
public class YrrpHotspotClientsPreferenceController extends BasePreferenceController
        implements YrrpHotspotMonitor.Listener, DefaultLifecycleObserver {
    private static final String ROW_PREFIX = "yrrp_hotspot_client_";
    private static final String EMPTY_KEY = "yrrp_hotspot_clients_empty";

    private final Map<String, YrrpHotspotClients.Row> mRows = new HashMap<>();
    private Fragment mHost;
    private YrrpHotspotMonitor mMonitor;
    private PreferenceCategory mCategory;
    private AlertDialog mDialog;

    public YrrpHotspotClientsPreferenceController(@NonNull Context context, @NonNull String key) {
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

    /** Closes the details dialog, so it neither leaks nor acts on a stopped page. */
    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    @Override
    public void onHotspotChanged() {
        if (mCategory == null) {
            return;
        }
        final boolean apEnabled = mMonitor != null && mMonitor.apEnabled();
        mCategory.setVisible(apEnabled);
        mCategory.removeAll();
        mRows.clear();
        if (!apEnabled) {
            return;
        }
        final List<YrrpHotspotClients.Row> rows = mMonitor.rows();
        for (int i = 0; i < rows.size(); i++) {
            final String key = ROW_PREFIX + i;
            mRows.put(key, rows.get(i));
            mCategory.addPreference(rowPreference(key, rows.get(i)));
        }
        if (rows.isEmpty()) {
            final Preference empty = new Preference(mContext);
            empty.setKey(EMPTY_KEY);
            empty.setTitle(R.string.yrrp_hotspot_clients_empty);
            empty.setSelectable(false);
            mCategory.addPreference(empty);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(@NonNull Preference preference) {
        final String key = preference.getKey();
        final YrrpHotspotClients.Row row = key == null ? null : mRows.get(key);
        if (row == null || mCategory == null || mCategory.findPreference(key) != preference) {
            return false;
        }
        showDetails(row, preference.getTitle());
        return true;
    }

    private Preference rowPreference(String key, YrrpHotspotClients.Row row) {
        final Preference preference = new Preference(mContext);
        preference.setKey(key);
        preference.setTitle(row.hostname != null
                ? row.hostname : mContext.getString(R.string.yrrp_hotspot_client_unknown));
        preference.setSummary(row.ipv4 != null
                ? mContext.getString(R.string.yrrp_hotspot_client_summary, row.ipv4, row.mac)
                : row.mac);
        return preference;
    }

    private void showDetails(YrrpHotspotClients.Row row, CharSequence title) {
        if (mHost == null || !mHost.isAdded() || mMonitor == null || mDialog != null) {
            return;
        }
        final boolean blockAllowed = YrrpHotspotDump.blockAllowed(mMonitor.snapshot());
        String message = row.ipv4 != null
                ? mContext.getString(R.string.yrrp_hotspot_client_details_ip, row.ipv4, row.mac)
                : mContext.getString(R.string.yrrp_hotspot_client_details, row.mac);
        if (blockAllowed) {
            message += "\n\n" + mContext.getString(R.string.yrrp_hotspot_block_note);
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(mHost.requireContext())
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(R.string.yrrp_hotspot_close, /* listener= */ null)
                .setOnDismissListener(dialog -> mDialog = null);
        if (blockAllowed) {
            builder.setPositiveButton(R.string.yrrp_hotspot_block,
                    (dialog, which) -> block(row.mac));
        }
        mDialog = builder.show();
    }

    private void block(String mac) {
        if (!mMonitor.writer().block(mac)) {
            Toast.makeText(mContext, R.string.yrrp_hotspot_write_failed, Toast.LENGTH_SHORT)
                    .show();
        }
    }
}
