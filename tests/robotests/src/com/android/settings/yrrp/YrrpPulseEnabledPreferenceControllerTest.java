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
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.MainSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowContentResolver;

@RunWith(RobolectricTestRunner.class)
public class YrrpPulseEnabledPreferenceControllerTest {
    /** Read back when a key was never written. */
    private static final int MISSING = Integer.MIN_VALUE;

    private static final String PREF_KEY = "yrrp_pulse_enabled";

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ContentResolver mContentResolver;
    private ShadowContentResolver mShadowContentResolver;
    private YrrpRecordingSecureBackend mBackend;
    private YrrpPulseEnabledPreferenceController mController;
    private MainSwitchPreference mPreference;
    private PreferenceScreen mScreen;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        mContentResolver = mContext.getContentResolver();
        mShadowContentResolver = shadowOf(mContentResolver);
        mBackend = new YrrpRecordingSecureBackend(mContext);
        mController =
                new YrrpPulseEnabledPreferenceController(mContext, PREF_KEY, mBackend.newStore());
        mPreference = new MainSwitchPreference(mContext);
        mPreference.setKey(PREF_KEY);
        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mScreen.addPreference(mPreference);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mLifecycle.addObserver(mController);
    }

    @Test
    public void isChecked_missingSetting_isOff() {
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_zero_isOff() {
        putEnabled(0);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_one_isOn() {
        putEnabled(1);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_otherNonZero_isOnWithoutWriting() {
        putEnabled(7);

        assertThat(mController.isChecked()).isTrue();
        assertThat(mBackend.mWrites).isEmpty();
        assertThat(rawEnabled()).isEqualTo(7);
    }

    @Test
    public void setChecked_true_writesExactlyOne() {
        putEnabled(7);

        assertThat(mController.setChecked(true)).isTrue();

        assertThat(mBackend.mWrites).containsExactly("lineage_pulse_enabled=1");
        assertThat(rawEnabled()).isEqualTo(1);
    }

    @Test
    public void setChecked_false_writesExactlyZero() {
        putEnabled(1);

        assertThat(mController.setChecked(false)).isTrue();

        assertThat(mBackend.mWrites).containsExactly("lineage_pulse_enabled=0");
        assertThat(rawEnabled()).isEqualTo(0);
    }

    @Test
    public void setChecked_writeFails_returnsFalseAndShowsPersistedState() {
        mController.displayPreference(mScreen);
        mPreference.setChecked(true);
        mBackend.mFailingKeys.add(YrrpSettingsStore.PULSE_ENABLED);

        assertThat(mController.setChecked(true)).isFalse();

        assertThat(mBackend.mWrites).containsExactly("lineage_pulse_enabled=1");
        assertThat(mPreference.isChecked()).isFalse();
        assertThat(rawEnabled()).isEqualTo(MISSING);
    }

    @Test
    public void isSliceable_isFalse() {
        assertThat(mController.isSliceable()).isFalse();
    }

    @Test
    public void getSliceHighlightMenuRes_isYrrpMenu() {
        assertThat(mController.getSliceHighlightMenuRes()).isEqualTo(R.string.yrrp_menu_key);
    }

    @Test
    public void onStart_registersObserverForEnabledKey() {
        mLifecycle.handleLifecycleEvent(ON_START);

        assertThat(mShadowContentResolver.getContentObservers(enabledUri())).hasSize(1);
    }

    @Test
    public void onStop_unregistersObserver() {
        mLifecycle.handleLifecycleEvent(ON_START);
        mLifecycle.handleLifecycleEvent(ON_STOP);

        assertThat(mShadowContentResolver.getContentObservers(enabledUri())).isEmpty();
    }

    @Test
    public void externalChange_whileStarted_refreshesSwitchWithoutWriting() {
        mController.displayPreference(mScreen);
        mLifecycle.handleLifecycleEvent(ON_START);
        assertThat(mPreference.isChecked()).isFalse();

        putEnabled(1);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mPreference.isChecked()).isTrue();
        assertThat(mBackend.mWrites).isEmpty();
    }

    @Test
    public void externalChange_afterStop_doesNotRefreshSwitch() {
        mController.displayPreference(mScreen);
        mLifecycle.handleLifecycleEvent(ON_START);
        mLifecycle.handleLifecycleEvent(ON_STOP);

        putEnabled(1);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mPreference.isChecked()).isFalse();
    }

    private void putEnabled(int value) {
        Settings.Secure.putInt(mContentResolver, YrrpSettingsStore.PULSE_ENABLED, value);
    }

    private int rawEnabled() {
        return Settings.Secure.getInt(mContentResolver, YrrpSettingsStore.PULSE_ENABLED, MISSING);
    }

    private static Uri enabledUri() {
        return Settings.Secure.getUriFor(YrrpSettingsStore.PULSE_ENABLED);
    }
}
