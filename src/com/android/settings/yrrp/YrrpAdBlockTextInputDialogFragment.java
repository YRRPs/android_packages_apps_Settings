/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

/**
 * One-line text dialog for adding a list URL or an allowed host. Only OK publishes a fragment
 * result, under the request key the caller chose; the caller validates and saves it, like the
 * Finish intro dialog. The typed text survives rotation through the EditText's own state.
 */
public class YrrpAdBlockTextInputDialogFragment extends InstrumentedDialogFragment {
    static final String RESULT_TEXT = "yrrp_ad_block_text";

    private static final String ARG_REQUEST_KEY = "yrrp_request_key";
    private static final String ARG_TITLE = "yrrp_title";
    private static final String ARG_HINT = "yrrp_hint";
    private static final String ARG_INPUT_TYPE = "yrrp_input_type";
    private static final int MAX_LENGTH = 2048;

    @NonNull
    static YrrpAdBlockTextInputDialogFragment newInstance(@NonNull String requestKey,
            @StringRes int title, @StringRes int hint, int inputType) {
        final Bundle args = new Bundle();
        args.putString(ARG_REQUEST_KEY, requestKey);
        args.putInt(ARG_TITLE, title);
        args.putInt(ARG_HINT, hint);
        args.putInt(ARG_INPUT_TYPE, inputType);
        final YrrpAdBlockTextInputDialogFragment fragment =
                new YrrpAdBlockTextInputDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    static YrrpAdBlockTextInputDialogFragment forUrl(@NonNull String requestKey) {
        return newInstance(requestKey, R.string.yrrp_ad_block_add_source,
                R.string.yrrp_ad_block_add_source_hint,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
    }

    @NonNull
    static YrrpAdBlockTextInputDialogFragment forHost(@NonNull String requestKey) {
        return newInstance(requestKey, R.string.yrrp_ad_block_add_allowed,
                R.string.yrrp_ad_block_add_allowed_hint,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
    }

    @Override
    public int getMetricsCategory() {
        // Deliberate: a dedicated SettingsEnums value would need a public framework constant.
        return SettingsEnums.PAGE_UNKNOWN;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Bundle args = requireArguments();
        // The shared rename layout; its 50-character limit is too short for a list URL.
        final View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edittext, /* root= */ null, false);
        final EditText input = view.findViewById(R.id.edittext);
        input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(MAX_LENGTH)});
        input.setHint(args.getInt(ARG_HINT));
        input.setInputType(args.getInt(ARG_INPUT_TYPE));
        return new AlertDialog.Builder(requireContext())
                .setTitle(args.getInt(ARG_TITLE))
                .setView(view)
                .setPositiveButton(R.string.okay,
                        (dialog, which) -> publish(args.getString(ARG_REQUEST_KEY),
                                input.getText().toString()))
                .setNegativeButton(R.string.cancel, /* listener= */ null)
                .create();
    }

    private void publish(String requestKey, String text) {
        final Bundle result = new Bundle();
        result.putString(RESULT_TEXT, text);
        getParentFragmentManager().setFragmentResult(requestKey, result);
    }
}
