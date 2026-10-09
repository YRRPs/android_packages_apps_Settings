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
public class YrrpPulseColorModePreferenceControllerTest {
    /** Read back when a key was never written. */
    private static final int MISSING = Integer.MIN_VALUE;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ContentResolver mContentResolver;
    private YrrpRecordingSecureBackend mBackend;
    private YrrpPulseColorModePreferenceController mSolidController;
    private YrrpPulseColorModePreferenceController mMatchThemeController;
    private SelectorWithWidgetPreference mSolidPreference;
    private SelectorWithWidgetPreference mMatchThemePreference;
    private PreferenceScreen mScreen;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        mContentResolver = mContext.getContentResolver();
        mBackend = new YrrpRecordingSecureBackend(mContext);
        mSolidController =
                new YrrpPulseColorModePreferenceController(
                        mContext,
                        YrrpPulseColorModePreferenceController.KEY_SOLID,
                        mBackend.newStore());
        mMatchThemeController =
                new YrrpPulseColorModePreferenceController(
                        mContext,
                        YrrpPulseColorModePreferenceController.KEY_MATCH_THEME,
                        mBackend.newStore());
        mSolidPreference = newRadio(YrrpPulseColorModePreferenceController.KEY_SOLID);
        mMatchThemePreference = newRadio(YrrpPulseColorModePreferenceController.KEY_MATCH_THEME);
        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mScreen.addPreference(mSolidPreference);
        mScreen.addPreference(mMatchThemePreference);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mLifecycle.addObserver(mSolidController);
        mLifecycle.addObserver(mMatchThemeController);
    }

    @Test
    public void keys_areTheFourRadioKeys() {
        assertThat(YrrpPulseColorModePreferenceController.KEY_SOLID)
                .isEqualTo("yrrp_pulse_color_mode_solid");
        assertThat(YrrpPulseColorModePreferenceController.KEY_MATCH_THEME)
                .isEqualTo("yrrp_pulse_color_mode_match_theme");
        assertThat(YrrpPulseColorModePreferenceController.KEY_RAINBOW_GRADIENT)
                .isEqualTo("yrrp_pulse_color_mode_rainbow_gradient");
        assertThat(YrrpPulseColorModePreferenceController.KEY_RAINBOW_CYCLE)
                .isEqualTo("yrrp_pulse_color_mode_rainbow_cycle");
    }

    @Test
    public void updateState_twoAndThree_checkOnlyTheirRainbowRow() {
        final SelectorWithWidgetPreference gradient =
                newRadio(YrrpPulseColorModePreferenceController.KEY_RAINBOW_GRADIENT);
        final SelectorWithWidgetPreference cycle =
                newRadio(YrrpPulseColorModePreferenceController.KEY_RAINBOW_CYCLE);
        final YrrpPulseColorModePreferenceController gradientController =
                newController(YrrpPulseColorModePreferenceController.KEY_RAINBOW_GRADIENT);
        final YrrpPulseColorModePreferenceController cycleController =
                newController(YrrpPulseColorModePreferenceController.KEY_RAINBOW_CYCLE);

        putColorMode(2);
        gradientController.updateState(gradient);
        cycleController.updateState(cycle);
        updateBothRows();
        assertThat(gradient.isChecked()).isTrue();
        assertThat(cycle.isChecked()).isFalse();
        assertThat(mSolidPreference.isChecked()).isFalse();
        assertThat(mMatchThemePreference.isChecked()).isFalse();

        putColorMode(3);
        gradientController.updateState(gradient);
        cycleController.updateState(cycle);
        assertThat(gradient.isChecked()).isFalse();
        assertThat(cycle.isChecked()).isTrue();
    }

    @Test
    public void clickRainbowRows_writeTwoAndThree() {
        final SelectorWithWidgetPreference gradient =
                newRadio(YrrpPulseColorModePreferenceController.KEY_RAINBOW_GRADIENT);
        final SelectorWithWidgetPreference cycle =
                newRadio(YrrpPulseColorModePreferenceController.KEY_RAINBOW_CYCLE);
        mScreen.addPreference(gradient);
        mScreen.addPreference(cycle);
        newController(YrrpPulseColorModePreferenceController.KEY_RAINBOW_GRADIENT)
                .displayPreference(mScreen);
        newController(YrrpPulseColorModePreferenceController.KEY_RAINBOW_CYCLE)
                .displayPreference(mScreen);

        gradient.onClick();
        cycle.onClick();

        assertThat(mBackend.mWrites)
                .containsExactly("lineage_pulse_color_mode=2", "lineage_pulse_color_mode=3")
                .inOrder();
        assertThat(rawColorMode()).isEqualTo(3);
    }

    @Test
    public void keys_matchTheRadioRowsInThePageXml() throws Exception {
        final List<Bundle> metadata =
                PreferenceXmlParserUtils.extractMetadata(
                        mContext,
                        R.xml.yrrp_pulse_settings,
                        MetadataFlag.FLAG_NEED_KEY | MetadataFlag.FLAG_NEED_PREF_CONTROLLER);
        final Map<String, String> controllerByKey = new HashMap<>();
        for (Bundle bundle : metadata) {
            final String controller =
                    bundle.getString(PreferenceXmlParserUtils.METADATA_CONTROLLER);
            if (YrrpPulseColorModePreferenceController.class.getName().equals(controller)) {
                controllerByKey.put(
                        bundle.getString(PreferenceXmlParserUtils.METADATA_KEY), controller);
            }
        }

        final String controllerName = YrrpPulseColorModePreferenceController.class.getName();
        assertThat(controllerByKey)
                .containsExactly(
                        YrrpPulseColorModePreferenceController.KEY_SOLID, controllerName,
                        YrrpPulseColorModePreferenceController.KEY_MATCH_THEME, controllerName,
                        YrrpPulseColorModePreferenceController.KEY_RAINBOW_GRADIENT,
                                controllerName,
                        YrrpPulseColorModePreferenceController.KEY_RAINBOW_CYCLE, controllerName);
    }

    @Test
    public void constructor_unknownKey_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new YrrpPulseColorModePreferenceController(mContext, "unknown_key"));
    }

    @Test
    public void isSliceable_isFalse() {
        assertThat(mSolidController.isSliceable()).isFalse();
        assertThat(mMatchThemeController.isSliceable()).isFalse();
    }

    @Test
    public void updateState_missingSetting_checksSolidWithoutWriting() {
        updateBothRows();

        assertThat(mSolidPreference.isChecked()).isTrue();
        assertThat(mMatchThemePreference.isChecked()).isFalse();
        assertThat(rawColorMode()).isEqualTo(MISSING);
        assertThat(mBackend.mWrites).isEmpty();
    }

    @Test
    public void updateState_zero_checksSolid() {
        putColorMode(0);

        updateBothRows();

        assertThat(mSolidPreference.isChecked()).isTrue();
        assertThat(mMatchThemePreference.isChecked()).isFalse();
    }

    @Test
    public void updateState_one_checksMatchTheme() {
        putColorMode(1);

        updateBothRows();

        assertThat(mSolidPreference.isChecked()).isFalse();
        assertThat(mMatchThemePreference.isChecked()).isTrue();
    }

    @Test
    public void updateState_unknownValue_checksSolidWithoutWriting() {
        putColorMode(99);

        updateBothRows();

        assertThat(mSolidPreference.isChecked()).isTrue();
        assertThat(mMatchThemePreference.isChecked()).isFalse();
        assertThat(rawColorMode()).isEqualTo(99);
        assertThat(mBackend.mWrites).isEmpty();
    }

    @Test
    public void clickMatchTheme_writesOne() {
        putColorMode(99);
        displayBothRows();

        mMatchThemePreference.onClick();

        assertThat(mBackend.mWrites).containsExactly("lineage_pulse_color_mode=1");
        assertThat(rawColorMode()).isEqualTo(1);
    }

    @Test
    public void clickSolid_writesZero() {
        putColorMode(99);
        displayBothRows();

        mSolidPreference.onClick();

        assertThat(mBackend.mWrites).containsExactly("lineage_pulse_color_mode=0");
        assertThat(rawColorMode()).isEqualTo(0);
    }

    @Test
    public void clickSolid_writeFails_keepsMatchThemeChecked() {
        putColorMode(1);
        displayBothRows();
        mLifecycle.handleLifecycleEvent(ON_START);
        updateBothRows();
        mBackend.mFailingKeys.add(YrrpSettingsStore.PULSE_COLOR_MODE);

        mSolidPreference.onClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mBackend.mWrites).containsExactly("lineage_pulse_color_mode=0");
        assertThat(rawColorMode()).isEqualTo(1);
        assertThat(mSolidPreference.isChecked()).isFalse();
        assertThat(mMatchThemePreference.isChecked()).isTrue();
    }

    @Test
    public void onStart_eachRowRegistersObserverForColorModeKey() {
        mLifecycle.handleLifecycleEvent(ON_START);

        assertThat(shadowOf(mContentResolver).getContentObservers(colorModeUri())).hasSize(2);
    }

    @Test
    public void onStop_unregistersObservers() {
        mLifecycle.handleLifecycleEvent(ON_START);
        mLifecycle.handleLifecycleEvent(ON_STOP);

        assertThat(shadowOf(mContentResolver).getContentObservers(colorModeUri())).isEmpty();
    }

    @Test
    public void clickMatchTheme_whileStarted_refreshesBothRows() {
        displayBothRows();
        mLifecycle.handleLifecycleEvent(ON_START);
        updateBothRows();

        mMatchThemePreference.onClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mSolidPreference.isChecked()).isFalse();
        assertThat(mMatchThemePreference.isChecked()).isTrue();
    }

    @Test
    public void externalChange_whileStarted_refreshesBothRows() {
        putColorMode(1);
        displayBothRows();
        mLifecycle.handleLifecycleEvent(ON_START);
        updateBothRows();

        putColorMode(0);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mSolidPreference.isChecked()).isTrue();
        assertThat(mMatchThemePreference.isChecked()).isFalse();
    }

    @Test
    public void updateState_pulseOff_disablesBothRows() {
        Settings.Secure.putInt(mContentResolver, YrrpSettingsStore.PULSE_ENABLED, 0);

        updateBothRows();

        assertThat(mSolidPreference.isEnabled()).isFalse();
        assertThat(mMatchThemePreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_pulseOn_enablesBothRows() {
        Settings.Secure.putInt(mContentResolver, YrrpSettingsStore.PULSE_ENABLED, 1);

        updateBothRows();

        assertThat(mSolidPreference.isEnabled()).isTrue();
        assertThat(mMatchThemePreference.isEnabled()).isTrue();
    }

    @Test
    public void pulseSwitchedOn_whileStarted_enablesBothRows() {
        displayBothRows();
        mLifecycle.handleLifecycleEvent(ON_START);
        updateBothRows();

        Settings.Secure.putInt(mContentResolver, YrrpSettingsStore.PULSE_ENABLED, 1);
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mSolidPreference.isEnabled()).isTrue();
        assertThat(mMatchThemePreference.isEnabled()).isTrue();
    }

    private static Uri colorModeUri() {
        return Settings.Secure.getUriFor(YrrpSettingsStore.PULSE_COLOR_MODE);
    }

    private SelectorWithWidgetPreference newRadio(String key) {
        final SelectorWithWidgetPreference preference = new SelectorWithWidgetPreference(mContext);
        preference.setKey(key);
        return preference;
    }

    private YrrpPulseColorModePreferenceController newController(String key) {
        return new YrrpPulseColorModePreferenceController(mContext, key, mBackend.newStore());
    }

    private void displayBothRows() {
        mSolidController.displayPreference(mScreen);
        mMatchThemeController.displayPreference(mScreen);
    }

    private void updateBothRows() {
        mSolidController.updateState(mSolidPreference);
        mMatchThemeController.updateState(mMatchThemePreference);
    }

    private void putColorMode(int value) {
        Settings.Secure.putInt(mContentResolver, YrrpSettingsStore.PULSE_COLOR_MODE, value);
    }

    private int rawColorMode() {
        return Settings.Secure.getInt(
                mContentResolver, YrrpSettingsStore.PULSE_COLOR_MODE, MISSING);
    }
}
