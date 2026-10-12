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

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class YrrpFoldAnimationSettingsTest {
    private static final String WIDGET = "com.android.settingslib.widget.";
    private static final String SELECTOR = WIDGET + "SelectorWithWidgetPreference";

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @After
    public void tearDown() {
        YrrpFoldAnimationAvailability.sSource = YrrpFoldAnimationAvailability.READ_DEVICE;
    }

    @Test
    public void getPreferenceScreenResId_isFoldAnimationXml() {
        assertThat(new YrrpFoldAnimationSettings().getPreferenceScreenResId())
                .isEqualTo(R.xml.yrrp_fold_animation_settings);
    }

    @Test
    public void searchProvider_indexesFoldAnimationXml() {
        assertThat(
                        YrrpXmlElements.indexedXml(
                                mContext, YrrpFoldAnimationSettings.SEARCH_INDEX_DATA_PROVIDER))
                .containsExactly(R.xml.yrrp_fold_animation_settings);
    }

    @Test
    public void foldAnimationXml_hasStyleSelectors() throws Exception {
        assertThat(YrrpXmlElements.outline(mContext, R.xml.yrrp_fold_animation_settings))
                .containsExactly(
                        "PreferenceScreen#yrrp_fold_animation_settings_screen",
                        "  " + WIDGET + "TopIntroPreference#yrrp_fold_animation_intro",
                        "  PreferenceCategory#yrrp_fold_animation_style_category",
                        "    " + SELECTOR + "#yrrp_fold_animation_stock",
                        "    " + SELECTOR + "#yrrp_fold_animation_pixel",
                        "    " + SELECTOR + "#yrrp_fold_animation_duo",
                        "  " + WIDGET + "FooterPreference#yrrp_fold_animation_footer")
                .inOrder();
    }

    @Test
    public void searchProvider_unfoldAvailable_pageKeysAreIndexable() {
        YrrpFoldAnimationAvailability.sSource = c -> true;

        assertThat(
                        YrrpFoldAnimationSettings.SEARCH_INDEX_DATA_PROVIDER
                                .getNonIndexableKeys(mContext))
                .doesNotContain("yrrp_fold_animation_duo");
    }

    @Test
    public void searchProvider_unfoldUnavailable_pageKeysAreNonIndexable() {
        YrrpFoldAnimationAvailability.sSource = c -> false;

        assertThat(
                        YrrpFoldAnimationSettings.SEARCH_INDEX_DATA_PROVIDER
                                .getNonIndexableKeys(mContext))
                .containsAtLeast(
                        "yrrp_fold_animation_settings_screen",
                        "yrrp_fold_animation_stock",
                        "yrrp_fold_animation_pixel",
                        "yrrp_fold_animation_duo");
    }
}
