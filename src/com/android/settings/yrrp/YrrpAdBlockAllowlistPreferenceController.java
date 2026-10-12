/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settings.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Host names the hosts file never blocks. Tapping a row offers to remove it. */
public class YrrpAdBlockAllowlistPreferenceController extends YrrpAdBlockListPreferenceController {
    private static final String REQUEST_KEY = "yrrp_ad_block_add_allowed";
    /** Row keys are prefixed so a host name cannot collide with the Add row's key. */
    private static final String ROW_PREFIX = "allowed:";

    public YrrpAdBlockAllowlistPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    String requestKey() {
        return REQUEST_KEY;
    }

    @Override
    YrrpAdBlockTextInputDialogFragment newAddDialog() {
        return YrrpAdBlockTextInputDialogFragment.forHost(REQUEST_KEY);
    }

    @Override
    int addTitle() {
        return R.string.yrrp_ad_block_add_allowed;
    }

    @Override
    List<Preference> rows(YrrpAdBlockStore.State state) {
        final List<Preference> rows = new ArrayList<>();
        for (String host : state.allowlist) {
            final Preference row = new Preference(mContext);
            row.setKey(ROW_PREFIX + host);
            row.setTitle(host);
            row.setPersistent(false);
            rows.add(row);
        }
        return rows;
    }

    @Override
    YrrpAdBlockStore.AddResult add(YrrpAdBlockStore.State state, String text) {
        return state.addAllowed(text);
    }

    @Override
    void remove(YrrpAdBlockUpdater updater, String rowKey) throws IOException {
        final String host = rowKey.substring(ROW_PREFIX.length());
        updater.edit(state -> state.removeAllowed(host));
    }
}
