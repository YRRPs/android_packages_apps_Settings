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

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.os.Bundle;
import android.os.Looper;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;

import com.android.settings.R;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(qualifiers = "en-rUS")
public class YrrpColorPickerDialogFragmentTest {
    private final List<Bundle> mResults = new ArrayList<>();
    private FragmentScenario<Fragment> mHostScenario;

    @After
    public void tearDown() {
        if (mHostScenario != null) {
            mHostScenario.close();
        }
    }

    @Test
    public void newInstance_defaults_showsDefaultColorAndOpacity() {
        showPicker(0xFFFFFF, 217);

        assertThat(hexText()).isEqualTo("#D9FFFFFF");
        assertThat(seekBar(R.id.yrrp_color_red).getProgress()).isEqualTo(0xFF);
        assertThat(seekBar(R.id.yrrp_color_green).getProgress()).isEqualTo(0xFF);
        assertThat(seekBar(R.id.yrrp_color_blue).getProgress()).isEqualTo(0xFF);
        final SeekBar alpha = seekBar(R.id.yrrp_color_alpha);
        assertThat(alpha.getProgress()).isEqualTo(217);
        assertThat(alpha.getMin()).isEqualTo(26);
        assertThat(alpha.getMax()).isEqualTo(255);
    }

    @Test
    public void newInstance_outOfRangeArguments_masksRgbAndClampsAlpha() {
        showPicker(0xAB123456, 300);

        assertThat(hexText()).isEqualTo("#FF123456");
        assertThat(seekBar(R.id.yrrp_color_red).getProgress()).isEqualTo(0x12);
        assertThat(seekBar(R.id.yrrp_color_alpha).getProgress()).isEqualTo(255);
    }

    @Test
    public void newInstance_alphaBelowMinimum_isClamped() {
        showPicker(0x123456, 0);

        assertThat(hexText()).isEqualTo("#1A123456");
        assertThat(seekBar(R.id.yrrp_color_alpha).getProgress()).isEqualTo(26);
    }

    @Test
    public void moveChannel_updatesHexAndStateLocallyWithoutPublishing() {
        showPicker(0xFFFFFF, 217);

        final SeekBar red = seekBar(R.id.yrrp_color_red);
        red.setProgress(0x80);

        assertThat(hexText()).isEqualTo("#D980FFFF");
        assertThat(String.valueOf(red.getStateDescription())).isEqualTo("128");
        assertThat(mResults).isEmpty();
    }

    @Test
    public void opacityStateDescription_isRoundedPercentage() {
        showPicker(0xFFFFFF, 217);

        final SeekBar alpha = seekBar(R.id.yrrp_color_alpha);
        assertThat(String.valueOf(alpha.getStateDescription())).isEqualTo("85%");

        alpha.setProgress(128);

        assertThat(String.valueOf(alpha.getStateDescription())).isEqualTo("50%");
        assertThat(hexText()).isEqualTo("#80FFFFFF");
    }

    @Test
    public void recreation_restoresLocalChoice() {
        showPicker(0xFFFFFF, 217);
        seekBar(R.id.yrrp_color_red).setProgress(0x80);
        seekBar(R.id.yrrp_color_alpha).setProgress(128);

        mHostScenario.recreate();
        mHostScenario.onFragment(this::listenForResults);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(hexText()).isEqualTo("#8080FFFF");
        assertThat(seekBar(R.id.yrrp_color_red).getProgress()).isEqualTo(0x80);
        assertThat(seekBar(R.id.yrrp_color_alpha).getProgress()).isEqualTo(128);
        assertThat(mResults).isEmpty();
    }

    @Test
    public void positiveButton_publishesOneResultWithChosenColor() {
        showPicker(0xFFFFFF, 217);
        seekBar(R.id.yrrp_color_green).setProgress(0);
        seekBar(R.id.yrrp_color_alpha).setProgress(128);

        clickButton(BUTTON_POSITIVE);

        assertThat(mResults).hasSize(1);
        final Bundle result = mResults.get(0);
        assertThat(result.getInt(YrrpColorPickerDialogFragment.RESULT_RGB)).isEqualTo(0xFF00FF);
        assertThat(result.getInt(YrrpColorPickerDialogFragment.RESULT_ALPHA)).isEqualTo(128);
    }

    @Test
    public void positiveButton_outOfRangeArguments_publishesMaskedRgbAndClampedAlpha() {
        showPicker(0xAB123456, 300);

        clickButton(BUTTON_POSITIVE);

        assertThat(mResults).hasSize(1);
        final Bundle result = mResults.get(0);
        assertThat(result.getInt(YrrpColorPickerDialogFragment.RESULT_RGB)).isEqualTo(0x123456);
        assertThat(result.getInt(YrrpColorPickerDialogFragment.RESULT_ALPHA)).isEqualTo(255);
    }

    @Test
    public void negativeButton_publishesNothing() {
        showPicker(0xFFFFFF, 217);
        seekBar(R.id.yrrp_color_red).setProgress(0);

        clickButton(BUTTON_NEGATIVE);

        assertThat(mResults).isEmpty();
    }

    @Test
    public void back_publishesNothing() {
        showPicker(0xFFFFFF, 217);
        seekBar(R.id.yrrp_color_red).setProgress(0);

        dialog().getOnBackPressedDispatcher().onBackPressed();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mResults).isEmpty();
    }

    @Test
    public void cancel_publishesNothing() {
        showPicker(0xFFFFFF, 217);
        seekBar(R.id.yrrp_color_red).setProgress(0);

        dialog().cancel();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mResults).isEmpty();
    }

    /** Shows the picker in a host fragment's child manager, as the color row's controller does. */
    private void showPicker(int rgb, int alpha) {
        mHostScenario =
                FragmentScenario.launch(
                        Fragment.class,
                        /* fragmentArgs= */ null,
                        androidx.appcompat.R.style.Theme_AppCompat,
                        Lifecycle.State.RESUMED);
        mHostScenario.onFragment(
                host -> {
                    listenForResults(host);
                    YrrpColorPickerDialogFragment.newInstance(rgb, alpha)
                            .showNow(
                                    host.getChildFragmentManager(),
                                    YrrpColorPickerDialogFragment.TAG);
                });
        shadowOf(Looper.getMainLooper()).idle();
    }

    private void listenForResults(Fragment host) {
        host.getChildFragmentManager()
                .setFragmentResultListener(
                        YrrpColorPickerDialogFragment.RESULT_KEY,
                        host,
                        (requestKey, result) -> mResults.add(result));
    }

    private AlertDialog dialog() {
        final AlertDialog[] dialog = new AlertDialog[1];
        mHostScenario.onFragment(
                host -> {
                    final DialogFragment picker =
                            (DialogFragment)
                                    host.getChildFragmentManager()
                                            .findFragmentByTag(YrrpColorPickerDialogFragment.TAG);
                    assertThat(picker).isNotNull();
                    dialog[0] = (AlertDialog) picker.requireDialog();
                });
        return dialog[0];
    }

    private SeekBar seekBar(int id) {
        return dialog().findViewById(id);
    }

    private String hexText() {
        final TextView hex = dialog().findViewById(R.id.yrrp_color_hex);
        return hex.getText().toString();
    }

    private void clickButton(int which) {
        dialog().getButton(which).performClick();
        shadowOf(Looper.getMainLooper()).idle();
    }
}
