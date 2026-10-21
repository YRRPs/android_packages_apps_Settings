/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
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
public class YrrpBootAnimationSettingsTest {
    private static final String WIDGET = "com.android.settingslib.widget.";
    private static final String SELECTOR = WIDGET + "SelectorWithWidgetPreference";

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void getPreferenceScreenResId_isBootAnimationXml() {
        assertThat(new YrrpBootAnimationSettings().getPreferenceScreenResId())
                .isEqualTo(R.xml.yrrp_boot_animation_settings);
    }

    @Test
    public void searchProvider_indexesBootAnimationXml() {
        assertThat(
                        YrrpXmlElements.indexedXml(
                                mContext, YrrpBootAnimationSettings.SEARCH_INDEX_DATA_PROVIDER))
                .containsExactly(R.xml.yrrp_boot_animation_settings);
    }

    @Test
    public void bootAnimationXml_layout() throws Exception {
        assertThat(YrrpXmlElements.outline(mContext, R.xml.yrrp_boot_animation_settings))
                .containsExactly(
                        "PreferenceScreen#yrrp_boot_animation_settings_screen",
                        "  " + WIDGET + "TopIntroPreference#yrrp_boot_animation_intro",
                        "  PreferenceCategory#yrrp_boot_animation_style_category",
                        "    " + SELECTOR + "#yrrp_boot_animation_matrix",
                        "    " + SELECTOR + "#yrrp_boot_animation_lineage",
                        "    " + SELECTOR + "#yrrp_boot_animation_xp",
                        "  " + WIDGET + "FooterPreference#yrrp_boot_animation_footer")
                .inOrder();
        // Kept alongside the outline check on purpose: this one runs the production key parser.
        assertThat(
                        XmlTestUtils.getKeysFromPreferenceXml(
                                mContext, R.xml.yrrp_boot_animation_settings))
                .containsExactly(
                        "yrrp_boot_animation_settings_screen",
                        "yrrp_boot_animation_intro",
                        "yrrp_boot_animation_style_category",
                        "yrrp_boot_animation_matrix",
                        "yrrp_boot_animation_lineage",
                        "yrrp_boot_animation_xp",
                        "yrrp_boot_animation_footer")
                .inOrder();
    }

    @Test
    public void rootTitle_matchesHubEntryTitle() throws Exception {
        final YrrpXmlElements.Element root =
                YrrpXmlElements.find(
                        YrrpXmlElements.read(mContext, R.xml.yrrp_boot_animation_settings),
                        "yrrp_boot_animation_settings_screen");
        final YrrpXmlElements.Element entry =
                YrrpXmlElements.find(
                        YrrpXmlElements.read(mContext, R.xml.yrrp_settings),
                        "yrrp_boot_animation_entry");

        assertThat(root.resourceId("title")).isEqualTo(R.string.yrrp_boot_animation_title);
        assertThat(entry.resourceId("title")).isEqualTo(R.string.yrrp_boot_animation_title);
    }

    @Test
    public void styleRows_carryTheirControllersAndMatrixKeywords() throws Exception {
        final List<YrrpXmlElements.Element> elements =
                YrrpXmlElements.read(mContext, R.xml.yrrp_boot_animation_settings);
        final YrrpXmlElements.Element matrix =
                YrrpXmlElements.find(elements, "yrrp_boot_animation_matrix");
        final YrrpXmlElements.Element lineage =
                YrrpXmlElements.find(elements, "yrrp_boot_animation_lineage");
        final YrrpXmlElements.Element xp =
                YrrpXmlElements.find(elements, "yrrp_boot_animation_xp");

        assertThat(matrix.value("controller"))
                .isEqualTo(YrrpBootAnimationMatrixPreferenceController.class.getName());
        assertThat(matrix.resourceId("keywords"))
                .isEqualTo(R.string.yrrp_boot_animation_keywords);
        assertThat(lineage.value("controller"))
                .isEqualTo(YrrpBootAnimationStylePreferenceController.class.getName());
        assertThat(xp.value("controller"))
                .isEqualTo(YrrpBootAnimationStylePreferenceController.class.getName());
        assertThat(nonIndexableKeys())
                .containsNoneOf(
                        "yrrp_boot_animation_matrix",
                        "yrrp_boot_animation_lineage",
                        "yrrp_boot_animation_xp");
    }

    @Test
    public void searchProvider_hidesStructuralRows() throws Exception {
        final List<YrrpXmlElements.Element> elements =
                YrrpXmlElements.read(mContext, R.xml.yrrp_boot_animation_settings);
        for (String key :
                new String[] {
                    "yrrp_boot_animation_intro",
                    "yrrp_boot_animation_style_category",
                    "yrrp_boot_animation_footer"
                }) {
            assertThat(YrrpXmlElements.find(elements, key).value("searchable"))
                    .isEqualTo("false");
        }
        assertThat(nonIndexableKeys())
                .containsAtLeast(
                        "yrrp_boot_animation_intro",
                        "yrrp_boot_animation_style_category",
                        "yrrp_boot_animation_footer");
    }

    private List<String> nonIndexableKeys() {
        return YrrpBootAnimationSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);
    }
}
