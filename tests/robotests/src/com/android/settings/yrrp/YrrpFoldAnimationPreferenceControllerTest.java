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
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceXmlParserUtils;
import com.android.settings.core.PreferenceXmlParserUtils.MetadataFlag;
import com.android.settings.display.FoldLockBehaviorSettings;
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
public class YrrpFoldAnimationPreferenceControllerTest {
    /** Read back when a key was never written. */
    private static final int MISSING = Integer.MIN_VALUE;
    private static final String FOLD_LOCK = YrrpFoldLockStore.FOLD_LOCK;
    private static final String ALWAYS = FoldLockBehaviorSettings.SETTING_VALUE_STAY_AWAKE_ON_FOLD;
    private static final String SWIPE = FoldLockBehaviorSettings.SETTING_VALUE_SELECTIVE_STAY_AWAKE;
    private static final String NEVER = FoldLockBehaviorSettings.SETTING_VALUE_SLEEP_ON_FOLD;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ContentResolver mContentResolver;
    private YrrpRecordingSecureBackend mBackend;
    private YrrpFakeFoldLockBackend mFoldLock;
    private YrrpFoldAnimationPreferenceController mStockController;
    private YrrpFoldAnimationPreferenceController mPixelController;
    private YrrpFoldAnimationPreferenceController mDuoController;
    private SelectorWithWidgetPreference mStockPreference;
    private SelectorWithWidgetPreference mPixelPreference;
    private SelectorWithWidgetPreference mDuoPreference;
    private PreferenceScreen mScreen;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        mContentResolver = mContext.getContentResolver();
        mBackend = new YrrpRecordingSecureBackend(mContext);
        mFoldLock = new YrrpFakeFoldLockBackend();
        mStockController = controller(YrrpFoldAnimationPreferenceController.KEY_STOCK, true);
        mPixelController = controller(YrrpFoldAnimationPreferenceController.KEY_PIXEL, true);
        mDuoController = controller(YrrpFoldAnimationPreferenceController.KEY_DUO, true);
        mStockPreference = newRadio(YrrpFoldAnimationPreferenceController.KEY_STOCK);
        mPixelPreference = newRadio(YrrpFoldAnimationPreferenceController.KEY_PIXEL);
        mDuoPreference = newRadio(YrrpFoldAnimationPreferenceController.KEY_DUO);
        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mScreen.addPreference(mStockPreference);
        mScreen.addPreference(mPixelPreference);
        mScreen.addPreference(mDuoPreference);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mLifecycle.addObserver(mStockController);
        mLifecycle.addObserver(mPixelController);
        mLifecycle.addObserver(mDuoController);
    }

    @Test
    public void keys_areTheStyleRadioKeys() {
        assertThat(YrrpFoldAnimationPreferenceController.KEY_STOCK)
                .isEqualTo("yrrp_fold_animation_stock");
        assertThat(YrrpFoldAnimationPreferenceController.KEY_PIXEL)
                .isEqualTo("yrrp_fold_animation_pixel");
        assertThat(YrrpFoldAnimationPreferenceController.KEY_DUO)
                .isEqualTo("yrrp_fold_animation_duo");
    }

    @Test
    public void keys_matchTheRadioRowsInThePageXml() throws Exception {
        final List<Bundle> metadata =
                PreferenceXmlParserUtils.extractMetadata(
                        mContext,
                        R.xml.yrrp_fold_animation_settings,
                        MetadataFlag.FLAG_NEED_KEY | MetadataFlag.FLAG_NEED_PREF_CONTROLLER);
        final Map<String, String> controllerByKey = new HashMap<>();
        for (Bundle bundle : metadata) {
            final String controller =
                    bundle.getString(PreferenceXmlParserUtils.METADATA_CONTROLLER);
            if (YrrpFoldAnimationPreferenceController.class.getName().equals(controller)) {
                controllerByKey.put(
                        bundle.getString(PreferenceXmlParserUtils.METADATA_KEY), controller);
            }
        }

        final String controllerName = YrrpFoldAnimationPreferenceController.class.getName();
        assertThat(controllerByKey)
                .containsExactly(
                        YrrpFoldAnimationPreferenceController.KEY_STOCK, controllerName,
                        YrrpFoldAnimationPreferenceController.KEY_PIXEL, controllerName,
                        YrrpFoldAnimationPreferenceController.KEY_DUO, controllerName);
    }

    @Test
    public void constructor_unknownKey_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new YrrpFoldAnimationPreferenceController(mContext, "unknown_key"));
    }

    @Test
    public void isSliceable_isFalse() {
        assertThat(mStockController.isSliceable()).isFalse();
        assertThat(mDuoController.isSliceable()).isFalse();
    }

    @Test
    public void availability_followsUnfoldConfig() {
        assertThat(
                        controller(YrrpFoldAnimationPreferenceController.KEY_STOCK, true)
                                .getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
        assertThat(
                        controller(YrrpFoldAnimationPreferenceController.KEY_STOCK, false)
                                .getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void updateState_missingSetting_checksStockWithoutWriting() {
        updateAllRows();

        assertThat(mStockPreference.isChecked()).isTrue();
        assertThat(mPixelPreference.isChecked()).isFalse();
        assertThat(mDuoPreference.isChecked()).isFalse();
        assertThat(rawStyle()).isEqualTo(MISSING);
        assertThat(mBackend.mWrites).isEmpty();
    }

    @Test
    public void updateState_pixel_checksOnlyPixel() {
        putStyle(1);

        updateAllRows();

        assertThat(mStockPreference.isChecked()).isFalse();
        assertThat(mPixelPreference.isChecked()).isTrue();
        assertThat(mDuoPreference.isChecked()).isFalse();
    }

    @Test
    public void updateState_duo_checksOnlyDuo() {
        putStyle(2);

        updateAllRows();

        assertThat(mStockPreference.isChecked()).isFalse();
        assertThat(mPixelPreference.isChecked()).isFalse();
        assertThat(mDuoPreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_checksOnlyTheStoredStyle_unknownShowsStock() {
        putStyle(7);

        updateAllRows();

        assertThat(mStockPreference.isChecked()).isTrue();
        assertThat(mDuoPreference.isChecked()).isFalse();
        assertThat(rawStyle()).isEqualTo(7);
        assertThat(mBackend.mWrites).isEmpty();
    }

    @Test
    public void onRadioButtonClicked_writesThisRowsStyle() {
        mDuoController.onRadioButtonClicked(mDuoPreference);

        assertThat(mBackend.mWrites).containsExactly("lineage_fold_animation=2");
        assertThat(rawStyle()).isEqualTo(YrrpSettingsStore.FOLD_DUO);
    }

    @Test
    public void clickPixel_writesOne() {
        putStyle(99);
        displayAllRows();

        mPixelPreference.onClick();

        assertThat(mBackend.mWrites).containsExactly("lineage_fold_animation=1");
        assertThat(rawStyle()).isEqualTo(1);
    }

    @Test
    public void clickStock_writeFails_keepsDuoChecked() {
        putStyle(2);
        displayAllRows();
        mLifecycle.handleLifecycleEvent(ON_START);
        updateAllRows();
        mBackend.mFailingKeys.add(YrrpSettingsStore.FOLD_ANIMATION);

        mStockPreference.onClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mBackend.mWrites).containsExactly("lineage_fold_animation=0");
        assertThat(rawStyle()).isEqualTo(2);
        assertThat(mStockPreference.isChecked()).isFalse();
        assertThat(mDuoPreference.isChecked()).isTrue();
    }

    @Test
    public void clickDuo_storesTheFoldLockValueAndForcesAlways() {
        mFoldLock.mSystem.put(FOLD_LOCK, NEVER);
        displayAllRows();

        mDuoPreference.onClick();

        assertThat(mFoldLock.mSecure.get(YrrpFoldLockStore.BACKUP_BEFORE_DUO)).isEqualTo(NEVER);
        assertThat(mFoldLock.mSystem.get(FOLD_LOCK)).isEqualTo(ALWAYS);
        assertThat(rawStyle()).isEqualTo(YrrpSettingsStore.FOLD_DUO);
    }

    @Test
    public void clickDuo_foldLockBackupFails_leavesTheStyleUnchanged() {
        putStyle(1);
        mFoldLock.mFailingKeys.add(YrrpFoldLockStore.BACKUP_BEFORE_DUO);
        displayAllRows();

        mDuoPreference.onClick();

        assertThat(mBackend.mWrites).isEmpty();
        assertThat(rawStyle()).isEqualTo(1);
    }

    @Test
    public void clickStock_fromDuo_restoresTheStoredFoldLockValue() {
        putStyle(2);
        mFoldLock.mSecure.put(YrrpFoldLockStore.BACKUP_BEFORE_DUO, NEVER);
        mFoldLock.mSystem.put(FOLD_LOCK, ALWAYS);
        displayAllRows();

        mStockPreference.onClick();

        assertThat(rawStyle()).isEqualTo(YrrpSettingsStore.FOLD_STOCK);
        assertThat(mFoldLock.mSystem.get(FOLD_LOCK)).isEqualTo(NEVER);
        assertThat(mFoldLock.mSecure).doesNotContainKey(YrrpFoldLockStore.BACKUP_BEFORE_DUO);
    }

    @Test
    public void clickPixel_fromDuoWithoutBackup_writesSwipeUp() {
        putStyle(2);
        mFoldLock.mSystem.put(FOLD_LOCK, ALWAYS);
        displayAllRows();

        mPixelPreference.onClick();

        assertThat(mFoldLock.mSystem.get(FOLD_LOCK)).isEqualTo(SWIPE);
    }

    @Test
    public void clickPixel_fromStock_leavesTheFoldLockAlone() {
        mFoldLock.mSystem.put(FOLD_LOCK, ALWAYS);
        displayAllRows();

        mPixelPreference.onClick();

        assertThat(mFoldLock.mWrites).isEmpty();
        assertThat(mFoldLock.mSystem.get(FOLD_LOCK)).isEqualTo(ALWAYS);
    }

    @Test
    public void clickStock_fromDuo_styleWriteFails_keepsAlways() {
        putStyle(2);
        mFoldLock.mSecure.put(YrrpFoldLockStore.BACKUP_BEFORE_DUO, NEVER);
        mFoldLock.mSystem.put(FOLD_LOCK, ALWAYS);
        mBackend.mFailingKeys.add(YrrpSettingsStore.FOLD_ANIMATION);
        displayAllRows();

        mStockPreference.onClick();

        assertThat(mFoldLock.mWrites).isEmpty();
        assertThat(mFoldLock.mSystem.get(FOLD_LOCK)).isEqualTo(ALWAYS);
    }

    @Test
    public void onStart_eachRowRegistersObserverForFoldKey() {
        mLifecycle.handleLifecycleEvent(ON_START);

        assertThat(shadowOf(mContentResolver).getContentObservers(foldUri())).hasSize(3);
    }

    @Test
    public void onStop_unregistersObservers() {
        mLifecycle.handleLifecycleEvent(ON_START);
        mLifecycle.handleLifecycleEvent(ON_STOP);

        assertThat(shadowOf(mContentResolver).getContentObservers(foldUri())).isEmpty();
    }

    @Test
    public void clickDuo_whileStarted_refreshesAllRows() {
        displayAllRows();
        mLifecycle.handleLifecycleEvent(ON_START);
        updateAllRows();

        mDuoPreference.onClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mStockPreference.isChecked()).isFalse();
        assertThat(mPixelPreference.isChecked()).isFalse();
        assertThat(mDuoPreference.isChecked()).isTrue();
    }

    @Test
    public void externalChange_whileStarted_refreshesAllRows() {
        putStyle(1);
        displayAllRows();
        mLifecycle.handleLifecycleEvent(ON_START);
        updateAllRows();

        putStyle(0);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mStockPreference.isChecked()).isTrue();
        assertThat(mPixelPreference.isChecked()).isFalse();
    }

    private YrrpFoldAnimationPreferenceController controller(
            String key, boolean unfoldEnabled) {
        return new YrrpFoldAnimationPreferenceController(
                mContext, key, mBackend.newStore(), mFoldLock.newStore(), () -> unfoldEnabled);
    }

    private static Uri foldUri() {
        return Settings.Secure.getUriFor(YrrpSettingsStore.FOLD_ANIMATION);
    }

    private SelectorWithWidgetPreference newRadio(String key) {
        final SelectorWithWidgetPreference preference = new SelectorWithWidgetPreference(mContext);
        preference.setKey(key);
        return preference;
    }

    private void displayAllRows() {
        mStockController.displayPreference(mScreen);
        mPixelController.displayPreference(mScreen);
        mDuoController.displayPreference(mScreen);
    }

    private void updateAllRows() {
        mStockController.updateState(mStockPreference);
        mPixelController.updateState(mPixelPreference);
        mDuoController.updateState(mDuoPreference);
    }

    private void putStyle(int value) {
        Settings.Secure.putInt(mContentResolver, YrrpSettingsStore.FOLD_ANIMATION, value);
    }

    private int rawStyle() {
        return Settings.Secure.getInt(mContentResolver, YrrpSettingsStore.FOLD_ANIMATION, MISSING);
    }
}
