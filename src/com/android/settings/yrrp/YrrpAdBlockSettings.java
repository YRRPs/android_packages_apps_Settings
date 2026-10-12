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

/** The ad blocking page. Behavior lives in the page's preference controllers. */
@SearchIndexable
public class YrrpAdBlockSettings extends DashboardFragment {
    private static final String TAG = "YrrpAdBlock";

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Runs on every attach, so the add-dialog result listeners survive recreation.
        bind(use(YrrpAdBlockSourcesPreferenceController.class));
        bind(use(YrrpAdBlockAllowlistPreferenceController.class));
    }

    private void bind(YrrpAdBlockListPreferenceController controller) {
        if (controller != null) {
            controller.init(this);
        }
    }

    @Override
    public int getMetricsCategory() {
        // Deliberate: a dedicated SettingsEnums value would need a public framework constant.
        return SettingsEnums.PAGE_UNKNOWN;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.yrrp_ad_block_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    /** For Search. */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.yrrp_ad_block_settings);
}
