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

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class YrrpScreenOffAnimationSpeedPreferenceControllerTest {
    /** Read back when a key was never written. */
    private static final int MISSING = Integer.MIN_VALUE;

    private static final String[] KEYS = {
        YrrpScreenOffAnimationSpeedPreferenceController.KEY_50,
        YrrpScreenOffAnimationSpeedPreferenceController.KEY_75,
        YrrpScreenOffAnimationSpeedPreferenceController.KEY_100,
        YrrpScreenOffAnimationSpeedPreferenceController.KEY_150,
        YrrpScreenOffAnimationSpeedPreferenceController.KEY_200,
    };

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final List<YrrpScreenOffAnimationSpeedPreferenceController> mControllers =
            new ArrayList<>();
    private final List<SelectorWithWidgetPreference> mRows = new ArrayList<>();
    private ContentResolver mContentResolver;
    private YrrpRecordingSecureBackend mBackend;
    private PreferenceScreen mScreen;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        mContentResolver = mContext.getContentResolver();
        mBackend = new YrrpRecordingSecureBackend(mContext);
        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        for (String key : KEYS) {
            final YrrpScreenOffAnimationSpeedPreferenceController controller =
                    new YrrpScreenOffAnimationSpeedPreferenceController(
                            mContext, key, mBackend.newStore());
            final SelectorWithWidgetPreference row = new SelectorWithWidgetPreference(mContext);
            row.setKey(key);
            mScreen.addPreference(row);
            mControllers.add(controller);
            mRows.add(row);
            mLifecycle.addObserver(controller);
        }
    }

    @Test
    public void keys_matchTheSpeedRowsInThePageXml() throws Exception {
        final List<Bundle> metadata =
                PreferenceXmlParserUtils.extractMetadata(
                        mContext,
                        R.xml.yrrp_screen_off_animation_settings,
                        MetadataFlag.FLAG_NEED_KEY | MetadataFlag.FLAG_NEED_PREF_CONTROLLER);
        final String controllerName =
                YrrpScreenOffAnimationSpeedPreferenceController.class.getName();
        final List<String> keys = new ArrayList<>();
        for (Bundle bundle : metadata) {
            if (controllerName.equals(
                    bundle.getString(PreferenceXmlParserUtils.METADATA_CONTROLLER))) {
                keys.add(bundle.getString(PreferenceXmlParserUtils.METADATA_KEY));
            }
        }

        assertThat(keys).containsExactlyElementsIn(KEYS).inOrder();
    }

    @Test
    public void constructor_unknownKey_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new YrrpScreenOffAnimationSpeedPreferenceController(mContext, "unknown"));
    }

    @Test
    public void isSliceable_isFalse() {
        for (YrrpScreenOffAnimationSpeedPreferenceController controller : mControllers) {
            assertThat(controller.isSliceable()).isFalse();
        }
    }

    @Test
    public void updateState_missingSpeed_checks1xWithoutWriting() {
        updateAllRows();

        assertThat(checkedKeys()).containsExactly(key(100));
        assertThat(rawSpeed()).isEqualTo(MISSING);
        assertThat(mBackend.mWrites).isEmpty();
    }

    @Test
    public void updateState_outOfRange_checksTheClampedRowWithoutWriting() {
        putSpeed(10);

        updateAllRows();

        assertThat(checkedKeys()).containsExactly(key(50));
        assertThat(rawSpeed()).isEqualTo(10);
        assertThat(mBackend.mWrites).isEmpty();
    }

    @Test
    public void updateState_stock_disablesEveryRow() {
        putAnimation(0);

        updateAllRows();

        for (SelectorWithWidgetPreference row : mRows) {
            assertThat(row.isEnabled()).isFalse();
        }
    }

    @Test
    public void updateState_crt_enablesEveryRow() {
        putAnimation(1);

        updateAllRows();

        for (SelectorWithWidgetPreference row : mRows) {
            assertThat(row.isEnabled()).isTrue();
        }
    }

    @Test
    public void click2x_writes200() {
        putAnimation(1);
        displayAllRows();

        row(200).onClick();

        assertThat(mBackend.mWrites).containsExactly("lineage_screen_off_animation_speed=200");
        assertThat(rawSpeed()).isEqualTo(200);
    }

    @Test
    public void clickHalf_writeFails_keepsPreviousRowChecked() {
        putAnimation(1);
        putSpeed(150);
        displayAllRows();
        mLifecycle.handleLifecycleEvent(ON_START);
        updateAllRows();
        mBackend.mFailingKeys.add(YrrpSettingsStore.SCREEN_OFF_ANIMATION_SPEED);

        row(50).onClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mBackend.mWrites).containsExactly("lineage_screen_off_animation_speed=50");
        assertThat(rawSpeed()).isEqualTo(150);
        assertThat(checkedKeys()).containsExactly(key(150));
    }

    @Test
    public void onStart_eachRowObservesSpeedAndEffect() {
        mLifecycle.handleLifecycleEvent(ON_START);

        assertThat(shadowOf(mContentResolver).getContentObservers(speedUri()))
                .hasSize(KEYS.length);
        assertThat(shadowOf(mContentResolver).getContentObservers(animationUri()))
                .hasSize(KEYS.length);
    }

    @Test
    public void onStop_unregistersObservers() {
        mLifecycle.handleLifecycleEvent(ON_START);
        mLifecycle.handleLifecycleEvent(ON_STOP);

        assertThat(shadowOf(mContentResolver).getContentObservers(speedUri())).isEmpty();
        assertThat(shadowOf(mContentResolver).getContentObservers(animationUri())).isEmpty();
    }

    @Test
    public void effectChange_whileStarted_togglesEnabled() {
        putAnimation(1);
        displayAllRows();
        mLifecycle.handleLifecycleEvent(ON_START);
        updateAllRows();

        putAnimation(0);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(row(100).isEnabled()).isFalse();
    }

    @Test
    public void click_whileStarted_movesTheCheck() {
        putAnimation(1);
        displayAllRows();
        mLifecycle.handleLifecycleEvent(ON_START);
        updateAllRows();

        row(75).onClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(checkedKeys()).containsExactly(key(75));
    }

    private static String key(int speed) {
        return "yrrp_screen_off_animation_speed_" + speed;
    }

    private SelectorWithWidgetPreference row(int speed) {
        return mScreen.findPreference(key(speed));
    }

    private List<String> checkedKeys() {
        final List<String> checked = new ArrayList<>();
        for (SelectorWithWidgetPreference row : mRows) {
            if (row.isChecked()) {
                checked.add(row.getKey());
            }
        }
        return checked;
    }

    private void displayAllRows() {
        for (YrrpScreenOffAnimationSpeedPreferenceController controller : mControllers) {
            controller.displayPreference(mScreen);
        }
    }

    private void updateAllRows() {
        for (int i = 0; i < mControllers.size(); i++) {
            mControllers.get(i).updateState(mRows.get(i));
        }
    }

    private static Uri speedUri() {
        return Settings.Secure.getUriFor(YrrpSettingsStore.SCREEN_OFF_ANIMATION_SPEED);
    }

    private static Uri animationUri() {
        return Settings.Secure.getUriFor(YrrpSettingsStore.SCREEN_OFF_ANIMATION);
    }

    private void putSpeed(int value) {
        Settings.Secure.putInt(
                mContentResolver, YrrpSettingsStore.SCREEN_OFF_ANIMATION_SPEED, value);
    }

    private void putAnimation(int value) {
        Settings.Secure.putInt(mContentResolver, YrrpSettingsStore.SCREEN_OFF_ANIMATION, value);
    }

    private int rawSpeed() {
        return Settings.Secure.getInt(
                mContentResolver, YrrpSettingsStore.SCREEN_OFF_ANIMATION_SPEED, MISSING);
    }
}
