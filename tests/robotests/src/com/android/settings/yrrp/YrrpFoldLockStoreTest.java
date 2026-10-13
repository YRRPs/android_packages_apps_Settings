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

import com.android.settings.display.FoldLockBehaviorSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class YrrpFoldLockStoreTest {
    private static final String LOCK = "fold_lock_behavior_setting";
    private static final String BACKUP = "yrrp_fold_lock_before_duo";
    private static final String ALWAYS = FoldLockBehaviorSettings.SETTING_VALUE_STAY_AWAKE_ON_FOLD;
    private static final String SWIPE = FoldLockBehaviorSettings.SETTING_VALUE_SELECTIVE_STAY_AWAKE;
    private static final String NEVER = FoldLockBehaviorSettings.SETTING_VALUE_SLEEP_ON_FOLD;

    private YrrpFakeFoldLockBackend mBackend;
    private YrrpFoldLockStore mStore;

    @Before
    public void setUp() {
        mBackend = new YrrpFakeFoldLockBackend();
        mStore = mBackend.newStore();
    }

    @Test
    public void keys_matchTheFrameworkAndIssueContract() {
        assertThat(YrrpFoldLockStore.FOLD_LOCK).isEqualTo(LOCK);
        assertThat(YrrpFoldLockStore.BACKUP_BEFORE_DUO).isEqualTo(BACKUP);
    }

    @Test
    public void enterDuo_storesTheCurrentValueThenWritesAlways() {
        mBackend.mSystem.put(LOCK, NEVER);

        assertThat(mStore.enterDuo()).isTrue();

        assertThat(mBackend.mWrites)
                .containsExactly("secure:" + BACKUP + "=" + NEVER, "system:" + LOCK + "=" + ALWAYS)
                .inOrder();
    }

    @Test
    public void enterDuo_unsetValue_storesTheSwipeUpLiteral() {
        assertThat(mStore.enterDuo()).isTrue();

        assertThat(mBackend.mSecure.get(BACKUP)).isEqualTo(SWIPE);
        assertThat(mBackend.mSystem.get(LOCK)).isEqualTo(ALWAYS);
    }

    @Test
    public void enterDuo_existingBackup_isKeptNotOverwritten() {
        mBackend.mSecure.put(BACKUP, NEVER);
        mBackend.mSystem.put(LOCK, SWIPE);

        assertThat(mStore.enterDuo()).isTrue();

        assertThat(mBackend.mWrites).containsExactly("system:" + LOCK + "=" + ALWAYS);
        assertThat(mBackend.mSecure.get(BACKUP)).isEqualTo(NEVER);
    }

    @Test
    public void enterDuo_backupWriteFails_leavesTheValueAlone() {
        mBackend.mSystem.put(LOCK, NEVER);
        mBackend.mFailingKeys.add(BACKUP);

        assertThat(mStore.enterDuo()).isFalse();

        assertThat(mBackend.mWrites).containsExactly("secure:" + BACKUP + "=" + NEVER);
        assertThat(mBackend.mSystem.get(LOCK)).isEqualTo(NEVER);
    }

    @Test
    public void leaveDuo_restoresTheStoredValueThenDeletesIt() {
        mBackend.mSecure.put(BACKUP, NEVER);
        mBackend.mSystem.put(LOCK, ALWAYS);

        assertThat(mStore.leaveDuo()).isTrue();

        assertThat(mBackend.mWrites)
                .containsExactly("system:" + LOCK + "=" + NEVER, "delete:" + BACKUP)
                .inOrder();
        assertThat(mBackend.mSecure).doesNotContainKey(BACKUP);
    }

    @Test
    public void leaveDuo_noStoredValue_writesSwipeUp() {
        mBackend.mSystem.put(LOCK, ALWAYS);

        assertThat(mStore.leaveDuo()).isTrue();

        assertThat(mBackend.mSystem.get(LOCK)).isEqualTo(SWIPE);
        assertThat(mBackend.mSecure).doesNotContainKey(BACKUP);
    }

    @Test
    public void leaveDuo_restoreFails_keepsTheStoredValue() {
        mBackend.mSecure.put(BACKUP, NEVER);
        mBackend.mFailingKeys.add(LOCK);

        assertThat(mStore.leaveDuo()).isFalse();

        assertThat(mBackend.mWrites).containsExactly("system:" + LOCK + "=" + NEVER);
        assertThat(mBackend.mSecure.get(BACKUP)).isEqualTo(NEVER);
    }

    @Test
    public void hasBackup_followsTheStoredKey() {
        assertThat(mStore.hasBackup()).isFalse();

        mBackend.mSecure.put(BACKUP, SWIPE);

        assertThat(mStore.hasBackup()).isTrue();
    }
}
