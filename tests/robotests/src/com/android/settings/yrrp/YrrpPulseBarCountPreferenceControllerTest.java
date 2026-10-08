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
public class YrrpPulseBarCountPreferenceControllerTest {
    /** Read back when a key was never written. */
    private static final int MISSING = Integer.MIN_VALUE;

    private static final String PREF_KEY = "yrrp_pulse_bar_count";
    private static final String KEY = YrrpSettingsStore.PULSE_BAR_COUNT;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ContentResolver mContentResolver;
    private YrrpRecordingSecureBackend mBackend;
    private YrrpPulseBarCountPreferenceController mController;
    private SliderPreference mPreference;
    private PreferenceScreen mScreen;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        mContentResolver = mContext.getContentResolver();
        mBackend = new YrrpRecordingSecureBackend(mContext);
        mController =
                new YrrpPulseBarCountPreferenceController(mContext, PREF_KEY, mBackend.newStore());
        mPreference = spy(new SliderPreference(mContext));
        mPreference.setKey(PREF_KEY);
        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mScreen.addPreference(mPreference);
        final LifecycleOwner owner = () -> mLifecycle;
        mLifecycle = new Lifecycle(owner);
        mLifecycle.addObserver(mController);
    }

    @Test
    public void displayPreference_configuresSliderOnTheStepGrid() {
        mController.displayPreference(mScreen);

        assertThat(mPreference.getMin()).isEqualTo(16);
        assertThat(mPreference.getMax()).isEqualTo(64);
        assertThat(mPreference.getSliderIncrement()).isEqualTo(4);
        assertThat(mPreference.getUpdatesContinuously()).isTrue();
        assertThat(mPreference.getValue()).isEqualTo(32);
    }

    @Test
    public void displayPreference_labelFormatterShowsUnit() {
        mController.displayPreference(mScreen);

        final ArgumentCaptor<LabelFormatter> formatter =
                ArgumentCaptor.forClass(LabelFormatter.class);
        verify(mPreference).setLabelFormater(formatter.capture());
        assertThat(formatter.getValue().getFormattedValue(32f)).isEqualTo("32 bars");
    }

    @Test
    public void updateState_offGrid_showsExactTextAndOnGridThumb() {
        putSecure(KEY, 50);

        displayAndUpdate();

        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo("50 bars");
        assertThat(lastStateDescription()).isEqualTo("50 bars");
        assertThat(mController.getSliderPosition()).isEqualTo(52);
    }

    @Test
    public void updateState_outOfRange_showsClampedValue() {
        putSecure(KEY, 100);
        displayAndUpdate();
        assertThat(mController.getSliderPosition()).isEqualTo(64);

        putSecure(KEY, 0);
        mController.updateState(mPreference);
        assertThat(mController.getSliderPosition()).isEqualTo(16);
    }

    @Test
    public void openingPage_neverWrites() {
        putSecure(KEY, 50);

        displayAndUpdate();
        mLifecycle.handleLifecycleEvent(ON_START);

        assertThat(mBackend.mWrites).isEmpty();
        assertThat(raw(KEY)).isEqualTo(50);
    }

    @Test
    public void setSliderPosition_offGrid_writesSnappedValueOnce() {
        mController.displayPreference(mScreen);

        assertThat(mController.setSliderPosition(17)).isTrue();

        assertThat(mBackend.mWrites).containsExactly(KEY + "=16");
        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo("16 bars");
    }

    @Test
    public void setSliderPosition_outOfRange_writesClampedValue() {
        mController.displayPreference(mScreen);

        mController.setSliderPosition(0);
        mController.setSliderPosition(100);

        assertThat(mBackend.mWrites)
                .containsExactly(KEY + "=16", KEY + "=64")
                .inOrder();
    }

    @Test
    public void setSliderPosition_writeFails_returnsFalseAndShowsPersisted() {
        putSecure(KEY, 32);
        mController.displayPreference(mScreen);
        mBackend.mFailingKeys.add(KEY);

        assertThat(mController.setSliderPosition(64)).isFalse();

        assertThat(raw(KEY)).isEqualTo(32);
        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo("32 bars");
    }

    @Test
    public void updateState_followsPulseSwitch() {
        putSecure(YrrpSettingsStore.PULSE_ENABLED, 0);
        displayAndUpdate();
        assertThat(mPreference.isEnabled()).isFalse();

        putSecure(YrrpSettingsStore.PULSE_ENABLED, 1);
        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void externalChange_whileStarted_refreshesRow() {
        displayAndUpdate();
        mLifecycle.handleLifecycleEvent(ON_START);

        putSecure(KEY, 16);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo("16 bars");
        assertThat(mPreference.getValue()).isEqualTo(16);
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
