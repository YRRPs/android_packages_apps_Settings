/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/** Wi-Fi hotspot → Blocked devices: the MACs the hotspot refuses, each with Unblock. */
@SearchIndexable
public class YrrpHotspotBlockedSettings extends DashboardFragment {
    private static final String TAG = "YrrpHotspot";

    private YrrpHotspotMonitor mMonitor;

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

    /** For Search. */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.yrrp_hotspot_blocked_settings);
}
