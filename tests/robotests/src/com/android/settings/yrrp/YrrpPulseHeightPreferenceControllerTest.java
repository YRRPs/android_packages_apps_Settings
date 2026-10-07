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
public class YrrpPulseHeightPreferenceControllerTest {
    private static final String PREF_KEY = "yrrp_pulse_height";

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ContentResolver mContentResolver;
    private YrrpRecordingSecureBackend mBackend;
    private YrrpPulseHeightPreferenceController mController;
    private SliderPreference mPreference;
    private PreferenceScreen mScreen;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        mContentResolver = mContext.getContentResolver();
        mBackend = new YrrpRecordingSecureBackend(mContext);
        mController =
                new YrrpPulseHeightPreferenceController(mContext, PREF_KEY, mBackend.newStore());
        mPreference = spy(new SliderPreference(mContext));
        mPreference.setKey(PREF_KEY);
        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mScreen.addPreference(mPreference);
        final LifecycleOwner lifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(lifecycleOwner);
        mLifecycle.addObserver(mController);
    }

    @Test
    public void range_isEightToNinetySixDp() {
        assertThat(mController.getMin()).isEqualTo(8);
        assertThat(mController.getMax()).isEqualTo(96);
    }

    @Test
    public void displayPreference_configuresSliderOnTheStepGrid() {
        mController.displayPreference(mScreen);

        assertThat(mPreference.getMin()).isEqualTo(8);
        assertThat(mPreference.getMax()).isEqualTo(96);
        assertThat(mPreference.getSliderIncrement()).isEqualTo(4);
        assertThat(mPreference.getUpdatesContinuously()).isTrue();
        assertThat(mPreference.getShowSliderValue()).isTrue();
        assertThat(mPreference.getValue()).isEqualTo(48);
    }

    @Test
    public void displayPreference_labelFormatterShowsDp() {
        mController.displayPreference(mScreen);

        final ArgumentCaptor<LabelFormatter> formatter =
                ArgumentCaptor.forClass(LabelFormatter.class);
        verify(mPreference).setLabelFormater(formatter.capture());
        assertThat(formatter.getValue().getFormattedValue(48f)).isEqualTo("48 dp");
        assertThat(formatter.getValue().getFormattedValue(51.6f)).isEqualTo("52 dp");
    }

    @Test
    public void updateState_offGridTen_showsExactTextAndOnGridThumb() {
        putSecure(YrrpSettingsStore.PULSE_HEIGHT_DP, 10);

        displayAndUpdate();

        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo("10 dp");
        assertThat(lastStateDescription()).isEqualTo("10 dp");
        assertThat(mController.getSliderPosition()).isEqualTo(12);
        assertThat(mPreference.getValue()).isEqualTo(12);
    }

    @Test
    public void updateState_offGridFifty_showsExactTextAndOnGridThumb() {
        putSecure(YrrpSettingsStore.PULSE_HEIGHT_DP, 50);

        displayAndUpdate();

        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo("50 dp");
        assertThat(lastStateDescription()).isEqualTo("50 dp");
        assertThat(mController.getSliderPosition()).isEqualTo(52);
    }

    @Test
    public void updateState_belowRange_showsMinimum() {
        putSecure(YrrpSettingsStore.PULSE_HEIGHT_DP, 0);

        displayAndUpdate();

        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo("8 dp");
        assertThat(mController.getSliderPosition()).isEqualTo(8);
    }

    @Test
    public void updateState_aboveRange_showsMaximum() {
        putSecure(YrrpSettingsStore.PULSE_HEIGHT_DP, 200);

        displayAndUpdate();

        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo("96 dp");
        assertThat(mController.getSliderPosition()).isEqualTo(96);
    }

    @Test
    public void openingPage_neverWrites() {
        putSecure(YrrpSettingsStore.PULSE_HEIGHT_DP, 10);

        displayAndUpdate();
        mLifecycle.handleLifecycleEvent(ON_START);

        assertThat(mBackend.mWrites).isEmpty();
        assertThat(raw(YrrpSettingsStore.PULSE_HEIGHT_DP)).isEqualTo("10");
    }

    @Test
    public void setSliderPosition_nine_writesEightOnce() {
        mController.displayPreference(mScreen);

        assertThat(mController.setSliderPosition(9)).isTrue();

        assertThat(mBackend.mWrites).containsExactly("lineage_pulse_height_dp=8");
        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo("8 dp");
    }

    @Test
    public void setSliderPosition_ten_writesTwelveOnce() {
        mController.displayPreference(mScreen);

        assertThat(mController.setSliderPosition(10)).isTrue();

        assertThat(mBackend.mWrites).containsExactly("lineage_pulse_height_dp=12");
        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo("12 dp");
    }

    @Test
    public void onPreferenceChange_writesOncePerCall() {
        mController.displayPreference(mScreen);

        assertThat(mController.onPreferenceChange(mPreference, 20)).isTrue();
        assertThat(mController.onPreferenceChange(mPreference, 24)).isTrue();

        assertThat(mBackend.mWrites)
                .containsExactly("lineage_pulse_height_dp=20", "lineage_pulse_height_dp=24")
                .inOrder();
    }

    @Test
    public void setSliderPosition_writeFails_returnsFalseAndShowsPersisted() {
        putSecure(YrrpSettingsStore.PULSE_HEIGHT_DP, 40);
        mController.displayPreference(mScreen);
        mBackend.mFailingKeys.add(YrrpSettingsStore.PULSE_HEIGHT_DP);

        assertThat(mController.setSliderPosition(60)).isFalse();

        assertThat(mBackend.mWrites).containsExactly("lineage_pulse_height_dp=60");
        assertThat(raw(YrrpSettingsStore.PULSE_HEIGHT_DP)).isEqualTo("40");
        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo("40 dp");
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
        putSecure(YrrpSettingsStore.PULSE_HEIGHT_DP, 20);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(String.valueOf(mPreference.getSummary())).isEqualTo("20 dp");
        assertThat(mPreference.getValue()).isEqualTo(20);
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

    private String raw(String key) {
        return Settings.Secure.getString(mContentResolver, key);
    }
}
