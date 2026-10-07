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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.XmlTestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class YrrpScreenOffAnimationSettingsTest {
    private static final String SELECTOR =
            "com.android.settingslib.widget.SelectorWithWidgetPreference";

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void getPreferenceScreenResId_isScreenOffAnimationXml() {
        assertThat(new YrrpScreenOffAnimationSettings().getPreferenceScreenResId())
                .isEqualTo(R.xml.yrrp_screen_off_animation_settings);
    }

    @Test
    public void searchProvider_indexesScreenOffAnimationXml() {
        assertThat(
                        YrrpXmlElements.indexedXml(
                                mContext,
                                YrrpScreenOffAnimationSettings.SEARCH_INDEX_DATA_PROVIDER))
                .containsExactly(R.xml.yrrp_screen_off_animation_settings);
    }

    @Test
    public void screenOffAnimationXml_hasOnlyStockAndCrtSelectors() throws Exception {
        assertThat(YrrpXmlElements.outline(mContext, R.xml.yrrp_screen_off_animation_settings))
                .containsExactly(
                        "PreferenceScreen#yrrp_screen_off_animation_settings_screen",
                        "  " + SELECTOR + "#yrrp_screen_off_animation_stock",
                        "  " + SELECTOR + "#yrrp_screen_off_animation_crt")
                .inOrder();
        // Kept alongside the outline check on purpose: this one runs the production key parser.
        assertThat(
                        XmlTestUtils.getKeysFromPreferenceXml(
                                mContext, R.xml.yrrp_screen_off_animation_settings))
                .containsExactly(
                        "yrrp_screen_off_animation_settings_screen",
                        "yrrp_screen_off_animation_stock",
                        "yrrp_screen_off_animation_crt")
                .inOrder();
    }
}
