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
 * The Connected devices category on the Wi-Fi hotspot page: one row per client while the hotspot
 * is on, hidden while it is off. Tapping a row shows its details and, when the module can
 * force-disconnect, a Block button.
 *
 * <p>Row keys come from {@link YrrpHotspotRowKeys}: indexes, never MACs, resolved by key on tap,
 * and rebuilt only when the rows change.
 */
public class YrrpHotspotClientsPreferenceController extends BasePreferenceController
        implements YrrpHotspotMonitor.Listener, DefaultLifecycleObserver {
    private static final String ROW_PREFIX = "yrrp_hotspot_client_";
    private static final String EMPTY_KEY = "yrrp_hotspot_clients_empty";

    private final YrrpHotspotRowKeys<YrrpHotspotClients.Row> mKeys =
            new YrrpHotspotRowKeys<>(ROW_PREFIX);
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
        mKeys.reset();
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
        if (!apEnabled) {
            mCategory.removeAll();
            mKeys.reset();
            return;
        }
        final List<YrrpHotspotClients.Row> rows = mMonitor.rows();
        if (!mKeys.update(rows)) {
            return;
        }
        mCategory.removeAll();
        for (int i = 0; i < rows.size(); i++) {
            mCategory.addPreference(rowPreference(mKeys.key(i), rows.get(i)));
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
        // By key only: the tapped object can be a detached row whose view was never rebound.
        final YrrpHotspotClients.Row row = mKeys.get(preference.getKey());
        if (row == null) {
            return false;
        }
        showDetails(row);
        return true;
    }

    private Preference rowPreference(String key, YrrpHotspotClients.Row row) {
        final Preference preference = new Preference(mContext);
        preference.setKey(key);
        preference.setTitle(title(row));
        preference.setSummary(row.ipv4 != null
                ? mContext.getString(R.string.yrrp_hotspot_client_summary, row.ipv4, row.mac)
                : row.mac);
        return preference;
    }

    private void showDetails(YrrpHotspotClients.Row row) {
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
                .setTitle(title(row))
                .setMessage(message)
                .setNegativeButton(R.string.yrrp_hotspot_close, /* listener= */ null)
                .setOnDismissListener(dialog -> mDialog = null);
        if (blockAllowed) {
            builder.setPositiveButton(R.string.yrrp_hotspot_block,
                    (dialog, which) -> block(row.mac));
        }
        mDialog = builder.show();
    }

    private String title(YrrpHotspotClients.Row row) {
        return row.hostname != null
                ? row.hostname : mContext.getString(R.string.yrrp_hotspot_client_unknown);
    }

    private void block(String mac) {
        if (!mMonitor.writer().block(mac)) {
            Toast.makeText(mContext, R.string.yrrp_hotspot_write_failed, Toast.LENGTH_SHORT)
                    .show();
        }
    }
}
