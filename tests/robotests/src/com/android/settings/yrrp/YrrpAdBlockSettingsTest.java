/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.core.gateway.SettingsGateway;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.List;

/** Robolectric; compiles in SettingsRoboTests, which executes no test in this tree today. */
@RunWith(RobolectricTestRunner.class)
public class YrrpAdBlockSettingsTest {
    private static final String WIDGET = "com.android.settingslib.widget.";

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void getPreferenceScreenResId_isAdBlockXml() {
        assertThat(new YrrpAdBlockSettings().getPreferenceScreenResId())
                .isEqualTo(R.xml.yrrp_ad_block_settings);
    }

    @Test
    public void searchProvider_indexesAdBlockXml() {
        assertThat(YrrpXmlElements.indexedXml(
                        mContext, YrrpAdBlockSettings.SEARCH_INDEX_DATA_PROVIDER))
                .containsExactly(R.xml.yrrp_ad_block_settings);
    }

    @Test
    public void gateway_listsAdBlockPage() {
        assertThat(Arrays.asList(SettingsGateway.ENTRY_FRAGMENTS))
                .contains(YrrpAdBlockSettings.class.getName());
    }

    @Test
    public void adBlockXml_layout() throws Exception {
        assertThat(YrrpXmlElements.outline(mContext, R.xml.yrrp_ad_block_settings))
                .containsExactly(
                        "PreferenceScreen#yrrp_ad_block_settings_screen",
                        "  " + WIDGET + "TopIntroPreference#yrrp_ad_block_intro",
                        "  " + WIDGET + "MainSwitchPreference#yrrp_ad_block_enabled",
                        "  Preference#yrrp_ad_block_update_now",
                        "  SwitchPreferenceCompat#yrrp_ad_block_auto_update",
                        "  PreferenceCategory#yrrp_ad_block_sources",
                        "  PreferenceCategory#yrrp_ad_block_allowlist",
                        "  " + WIDGET + "FooterPreference#yrrp_ad_block_footer")
                .inOrder();
    }

    @Test
    public void rowsCarryTheirControllers() throws Exception {
        final List<YrrpXmlElements.Element> elements =
                YrrpXmlElements.read(mContext, R.xml.yrrp_ad_block_settings);

        assertThat(YrrpXmlElements.find(elements, "yrrp_ad_block_enabled").value("controller"))
                .isEqualTo(YrrpAdBlockEnabledPreferenceController.class.getName());
        assertThat(YrrpXmlElements.find(elements, "yrrp_ad_block_update_now").value("controller"))
                .isEqualTo(YrrpAdBlockUpdateNowPreferenceController.class.getName());
        assertThat(YrrpXmlElements.find(elements, "yrrp_ad_block_auto_update").value("controller"))
                .isEqualTo(YrrpAdBlockAutoUpdatePreferenceController.class.getName());
        assertThat(YrrpXmlElements.find(elements, "yrrp_ad_block_sources").value("controller"))
                .isEqualTo(YrrpAdBlockSourcesPreferenceController.class.getName());
        assertThat(YrrpXmlElements.find(elements, "yrrp_ad_block_allowlist").value("controller"))
                .isEqualTo(YrrpAdBlockAllowlistPreferenceController.class.getName());
    }

    @Test
    public void rootTitle_matchesHubEntryTitle() throws Exception {
        final YrrpXmlElements.Element root = YrrpXmlElements.find(
                YrrpXmlElements.read(mContext, R.xml.yrrp_ad_block_settings),
                "yrrp_ad_block_settings_screen");
        final YrrpXmlElements.Element entry = YrrpXmlElements.find(
                YrrpXmlElements.read(mContext, R.xml.yrrp_settings), "yrrp_ad_block_entry");

        assertThat(root.resourceId("title")).isEqualTo(R.string.yrrp_ad_block_title);
        assertThat(entry.resourceId("title")).isEqualTo(R.string.yrrp_ad_block_title);
    }
}
