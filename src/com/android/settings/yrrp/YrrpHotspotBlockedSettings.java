/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.UserManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.wifi.WifiUtils;
import com.android.settingslib.TetherUtil;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.wifi.WifiEnterpriseRestrictionUtils;

/**
 * Wi-Fi hotspot → Blocked devices: the MACs the hotspot refuses, each with Unblock. It edits the
 * hotspot configuration, so it applies the same restrictions as {@code WifiTetherSettings}: admin
 * users only, {@code DISALLOW_CONFIG_TETHERING}, and the tethering and Wi-Fi tethering policies.
 */
@SearchIndexable
public class YrrpHotspotBlockedSettings extends RestrictedDashboardFragment {
    private static final String TAG = "YrrpHotspot";

    private YrrpHotspotMonitor mMonitor;
    private boolean mUnavailable;

    public YrrpHotspotBlockedSettings() {
        super(UserManager.DISALLOW_CONFIG_TETHERING);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (mMonitor == null) {
            mMonitor = new YrrpHotspotMonitor(context);
            getSettingsLifecycle().addObserver(mMonitor);
        }
        final YrrpHotspotBlockedListPreferenceController list =
                use(YrrpHotspotBlockedListPreferenceController.class);
        if (list != null) {
            list.init(this, mMonitor);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle icicle) {
        super.onCreate(icicle);
        if (!WifiUtils.canShowWifiHotspot(getContext())) {
            finish();
            return;
        }
        setIfOnlyAvailableForAdmins(true);
        mUnavailable = isUiRestricted() || !isHotspotAllowed(getContext());
    }

    /** Matches {@code WifiTetherSettings.onStart}: a restricted page shows no rows. */
    @Override
    public void onStart() {
        super.onStart();
        if (mUnavailable && getPreferenceScreen() != null) {
            getPreferenceScreen().removeAll();
        }
    }

    @Override
    public int getMetricsCategory() {
        // Deliberate: a dedicated SettingsEnums value would need a public framework constant.
        return SettingsEnums.PAGE_UNKNOWN;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.yrrp_hotspot_blocked_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    private static boolean isHotspotAllowed(@Nullable Context context) {
        return context != null && TetherUtil.isTetherAvailable(context)
                && WifiEnterpriseRestrictionUtils.isWifiTetheringAllowed(context);
    }

    /** For Search: only where the hotspot page itself is searchable and usable. */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.yrrp_hotspot_blocked_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    if (context == null || !WifiUtils.canShowWifiHotspot(context)) {
                        return false;
                    }
                    final UserManager userManager = context.getSystemService(UserManager.class);
                    return userManager != null && userManager.isAdminUser()
                            && isHotspotAllowed(context);
                }
            };
}
