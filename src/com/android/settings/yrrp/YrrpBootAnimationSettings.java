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

/** The boot animation page. Setting behavior lives in the page's preference controllers. */
@SearchIndexable
public class YrrpBootAnimationSettings extends DashboardFragment {
    private static final String TAG = "YrrpBootAnimation";

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Runs on every attach, so the Finish intro result listener survives recreation.
        final YrrpBootAnimationMatrixPreferenceController matrix =
                use(YrrpBootAnimationMatrixPreferenceController.class);
        if (matrix != null) {
            matrix.init(this);
        }
    }

    @Override
    public int getMetricsCategory() {
        // Deliberate: a dedicated SettingsEnums value would need a public framework constant.
        return SettingsEnums.PAGE_UNKNOWN;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.yrrp_boot_animation_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    /** For Search. */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.yrrp_boot_animation_settings);
}
