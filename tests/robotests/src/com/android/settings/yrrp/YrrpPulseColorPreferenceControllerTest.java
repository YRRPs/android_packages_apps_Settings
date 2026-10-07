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
    public void updateState_missingSettings_showsDefaultArgbWithoutWriting() {
        mController.updateState(mPreference);

        assertThat(summary()).isEqualTo("#D9FFFFFF");
        assertThat(mBackend.mWrites).isEmpty();
    }

    @Test
    public void updateState_storedColorAndAlpha_showsArgb() {
        putSecure(YrrpSettingsStore.PULSE_COLOR, 0xFF00FF);
        putSecure(YrrpSettingsStore.PULSE_ALPHA, 128);

        mController.updateState(mPreference);

        assertThat(summary()).isEqualTo("#80FF00FF");
    }

    @Test
    public void updateState_outOfRangeStoredValues_showsNormalizedArgbWithoutWriting() {
        putSecure(YrrpSettingsStore.PULSE_COLOR, 0xAB123456);
        putSecure(YrrpSettingsStore.PULSE_ALPHA, 0);

        mController.updateState(mPreference);

        assertThat(summary()).isEqualTo("#1A123456");
        assertThat(mBackend.mWrites).isEmpty();
        assertThat(raw(YrrpSettingsStore.PULSE_ALPHA)).isEqualTo(0);
    }

    @Test
    public void updateState_pulseOff_disablesRow() {
        putSecure(YrrpSettingsStore.PULSE_ENABLED, 0);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_pulseOn_enablesRow() {
        putSecure(YrrpSettingsStore.PULSE_ENABLED, 1);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
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
    public void externalColorChange_whileStarted_refreshesSummary() {
        mLifecycle.handleLifecycleEvent(ON_START);
        mController.updateState(mPreference);

        putSecure(YrrpSettingsStore.PULSE_ALPHA, 128);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(summary()).isEqualTo("#80FFFFFF");
    }

    @Test
    public void onStart_observesColorAlphaAndEnabled() {
        mLifecycle.handleLifecycleEvent(ON_START);

        assertThat(observerCount(YrrpSettingsStore.PULSE_COLOR)).isEqualTo(1);
        assertThat(observerCount(YrrpSettingsStore.PULSE_ALPHA)).isEqualTo(1);
        assertThat(observerCount(YrrpSettingsStore.PULSE_ENABLED)).isEqualTo(1);
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
    public void handlePreferenceTreeClick_opensPickerAtStoredColorWithoutWriting() {
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
                    assertThat(hex.getText().toString()).isEqualTo("#80FF00FF");
                });
        assertThat(mBackend.mWrites).isEmpty();
    }

    @Test
    public void colorConfirmed_writesColorThenAlphaAndShowsThem() {
        launchHost();

        confirm(result(0xFF00FF, 128));

        assertThat(mBackend.mWrites)
                .containsExactly("lineage_pulse_color=" + 0xFF00FF, "lineage_pulse_alpha=128")
                .inOrder();
        assertThat(summary()).isEqualTo("#80FF00FF");
    }

    @Test
    public void colorConfirmed_masksRgbAndClampsHighAlpha() {
        launchHost();

        confirm(result(0xAB123456, 300));

        assertThat(mBackend.mWrites)
                .containsExactly("lineage_pulse_color=" + 0x123456, "lineage_pulse_alpha=255")
                .inOrder();
    }

    @Test
    public void colorConfirmed_clampsLowAlpha() {
        launchHost();

        confirm(result(0x123456, 0));

        assertThat(mBackend.mWrites)
                .containsExactly("lineage_pulse_color=" + 0x123456, "lineage_pulse_alpha=26")
                .inOrder();
    }

    @Test
    public void colorConfirmed_missingAlpha_writesNothingAndShowsPersisted() {
        putSecure(YrrpSettingsStore.PULSE_ALPHA, 128);
        launchHost();
        final Bundle result = new Bundle();
        result.putInt(YrrpColorPickerDialogFragment.RESULT_RGB, 0x00FF00);

        confirm(result);

        assertThat(mBackend.mWrites).isEmpty();
        assertThat(summary()).isEqualTo("#80FFFFFF");
    }

    @Test
    public void colorConfirmed_missingRgb_writesNothing() {
        launchHost();
        final Bundle result = new Bundle();
        result.putInt(YrrpColorPickerDialogFragment.RESULT_ALPHA, 128);

        confirm(result);

        assertThat(mBackend.mWrites).isEmpty();
    }

    @Test
    public void colorConfirmed_colorWriteFails_skipsAlphaAndShowsPersisted() {
        putSecure(YrrpSettingsStore.PULSE_COLOR, 0x00FF00);
        putSecure(YrrpSettingsStore.PULSE_ALPHA, 128);
        mBackend.mFailingKeys.add(YrrpSettingsStore.PULSE_COLOR);
        launchHost();

        confirm(result(0xFF00FF, 255));

        assertThat(mBackend.mWrites).containsExactly("lineage_pulse_color=" + 0xFF00FF);
        assertThat(raw(YrrpSettingsStore.PULSE_ALPHA)).isEqualTo(128);
        assertThat(summary()).isEqualTo("#8000FF00");
    }

    @Test
    public void colorConfirmed_alphaWriteFails_showsBothPersistedValues() {
        putSecure(YrrpSettingsStore.PULSE_COLOR, 0x00FF00);
        putSecure(YrrpSettingsStore.PULSE_ALPHA, 128);
        mBackend.mFailingKeys.add(YrrpSettingsStore.PULSE_ALPHA);
        launchHost();

        confirm(result(0xFF00FF, 255));

        assertThat(mBackend.mWrites)
                .containsExactly("lineage_pulse_color=" + 0xFF00FF, "lineage_pulse_alpha=255")
                .inOrder();
        assertThat(summary()).isEqualTo("#80FF00FF");
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

    private static Bundle result(int rgb, int alpha) {
        final Bundle result = new Bundle();
        result.putInt(YrrpColorPickerDialogFragment.RESULT_RGB, rgb);
        result.putInt(YrrpColorPickerDialogFragment.RESULT_ALPHA, alpha);
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
