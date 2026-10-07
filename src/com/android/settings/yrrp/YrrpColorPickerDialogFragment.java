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

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.Utils;

import java.util.function.IntConsumer;

/**
 * Picks a 24-bit RGB color with three channel sliders and an opacity with a fourth.
 *
 * <p>The choice stays local until the positive button is pressed; only then is one fragment result
 * with the masked RGB and the clamped alpha published to the parent fragment manager. Every other
 * way of leaving the dialog publishes nothing.
 */
public class YrrpColorPickerDialogFragment extends InstrumentedDialogFragment {
    static final String TAG = "YrrpColorPickerDialog";
    static final String RESULT_KEY = "yrrp_pulse_color_result";
    static final String RESULT_RGB = "yrrp_pulse_color_rgb";
    static final String RESULT_ALPHA = "yrrp_pulse_color_alpha";

    private static final String ARG_INITIAL_RGB = "yrrp_initial_rgb";
    private static final String ARG_INITIAL_ALPHA = "yrrp_initial_alpha";
    private static final String STATE_RGB = "yrrp_current_rgb";
    private static final String STATE_ALPHA = "yrrp_current_alpha";
    private static final int CHANNEL_MAX = 0xFF;

    private static final int[] SEEK_BAR_IDS = {
        R.id.yrrp_color_red, R.id.yrrp_color_green, R.id.yrrp_color_blue
    };
    private static final int[] CHANNEL_SHIFTS = {16, 8, 0};

    private final int[] mChannels = new int[SEEK_BAR_IDS.length];
    private int mAlpha = YrrpSettingsStore.PULSE_ALPHA_DEFAULT;
    private @Nullable GradientDrawable mSwatchFill;
    private @Nullable TextView mHex;

