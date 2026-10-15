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
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.yrrp.YrrpFeatureAvailability.HotspotControls;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;

/** Read-only inventory of YRRP feature availability on this build and device. */
@SearchIndexable
public class YrrpFeatureInventorySettings extends DashboardFragment {
    private static final String TAG = "YrrpFeatureInventory";

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.yrrp_feature_inventory_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PAGE_UNKNOWN;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final YrrpHotspotMonitor monitor = new YrrpHotspotMonitor(context);
        final YrrpFeatureEnvironment environment =
                YrrpFeatureEnvironment.AndroidEnvironment.create(
                        context, () -> hotspotControls(monitor.snapshot()));
        final YrrpFeatureInventoryController controller =
                new YrrpFeatureInventoryController(context, environment, monitor);
        getSettingsLifecycle().addObserver(monitor);
        getSettingsLifecycle().addObserver(controller);
        return List.of(controller);
    }

    private static HotspotControls hotspotControls(
            @NonNull YrrpHotspotDump.Snapshot snapshot) {
        if (!snapshot.capabilityKnown) {
            return HotspotControls.UNDETERMINED;
        }
        return snapshot.forceDisconnectSupported
                ? HotspotControls.SUPPORTED : HotspotControls.UNSUPPORTED;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.yrrp_feature_inventory_settings);
}
