/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.Context;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class YrrpBootAnimationStylePreferenceControllerTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private YrrpRecordingPropertyBackend mBackend;
    private YrrpBootAnimationStylePreferenceController mMatrix;
    private YrrpBootAnimationStylePreferenceController mLineage;
    private YrrpBootAnimationStylePreferenceController mXp;
    private SelectorWithWidgetPreference mMatrixRow;
    private SelectorWithWidgetPreference mLineageRow;
    private SelectorWithWidgetPreference mXpRow;

    @Before
    public void setUp() {
        mBackend = new YrrpRecordingPropertyBackend();
        mMatrix =
                new YrrpBootAnimationMatrixPreferenceController(
                        mContext,
                        YrrpBootAnimationStylePreferenceController.KEY_MATRIX,
                        mBackend.newStore());
        mLineage =
                new YrrpBootAnimationStylePreferenceController(
                        mContext,
                        YrrpBootAnimationStylePreferenceController.KEY_LINEAGE,
                        mBackend.newStore());
        mXp =
                new YrrpBootAnimationStylePreferenceController(
                        mContext,
                        YrrpBootAnimationStylePreferenceController.KEY_XP,
                        mBackend.newStore());
        mMatrixRow = radio(YrrpBootAnimationStylePreferenceController.KEY_MATRIX);
        mLineageRow = radio(YrrpBootAnimationStylePreferenceController.KEY_LINEAGE);
        mXpRow = radio(YrrpBootAnimationStylePreferenceController.KEY_XP);
        final PreferenceScreen screen =
                new PreferenceManager(mContext).createPreferenceScreen(mContext);
        screen.addPreference(mMatrixRow);
        screen.addPreference(mLineageRow);
        screen.addPreference(mXpRow);
        mMatrix.displayPreference(screen);
        mLineage.displayPreference(screen);
        mXp.displayPreference(screen);
    }

    @Test
    public void keys() {
        assertThat(YrrpBootAnimationStylePreferenceController.KEY_MATRIX)
                .isEqualTo("yrrp_boot_animation_matrix");
        assertThat(YrrpBootAnimationStylePreferenceController.KEY_LINEAGE)
                .isEqualTo("yrrp_boot_animation_lineage");
        assertThat(YrrpBootAnimationStylePreferenceController.KEY_XP)
                .isEqualTo("yrrp_boot_animation_xp");
    }

    @Test
    public void unknownKey_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new YrrpBootAnimationStylePreferenceController(
                        mContext, "yrrp_nope", mBackend.newStore()));
    }

    @Test
    public void isSliceable_isFalse() {
        assertThat(mMatrix.isSliceable()).isFalse();
        assertThat(mLineage.isSliceable()).isFalse();
    }

    @Test
    public void unset_checksMatrix() {
        mMatrix.updateState(mMatrixRow);
        mLineage.updateState(mLineageRow);
        assertThat(mMatrixRow.isChecked()).isTrue();
        assertThat(mLineageRow.isChecked()).isFalse();
    }

    @Test
    public void unknownStoredValue_showsMatrixWithoutRewriting() {
        mBackend.values.put(YrrpBootAnimationStore.STYLE_PROPERTY, "garbage");
        mMatrix.updateState(mMatrixRow);
        assertThat(mMatrixRow.isChecked()).isTrue();
        assertThat(mBackend.writes).isEmpty();
    }

    @Test
    public void clickingLineage_writesAndRefreshesBothRows() {
        mLineageRow.onClick();
        assertThat(mBackend.writes).containsExactly("persist.yrrp.bootanim.style=lineage");
        assertThat(mLineageRow.isChecked()).isTrue();
        assertThat(mMatrixRow.isChecked()).isFalse();
    }

    @Test
    public void clickingXp_writesAndRefreshesAllRows() {
        mXpRow.onClick();
        assertThat(mBackend.writes).containsExactly("persist.yrrp.bootanim.style=xp");
        assertThat(mXpRow.isChecked()).isTrue();
        assertThat(mMatrixRow.isChecked()).isFalse();
        assertThat(mLineageRow.isChecked()).isFalse();
    }

    @Test
    public void failedWrite_keepsTheStoredSelection() {
        mBackend.failWrites = true;
        mLineageRow.onClick();
        assertThat(mMatrixRow.isChecked()).isTrue();
        assertThat(mLineageRow.isChecked()).isFalse();
    }

    @Test
    public void matrixRow_summaryStatesFinishIntro() {
        mMatrix.updateState(mMatrixRow);
        assertThat(mMatrixRow.getSummary().toString())
                .isEqualTo(mContext.getString(
                        com.android.settings.R.string.yrrp_boot_animation_finish_intro_on));
        mBackend.values.put(YrrpBootAnimationStore.FULL_INTRO_PROPERTY, "false");
        mMatrix.updateState(mMatrixRow);
        assertThat(mMatrixRow.getSummary().toString())
                .isEqualTo(mContext.getString(
                        com.android.settings.R.string.yrrp_boot_animation_finish_intro_off));
    }

    private SelectorWithWidgetPreference radio(String key) {
        final SelectorWithWidgetPreference preference = new SelectorWithWidgetPreference(mContext);
        preference.setKey(key);
        return preference;
    }
}
