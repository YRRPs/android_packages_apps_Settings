/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/**
 * Update lists now. A tap downloads every enabled list in the background; the row stays
 * disabled until the update ends, then shows when it last succeeded or why it failed. The
 * summary reads only saved state, so it is the same after leaving and reopening the page, and
 * it redraws after an update the job started.
 */
public class YrrpAdBlockUpdateNowPreferenceController extends BasePreferenceController
        implements DefaultLifecycleObserver {
    private final Runnable mRefresh = this::refresh;
    private Preference mPreference;
    private boolean mUpdating;

    public YrrpAdBlockUpdateNowPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return YrrpAdBlockAndroid.isPrimaryUser(mContext) ? AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        YrrpAdBlockAndroid.updateObservers().add(mRefresh);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        YrrpAdBlockAndroid.updateObservers().remove(mRefresh);
    }

    private void refresh() {
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        super.updateState(preference);
        preference.setEnabled(!mUpdating);
    }

    @Override
    public boolean handlePreferenceTreeClick(@NonNull Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey()) || mUpdating) {
            return false;
        }
        mUpdating = true;
        updateState(preference);
        // The update observers redraw the row once this clears the flag.
        YrrpAdBlockAndroid.runUpdate(mContext, true, outcome -> mUpdating = false);
        return true;
    }

    @Override
    public CharSequence getSummary() {
        if (mUpdating) {
            return mContext.getText(R.string.yrrp_ad_block_updating);
        }
        final YrrpAdBlockStore.State state = YrrpAdBlockAndroid.readState(mContext);
        if (state.lastUpdateMillis == 0) {
            return mContext.getText(R.string.yrrp_ad_block_never_updated);
        }
        final String result = state.lastResult;
        if (YrrpAdBlockUpdater.Result.NO_DATA.name().equals(result)) {
            return mContext.getText(R.string.yrrp_ad_block_no_data);
        }
        if (YrrpAdBlockUpdater.Result.FAILED.name().equals(result)) {
            return mContext.getText(R.string.yrrp_ad_block_failed);
        }
        final int failed = state.failedSources();
        if (failed > 0) {
            return mContext.getString(R.string.yrrp_ad_block_some_failed,
                    failed, state.enabledSources());
        }
        return mContext.getString(R.string.yrrp_ad_block_updated,
                DateUtils.getRelativeTimeSpanString(state.lastUpdateMillis),
                state.lastEntries);
    }
}
