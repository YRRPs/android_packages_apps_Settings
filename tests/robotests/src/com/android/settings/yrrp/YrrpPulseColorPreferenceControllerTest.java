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
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class YrrpPulseColorPreferenceControllerTest {
    /** Read back when a key was never written. */
    private static final int MISSING = Integer.MIN_VALUE;

    private static final String PREF_KEY = "yrrp_pulse_color";

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ContentResolver mContentResolver;
    private YrrpRecordingSecureBackend mBackend;
    private YrrpPulseColorPreferenceController mController;
    private YrrpColorPreference mPreference;
    private PreferenceScreen mScreen;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private FragmentScenario<Fragment> mHostScenario;

    @Before
    public void setUp() {
        mContentResolver = mContext.getContentResolver();
        mBackend = new YrrpRecordingSecureBackend(mContext);
        mController =
                new YrrpPulseColorPreferenceController(mContext, PREF_KEY, mBackend.newStore());
        mPreference = new YrrpColorPreference(mContext, /* attrs= */ null);
        mPreference.setKey(PREF_KEY);
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
    public void updateState_missingSettings_showsDefaultRgbWithoutWriting() {
        mController.updateState(mPreference);

        assertThat(summary()).isEqualTo("#FFFFFF");
        assertThat(mBackend.mWrites).isEmpty();
    }

    @Test
    public void updateState_storedColor_showsRgbIgnoringAlpha() {
        putSecure(YrrpSettingsStore.PULSE_COLOR, 0xFF00FF);
        putSecure(YrrpSettingsStore.PULSE_ALPHA, 128);

        mController.updateState(mPreference);

        assertThat(summary()).isEqualTo("#FF00FF");
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
    public void updateState_pulseOff_disablesRow() {
        putSecure(YrrpSettingsStore.PULSE_ENABLED, 0);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_pulseOnInSolid_enablesRow() {
        putSecure(YrrpSettingsStore.PULSE_ENABLED, 1);
        putSecure(YrrpSettingsStore.PULSE_COLOR_MODE, YrrpSettingsStore.PULSE_COLOR_MODE_SOLID);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void updateState_pulseOnInMatchTheme_disablesVisibleRow() {
        putSecure(YrrpSettingsStore.PULSE_ENABLED, 1);
        putSecure(
                YrrpSettingsStore.PULSE_COLOR_MODE,
                YrrpSettingsStore.PULSE_COLOR_MODE_MATCH_THEME);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void updateState_pulseOnInRainbowModes_disablesVisibleRow() {
        putSecure(YrrpSettingsStore.PULSE_ENABLED, 1);
        for (int mode :
                new int[] {
                    YrrpSettingsStore.PULSE_COLOR_MODE_RAINBOW_GRADIENT,
                    YrrpSettingsStore.PULSE_COLOR_MODE_RAINBOW_CYCLE
                }) {
            putSecure(YrrpSettingsStore.PULSE_COLOR_MODE, mode);

            mController.updateState(mPreference);

            assertThat(mPreference.isEnabled()).isFalse();
            assertThat(mPreference.isVisible()).isTrue();
        }
    }

    @Test
    public void updateState_unknownColorMode_treatsAsSolid() {
        putSecure(YrrpSettingsStore.PULSE_ENABLED, 1);
        putSecure(YrrpSettingsStore.PULSE_COLOR_MODE, 99);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mBackend.mWrites).isEmpty();
    }

    @Test
    public void externalEnableChange_whileStarted_enablesRow() {
        mLifecycle.handleLifecycleEvent(ON_START);
        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isFalse();

        putSecure(YrrpSettingsStore.PULSE_ENABLED, 1);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void externalColorModeChange_whileStarted_togglesRow() {
        putSecure(YrrpSettingsStore.PULSE_ENABLED, 1);
        mLifecycle.handleLifecycleEvent(ON_START);
        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isTrue();

        putSecure(
                YrrpSettingsStore.PULSE_COLOR_MODE,
                YrrpSettingsStore.PULSE_COLOR_MODE_MATCH_THEME);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isEnabled()).isFalse();

        putSecure(YrrpSettingsStore.PULSE_COLOR_MODE, YrrpSettingsStore.PULSE_COLOR_MODE_SOLID);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreference.isEnabled()).isTrue();
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
    public void onStart_observesColorEnabledAndColorMode() {
        mLifecycle.handleLifecycleEvent(ON_START);

        assertThat(observerCount(YrrpSettingsStore.PULSE_COLOR)).isEqualTo(1);
        assertThat(observerCount(YrrpSettingsStore.PULSE_ENABLED)).isEqualTo(1);
        assertThat(observerCount(YrrpSettingsStore.PULSE_COLOR_MODE)).isEqualTo(1);
        assertThat(observerCount(YrrpSettingsStore.PULSE_ALPHA)).isEqualTo(0);
    }

    @Test
    public void isSliceable_isFalse() {
        assertThat(mController.isSliceable()).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_otherKey_isNotHandled() {
        final Preference other = new Preference(mContext);
        other.setKey("other");

        assertThat(mController.handlePreferenceTreeClick(other)).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_opensPickerAtStoredRgbWithoutWriting() {
        putSecure(YrrpSettingsStore.PULSE_COLOR, 0xFF00FF);
        putSecure(YrrpSettingsStore.PULSE_ALPHA, 128);
        launchHost();

        mHostScenario.onFragment(
                host -> {
                    assertThat(mController.handlePreferenceTreeClick(mPreference)).isTrue();
                    final DialogFragment picker =
                            (DialogFragment)
                                    host.getChildFragmentManager()
                                            .findFragmentByTag(YrrpColorPickerDialogFragment.TAG);
                    assertThat(picker).isNotNull();
                    final TextView hex = picker.requireDialog().findViewById(R.id.yrrp_color_hex);
                    assertThat(hex.getText().toString()).isEqualTo("#FF00FF");
                });
        assertThat(mBackend.mWrites).isEmpty();
    }

    @Test
    public void colorConfirmed_writesOnlyColorAndShowsIt() {
        launchHost();

        confirm(result(0xFF00FF));

        assertThat(mBackend.mWrites).containsExactly("lineage_pulse_color=" + 0xFF00FF);
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

    private void launchHost() {
        mHostScenario =
                FragmentScenario.launch(
                        Fragment.class,
                        /* fragmentArgs= */ null,
                        androidx.appcompat.R.style.Theme_AppCompat,
                        androidx.lifecycle.Lifecycle.State.RESUMED);
        mHostScenario.onFragment(host -> mController.init(host));
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
