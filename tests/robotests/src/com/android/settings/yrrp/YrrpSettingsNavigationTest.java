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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class YrrpSettingsNavigationTest {
    private static final String YRRP_CATEGORY = "yrrp_top_level_category";
    private static final String YRRP_HOMEPAGE_ENTRY = "top_level_yrrp";

    /**
     * Private static fields of {@link HighlightableMenu}, read and written by reflection. The names
     * are load-bearing: renaming either field still compiles but fails these tests at runtime.
     */
    private static final String FIELD_XML_PARSED = "sXmlParsed";

    private static final String FIELD_MENU_TO_PREFERENCE_KEY_MAP = "MENU_TO_PREFERENCE_KEY_MAP";

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private Map<String, String> mSavedMenuToPreferenceKey;
    private boolean mSavedXmlParsed;

    /**
     * Saves the process-wide HighlightableMenu state, then clears it so each test parses afresh.
     */
    @Before
    public void setUp() {
        mSavedMenuToPreferenceKey = new HashMap<>(menuToPreferenceKeyMap());
        mSavedXmlParsed =
                ReflectionHelpers.getStaticField(HighlightableMenu.class, FIELD_XML_PARSED);
        ReflectionHelpers.setStaticField(HighlightableMenu.class, FIELD_XML_PARSED, false);
        menuToPreferenceKeyMap().clear();
    }

    /** Restores the state saved in {@link #setUp()} for later tests in this process. */
    @After
    public void tearDown() {
        final Map<String, String> map = menuToPreferenceKeyMap();
        map.clear();
        map.putAll(mSavedMenuToPreferenceKey);
        ReflectionHelpers.setStaticField(
                HighlightableMenu.class, FIELD_XML_PARSED, mSavedXmlParsed);
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

        // Depth 2 is a direct child of the PreferenceScreen root.
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

    private static Map<String, String> menuToPreferenceKeyMap() {
        return ReflectionHelpers.getStaticField(
                HighlightableMenu.class, FIELD_MENU_TO_PREFERENCE_KEY_MAP);
    }
}
