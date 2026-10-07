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

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class YrrpSettingsTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void getPreferenceScreenResId_isHubXml() {
        assertThat(new YrrpSettings().getPreferenceScreenResId()).isEqualTo(R.xml.yrrp_settings);
    }

    @Test
    public void searchProvider_indexesHubXml() {
        assertThat(YrrpXmlElements.indexedXml(mContext, YrrpSettings.SEARCH_INDEX_DATA_PROVIDER))
                .containsExactly(R.xml.yrrp_settings);
    }

    @Test
    public void hubXml_hasOnlyAudioAndAnimationsEntries() throws Exception {
        assertThat(YrrpXmlElements.outline(mContext, R.xml.yrrp_settings))
                .containsExactly(
                        "PreferenceScreen#yrrp_settings_screen",
                        "  PreferenceCategory#yrrp_category_audio",
                        "    Preference#yrrp_pulse_entry",
                        "  PreferenceCategory#yrrp_category_animations",
                        "    Preference#yrrp_screen_off_animation_entry")
                .inOrder();
        assertThat(XmlTestUtils.getKeysFromPreferenceXml(mContext, R.xml.yrrp_settings))
                .containsExactly(
                        "yrrp_settings_screen",
                        "yrrp_category_audio",
                        "yrrp_pulse_entry",
                        "yrrp_category_animations",
                        "yrrp_screen_off_animation_entry")
                .inOrder();
    }

    @Test
    public void pulseEntry_opensPulsePageWithPulseKeywords() throws Exception {
        final YrrpXmlElements.Element entry =
                YrrpXmlElements.find(
                        YrrpXmlElements.read(mContext, R.xml.yrrp_settings), "yrrp_pulse_entry");

        assertThat(entry.value("fragment")).isEqualTo(YrrpPulseSettings.class.getName());
        assertThat(entry.resourceId("keywords")).isEqualTo(R.string.yrrp_pulse_keywords);
    }

    @Test
    public void screenOffAnimationEntry_opensScreenOffPageWithItsKeywords() throws Exception {
        final YrrpXmlElements.Element entry =
                YrrpXmlElements.find(
                        YrrpXmlElements.read(mContext, R.xml.yrrp_settings),
                        "yrrp_screen_off_animation_entry");

        assertThat(entry.value("fragment"))
                .isEqualTo(YrrpScreenOffAnimationSettings.class.getName());
        assertThat(entry.resourceId("keywords"))
                .isEqualTo(R.string.yrrp_screen_off_animation_keywords);
    }

    @Test
    public void searchProvider_hidesCategoriesButIndexesEntriesAndScreen() {
        final List<String> nonIndexable =
                YrrpSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(nonIndexable).containsAtLeast("yrrp_category_audio", "yrrp_category_animations");
        assertThat(nonIndexable)
                .containsNoneOf(
                        "yrrp_pulse_entry",
                        "yrrp_screen_off_animation_entry",
                        "yrrp_settings_screen");
    }
}
