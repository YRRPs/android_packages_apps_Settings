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
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Looper;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class YrrpPulseEntryPreferenceControllerTest {
    private static final String PREF_KEY = "yrrp_pulse";

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ContentResolver mContentResolver;
    private YrrpPulseEntryPreferenceController mController;
    private Preference mPreference;
    private PreferenceScreen mScreen;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        mContentResolver = mContext.getContentResolver();
        mController = new YrrpPulseEntryPreferenceController(mContext, PREF_KEY);
        mPreference = new Preference(mContext);
        mPreference.setKey(PREF_KEY);
        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mScreen.addPreference(mPreference);
        final LifecycleOwner lifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(lifecycleOwner);
        mLifecycle.addObserver(mController);
    }

    @Test
    public void getSummary_missingSetting_isOffWithoutWriting() {
        assertThat(mController.getSummary().toString()).isEqualTo(text(R.string.switch_off_text));
        assertThat(rawEnabled()).isNull();
    }

    @Test
    public void getSummary_zero_isOff() {
        putEnabled(0);

        assertThat(mController.getSummary().toString()).isEqualTo(text(R.string.switch_off_text));
    }

    @Test
    public void getSummary_one_isOn() {
        putEnabled(1);

        assertThat(mController.getSummary().toString()).isEqualTo(text(R.string.switch_on_text));
    }

    @Test
    public void getSummary_otherNonZero_isOnWithoutWriting() {
        putEnabled(7);

        assertThat(mController.getSummary().toString()).isEqualTo(text(R.string.switch_on_text));
        assertThat(rawEnabled()).isEqualTo("7");
    }

    @Test
    public void updateState_showsSummaryWithoutWriting() {
        putEnabled(7);
        mController.displayPreference(mScreen);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary().toString()).isEqualTo(text(R.string.switch_on_text));
        assertThat(rawEnabled()).isEqualTo("7");
    }

    @Test
    public void externalChange_whileStarted_refreshesSummary() {
        mController.displayPreference(mScreen);
        mLifecycle.handleLifecycleEvent(ON_START);
        mController.updateState(mPreference);

        putEnabled(1);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mPreference.getSummary().toString()).isEqualTo(text(R.string.switch_on_text));
    }

    @Test
    public void onStop_unregistersObserver() {
        mLifecycle.handleLifecycleEvent(ON_START);
        mLifecycle.handleLifecycleEvent(ON_STOP);

        assertThat(
                        shadowOf(mContentResolver)
                                .getContentObservers(
                                        Settings.Secure.getUriFor(YrrpSettingsStore.PULSE_ENABLED)))
                .isEmpty();
    }

    private String text(int resId) {
        return mContext.getString(resId);
    }

    private void putEnabled(int value) {
        Settings.Secure.putInt(mContentResolver, YrrpSettingsStore.PULSE_ENABLED, value);
    }

    private String rawEnabled() {
        return Settings.Secure.getString(mContentResolver, YrrpSettingsStore.PULSE_ENABLED);
    }
}
