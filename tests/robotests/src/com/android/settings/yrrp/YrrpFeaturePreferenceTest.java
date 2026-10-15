/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.yrrp.YrrpFeatureAvailability.ProbeStatus;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class YrrpFeaturePreferenceTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private YrrpFeaturePreference mPreference;

    @Before
    public void setUp() {
        mPreference = new YrrpFeaturePreference(mContext, null);
        mPreference.setTitle("Feature title");
        mPreference.setSummary("Feature description");
    }

    @Test
    public void row_remainsEnabledAndReadOnlyForEveryResult() {
        mPreference.bind(result(true, R.string.yrrp_feature_status_available));
        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.isSelectable()).isFalse();

        mPreference.bind(result(false, R.string.yrrp_feature_status_unknown));
        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.isSelectable()).isFalse();
    }

    @Test
    public void bind_usesSeparateTitleDescriptionAndStatusViews() {
        mPreference.bind(result(false, R.string.yrrp_feature_status_unknown));
        final View row = LayoutInflater.from(mContext)
                .inflate(R.layout.yrrp_feature_preference, null);
        mPreference.onBindViewHolder(PreferenceViewHolder.createInstanceForTests(row));

        assertThat(((TextView) row.findViewById(android.R.id.title)).getText().toString())
                .isEqualTo("Feature title");
        assertThat(((TextView) row.findViewById(android.R.id.summary)).getText().toString())
                .isEqualTo("Feature description");
        assertThat(((TextView) row.findViewById(R.id.yrrp_feature_status)).getText().toString())
                .isEqualTo(mContext.getString(R.string.yrrp_feature_status_unknown));
    }

    @Test
    public void unavailableResult_dimsStatusAndIconWithoutDisablingRow() {
        mPreference.bind(result(false, R.string.yrrp_feature_status_unknown));
        final View row = LayoutInflater.from(mContext)
                .inflate(R.layout.yrrp_feature_preference, null);
        mPreference.onBindViewHolder(PreferenceViewHolder.createInstanceForTests(row));

        assertThat(row.findViewById(R.id.yrrp_feature_status).getAlpha()).isLessThan(1f);
        assertThat(((ImageView) row.findViewById(android.R.id.icon)).getAlpha()).isLessThan(1f);
        assertThat(mPreference.isEnabled()).isTrue();
    }

    private static YrrpFeatureAvailability result(boolean available, int statusRes) {
        return new YrrpFeatureAvailability(
                available,
                statusRes,
                available ? "available" : "probe_unknown",
                available ? "none" : "testProbe",
                available ? ProbeStatus.KNOWN : ProbeStatus.FAILED);
    }
}
