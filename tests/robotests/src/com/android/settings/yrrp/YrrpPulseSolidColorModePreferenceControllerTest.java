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

import static androidx.lifecycle.Lifecycle.Event.ON_START;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class YrrpPulseSolidColorModePreferenceControllerTest {
    /** Read back when a key was never written. */
    private static final int MISSING = Integer.MIN_VALUE;

    private static final int[] ALL_MODES = {
        YrrpSettingsStore.PULSE_COLOR_MODE_SOLID,
        YrrpSettingsStore.PULSE_COLOR_MODE_MATCH_THEME,
        YrrpSettingsStore.PULSE_COLOR_MODE_RAINBOW_GRADIENT,
        YrrpSettingsStore.PULSE_COLOR_MODE_RAINBOW_CYCLE
    };

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ContentResolver mContentResolver;
    private YrrpRecordingSecureBackend mBackend;
    private YrrpPulseSolidColorModePreferenceController mController;
    private SelectorWithWidgetPreference mPreference;
    private PreferenceScreen mScreen;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private FragmentScenario<Fragment> mHostScenario;

    @Before
    public void setUp() {
        mContentResolver = mContext.getContentResolver();
        mBackend = new YrrpRecordingSecureBackend(mContext);
        mController =
                new YrrpPulseSolidColorModePreferenceController(
                        mContext,
                        YrrpPulseColorModePreferenceController.KEY_SOLID,
                        mBackend.newStore());
        mPreference = spy(new SelectorWithWidgetPreference(mContext));
        mPreference.setKey(YrrpPulseColorModePreferenceController.KEY_SOLID);
        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mScreen.addPreference(mPreference);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mLifecycle.addObserver(mController);
        mController.displayPreference(mScreen);
    }

    @After
    public void tearDown() {
        if (mHostScenario != null) {
            mHostScenario.close();
        }
    }

    @Test
    public void displayPreference_addsGearNamedPulseColor() {
        verify(mPreference).setExtraWidgetOnClickListener(notNull());
        verify(mPreference).setExtraWidgetContentDescription("Pulse color");
    }

    @Test
    public void updateState_missingSettings_showsDefaultRgbSummaryWithoutWriting() {
        mController.updateState(mPreference);

        assertThat(summary()).isEqualTo("#FFFFFF");
        assertThat(mBackend.mWrites).isEmpty();
    }

    @Test
    public void updateState_highColorBits_showsMaskedRgbWithoutWriting() {
        putSecure(YrrpSettingsStore.PULSE_COLOR, 0xAB123456);

        mController.updateState(mPreference);

        assertThat(summary()).isEqualTo("#123456");
        assertThat(mBackend.mWrites).isEmpty();
        assertThat(raw(YrrpSettingsStore.PULSE_COLOR)).isEqualTo(0xAB123456);
    }

    @Test
    public void updateState_checksRowOnlyInSolid() {
        putSecure(YrrpSettingsStore.PULSE_COLOR_MODE, YrrpSettingsStore.PULSE_COLOR_MODE_SOLID);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();

        putSecure(
                YrrpSettingsStore.PULSE_COLOR_MODE,
                YrrpSettingsStore.PULSE_COLOR_MODE_RAINBOW_CYCLE);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void updateState_pulseOff_disablesRowAndGear() {
        putSecure(YrrpSettingsStore.PULSE_ENABLED, 0);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void gear_inEveryMode_opensPickerAtStoredRgbWithoutWriting() {
        putSecure(YrrpSettingsStore.PULSE_ENABLED, 1);
        putSecure(YrrpSettingsStore.PULSE_COLOR, 0xFF00FF);
        launchHost();

        for (int mode : ALL_MODES) {
            putSecure(YrrpSettingsStore.PULSE_COLOR_MODE, mode);
            mHostScenario.onFragment(
                    host -> {
                        mController.onGearClicked();
                        final DialogFragment picker = picker(host);
                        assertThat(picker).isNotNull();
                        final TextView hex =
                                picker.requireDialog().findViewById(R.id.yrrp_color_hex);
                        assertThat(hex.getText().toString()).isEqualTo("#FF00FF");
                        picker.dismissNow();
                    });
        }
        assertThat(mBackend.mWrites).isEmpty();
    }

    @Test
    public void gear_pulseOff_opensNothing() {
        putSecure(YrrpSettingsStore.PULSE_ENABLED, 0);
        launchHost();

        mHostScenario.onFragment(
                host -> {
                    mController.onGearClicked();
                    assertThat(picker(host)).isNull();
                });
        assertThat(mBackend.mWrites).isEmpty();
    }

    @Test
    public void gear_twice_opensOnePicker() {
        putSecure(YrrpSettingsStore.PULSE_ENABLED, 1);
        launchHost();

        mHostScenario.onFragment(
                host -> {
                    mController.onGearClicked();
                    mController.onGearClicked();
                    assertThat(
                                    host.getChildFragmentManager().getFragments().stream()
                                            .filter(f -> f instanceof YrrpColorPickerDialogFragment)
                                            .count())
                            .isEqualTo(1);
                });
    }

    @Test
    public void colorConfirmed_writesOnlyColorAndKeepsMode() {
        putSecure(
                YrrpSettingsStore.PULSE_COLOR_MODE,
                YrrpSettingsStore.PULSE_COLOR_MODE_MATCH_THEME);
        launchHost();

        confirm(result(0xFF00FF));

        assertThat(mBackend.mWrites).containsExactly("lineage_pulse_color=" + 0xFF00FF);
        assertThat(raw(YrrpSettingsStore.PULSE_COLOR_MODE))
                .isEqualTo(YrrpSettingsStore.PULSE_COLOR_MODE_MATCH_THEME);
        assertThat(summary()).isEqualTo("#FF00FF");
    }

    @Test
    public void colorConfirmed_masksRgb() {
        launchHost();

        confirm(result(0xAB123456));

        assertThat(mBackend.mWrites).containsExactly("lineage_pulse_color=" + 0x123456);
    }

    @Test
    public void colorConfirmed_missingRgb_writesNothing() {
        launchHost();

        confirm(new Bundle());

        assertThat(mBackend.mWrites).isEmpty();
    }

    @Test
    public void colorConfirmed_writeFails_showsPersisted() {
        putSecure(YrrpSettingsStore.PULSE_COLOR, 0x00FF00);
        mBackend.mFailingKeys.add(YrrpSettingsStore.PULSE_COLOR);
        launchHost();

        confirm(result(0xFF00FF));

        assertThat(mBackend.mWrites).containsExactly("lineage_pulse_color=" + 0xFF00FF);
        assertThat(summary()).isEqualTo("#00FF00");
    }

    @Test
    public void externalColorChange_whileStarted_refreshesSummary() {
        mLifecycle.handleLifecycleEvent(ON_START);
        mController.updateState(mPreference);

        putSecure(YrrpSettingsStore.PULSE_COLOR, 0x00FF00);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(summary()).isEqualTo("#00FF00");
    }

    @Test
    public void onStart_observesModeEnabledAndColor() {
        mLifecycle.handleLifecycleEvent(ON_START);

        assertThat(observerCount(YrrpSettingsStore.PULSE_COLOR_MODE)).isEqualTo(1);
        assertThat(observerCount(YrrpSettingsStore.PULSE_ENABLED)).isEqualTo(1);
        assertThat(observerCount(YrrpSettingsStore.PULSE_COLOR)).isEqualTo(1);
        assertThat(observerCount(YrrpSettingsStore.PULSE_ALPHA)).isEqualTo(0);
    }

    @Test
    public void isSliceable_isFalse() {
        assertThat(mController.isSliceable()).isFalse();
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

    private static DialogFragment picker(Fragment host) {
        return (DialogFragment)
                host.getChildFragmentManager()
                        .findFragmentByTag(YrrpColorPickerDialogFragment.TAG);
    }

    /** Delivers {@code result} the way the picker does, to the host's child fragment manager. */
    private void confirm(Bundle result) {
        mHostScenario.onFragment(
                host ->
                        host.getChildFragmentManager()
                                .setFragmentResult(
                                        YrrpColorPickerDialogFragment.RESULT_KEY, result));
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static Bundle result(int rgb) {
        final Bundle result = new Bundle();
        result.putInt(YrrpColorPickerDialogFragment.RESULT_RGB, rgb);
        return result;
    }

    private String summary() {
        return String.valueOf(mPreference.getSummary());
    }

    private int observerCount(String key) {
        return shadowOf(mContentResolver)
                .getContentObservers(Settings.Secure.getUriFor(key))
                .size();
    }

    private void putSecure(String key, int value) {
        Settings.Secure.putInt(mContentResolver, key, value);
    }

    private int raw(String key) {
        return Settings.Secure.getInt(mContentResolver, key, MISSING);
    }
}
