/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class YrrpBootAnimationMatrixPreferenceControllerTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private YrrpRecordingPropertyBackend mBackend;
    private YrrpBootAnimationMatrixPreferenceController mController;
    private SelectorWithWidgetPreference mPreference;
    private FragmentScenario<Fragment> mHostScenario;

    @Before
    public void setUp() {
        mBackend = new YrrpRecordingPropertyBackend();
        mController =
                new YrrpBootAnimationMatrixPreferenceController(
                        mContext,
                        YrrpBootAnimationStylePreferenceController.KEY_MATRIX,
                        mBackend.newStore());
        mPreference = spy(new SelectorWithWidgetPreference(mContext));
        mPreference.setKey(YrrpBootAnimationStylePreferenceController.KEY_MATRIX);
        final PreferenceScreen screen =
                new PreferenceManager(mContext).createPreferenceScreen(mContext);
        screen.addPreference(mPreference);
        mController.displayPreference(screen);
    }

    @After
    public void tearDown() {
        if (mHostScenario != null) {
            mHostScenario.close();
        }
    }

    @Test
    public void displayPreference_addsGearNamedFinishIntro() {
        verify(mPreference).setExtraWidgetOnClickListener(notNull());
        verify(mPreference)
                .setExtraWidgetContentDescription(
                        mContext.getString(R.string.yrrp_boot_animation_finish_intro_title));
    }

    @Test
    public void gear_withoutHost_opensNothingAndWritesNothing() {
        mController.onGearClicked();

        assertThat(mBackend.writes).isEmpty();
    }

    @Test
    public void gear_lineageSelected_opensOneDialogWithoutWriting() {
        mBackend.values.put(YrrpBootAnimationStore.STYLE_PROPERTY, "lineage");
        launchHost();

        mHostScenario.onFragment(
                host -> {
                    mController.onGearClicked();
                    mController.onGearClicked();
                    assertThat(dialogCount(host)).isEqualTo(1);
                });
        assertThat(mBackend.writes).isEmpty();
    }

    @Test
    public void gear_opensDialogAtStoredFullIntro() {
        mBackend.values.put(YrrpBootAnimationStore.FULL_INTRO_PROPERTY, "false");
        launchHost();

        mHostScenario.onFragment(
                host -> {
                    mController.onGearClicked();
                    final DialogFragment dialog =
                            (DialogFragment)
                                    host.getChildFragmentManager()
                                            .findFragmentByTag(
                                                    YrrpBootAnimationFinishIntroDialogFragment
                                                            .TAG);
                    assertThat(dialog).isNotNull();
                    final AlertDialog alert = (AlertDialog) dialog.requireDialog();
                    assertThat(alert.getListView().isItemChecked(0)).isFalse();
                    dialog.dismissNow();
                });
        assertThat(mBackend.writes).isEmpty();
    }

    @Test
    public void confirmedOff_writesOnlyFullIntroAndRefreshesSummary() {
        mBackend.values.put(YrrpBootAnimationStore.STYLE_PROPERTY, "lineage");
        launchHost();

        confirm(result(false));

        assertThat(mBackend.writes).containsExactly("persist.yrrp.bootanim.full_intro=false");
        assertThat(summary())
                .isEqualTo(mContext.getString(R.string.yrrp_boot_animation_finish_intro_off));
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void confirmedOn_writesTrue() {
        launchHost();

        confirm(result(true));

        assertThat(mBackend.writes).containsExactly("persist.yrrp.bootanim.full_intro=true");
    }

    @Test
    public void confirmedWithoutValue_writesNothing() {
        launchHost();

        confirm(new Bundle());

        assertThat(mBackend.writes).isEmpty();
    }

    @Test
    public void confirmed_writeFails_showsPersistedValue() {
        mBackend.failWrites = true;
        launchHost();

        confirm(result(false));

        assertThat(mBackend.writes).containsExactly("persist.yrrp.bootanim.full_intro=false");
        assertThat(summary())
                .isEqualTo(mContext.getString(R.string.yrrp_boot_animation_finish_intro_on));
    }

    private void launchHost() {
        mHostScenario =
                FragmentScenario.launch(
                        Fragment.class,
                        /* fragmentArgs= */ null,
                        androidx.appcompat.R.style.Theme_AppCompat,
                        androidx.lifecycle.Lifecycle.State.RESUMED);
        mHostScenario.onFragment(host -> mController.init(host));
    }

    /** Delivers {@code result} the way the dialog does, to the host's child fragment manager. */
    private void confirm(Bundle result) {
        mHostScenario.onFragment(
                host ->
                        host.getChildFragmentManager()
                                .setFragmentResult(
                                        YrrpBootAnimationFinishIntroDialogFragment.RESULT_KEY,
                                        result));
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static Bundle result(boolean fullIntro) {
        final Bundle result = new Bundle();
        result.putBoolean(YrrpBootAnimationFinishIntroDialogFragment.RESULT_FULL_INTRO, fullIntro);
        return result;
    }

    private static long dialogCount(Fragment host) {
        return host.getChildFragmentManager().getFragments().stream()
                .filter(YrrpBootAnimationFinishIntroDialogFragment.class::isInstance)
                .count();
    }

    private String summary() {
        return String.valueOf(mPreference.getSummary());
    }
}
