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

@RunWith(RobolectricTestRunner.class)
public class YrrpBootAnimationEntryPreferenceControllerTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final YrrpRecordingPropertyBackend mBackend = new YrrpRecordingPropertyBackend();

    @Test
    public void summary_namesTheStoredStyle() {
        final YrrpBootAnimationEntryPreferenceController controller =
                new YrrpBootAnimationEntryPreferenceController(
                        mContext, "yrrp_boot_animation_entry", mBackend.newStore());
        assertThat(controller.getSummary().toString())
                .isEqualTo(mContext.getString(R.string.yrrp_boot_animation_matrix));
        mBackend.values.put(YrrpBootAnimationStore.STYLE_PROPERTY, "lineage");
        assertThat(controller.getSummary().toString())
                .isEqualTo(mContext.getString(R.string.yrrp_boot_animation_lineage));
        mBackend.values.put(YrrpBootAnimationStore.STYLE_PROPERTY, "xp");
        assertThat(controller.getSummary().toString())
                .isEqualTo(mContext.getString(R.string.yrrp_boot_animation_xp));
        assertThat(mBackend.writes).isEmpty();
    }
}
