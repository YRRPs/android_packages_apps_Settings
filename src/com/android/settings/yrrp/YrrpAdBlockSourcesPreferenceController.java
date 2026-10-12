/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.PrimarySwitchPreference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The hosts lists: one row per list, titled by its host, with a switch that includes or
 * excludes it. Tapping the row offers to remove it; the last row adds an https list.
 */
public class YrrpAdBlockSourcesPreferenceController extends YrrpAdBlockListPreferenceController {
    private static final String REQUEST_KEY = "yrrp_ad_block_add_source";

    public YrrpAdBlockSourcesPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    String requestKey() {
        return REQUEST_KEY;
    }

    @Override
    YrrpAdBlockTextInputDialogFragment newAddDialog() {
        return YrrpAdBlockTextInputDialogFragment.forUrl(REQUEST_KEY);
    }

    @Override
    int addTitle() {
        return R.string.yrrp_ad_block_add_source;
    }

    @Override
    List<Preference> rows(YrrpAdBlockStore.State state) {
        final List<Preference> rows = new ArrayList<>();
        for (YrrpAdBlockSource source : state.sources) {
            final PrimarySwitchPreference row = new PrimarySwitchPreference(mContext);
            row.setKey(source.url);
            row.setTitle(source.host());
            row.setSummary(mContext.getString(R.string.yrrp_ad_block_source_summary,
                    source.status, source.entries));
            row.setChecked(source.enabled);
            row.setPersistent(false);
            row.setOnPreferenceChangeListener((preference, value) -> {
                final boolean enabled = (Boolean) value;
                saveAndRebuild(updater -> updater.edit(s -> setEnabled(s, source.url, enabled)));
                return true;
            });
            rows.add(row);
        }
        return rows;
    }

    @Override
    YrrpAdBlockStore.AddResult add(YrrpAdBlockStore.State state, String text) {
        return state.addSource(text);
    }

    @Override
    void remove(YrrpAdBlockUpdater updater, String rowKey) throws IOException {
        updater.removeSource(rowKey);
    }

    private static YrrpAdBlockStore.AddResult setEnabled(
            YrrpAdBlockStore.State state, String url, boolean enabled) {
        final YrrpAdBlockSource source = state.findSource(url);
        if (source == null) {
            return YrrpAdBlockStore.AddResult.MALFORMED;
        }
        source.enabled = enabled;
        return YrrpAdBlockStore.AddResult.OK;
    }
}
