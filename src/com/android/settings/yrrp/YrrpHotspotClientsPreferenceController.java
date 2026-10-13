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
 * The Connected devices category on the Wi-Fi hotspot page: one row per client while the hotspot
 * is on, hidden while it is off. Tapping a row shows its details and, when the module can
 * force-disconnect, a Block button.
 */
public class YrrpHotspotClientsPreferenceController extends BasePreferenceController
        implements YrrpHotspotMonitor.Listener {
    private static final String ROW_PREFIX = "yrrp_hotspot_client_";
    private static final String EMPTY_KEY = "yrrp_hotspot_clients_empty";

    private Fragment mHost;
    private YrrpHotspotMonitor mMonitor;
    private PreferenceCategory mCategory;

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

    @Override
    public void onHotspotChanged() {
        if (mCategory == null) {
            return;
        }
        final boolean apEnabled = mMonitor != null && mMonitor.snapshot().apEnabled;
        mCategory.setVisible(apEnabled);
        mCategory.removeAll();
        if (!apEnabled) {
            return;
        }
        final List<YrrpHotspotClients.Row> rows = mMonitor.rows();
        for (YrrpHotspotClients.Row row : rows) {
            mCategory.addPreference(rowPreference(row));
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
        if (key == null || !key.startsWith(ROW_PREFIX) || mCategory == null
                || mCategory.findPreference(key) != preference) {
            return false;
        }
        showDetails(key.substring(ROW_PREFIX.length()), preference.getTitle());
        return true;
    }

    private Preference rowPreference(YrrpHotspotClients.Row row) {
        final Preference preference = new Preference(mContext);
        preference.setKey(ROW_PREFIX + row.mac);
        preference.setTitle(row.hostname != null
                ? row.hostname : mContext.getString(R.string.yrrp_hotspot_client_unknown));
        preference.setSummary(row.ipv4 != null
                ? mContext.getString(R.string.yrrp_hotspot_client_summary, row.ipv4, row.mac)
                : row.mac);
        return preference;
    }

    private void showDetails(String mac, CharSequence title) {
        if (mHost == null || !mHost.isAdded() || mMonitor == null) {
            return;
        }
        final String ipv4 = ipv4Of(mac);
        final boolean blockAllowed = YrrpHotspotDump.blockAllowed(mMonitor.snapshot());
        String message = ipv4 != null
                ? mContext.getString(R.string.yrrp_hotspot_client_details_ip, ipv4, mac)
                : mContext.getString(R.string.yrrp_hotspot_client_details, mac);
        if (blockAllowed) {
            message += "\n\n" + mContext.getString(R.string.yrrp_hotspot_block_note);
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(mHost.requireContext())
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(R.string.yrrp_hotspot_close, /* listener= */ null);
        if (blockAllowed) {
            builder.setPositiveButton(R.string.yrrp_hotspot_block, (dialog, which) -> block(mac));
        }
        builder.show();
    }

    private String ipv4Of(String mac) {
        for (YrrpHotspotClients.Row row : mMonitor.rows()) {
            if (row.mac.equals(mac)) {
                return row.ipv4;
            }
        }
        return null;
    }

    private void block(String mac) {
        if (!mMonitor.writer().block(mac)) {
            Toast.makeText(mContext, R.string.yrrp_hotspot_write_failed, Toast.LENGTH_SHORT)
                    .show();
        }
    }
}
