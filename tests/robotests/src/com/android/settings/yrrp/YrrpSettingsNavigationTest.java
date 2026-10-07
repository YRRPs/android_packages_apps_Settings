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

import androidx.annotation.XmlRes;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.core.gateway.SettingsGateway;
import com.android.settings.homepage.HighlightableMenu;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class YrrpSettingsNavigationTest {
    private static final String YRRP_CATEGORY = "yrrp_top_level_category";
    private static final String YRRP_HOMEPAGE_ENTRY = "top_level_yrrp";

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        resetHighlightableMenu();
    }

    @After
    public void tearDown() {
        resetHighlightableMenu();
    }

    @Test
    public void settingsGateway_allowsAllYrrpPages() {
        assertThat(Arrays.asList(SettingsGateway.ENTRY_FRAGMENTS))
                .containsAtLeast(
                        YrrpSettings.class.getName(),
                        YrrpPulseSettings.class.getName(),
                        YrrpScreenOffAnimationSettings.class.getName());
    }

    @Test
    public void standardHomepage_placesYrrpCategoryBeforeAccount() throws Exception {
        assertYrrpCategoryBefore(
                R.xml.top_level_settings, -150, "top_level_account_category", -140);
    }

    @Test
    public void expressiveHomepage_placesYrrpCategoryBeforeConnectivity() throws Exception {
        assertYrrpCategoryBefore(
                R.xml.top_level_settings_expressive, -140, "top_level_connectivity_category", -130);
    }

    @Test
    public void highlightableMenu_mapsYrrpAndKeepsSystem() {
        HighlightableMenu.fromXml(mContext, R.xml.top_level_settings);

        assertThat(menuTarget(R.string.menu_key_system)).isEqualTo("top_level_system");
        assertThat(menuTarget(R.string.yrrp_menu_key)).isEqualTo(YRRP_HOMEPAGE_ENTRY);
    }

    private String menuTarget(int menuKeyResId) {
        return HighlightableMenu.lookupPreferenceKey(mContext.getString(menuKeyResId));
    }

    /**
     * Asserts the YRRPs category is a top-level category ordered {@code yrrpOrder}, listed and
     * ordered before {@code nextKey}, and holds only the YRRPs homepage entry.
     */
    private void assertYrrpCategoryBefore(
            @XmlRes int xmlResId, int yrrpOrder, String nextKey, int nextOrder) throws Exception {
        final List<YrrpXmlElements.Element> elements = YrrpXmlElements.read(mContext, xmlResId);
        final YrrpXmlElements.Element category = YrrpXmlElements.find(elements, YRRP_CATEGORY);
        final YrrpXmlElements.Element next = YrrpXmlElements.find(elements, nextKey);

        assertThat(category.mDepth).isEqualTo(2);
        assertThat(category.order()).isEqualTo(yrrpOrder);
        assertThat(next.order()).isEqualTo(nextOrder);
        assertThat(elements.indexOf(category)).isLessThan(elements.indexOf(next));

        final List<YrrpXmlElements.Element> children =
                YrrpXmlElements.children(elements, YRRP_CATEGORY);
        assertThat(children).hasSize(1);
        final YrrpXmlElements.Element entry = children.get(0);
        assertThat(entry.mTag).isEqualTo("com.android.settings.widget.HomepagePreference");
        assertThat(entry.key()).isEqualTo(YRRP_HOMEPAGE_ENTRY);
        assertThat(entry.value("fragment")).isEqualTo(YrrpSettings.class.getName());
        assertThat(entry.resourceId("highlightableMenuKey")).isEqualTo(R.string.yrrp_menu_key);
    }

    /** HighlightableMenu parses once per process; clear it so each test parses afresh. */
    private static void resetHighlightableMenu() {
        ReflectionHelpers.setStaticField(HighlightableMenu.class, "sXmlParsed", false);
        final Map<String, String> menuToPreferenceKey =
                ReflectionHelpers.getStaticField(
                        HighlightableMenu.class, "MENU_TO_PREFERENCE_KEY_MAP");
        menuToPreferenceKey.clear();
    }
}
