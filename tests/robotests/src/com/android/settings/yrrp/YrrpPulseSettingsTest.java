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
public class YrrpPulseSettingsTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void getPreferenceScreenResId_isPulseXml() {
        assertThat(new YrrpPulseSettings().getPreferenceScreenResId())
                .isEqualTo(R.xml.yrrp_pulse_settings);
    }

    @Test
    public void searchProvider_indexesPulseXml() {
        assertThat(
                        YrrpXmlElements.indexedXml(
                                mContext, YrrpPulseSettings.SEARCH_INDEX_DATA_PROVIDER))
                .containsExactly(R.xml.yrrp_pulse_settings);
    }

    @Test
    public void pulseXml_hasOnlyShippedControlsInOrder() throws Exception {
        assertThat(YrrpXmlElements.outline(mContext, R.xml.yrrp_pulse_settings))
                .containsExactly(
                        "PreferenceScreen#yrrp_pulse_settings_screen",
                        "  com.android.settingslib.widget.TopIntroPreference#yrrp_pulse_intro",
                        "  com.android.settingslib.widget.MainSwitchPreference#yrrp_pulse_enabled",
                        "  PreferenceCategory#yrrp_pulse_color_mode_category",
                        "    com.android.settingslib.widget.SelectorWithWidgetPreference"
                                + "#yrrp_pulse_color_mode_solid",
                        "    com.android.settingslib.widget.SelectorWithWidgetPreference"
                                + "#yrrp_pulse_color_mode_match_theme",
                        "    com.android.settingslib.widget.SelectorWithWidgetPreference"
                                + "#yrrp_pulse_color_mode_rainbow_gradient",
                        "    com.android.settingslib.widget.SelectorWithWidgetPreference"
                                + "#yrrp_pulse_color_mode_rainbow_cycle",
                        "  PreferenceCategory#yrrp_pulse_appearance_category",
                        "    com.android.settingslib.widget.SliderPreference#yrrp_pulse_opacity",
                        "    com.android.settingslib.widget.SliderPreference#yrrp_pulse_height",
                        "    com.android.settingslib.widget.SliderPreference#yrrp_pulse_bar_count",
                        "    com.android.settingslib.widget.SliderPreference#yrrp_pulse_bar_gap",
                        "    com.android.settingslib.widget.SliderPreference#yrrp_pulse_boost",
                        "  com.android.settingslib.widget.FooterPreference#yrrp_pulse_privacy")
                .inOrder();
        // Kept alongside the outline check on purpose: this one runs the production key parser.
        assertThat(XmlTestUtils.getKeysFromPreferenceXml(mContext, R.xml.yrrp_pulse_settings))
                .containsExactly(
                        "yrrp_pulse_settings_screen",
                        "yrrp_pulse_intro",
                        "yrrp_pulse_enabled",
                        "yrrp_pulse_color_mode_category",
                        "yrrp_pulse_color_mode_solid",
                        "yrrp_pulse_color_mode_match_theme",
                        "yrrp_pulse_color_mode_rainbow_gradient",
                        "yrrp_pulse_color_mode_rainbow_cycle",
                        "yrrp_pulse_appearance_category",
                        "yrrp_pulse_opacity",
                        "yrrp_pulse_height",
                        "yrrp_pulse_bar_count",
                        "yrrp_pulse_bar_gap",
                        "yrrp_pulse_boost",
                        "yrrp_pulse_privacy")
                .inOrder();
    }

    @Test
    public void mainSwitch_carriesPulseKeywords() throws Exception {
        final YrrpXmlElements.Element mainSwitch =
                YrrpXmlElements.find(
                        YrrpXmlElements.read(mContext, R.xml.yrrp_pulse_settings),
                        "yrrp_pulse_enabled");

        assertThat(mainSwitch.resourceId("keywords")).isEqualTo(R.string.yrrp_pulse_keywords);
    }

    @Test
    public void solidRow_carriesColorKeywordsAndSolidController() throws Exception {
        final YrrpXmlElements.Element solid =
                YrrpXmlElements.find(
                        YrrpXmlElements.read(mContext, R.xml.yrrp_pulse_settings),
                        "yrrp_pulse_color_mode_solid");

        assertThat(solid.resourceId("keywords")).isEqualTo(R.string.yrrp_pulse_solid_keywords);
        assertThat(solid.value("controller"))
                .isEqualTo(YrrpPulseSolidColorModePreferenceController.class.getName());
    }

    @Test
    public void otherColorModeRows_carryColorModeKeywordsAndModeController() throws Exception {
        final List<YrrpXmlElements.Element> elements =
                YrrpXmlElements.read(mContext, R.xml.yrrp_pulse_settings);
        for (String key :
                new String[] {
                    "yrrp_pulse_color_mode_match_theme",
                    "yrrp_pulse_color_mode_rainbow_gradient",
                    "yrrp_pulse_color_mode_rainbow_cycle"
                }) {
            final YrrpXmlElements.Element row = YrrpXmlElements.find(elements, key);
            assertThat(row.resourceId("keywords"))
                    .isEqualTo(R.string.yrrp_pulse_color_mode_keywords);
            assertThat(row.value("controller"))
                    .isEqualTo(YrrpPulseColorModePreferenceController.class.getName());
        }
    }

    @Test
    public void colorModeCategory_isNotSearchable() throws Exception {
        final List<YrrpXmlElements.Element> elements =
                YrrpXmlElements.read(mContext, R.xml.yrrp_pulse_settings);
        assertThat(
                        YrrpXmlElements.find(elements, "yrrp_pulse_color_mode_category")
                                .value("searchable"))
                .isEqualTo("false");
    }

    @Test
    public void appearanceCategory_isTitledUnsearchableAndNotCollapsing() throws Exception {
        final YrrpXmlElements.Element appearance =
                YrrpXmlElements.find(
                        YrrpXmlElements.read(mContext, R.xml.yrrp_pulse_settings),
                        "yrrp_pulse_appearance_category");

        assertThat(appearance.resourceId("title")).isEqualTo(R.string.yrrp_pulse_appearance_title);
        assertThat(appearance.value("searchable")).isEqualTo("false");
        assertThat(appearance.value("initialExpandedChildrenCount")).isNull();
        assertThat(YrrpPulseSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext))
                .contains("yrrp_pulse_appearance_category");
    }

    @Test
    public void boostSlider_isSearchableWithKeywordsAndController() throws Exception {
        final YrrpXmlElements.Element boost =
                YrrpXmlElements.find(
                        YrrpXmlElements.read(mContext, R.xml.yrrp_pulse_settings),
                        "yrrp_pulse_boost");

        assertThat(boost.resourceId("title")).isEqualTo(R.string.yrrp_pulse_boost_title);
        assertThat(boost.resourceId("keywords")).isEqualTo(R.string.yrrp_pulse_boost_keywords);
        assertThat(boost.value("controller"))
                .isEqualTo(YrrpPulseBoostPreferenceController.class.getName());
        assertThat(YrrpPulseSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext))
                .doesNotContain("yrrp_pulse_boost");
    }

    @Test
    public void barSliders_areSearchableWithKeywordsAndControllers() throws Exception {
        final List<YrrpXmlElements.Element> elements =
                YrrpXmlElements.read(mContext, R.xml.yrrp_pulse_settings);
        final YrrpXmlElements.Element count =
                YrrpXmlElements.find(elements, "yrrp_pulse_bar_count");
        final YrrpXmlElements.Element gap = YrrpXmlElements.find(elements, "yrrp_pulse_bar_gap");

        assertThat(count.resourceId("title")).isEqualTo(R.string.yrrp_pulse_bar_count_title);
        assertThat(count.resourceId("keywords"))
                .isEqualTo(R.string.yrrp_pulse_bar_count_keywords);
        assertThat(count.value("controller"))
                .isEqualTo(YrrpPulseBarCountPreferenceController.class.getName());
        assertThat(gap.resourceId("title")).isEqualTo(R.string.yrrp_pulse_bar_gap_title);
        assertThat(gap.resourceId("keywords")).isEqualTo(R.string.yrrp_pulse_bar_gap_keywords);
        assertThat(gap.value("controller"))
                .isEqualTo(YrrpPulseBarGapPreferenceController.class.getName());
        assertThat(YrrpPulseSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext))
                .containsNoneOf("yrrp_pulse_bar_count", "yrrp_pulse_bar_gap");
    }

    @Test
    public void introAndFooter_areNotSearchable() throws Exception {
        final List<YrrpXmlElements.Element> elements =
                YrrpXmlElements.read(mContext, R.xml.yrrp_pulse_settings);

        assertThat(YrrpXmlElements.find(elements, "yrrp_pulse_intro").value("searchable"))
                .isEqualTo("false");
        assertThat(YrrpXmlElements.find(elements, "yrrp_pulse_privacy").value("searchable"))
                .isEqualTo("false");
        assertThat(YrrpPulseSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext))
                .containsAtLeast("yrrp_pulse_intro", "yrrp_pulse_privacy");
    }
}
