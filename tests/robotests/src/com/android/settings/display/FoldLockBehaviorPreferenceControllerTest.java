/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.display;


import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.settings.R;

import com.android.internal.foldables.FoldLockSettingAvailabilityProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class FoldLockBehaviorPreferenceControllerTest {

    @Mock
    private FoldLockSettingAvailabilityProvider mFoldLockSettingAvailabilityProvider;
    private Context mContext;
    private FoldLockBehaviorPreferenceController mController;
    private boolean mDuoActive;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mFoldLockSettingAvailabilityProvider = Mockito.mock(
                FoldLockSettingAvailabilityProvider.class);
        mController = new FoldLockBehaviorPreferenceController(mContext, "key",
                mFoldLockSettingAvailabilityProvider);
    }

    @Test
    public void updateState_duoActive_disablesTheRowAndShowsAlways() {
        Settings.System.putString(mContext.getContentResolver(),
                Settings.System.FOLD_LOCK_BEHAVIOR,
                FoldLockBehaviorSettings.SETTING_VALUE_SLEEP_ON_FOLD);
        mDuoActive = true;
        final Preference preference = new Preference(mContext);

        duoAwareController().updateState(preference);

        assertThat(preference.isEnabled()).isFalse();
        assertThat(preference.getSummary().toString()).isEqualTo(mContext.getString(
                R.string.yrrp_fold_lock_set_by_duo_summary,
                mContext.getString(R.string.stay_awake_on_fold_title)));
    }

    @Test
    public void updateState_duoInactive_enablesTheRowAndShowsTheStoredValue() {
        Settings.System.putString(mContext.getContentResolver(),
                Settings.System.FOLD_LOCK_BEHAVIOR,
                FoldLockBehaviorSettings.SETTING_VALUE_SLEEP_ON_FOLD);
        final Preference preference = new Preference(mContext);
        preference.setEnabled(false);

        duoAwareController().updateState(preference);

        assertThat(preference.isEnabled()).isTrue();
        assertThat(preference.getSummary().toString())
                .isEqualTo(mContext.getString(R.string.sleep_on_fold_title));
    }

    private FoldLockBehaviorPreferenceController duoAwareController() {
        return new FoldLockBehaviorPreferenceController(mContext, "key",
                mFoldLockSettingAvailabilityProvider, () -> mDuoActive);
    }

    @Test
    public void getAvailabilityStatus_withConfigNoShow_returnUnsupported() {
        when(mFoldLockSettingAvailabilityProvider.isFoldLockBehaviorAvailable()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_withConfigNoShow_returnAvailable() {
        when(mFoldLockSettingAvailabilityProvider.isFoldLockBehaviorAvailable()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }
}
