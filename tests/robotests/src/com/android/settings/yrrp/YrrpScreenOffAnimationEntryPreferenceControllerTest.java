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
public class YrrpScreenOffAnimationEntryPreferenceControllerTest {
    private static final String PREF_KEY = "yrrp_screen_off_animation";

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ContentResolver mContentResolver;
    private YrrpScreenOffAnimationEntryPreferenceController mController;
    private Preference mPreference;
    private PreferenceScreen mScreen;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        mContentResolver = mContext.getContentResolver();
        mController = new YrrpScreenOffAnimationEntryPreferenceController(mContext, PREF_KEY);
        mPreference = new Preference(mContext);
        mPreference.setKey(PREF_KEY);
        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mScreen.addPreference(mPreference);
        final LifecycleOwner lifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(lifecycleOwner);
        mLifecycle.addObserver(mController);
    }

    @Test
    public void getSummary_missingSetting_isStockWithoutWriting() {
        assertThat(mController.getSummary().toString()).isEqualTo(stock());
        assertThat(rawAnimation()).isNull();
    }

    @Test
    public void getSummary_zero_isStock() {
        putAnimation(0);

        assertThat(mController.getSummary().toString()).isEqualTo(stock());
    }

    @Test
    public void getSummary_one_isCrt() {
        putAnimation(1);

        assertThat(mController.getSummary().toString()).isEqualTo(crt());
    }

    @Test
    public void getSummary_unknownValue_isStockWithoutWriting() {
        putAnimation(99);

        assertThat(mController.getSummary().toString()).isEqualTo(stock());
        assertThat(rawAnimation()).isEqualTo("99");
    }

    @Test
    public void updateState_showsSummaryWithoutWriting() {
        putAnimation(99);
        mController.displayPreference(mScreen);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary().toString()).isEqualTo(stock());
        assertThat(rawAnimation()).isEqualTo("99");
    }

    @Test
    public void externalChange_whileStarted_refreshesSummary() {
        mController.displayPreference(mScreen);
        mLifecycle.handleLifecycleEvent(ON_START);
        mController.updateState(mPreference);

        putAnimation(1);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mPreference.getSummary().toString()).isEqualTo(crt());
    }

    private String stock() {
        return mContext.getString(R.string.yrrp_screen_off_animation_stock);
    }

    private String crt() {
        return mContext.getString(R.string.yrrp_screen_off_animation_crt);
    }

    private void putAnimation(int value) {
        Settings.Secure.putInt(mContentResolver, YrrpSettingsStore.SCREEN_OFF_ANIMATION, value);
    }

    private String rawAnimation() {
        return Settings.Secure.getString(mContentResolver, YrrpSettingsStore.SCREEN_OFF_ANIMATION);
    }
}
