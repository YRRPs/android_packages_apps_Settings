/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.os.Bundle;
import android.os.Looper;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class YrrpBootAnimationFinishIntroDialogFragmentTest {
    private final List<Bundle> mResults = new ArrayList<>();
    private FragmentScenario<Fragment> mHostScenario;

    @After
    public void tearDown() {
        if (mHostScenario != null) {
            mHostScenario.close();
        }
    }

    @Test
    public void opensAtTheGivenValue() {
        showDialog(false);

        assertThat(checkbox().isItemChecked(0)).isFalse();
        assertThat(mResults).isEmpty();
    }

    @Test
    public void positiveButton_publishesOneResultWithTheChoice() {
        showDialog(true);
        toggle();

        clickButton(BUTTON_POSITIVE);

        assertThat(mResults).hasSize(1);
        final Bundle result = mResults.get(0);
        assertThat(result.keySet())
                .containsExactly(YrrpBootAnimationFinishIntroDialogFragment.RESULT_FULL_INTRO);
        assertThat(result.getBoolean(YrrpBootAnimationFinishIntroDialogFragment.RESULT_FULL_INTRO))
                .isFalse();
    }

    @Test
    public void recreation_restoresLocalChoice() {
        showDialog(true);
        toggle();

        mHostScenario.recreate();
        mHostScenario.onFragment(this::listenForResults);
        shadowOf(Looper.getMainLooper()).idle();
        clickButton(BUTTON_POSITIVE);

        assertThat(mResults).hasSize(1);
        assertThat(
                        mResults.get(0)
                                .getBoolean(
                                        YrrpBootAnimationFinishIntroDialogFragment
                                                .RESULT_FULL_INTRO))
                .isFalse();
    }

    @Test
    public void negativeButton_publishesNothing() {
        showDialog(true);
        toggle();

        clickButton(BUTTON_NEGATIVE);

        assertThat(mResults).isEmpty();
    }

    @Test
    public void back_publishesNothing() {
        showDialog(true);
        toggle();

        dialog().getOnBackPressedDispatcher().onBackPressed();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mResults).isEmpty();
    }

    @Test
    public void cancel_publishesNothing() {
        showDialog(true);
        toggle();

        dialog().cancel();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mResults).isEmpty();
    }

    /** Shows the dialog in a host fragment's child manager, as the Matrix row's controller does. */
    private void showDialog(boolean fullIntro) {
        mHostScenario =
                FragmentScenario.launch(
                        Fragment.class,
                        /* fragmentArgs= */ null,
                        androidx.appcompat.R.style.Theme_AppCompat,
                        Lifecycle.State.RESUMED);
        mHostScenario.onFragment(
                host -> {
                    listenForResults(host);
                    YrrpBootAnimationFinishIntroDialogFragment.newInstance(fullIntro)
                            .showNow(
                                    host.getChildFragmentManager(),
                                    YrrpBootAnimationFinishIntroDialogFragment.TAG);
                });
        shadowOf(Looper.getMainLooper()).idle();
    }

    private void listenForResults(Fragment host) {
        host.getChildFragmentManager()
                .setFragmentResultListener(
                        YrrpBootAnimationFinishIntroDialogFragment.RESULT_KEY,
                        host,
                        (requestKey, result) -> mResults.add(result));
    }

    private AlertDialog dialog() {
        final AlertDialog[] dialog = new AlertDialog[1];
        mHostScenario.onFragment(
                host -> {
                    final DialogFragment fragment =
                            (DialogFragment)
                                    host.getChildFragmentManager()
                                            .findFragmentByTag(
                                                    YrrpBootAnimationFinishIntroDialogFragment
                                                            .TAG);
                    assertThat(fragment).isNotNull();
                    dialog[0] = (AlertDialog) fragment.requireDialog();
                });
        return dialog[0];
    }

    private ListView checkbox() {
        return dialog().getListView();
    }

    /** Taps the single checkbox row, as a user would. */
    private void toggle() {
        final ListView list = checkbox();
        list.performItemClick(list.getAdapter().getView(0, null, list), 0, 0);
        shadowOf(Looper.getMainLooper()).idle();
    }

    private void clickButton(int which) {
        dialog().getButton(which).performClick();
        shadowOf(Looper.getMainLooper()).idle();
    }
}
