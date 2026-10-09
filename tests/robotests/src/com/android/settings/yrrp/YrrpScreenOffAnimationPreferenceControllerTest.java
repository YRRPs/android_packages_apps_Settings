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

import static org.junit.Assert.assertThrows;
import static org.robolectric.Shadows.shadowOf;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.core.PreferenceXmlParserUtils;
import com.android.settings.core.PreferenceXmlParserUtils.MetadataFlag;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class YrrpScreenOffAnimationPreferenceControllerTest {
    /** Read back when a key was never written. */
    private static final int MISSING = Integer.MIN_VALUE;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ContentResolver mContentResolver;
    private YrrpRecordingSecureBackend mBackend;
    private YrrpScreenOffAnimationPreferenceController mStockController;
    private YrrpScreenOffAnimationPreferenceController mCrtController;
    private SelectorWithWidgetPreference mStockPreference;
    private SelectorWithWidgetPreference mCrtPreference;
    private PreferenceScreen mScreen;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        mContentResolver = mContext.getContentResolver();
        mBackend = new YrrpRecordingSecureBackend(mContext);
        mStockController =
                new YrrpScreenOffAnimationPreferenceController(
                        mContext,
                        YrrpScreenOffAnimationPreferenceController.KEY_STOCK,
                        mBackend.newStore());
        mCrtController =
                new YrrpScreenOffAnimationPreferenceController(
                        mContext,
                        YrrpScreenOffAnimationPreferenceController.KEY_CRT,
                        mBackend.newStore());
        mStockPreference = newRadio(YrrpScreenOffAnimationPreferenceController.KEY_STOCK);
        mCrtPreference = newRadio(YrrpScreenOffAnimationPreferenceController.KEY_CRT);
        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mScreen.addPreference(mStockPreference);
        mScreen.addPreference(mCrtPreference);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mLifecycle.addObserver(mStockController);
        mLifecycle.addObserver(mCrtController);
    }

    @Test
    public void keys_areTheStockAndCrtRadioKeys() {
        assertThat(YrrpScreenOffAnimationPreferenceController.KEY_STOCK)
                .isEqualTo("yrrp_screen_off_animation_stock");
        assertThat(YrrpScreenOffAnimationPreferenceController.KEY_CRT)
                .isEqualTo("yrrp_screen_off_animation_crt");
    }

    @Test
    public void keys_matchTheRadioRowsInThePageXml() throws Exception {
        final List<Bundle> metadata =
                PreferenceXmlParserUtils.extractMetadata(
                        mContext,
                        R.xml.yrrp_screen_off_animation_settings,
                        MetadataFlag.FLAG_NEED_KEY | MetadataFlag.FLAG_NEED_PREF_CONTROLLER);
        final Map<String, String> controllerByKey = new HashMap<>();
        for (Bundle bundle : metadata) {
            final String controller =
                    bundle.getString(PreferenceXmlParserUtils.METADATA_CONTROLLER);
            if (YrrpScreenOffAnimationPreferenceController.class.getName().equals(controller)) {
                controllerByKey.put(
                        bundle.getString(PreferenceXmlParserUtils.METADATA_KEY), controller);
            }
        }

        final String controllerName = YrrpScreenOffAnimationPreferenceController.class.getName();
        assertThat(controllerByKey)
                .containsExactly(
                        YrrpScreenOffAnimationPreferenceController.KEY_STOCK, controllerName,
                        YrrpScreenOffAnimationPreferenceController.KEY_CRT, controllerName);
    }

    @Test
    public void constructor_unknownKey_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new YrrpScreenOffAnimationPreferenceController(mContext, "unknown_key"));
    }

    @Test
    public void isSliceable_isFalse() {
        assertThat(mStockController.isSliceable()).isFalse();
        assertThat(mCrtController.isSliceable()).isFalse();
    }

    @Test
    public void updateState_missingSetting_checksStockWithoutWriting() {
        updateBothRows();

        assertThat(mStockPreference.isChecked()).isTrue();
        assertThat(mCrtPreference.isChecked()).isFalse();
        assertThat(rawAnimation()).isEqualTo(MISSING);
        assertThat(mBackend.mWrites).isEmpty();
    }

    @Test
    public void updateState_zero_checksStock() {
        putAnimation(0);

        updateBothRows();

        assertThat(mStockPreference.isChecked()).isTrue();
        assertThat(mCrtPreference.isChecked()).isFalse();
    }

    @Test
    public void updateState_one_checksCrt() {
        putAnimation(1);

        updateBothRows();

        assertThat(mStockPreference.isChecked()).isFalse();
        assertThat(mCrtPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_unknownValue_checksStockWithoutWriting() {
        putAnimation(99);

        updateBothRows();

        assertThat(mStockPreference.isChecked()).isTrue();
        assertThat(mCrtPreference.isChecked()).isFalse();
        assertThat(rawAnimation()).isEqualTo(99);
        assertThat(mBackend.mWrites).isEmpty();
    }

    @Test
    public void clickCrt_writesOne() {
        putAnimation(99);
        displayBothRows();

        mCrtPreference.onClick();

        assertThat(mBackend.mWrites).containsExactly("lineage_screen_off_animation=1");
        assertThat(rawAnimation()).isEqualTo(1);
    }

    @Test
    public void clickStock_writesZero() {
        putAnimation(99);
        displayBothRows();

        mStockPreference.onClick();

        assertThat(mBackend.mWrites).containsExactly("lineage_screen_off_animation=0");
        assertThat(rawAnimation()).isEqualTo(0);
    }

    @Test
    public void clickStock_writeFails_keepsCrtChecked() {
        putAnimation(1);
        displayBothRows();
        mLifecycle.handleLifecycleEvent(ON_START);
        updateBothRows();
        mBackend.mFailingKeys.add(YrrpSettingsStore.SCREEN_OFF_ANIMATION);

        mStockPreference.onClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mBackend.mWrites).containsExactly("lineage_screen_off_animation=0");
        assertThat(rawAnimation()).isEqualTo(1);
        assertThat(mStockPreference.isChecked()).isFalse();
        assertThat(mCrtPreference.isChecked()).isTrue();
    }

    @Test
    public void onStart_eachRowRegistersObserverForAnimationKey() {
        mLifecycle.handleLifecycleEvent(ON_START);

        assertThat(shadowOf(mContentResolver).getContentObservers(animationUri())).hasSize(2);
    }

    @Test
    public void onStop_unregistersObservers() {
        mLifecycle.handleLifecycleEvent(ON_START);
        mLifecycle.handleLifecycleEvent(ON_STOP);

        assertThat(shadowOf(mContentResolver).getContentObservers(animationUri())).isEmpty();
    }

    @Test
    public void clickCrt_whileStarted_refreshesBothRows() {
        displayBothRows();
        mLifecycle.handleLifecycleEvent(ON_START);
        updateBothRows();

        mCrtPreference.onClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mStockPreference.isChecked()).isFalse();
        assertThat(mCrtPreference.isChecked()).isTrue();
    }

    @Test
    public void externalChange_whileStarted_refreshesBothRows() {
        putAnimation(1);
        displayBothRows();
        mLifecycle.handleLifecycleEvent(ON_START);
        updateBothRows();

        putAnimation(0);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mStockPreference.isChecked()).isTrue();
        assertThat(mCrtPreference.isChecked()).isFalse();
    }

    private static Uri animationUri() {
        return Settings.Secure.getUriFor(YrrpSettingsStore.SCREEN_OFF_ANIMATION);
    }

    private SelectorWithWidgetPreference newRadio(String key) {
        final SelectorWithWidgetPreference preference = new SelectorWithWidgetPreference(mContext);
        preference.setKey(key);
        return preference;
    }

    private void displayBothRows() {
        mStockController.displayPreference(mScreen);
        mCrtController.displayPreference(mScreen);
    }

    private void updateBothRows() {
        mStockController.updateState(mStockPreference);
        mCrtController.updateState(mCrtPreference);
    }

    private void putAnimation(int value) {
        Settings.Secure.putInt(mContentResolver, YrrpSettingsStore.SCREEN_OFF_ANIMATION, value);
    }

    private int rawAnimation() {
        return Settings.Secure.getInt(
                mContentResolver, YrrpSettingsStore.SCREEN_OFF_ANIMATION, MISSING);
    }
}
