/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

/** Search and static structure contract for the read-only feature inventory. */
@RunWith(RobolectricTestRunner.class)
public class YrrpFeatureInventorySettingsTest {
    private static final String ROW = "com.android.settings.yrrp.YrrpFeaturePreference#";

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void fragmentAndSearchProvider_useInventoryXml() {
        assertThat(new YrrpFeatureInventorySettings().getPreferenceScreenResId())
                .isEqualTo(R.xml.yrrp_feature_inventory_settings);
        assertThat(YrrpXmlElements.indexedXml(
                mContext, YrrpFeatureInventorySettings.SEARCH_INDEX_DATA_PROVIDER))
                .containsExactly(R.xml.yrrp_feature_inventory_settings);
    }

    @Test
    public void inventoryXml_hasApprovedOrder() throws Exception {
        assertThat(YrrpXmlElements.outline(mContext, R.xml.yrrp_feature_inventory_settings))
                .containsExactly(
                        "PreferenceScreen#yrrp_feature_inventory_settings_screen",
                        "  PreferenceCategory#yrrp_feature_category_customization",
                        "    " + ROW + "yrrp_feature_pulse",
                        "    " + ROW + "yrrp_feature_screen_off",
                        "    " + ROW + "yrrp_feature_matrix_boot_shutdown",
                        "    " + ROW + "yrrp_feature_fold_animations",
                        "    " + ROW + "yrrp_feature_full_theming",
                        "  PreferenceCategory#yrrp_feature_category_system_services",
                        "    " + ROW + "yrrp_feature_ad_blocking",
                        "    " + ROW + "yrrp_feature_hotspot_client_management",
                        "    " + ROW + "yrrp_feature_gapps",
                        "  PreferenceCategory#yrrp_feature_category_device_integrations",
                        "    " + ROW + "yrrp_feature_indoor_auto_brightness",
                        "    " + ROW + "yrrp_feature_oneplus_camera",
                        "    " + ROW + "yrrp_feature_oneplus_gallery")
                .inOrder();
    }

    @Test
    public void inventoryXml_keepsRowsIndexableWithStaticMetadata() throws Exception {
        final List<YrrpXmlElements.Element> elements =
                YrrpXmlElements.read(mContext, R.xml.yrrp_feature_inventory_settings);
        for (YrrpFeatureCatalog.Id id : YrrpFeatureCatalog.ids()) {
            final YrrpXmlElements.Element row = YrrpXmlElements.find(elements, id.key);
            assertThat(row.resourceId("title")).isNotEqualTo(0);
            assertThat(row.resourceId("summary")).isNotEqualTo(0);
            assertThat(row.resourceId("icon")).isNotEqualTo(0);
            assertThat(row.resourceId("keywords")).isNotEqualTo(0);
            assertThat(row.value("searchable")).isNull();
        }
        for (String category : new String[] {
                "yrrp_feature_category_customization",
                "yrrp_feature_category_system_services",
                "yrrp_feature_category_device_integrations",
        }) {
            assertThat(YrrpXmlElements.find(elements, category).value("searchable"))
                    .isEqualTo("false");
        }
    }

    @Test
    public void searchProvider_indexesScreenAndEveryFeature() {
        final List<String> nonIndexable =
                YrrpFeatureInventorySettings.SEARCH_INDEX_DATA_PROVIDER
                        .getNonIndexableKeys(mContext);

        assertThat(nonIndexable).containsNoneOf(
                "yrrp_feature_inventory_settings_screen",
                "yrrp_feature_pulse",
                "yrrp_feature_screen_off",
                "yrrp_feature_matrix_boot_shutdown",
                "yrrp_feature_fold_animations",
                "yrrp_feature_full_theming",
                "yrrp_feature_ad_blocking",
                "yrrp_feature_hotspot_client_management",
                "yrrp_feature_gapps",
                "yrrp_feature_indoor_auto_brightness",
                "yrrp_feature_oneplus_camera",
                "yrrp_feature_oneplus_gallery");
    }
}
