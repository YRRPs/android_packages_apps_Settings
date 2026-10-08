/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.yrrp;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceGroup;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/** The Pulse page. Setting behavior lives in the page's preference controllers. */
@SearchIndexable
public class YrrpPulseSettings extends DashboardFragment {
    private static final String TAG = "YrrpPulseSettings";
    private static final String KEY_ADVANCED = "yrrp_pulse_advanced";

    // Keep getInitialExpandedChildCount() at its default 0: a collapsible screen around the
    // collapsible Advanced group makes PreferenceGroupAdapter throw on nested expandable groups.
    @Override
    public void onCreate(@Nullable Bundle icicle) {
        super.onCreate(icicle);
        expandAdvancedForHighlight(getPreferenceScreen(), getArguments());
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Runs on every attach, so the color result listener survives recreation.
        final YrrpPulseColorPreferenceController colorController =
                use(YrrpPulseColorPreferenceController.class);
        if (colorController != null) {
            colorController.init(this);
        }
    }

    @Override
    public int getMetricsCategory() {
        // Deliberate: a dedicated SettingsEnums value would need a public framework constant.
        return SettingsEnums.PAGE_UNKNOWN;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.yrrp_pulse_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    /**
     * Expands the collapsed Advanced group when the page opens to highlight a row, as
     * SettingsPreferenceFragment does for the screen itself; otherwise the highlighted row would
     * stay hidden behind the expand button.
     */
    @VisibleForTesting
    static void expandAdvancedForHighlight(
            @Nullable PreferenceGroup screen, @Nullable Bundle arguments) {
        if (screen == null || arguments == null) {
            return;
        }
        if (TextUtils.isEmpty(arguments.getString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY))) {
            return;
        }
        final PreferenceGroup advanced = screen.findPreference(KEY_ADVANCED);
        if (advanced != null) {
            advanced.setInitialExpandedChildrenCount(Integer.MAX_VALUE);
        }
    }

    /** For Search. */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.yrrp_pulse_settings);
}
