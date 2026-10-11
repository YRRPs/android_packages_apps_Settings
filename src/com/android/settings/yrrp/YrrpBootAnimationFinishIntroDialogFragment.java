/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * OK/Cancel dialog with one checkbox for Matrix Finish intro. The choice stays local until the
 * positive button is pressed; only then is one fragment result published to the parent fragment
 * manager. Cancel, back, and outside tap publish nothing.
 */
public class YrrpBootAnimationFinishIntroDialogFragment extends InstrumentedDialogFragment {
    static final String TAG = "YrrpBootAnimationFinishIntro";
    static final String RESULT_KEY = "yrrp_boot_animation_finish_intro_result";
    static final String RESULT_FULL_INTRO = "yrrp_boot_animation_full_intro";

    private static final String ARG_FULL_INTRO = "yrrp_initial_full_intro";
    private static final String STATE_CHECKED = "yrrp_current_full_intro";

    private boolean mChecked;

    /** Creates a dialog whose checkbox starts at {@code fullIntro}. */
    @NonNull
    static YrrpBootAnimationFinishIntroDialogFragment newInstance(boolean fullIntro) {
        final Bundle args = new Bundle();
        args.putBoolean(ARG_FULL_INTRO, fullIntro);
        final YrrpBootAnimationFinishIntroDialogFragment fragment =
                new YrrpBootAnimationFinishIntroDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getMetricsCategory() {
        // Deliberate: a dedicated SettingsEnums value would need a public framework constant.
        return SettingsEnums.PAGE_UNKNOWN;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mChecked =
                savedInstanceState != null && savedInstanceState.containsKey(STATE_CHECKED)
                        ? savedInstanceState.getBoolean(STATE_CHECKED)
                        : requireArguments().getBoolean(ARG_FULL_INTRO, true);
        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.yrrp_boot_animation_finish_intro_title)
                .setMultiChoiceItems(
                        new CharSequence[] {
                            getText(R.string.yrrp_boot_animation_finish_intro_option)
                        },
                        new boolean[] {mChecked},
                        (dialog, which, isChecked) -> mChecked = isChecked)
                .setPositiveButton(R.string.okay, (dialog, which) -> publishResult())
                .setNegativeButton(R.string.cancel, /* listener= */ null)
                .create();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_CHECKED, mChecked);
    }

    private void publishResult() {
        final Bundle result = new Bundle();
        result.putBoolean(RESULT_FULL_INTRO, mChecked);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
    }
}
