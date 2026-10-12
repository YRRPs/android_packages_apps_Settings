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
import android.net.Uri;
import android.os.Looper;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class YrrpFoldAnimationEntryPreferenceControllerTest {
    /** Read back when a key was never written. */
    private static final int MISSING = Integer.MIN_VALUE;

    private static final String PREF_KEY = "yrrp_fold_animation_entry";

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ContentResolver mContentResolver;
    private YrrpFoldAnimationEntryPreferenceController mController;
    private Preference mPreference;
    private PreferenceScreen mScreen;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        mContentResolver = mContext.getContentResolver();
        mController = entry(true);
        mPreference = new Preference(mContext);
        mPreference.setKey(PREF_KEY);
        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mScreen.addPreference(mPreference);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mLifecycle.addObserver(mController);
    }

    @Test
    public void getSummary_missingSetting_isStockWithoutWriting() {
        assertThat(mController.getSummary().toString()).isEqualTo(label(0));
        assertThat(rawStyle()).isEqualTo(MISSING);
    }

    @Test
    public void summary_namesStoredStyle() {
        putStyle(1);
        assertThat(mController.getSummary().toString()).isEqualTo("Pixel");
        putStyle(2);
        assertThat(mController.getSummary().toString()).isEqualTo("Duo");
    }

    @Test
    public void getSummary_unknownValue_isStockWithoutWriting() {
        putStyle(99);

        assertThat(mController.getSummary().toString()).isEqualTo(label(0));
        assertThat(rawStyle()).isEqualTo(99);
    }

    @Test
    public void unavailable_withoutUnfoldConfig() {
        assertThat(entry(false).getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void available_withUnfoldConfig() {
        assertThat(entry(true).getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void updateState_showsSummaryWithoutWriting() {
        putStyle(99);
        mController.displayPreference(mScreen);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary().toString()).isEqualTo(label(0));
        assertThat(rawStyle()).isEqualTo(99);
    }

    @Test
    public void externalChange_whileStarted_refreshesSummary() {
        mController.displayPreference(mScreen);
        mLifecycle.handleLifecycleEvent(ON_START);
        mController.updateState(mPreference);

        putStyle(2);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mPreference.getSummary().toString()).isEqualTo(label(2));
    }

    @Test
    public void isSliceable_isFalse() {
        assertThat(mController.isSliceable()).isFalse();
    }

    @Test
    public void onStart_registersObserverForFoldKey() {
        mLifecycle.handleLifecycleEvent(ON_START);

        assertThat(shadowOf(mContentResolver).getContentObservers(foldUri())).hasSize(1);
    }

    @Test
    public void onStop_unregistersObserver() {
        mLifecycle.handleLifecycleEvent(ON_START);
        mLifecycle.handleLifecycleEvent(ON_STOP);

        assertThat(shadowOf(mContentResolver).getContentObservers(foldUri())).isEmpty();
    }

    private YrrpFoldAnimationEntryPreferenceController entry(boolean unfoldEnabled) {
        return new YrrpFoldAnimationEntryPreferenceController(
                mContext, PREF_KEY, () -> unfoldEnabled);
    }

    private static Uri foldUri() {
        return Settings.Secure.getUriFor(YrrpSettingsStore.FOLD_ANIMATION);
    }

    private String label(int style) {
        return mContext.getString(YrrpFoldAnimationEntryPreferenceController.labelFor(style));
    }

    private void putStyle(int value) {
        Settings.Secure.putInt(mContentResolver, YrrpSettingsStore.FOLD_ANIMATION, value);
    }

    private int rawStyle() {
        return Settings.Secure.getInt(mContentResolver, YrrpSettingsStore.FOLD_ANIMATION, MISSING);
    }
}
