/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

/**
 * The Matrix row. Its gear opens Finish intro, which applies only to Matrix. The gear works
 * whichever style is selected and never changes the style; OK writes only full_intro.
 */
public class YrrpBootAnimationMatrixPreferenceController
        extends YrrpBootAnimationStylePreferenceController {

    private static final String TAG = "YrrpBootAnimationMatrix";

    private @Nullable Fragment mHost;
    private @Nullable Preference mRow;

    public YrrpBootAnimationMatrixPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        this(context, preferenceKey, YrrpBootAnimationStore.create());
    }

    /** Injects the store, so tests can count writes and make them fail. */
    @VisibleForTesting
    YrrpBootAnimationMatrixPreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey,
            @NonNull YrrpBootAnimationStore store) {
        super(context, preferenceKey, store);
    }

    /**
     * Binds the hosting page, which shows the dialog and delivers its result. Call from the
     * host's {@code onAttach()} so the listener is registered again after recreation and a
     * pending confirmed result still arrives.
     */
    public void init(@NonNull Fragment host) {
        mHost = host;
        host.getChildFragmentManager()
                .setFragmentResultListener(
                        YrrpBootAnimationFinishIntroDialogFragment.RESULT_KEY,
                        host,
                        (requestKey, result) -> onFinishIntroConfirmed(result));
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        final SelectorWithWidgetPreference preference = screen.findPreference(getPreferenceKey());
        mRow = preference;
        if (preference != null) {
            preference.setExtraWidgetContentDescription(
                    mContext.getString(R.string.yrrp_boot_animation_finish_intro_title));
            preference.setExtraWidgetOnClickListener(v -> onGearClicked());
        }
    }

    @Override
    public void updateState(@Nullable Preference preference) {
        super.updateState(preference);
        if (preference != null) {
            preference.setSummary(
                    mStore.isFullIntro()
                            ? R.string.yrrp_boot_animation_finish_intro_on
                            : R.string.yrrp_boot_animation_finish_intro_off);
        }
    }

    /** Opens the dialog at the stored value, unless the host is gone or a dialog is showing. */
    @VisibleForTesting
    void onGearClicked() {
        if (mHost == null) {
            Log.w(TAG, "init(Fragment) not called");
            return;
        }
        if (!mHost.isAdded()) {
            return;
        }
        final FragmentManager manager = mHost.getChildFragmentManager();
        if (manager.isStateSaved()
                || manager.findFragmentByTag(YrrpBootAnimationFinishIntroDialogFragment.TAG)
                        != null) {
            return;
        }
        YrrpBootAnimationFinishIntroDialogFragment.newInstance(mStore.isFullIntro())
                .showNow(manager, YrrpBootAnimationFinishIntroDialogFragment.TAG);
    }

    /**
     * The result bundle is untrusted input: the value key is required. Either way the row
     * re-reads the persisted value, so a failed write shows what is stored.
     */
    @VisibleForTesting
    void onFinishIntroConfirmed(@NonNull Bundle result) {
        final String key = YrrpBootAnimationFinishIntroDialogFragment.RESULT_FULL_INTRO;
        if (result.containsKey(key)) {
            mStore.setFullIntro(result.getBoolean(key));
        }
        updateState(mRow);
    }
}
