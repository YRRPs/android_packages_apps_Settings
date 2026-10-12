/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.utils.ThreadUtils;

import java.io.IOException;

/**
 * Update automatically. The flag is saved off the main thread, because a running update holds
 * the store lock; the weekly job is then scheduled or cancelled to match.
 */
public class YrrpAdBlockAutoUpdatePreferenceController extends TogglePreferenceController {
    private static final String TAG = "YrrpAdBlock";

    public YrrpAdBlockAutoUpdatePreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return YrrpAdBlockAndroid.isPrimaryUser(mContext) ? AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public boolean isChecked() {
        return YrrpAdBlockAndroid.readState(mContext).autoUpdate;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        final Context app = mContext.getApplicationContext();
        ThreadUtils.postOnBackgroundThread(() -> {
            try {
                YrrpAdBlockAndroid.updater(app).edit(state -> state.autoUpdate = isChecked);
            } catch (IOException e) {
                Log.w(TAG, "auto-update save failed error=" + e.getClass().getSimpleName());
            }
            YrrpAdBlockUpdateJobService.sync(app);
        });
        return true;
    }

    @Override
    public boolean isSliceable() {
        return false;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.yrrp_menu_key;
    }
}
