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

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.SliderPreference;

import com.google.android.material.slider.LabelFormatter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class YrrpPulseBoostPreferenceControllerTest {
    /** Read back when a key was never written. */
    private static final int MISSING = Integer.MIN_VALUE;

    private static final String PREF_KEY = "yrrp_pulse_boost";

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ContentResolver mContentResolver;
    private YrrpRecordingSecureBackend mBackend;
    private YrrpPulseBoostPreferenceController mController;
    private SliderPreference mPreference;
    private PreferenceScreen mScreen;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        mContentResolver = mContext.getContentResolver();
        mBackend = new YrrpRecordingSecureBackend(mContext);
        mController =
                new YrrpPulseBoostPreferenceController(mContext, PREF_KEY, mBackend.newStore());
        mPreference = spy(new SliderPreference(mContext));
        mPreference.setKey(PREF_KEY);
        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mScreen.addPreference(mPreference);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mLifecycle.addObserver(mController);
    }

    @Test
    public void range_isZeroToOneHundred() {
        assertThat(mController.getMin()).isEqualTo(0);
        assertThat(mController.getMax()).isEqualTo(100);
    }

    @Test
    public void displayPreference_configuresSliderInSingleSteps() {
        mController.displayPreference(mScreen);

        assertThat(mPreference.getMin()).isEqualTo(0);
        assertThat(mPreference.getMax()).isEqualTo(100);
        assertThat(mPreference.getSliderIncrement()).isEqualTo(1);
        assertThat(mPreference.getUpdatesContinuously()).isTrue();
        assertThat(mPreference.getShowSliderValue()).isTrue();
        assertThat(mPreference.getValue()).isEqualTo(20);
    }

    @Test
    public void displayPreference_labelFormatterShowsOffAtZero() {
        mController.displayPreference(mScreen);

        final ArgumentCaptor<LabelFormatter> formatter =
                ArgumentCaptor.forClass(LabelFormatter.class);
        verify(mPreference).setLabelFormater(formatter.capture());
        assertThat(formatter.getValue().getFormattedValue(0f)).isEqualTo("Off");
        assertThat(formatter.getValue().getFormattedValue(20f)).isEqualTo("20");
        assertThat(formatter.getValue().getFormattedValue(54.6f)).isEqualTo("55");
    }

    @Test
    public void displayPreference_labelsSliderEndsLinearAndLifted() {
        mController.displayPreference(mScreen);

        verify(mPreference).setTextStart(R.string.yrrp_pulse_boost_linear);
        verify(mPreference).setTextEnd(R.string.yrrp_pulse_boost_lifted);
    }

    @Test
    public void updateState_summaryExplainsBoostAndStateDescriptionIsValueOnly() {
        displayAndUpdate();

        assertThat(String.valueOf(mPreference.getSummary()))
                .isEqualTo("Makes quiet sounds move the bars more \u00b7 20");
        assertThat(lastStateDescription()).isEqualTo("20");
    }

    @Test
    public void updateState_unset_showsDefaultTwenty() {
        displayAndUpdate();

        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo(summary("20"));
        assertThat(lastStateDescription()).isEqualTo("20");
        assertThat(mController.getSliderPosition()).isEqualTo(20);
    }

    @Test
    public void updateState_zero_showsOff() {
        putSecure(YrrpSettingsStore.PULSE_BOOST, 0);

        displayAndUpdate();

        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo(summary("Off"));
        assertThat(mController.getSliderPosition()).isEqualTo(0);
    }

    @Test
    public void updateState_belowRange_showsOff() {
        putSecure(YrrpSettingsStore.PULSE_BOOST, -7);

        displayAndUpdate();

        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo(summary("Off"));
        assertThat(mController.getSliderPosition()).isEqualTo(0);
    }

    @Test
    public void updateState_aboveRange_showsMaximum() {
        putSecure(YrrpSettingsStore.PULSE_BOOST, 500);

        displayAndUpdate();

        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo(summary("100"));
        assertThat(mController.getSliderPosition()).isEqualTo(100);
    }

    @Test
    public void openingPage_neverWrites() {
        putSecure(YrrpSettingsStore.PULSE_BOOST, 500);

        displayAndUpdate();
        mLifecycle.handleLifecycleEvent(ON_START);

        assertThat(mBackend.mWrites).isEmpty();
        assertThat(raw(YrrpSettingsStore.PULSE_BOOST)).isEqualTo(500);
    }

    @Test
    public void setSliderPosition_writesValueOnce() {
        mController.displayPreference(mScreen);

        assertThat(mController.setSliderPosition(55)).isTrue();

        assertThat(mBackend.mWrites).containsExactly("lineage_pulse_log_boost=55");
        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo(summary("55"));
    }

    @Test
    public void setSliderPosition_outOfRange_writesClampedValue() {
        mController.displayPreference(mScreen);

        assertThat(mController.setSliderPosition(-1)).isTrue();
        assertThat(mController.setSliderPosition(101)).isTrue();

        assertThat(mBackend.mWrites)
                .containsExactly("lineage_pulse_log_boost=0", "lineage_pulse_log_boost=100")
                .inOrder();
    }

    @Test
    public void setSliderPosition_writeFails_returnsFalseAndShowsPersisted() {
        putSecure(YrrpSettingsStore.PULSE_BOOST, 40);
        mController.displayPreference(mScreen);
        mBackend.mFailingKeys.add(YrrpSettingsStore.PULSE_BOOST);

        assertThat(mController.setSliderPosition(60)).isFalse();

        assertThat(mBackend.mWrites).containsExactly("lineage_pulse_log_boost=60");
        assertThat(raw(YrrpSettingsStore.PULSE_BOOST)).isEqualTo(40);
        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo(summary("40"));
    }

    @Test
    public void updateState_pulseOff_disablesRow() {
        putSecure(YrrpSettingsStore.PULSE_ENABLED, 0);

        displayAndUpdate();

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_pulseOn_enablesRow() {
        putSecure(YrrpSettingsStore.PULSE_ENABLED, 1);

        displayAndUpdate();

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void externalChange_whileStarted_refreshesRow() {
        displayAndUpdate();
        mLifecycle.handleLifecycleEvent(ON_START);

        putSecure(YrrpSettingsStore.PULSE_ENABLED, 1);
        putSecure(YrrpSettingsStore.PULSE_BOOST, 70);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo(summary("70"));
        assertThat(mPreference.getValue()).isEqualTo(70);
        assertThat(mBackend.mWrites).isEmpty();
    }

    @Test
    public void isSliceable_isFalse() {
        assertThat(mController.isSliceable()).isFalse();
    }

    @Test
    public void getSliceHighlightMenuRes_isYrrpMenu() {
        assertThat(mController.getSliceHighlightMenuRes()).isEqualTo(R.string.yrrp_menu_key);
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

    /** The summary the row shows for a formatted value: the boost explanation, then the value. */
    private String summary(String value) {
        return mContext.getString(R.string.yrrp_pulse_boost_summary, value);
    }
}
