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
import android.view.View;
import android.view.ViewGroup;
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
    public void newInstance_default_showsDefaultRgb() {
        showPicker(0xFFFFFF);

        assertThat(hexText()).isEqualTo("#FFFFFF");
        assertThat(seekBar(R.id.yrrp_color_red).getProgress()).isEqualTo(0xFF);
        assertThat(seekBar(R.id.yrrp_color_green).getProgress()).isEqualTo(0xFF);
        assertThat(seekBar(R.id.yrrp_color_blue).getProgress()).isEqualTo(0xFF);
    }

    @Test
    public void layout_hasOnlyTheThreeChannelSliders() {
        showPicker(0xFFFFFF);

        final View swatch = dialog().findViewById(R.id.yrrp_color_swatch);
        assertThat(countSeekBars(swatch.getRootView())).isEqualTo(3);
    }

    @Test
    public void newInstance_highBits_masksRgb() {
        showPicker(0xAB123456);

        assertThat(hexText()).isEqualTo("#123456");
        assertThat(seekBar(R.id.yrrp_color_red).getProgress()).isEqualTo(0x12);
    }

    @Test
    public void moveChannel_updatesHexAndStateLocallyWithoutPublishing() {
        showPicker(0xFFFFFF);

        final SeekBar red = seekBar(R.id.yrrp_color_red);
        red.setProgress(0x80);

        assertThat(hexText()).isEqualTo("#80FFFF");
        assertThat(String.valueOf(red.getStateDescription())).isEqualTo("128");
        assertThat(mResults).isEmpty();
    }

    @Test
    public void recreation_restoresLocalChoice() {
        showPicker(0xFFFFFF);
        seekBar(R.id.yrrp_color_red).setProgress(0x80);

        mHostScenario.recreate();
        mHostScenario.onFragment(this::listenForResults);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(hexText()).isEqualTo("#80FFFF");
        assertThat(seekBar(R.id.yrrp_color_red).getProgress()).isEqualTo(0x80);
        assertThat(mResults).isEmpty();
    }

    @Test
    public void positiveButton_publishesOneResultWithOnlyChosenRgb() {
        showPicker(0xFFFFFF);
        seekBar(R.id.yrrp_color_green).setProgress(0);

        clickButton(BUTTON_POSITIVE);

        assertThat(mResults).hasSize(1);
        final Bundle result = mResults.get(0);
        assertThat(result.keySet()).containsExactly(YrrpColorPickerDialogFragment.RESULT_RGB);
        assertThat(result.getInt(YrrpColorPickerDialogFragment.RESULT_RGB)).isEqualTo(0xFF00FF);
    }

    @Test
    public void positiveButton_highBits_publishesMaskedRgb() {
        showPicker(0xAB123456);

        clickButton(BUTTON_POSITIVE);

        assertThat(mResults).hasSize(1);
        assertThat(mResults.get(0).getInt(YrrpColorPickerDialogFragment.RESULT_RGB))
                .isEqualTo(0x123456);
    }

    @Test
    public void negativeButton_publishesNothing() {
        showPicker(0xFFFFFF);
        seekBar(R.id.yrrp_color_red).setProgress(0);

        clickButton(BUTTON_NEGATIVE);

        assertThat(mResults).isEmpty();
    }

    @Test
    public void back_publishesNothing() {
        showPicker(0xFFFFFF);
        seekBar(R.id.yrrp_color_red).setProgress(0);

        dialog().getOnBackPressedDispatcher().onBackPressed();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mResults).isEmpty();
    }

    @Test
    public void cancel_publishesNothing() {
        showPicker(0xFFFFFF);
        seekBar(R.id.yrrp_color_red).setProgress(0);

        dialog().cancel();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mResults).isEmpty();
    }

    /** Shows the picker in a host fragment's child manager, as the color row's controller does. */
    private void showPicker(int rgb) {
        mHostScenario =
                FragmentScenario.launch(
                        Fragment.class,
                        /* fragmentArgs= */ null,
                        androidx.appcompat.R.style.Theme_AppCompat,
                        Lifecycle.State.RESUMED);
        mHostScenario.onFragment(
                host -> {
                    listenForResults(host);
                    YrrpColorPickerDialogFragment.newInstance(rgb)
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

    private static int countSeekBars(View view) {
        if (view instanceof SeekBar) {
            return 1;
        }
        int count = 0;
        if (view instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                count += countSeekBars(group.getChildAt(i));
            }
        }
        return count;
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