    /**
     * Creates a picker that starts at {@code rgb} and {@code alpha}; bits above 24 of {@code rgb}
     * are ignored and {@code alpha} is clamped to the supported range.
     */
    @NonNull
    static YrrpColorPickerDialogFragment newInstance(int rgb, int alpha) {
        final Bundle args = new Bundle();
        args.putInt(ARG_INITIAL_RGB, YrrpSettingsStore.normalizeColor(rgb));
        args.putInt(ARG_INITIAL_ALPHA, YrrpSettingsStore.normalizeAlpha(alpha));
        final YrrpColorPickerDialogFragment fragment = new YrrpColorPickerDialogFragment();
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
        final Context context = requireContext();
        setChannels(
                YrrpSettingsStore.normalizeColor(
                        initialValue(savedInstanceState, STATE_RGB, ARG_INITIAL_RGB, 0)));
        mAlpha =
                YrrpSettingsStore.normalizeAlpha(
                        initialValue(
                                savedInstanceState,
                                STATE_ALPHA,
                                ARG_INITIAL_ALPHA,
                                YrrpSettingsStore.PULSE_ALPHA_DEFAULT));
        final View view =
                LayoutInflater.from(context).inflate(R.layout.yrrp_color_picker_dialog, null);
        bindSwatch(view.findViewById(R.id.yrrp_color_swatch));
        mHex = view.findViewById(R.id.yrrp_color_hex);
        for (int i = 0; i < SEEK_BAR_IDS.length; i++) {
            bindChannel(view.findViewById(SEEK_BAR_IDS[i]), i);
        }
        bindAlpha(view.findViewById(R.id.yrrp_color_alpha));
        updatePreview();
        return new AlertDialog.Builder(context)
                .setTitle(R.string.yrrp_pulse_color_title)
                .setView(view)
                .setPositiveButton(R.string.okay, (d, which) -> publishResult())
                .setNegativeButton(R.string.cancel, /* listener= */ null)
                .create();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_RGB, currentRgb());
        outState.putInt(STATE_ALPHA, YrrpSettingsStore.normalizeAlpha(mAlpha));
    }

    /** The saved value after recreation, otherwise the argument; callers normalize it. */
    private int initialValue(
            @Nullable Bundle savedInstanceState, String stateKey, String argKey, int fallback) {
        if (savedInstanceState != null && savedInstanceState.containsKey(stateKey)) {
            return savedInstanceState.getInt(stateKey);
        }
        return requireArguments().getInt(argKey, fallback);
    }

    private void setChannels(int rgb) {
        for (int i = 0; i < mChannels.length; i++) {
            mChannels[i] = (rgb >> CHANNEL_SHIFTS[i]) & CHANNEL_MAX;
        }
    }

    private int currentRgb() {
        int rgb = 0;
        for (int i = 0; i < mChannels.length; i++) {
            rgb |= (mChannels[i] & CHANNEL_MAX) << CHANNEL_SHIFTS[i];
        }
        return rgb;
    }

    private void bindChannel(@NonNull SeekBar seekBar, int index) {
        seekBar.setMax(CHANNEL_MAX);
        seekBar.setProgress(mChannels[index]);
        seekBar.setStateDescription(channelState(index));
        seekBar.setOnSeekBarChangeListener(
                progressListener(
                        progress -> {
                            mChannels[index] = Math.max(0, Math.min(CHANNEL_MAX, progress));
                            seekBar.setStateDescription(channelState(index));
                        }));
    }

    private void bindAlpha(@NonNull SeekBar seekBar) {
        // Max before min, so the minimum never exceeds the default maximum while being set.
        seekBar.setMax(YrrpSettingsStore.PULSE_ALPHA_MAX);
        seekBar.setMin(YrrpSettingsStore.PULSE_ALPHA_MIN);
        seekBar.setProgress(mAlpha);
        seekBar.setStateDescription(alphaState());
        seekBar.setOnSeekBarChangeListener(
                progressListener(
                        progress -> {
                            mAlpha = YrrpSettingsStore.normalizeAlpha(progress);
                            seekBar.setStateDescription(alphaState());
                        }));
    }

    /** Applies each progress change through {@code onProgress}, then refreshes the preview. */
    @NonNull
    private SeekBar.OnSeekBarChangeListener progressListener(@NonNull IntConsumer onProgress) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                onProgress.accept(progress);
                updatePreview();
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {}

            @Override
            public void onStopTrackingTouch(SeekBar bar) {}
        };
    }

    /**
     * The channel value, for example "128", so TalkBack reads it rather than a percentage. The
     * channel name comes from the slider's label.
     */
    @NonNull
    private String channelState(int index) {
        return getString(R.string.yrrp_pulse_color_channel_state, mChannels[index]);
    }

    /** The opacity as a rounded, locale-formatted percentage of 255, for example "85%". */
    @NonNull
    private String alphaState() {
        return Utils.formatPercentage(
                Math.round(mAlpha * 100f / YrrpSettingsStore.PULSE_ALPHA_MAX));
    }

    /**
     * Finds the swatch's fill layer, drawn over a checkerboard so translucency shows, and clips the
     * swatch to the fill's rounded corners. The checkerboard bitmap reports a square outline, which
     * LayerDrawable would otherwise use, so the outline is set here.
     */
    private void bindSwatch(@NonNull View swatch) {
        final Drawable background = swatch.getBackground();
        final Drawable fill =
                background instanceof LayerDrawable
                        ? ((LayerDrawable) background.mutate())
                                .findDrawableByLayerId(R.id.yrrp_swatch_fill)
                        : null;
        if (!(fill instanceof GradientDrawable)) {
            Log.w(TAG, "Swatch fill layer missing; preview disabled");
            return;
        }
        final GradientDrawable gradient = (GradientDrawable) fill;
        mSwatchFill = gradient;
        swatch.setOutlineProvider(
                new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        outline.setRoundRect(
                                0,
                                0,
                                view.getWidth(),
                                view.getHeight(),
                                gradient.getCornerRadius());
                    }
                });
        swatch.setClipToOutline(true);
    }

    private void updatePreview() {
        final int argb = YrrpSettingsStore.toArgb(currentRgb(), mAlpha);
        final String hex = YrrpSettingsStore.formatArgb(argb);
        if (mHex != null) {
            mHex.setText(hex);
            mHex.setContentDescription(getString(R.string.yrrp_pulse_color_hex_description, hex));
        }
        // The fill is translucent; its stroke keeps the swatch outline visible.
        if (mSwatchFill != null) {
            mSwatchFill.setColor(argb);
        }
    }

    private void publishResult() {
        final Bundle result = new Bundle();
        result.putInt(RESULT_RGB, YrrpSettingsStore.normalizeColor(currentRgb()));
        result.putInt(RESULT_ALPHA, YrrpSettingsStore.normalizeAlpha(mAlpha));
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
    }
}
