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

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Looper;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.SliderPreference;

import com.google.android.material.slider.LabelFormatter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class YrrpPulseOpacityPreferenceControllerTest {
    /** Read back when a key was never written. */
    private static final int MISSING = Integer.MIN_VALUE;

    private static final String PREF_KEY = "yrrp_pulse_opacity";

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ContentResolver mContentResolver;
    private YrrpRecordingSecureBackend mBackend;
    private YrrpPulseOpacityPreferenceController mController;
    private SliderPreference mPreference;
    private PreferenceScreen mScreen;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        mContentResolver = mContext.getContentResolver();
        mBackend = new YrrpRecordingSecureBackend(mContext);
        mController =
                new YrrpPulseOpacityPreferenceController(mContext, PREF_KEY, mBackend.newStore());
        mPreference = spy(new SliderPreference(mContext));
        mPreference.setKey(PREF_KEY);
        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mScreen.addPreference(mPreference);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mLifecycle.addObserver(mController);
    }

    @Test
    public void range_isStoredAlphaBounds() {
        assertThat(mController.getMin()).isEqualTo(26);
        assertThat(mController.getMax()).isEqualTo(255);
    }

    @Test
    public void displayPreference_configuresSliderAtDefaultAlpha() {
        mController.displayPreference(mScreen);

        assertThat(mPreference.getMin()).isEqualTo(26);
        assertThat(mPreference.getMax()).isEqualTo(255);
        assertThat(mPreference.getSliderIncrement()).isEqualTo(1);
        assertThat(mPreference.getUpdatesContinuously()).isTrue();
        assertThat(mPreference.getShowSliderValue()).isTrue();
        assertThat(mPreference.getValue()).isEqualTo(217);
    }

    @Test
    public void displayPreference_labelFormatterShowsPercent() {
        mController.displayPreference(mScreen);

        final ArgumentCaptor<LabelFormatter> formatter =
                ArgumentCaptor.forClass(LabelFormatter.class);
        verify(mPreference).setLabelFormater(formatter.capture());
        assertThat(formatter.getValue().getFormattedValue(217f)).isEqualTo("85%");
        assertThat(formatter.getValue().getFormattedValue(128f)).isEqualTo("50%");
    }

    @Test
    public void updateState_showsStoredAlphaAsPercent() {
        putSecure(YrrpSettingsStore.PULSE_ALPHA, 128);

        displayAndUpdate();

        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo("50%");
        assertThat(lastStateDescription()).isEqualTo("50%");
        assertThat(mPreference.getValue()).isEqualTo(128);
    }

    @Test
    public void updateState_outOfRange_showsClampedWithoutWriting() {
        putSecure(YrrpSettingsStore.PULSE_ALPHA, 0);
        displayAndUpdate();
        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo("10%");
        assertThat(mController.getSliderPosition()).isEqualTo(26);

        putSecure(YrrpSettingsStore.PULSE_ALPHA, 300);
        mController.updateState(mPreference);
        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo("100%");
        assertThat(mController.getSliderPosition()).isEqualTo(255);

        assertThat(mBackend.mWrites).isEmpty();
        assertThat(raw(YrrpSettingsStore.PULSE_ALPHA)).isEqualTo(300);
    }

    @Test
    public void setSliderPosition_writesClampedAlphaOnce() {
        mController.displayPreference(mScreen);

        assertThat(mController.setSliderPosition(64)).isTrue();
        assertThat(mController.setSliderPosition(10)).isTrue();

        assertThat(mBackend.mWrites)
                .containsExactly("lineage_pulse_alpha=64", "lineage_pulse_alpha=26")
                .inOrder();
        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo("10%");
    }

    @Test
    public void setSliderPosition_writeFails_returnsFalseAndShowsPersisted() {
        putSecure(YrrpSettingsStore.PULSE_ALPHA, 255);
        mController.displayPreference(mScreen);
        mBackend.mFailingKeys.add(YrrpSettingsStore.PULSE_ALPHA);

        assertThat(mController.setSliderPosition(128)).isFalse();

        assertThat(raw(YrrpSettingsStore.PULSE_ALPHA)).isEqualTo(255);
        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo("100%");
    }

    @Test
    public void updateState_pulseOff_disablesRow() {
        putSecure(YrrpSettingsStore.PULSE_ENABLED, 0);

        displayAndUpdate();

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_pulseOnInEveryColorMode_enablesRow() {
        putSecure(YrrpSettingsStore.PULSE_ENABLED, 1);
        for (int mode :
                new int[] {
                    YrrpSettingsStore.PULSE_COLOR_MODE_SOLID,
                    YrrpSettingsStore.PULSE_COLOR_MODE_MATCH_THEME
                }) {
            putSecure(YrrpSettingsStore.PULSE_COLOR_MODE, mode);

            displayAndUpdate();

            assertThat(mPreference.isEnabled()).isTrue();
        }
    }

    @Test
    public void externalChange_whileStarted_refreshesRow() {
        displayAndUpdate();
        mLifecycle.handleLifecycleEvent(ON_START);

        putSecure(YrrpSettingsStore.PULSE_ENABLED, 1);
        putSecure(YrrpSettingsStore.PULSE_ALPHA, 64);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo("25%");
        assertThat(mPreference.getValue()).isEqualTo(64);
        assertThat(mBackend.mWrites).isEmpty();
    }

    @Test
    public void isSliceable_isFalse() {
        assertThat(mController.isSliceable()).isFalse();
    }

    private void displayAndUpdate() {
        mController.displayPreference(mScreen);
        mController.updateState(mPreference);
    }

    private String lastStateDescription() {
        final ArgumentCaptor<CharSequence> description =
                ArgumentCaptor.forClass(CharSequence.class);
        verify(mPreference, atLeastOnce()).setSliderStateDescription(description.capture());
        return String.valueOf(description.getValue());
    }

    private void putSecure(String key, int value) {
        Settings.Secure.putInt(mContentResolver, key, value);
    }

    private int raw(String key) {
        return Settings.Secure.getInt(mContentResolver, key, MISSING);
    }
}
