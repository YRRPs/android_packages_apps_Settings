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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class YrrpSettingsStoreTest {
    private static final int USER = 0;
    private static final int OTHER_USER = 10;

    private static final String KEY_ENABLED = "lineage_pulse_enabled";
    private static final String KEY_COLOR = "lineage_pulse_color";
    private static final String KEY_ALPHA = "lineage_pulse_alpha";
    private static final String KEY_HEIGHT = "lineage_pulse_height_dp";
    private static final String KEY_BOOST = "lineage_pulse_log_boost";
    private static final String KEY_BAR_COUNT = "lineage_pulse_bar_count";
    private static final String KEY_BAR_GAP = "lineage_pulse_bar_gap_percent";
    private static final String KEY_ANIMATION = "lineage_screen_off_animation";
    private static final String KEY_COLOR_MODE = "lineage_pulse_color_mode";

    private FakeBackend mBackend;
    private Deque<Integer> mUsers;
    private YrrpSettingsStore mStore;

    @Before
    public void setUp() {
        mBackend = new FakeBackend();
        mUsers = new ArrayDeque<>();
        mStore =
                new YrrpSettingsStore(
                        mBackend, () -> mUsers.isEmpty() ? USER : mUsers.removeFirst());
    }

    @Test
    public void setters_writeExactlyTheNineKeys() {
        mStore.setPulseEnabled(true);
        mStore.setPulseColor(0x123456);
        mStore.setPulseAlpha(128);
        mStore.setPulseHeightDp(48);
        mStore.setPulseColorMode(YrrpSettingsStore.PULSE_COLOR_MODE_MATCH_THEME);
        mStore.setPulseBoost(20);
        mStore.setPulseBarCount(32);
        mStore.setPulseBarGapPercent(30);
        mStore.setScreenOffAnimation(YrrpSettingsStore.SCREEN_OFF_CRT);

        assertThat(mBackend.keys(USER))
                .containsExactly(
                        KEY_ENABLED,
                        KEY_COLOR,
                        KEY_ALPHA,
                        KEY_HEIGHT,
                        KEY_COLOR_MODE,
                        KEY_BOOST,
                        KEY_BAR_COUNT,
                        KEY_BAR_GAP,
                        KEY_ANIMATION);
    }

    @Test
    public void getters_readTheNineKeys() {
        mBackend.put(KEY_ENABLED, 1, USER);
        mBackend.put(KEY_COLOR, 0x123456, USER);
        mBackend.put(KEY_ALPHA, 100, USER);
        mBackend.put(KEY_HEIGHT, 20, USER);
        mBackend.put(KEY_COLOR_MODE, 1, USER);
        mBackend.put(KEY_BOOST, 70, USER);
        mBackend.put(KEY_BAR_COUNT, 48, USER);
        mBackend.put(KEY_BAR_GAP, 50, USER);
        mBackend.put(KEY_ANIMATION, 1, USER);

        assertThat(mStore.getPulseColorMode())
                .isEqualTo(YrrpSettingsStore.PULSE_COLOR_MODE_MATCH_THEME);
        assertThat(mStore.isPulseEnabled()).isTrue();
        assertThat(mStore.getPulseColor()).isEqualTo(0x123456);
        assertThat(mStore.getPulseAlpha()).isEqualTo(100);
        assertThat(mStore.getPulseHeightDp()).isEqualTo(20);
        assertThat(mStore.getPulseBoost()).isEqualTo(70);
        assertThat(mStore.getPulseBarCount()).isEqualTo(48);
        assertThat(mStore.getPulseBarGapPercent()).isEqualTo(50);
        assertThat(mStore.getScreenOffAnimation()).isEqualTo(YrrpSettingsStore.SCREEN_OFF_CRT);
    }

    @Test
    public void reads_resolveCurrentUserOnEveryCall() {
        mBackend.put(KEY_ALPHA, 100, USER);
        mBackend.put(KEY_ALPHA, 200, OTHER_USER);

        mUsers.add(USER);
        mUsers.add(OTHER_USER);

        assertThat(mStore.getPulseAlpha()).isEqualTo(100);
        assertThat(mStore.getPulseAlpha()).isEqualTo(200);
    }

    @Test
    public void writes_resolveCurrentUserOnEveryCall() {
        mUsers.add(USER);
        mUsers.add(OTHER_USER);

        mStore.setPulseAlpha(100);
        mStore.setPulseAlpha(200);

        assertThat(mBackend.get(KEY_ALPHA, USER)).isEqualTo(100);
        assertThat(mBackend.get(KEY_ALPHA, OTHER_USER)).isEqualTo(200);
    }

    @Test
    public void missingValues_returnDefaults() {
        assertThat(mStore.isPulseEnabled()).isFalse();
        assertThat(mStore.getPulseColor()).isEqualTo(0xFFFFFF);
        assertThat(mStore.getPulseAlpha()).isEqualTo(217);
        assertThat(mStore.getPulseHeightDp()).isEqualTo(48);
        assertThat(mStore.getPulseColorMode()).isEqualTo(YrrpSettingsStore.PULSE_COLOR_MODE_SOLID);
        assertThat(mStore.getPulseBoost()).isEqualTo(20);
        assertThat(mStore.getPulseBarCount()).isEqualTo(32);
        assertThat(mStore.getPulseBarGapPercent()).isEqualTo(30);
        assertThat(mStore.getScreenOffAnimation()).isEqualTo(YrrpSettingsStore.SCREEN_OFF_STOCK);
    }

    @Test
    public void reads_neverWrite() {
        readAll();
        mBackend.put(KEY_ENABLED, 7, USER);
        mBackend.put(KEY_COLOR, 0xFF123456, USER);
        mBackend.put(KEY_ALPHA, 0, USER);
        mBackend.put(KEY_HEIGHT, 200, USER);
        mBackend.put(KEY_COLOR_MODE, 99, USER);
        mBackend.put(KEY_BOOST, 500, USER);
        mBackend.put(KEY_ANIMATION, 99, USER);
        readAll();

        assertThat(mBackend.mWrites).isEqualTo(0);
    }

    @Test
    public void getPulseColor_masksToLow24Bits() {
        mBackend.put(KEY_COLOR, 0xFF123456, USER);

        assertThat(mStore.getPulseColor()).isEqualTo(0x123456);
    }

    @Test
    public void setPulseColor_writesLow24Bits() {
        mStore.setPulseColor(0xFF123456);

        assertThat(mBackend.get(KEY_COLOR, USER)).isEqualTo(0x123456);
    }

    @Test
    public void getPulseAlpha_clampsTo26Through255() {
        assertThat(readAlpha(0)).isEqualTo(26);
        assertThat(readAlpha(25)).isEqualTo(26);
        assertThat(readAlpha(26)).isEqualTo(26);
        assertThat(readAlpha(128)).isEqualTo(128);
        assertThat(readAlpha(255)).isEqualTo(255);
        assertThat(readAlpha(300)).isEqualTo(255);
    }

    @Test
    public void getPulseAlpha_doesNotRewriteRawValue() {
        mBackend.put(KEY_ALPHA, 0, USER);

        mStore.getPulseAlpha();

        assertThat(mBackend.get(KEY_ALPHA, USER)).isEqualTo(0);
    }

    @Test
    public void setPulseAlpha_writesClampedValue() {
        assertThat(writeAlpha(0)).isEqualTo(26);
        assertThat(writeAlpha(25)).isEqualTo(26);
        assertThat(writeAlpha(128)).isEqualTo(128);
        assertThat(writeAlpha(300)).isEqualTo(255);
    }

    @Test
    public void getPulseHeightDp_clampsWithoutSnapping() {
        assertThat(readHeight(0)).isEqualTo(8);
        assertThat(readHeight(10)).isEqualTo(10);
        assertThat(readHeight(50)).isEqualTo(50);
        assertThat(readHeight(200)).isEqualTo(96);
    }

    @Test
    public void setPulseHeightDp_writesClampedValueSnappedToStep() {
        assertThat(writeHeight(-5)).isEqualTo(8);
        assertThat(writeHeight(0)).isEqualTo(8);
        assertThat(writeHeight(9)).isEqualTo(8);
        assertThat(writeHeight(10)).isEqualTo(12);
        assertThat(writeHeight(50)).isEqualTo(52);
        assertThat(writeHeight(96)).isEqualTo(96);
        assertThat(writeHeight(200)).isEqualTo(96);
    }

    @Test
    public void normalizeHeightForWrite_putsOffGridReadOnGrid() {
        assertThat(YrrpSettingsStore.normalizeHeightForWrite(readHeight(50))).isEqualTo(52);
        assertThat(YrrpSettingsStore.normalizeHeightForWrite(readHeight(10))).isEqualTo(12);
    }

    @Test
    public void getPulseBoost_clampsTo0Through100() {
        assertThat(readBoost(-1)).isEqualTo(0);
        assertThat(readBoost(0)).isEqualTo(0);
        assertThat(readBoost(20)).isEqualTo(20);
        assertThat(readBoost(100)).isEqualTo(100);
        assertThat(readBoost(101)).isEqualTo(100);
    }

    @Test
    public void getPulseBoost_doesNotRewriteRawValue() {
        mBackend.put(KEY_BOOST, 500, USER);

        mStore.getPulseBoost();

        assertThat(mBackend.get(KEY_BOOST, USER)).isEqualTo(500);
    }

    @Test
    public void setPulseBoost_writesClampedValue() {
        assertThat(writeBoost(-5)).isEqualTo(0);
        assertThat(writeBoost(0)).isEqualTo(0);
        assertThat(writeBoost(55)).isEqualTo(55);
        assertThat(writeBoost(100)).isEqualTo(100);
        assertThat(writeBoost(250)).isEqualTo(100);
    }

    @Test
    public void getPulseBarCount_clampsWithoutSnapping() {
        assertThat(readBarCount(-1)).isEqualTo(16);
        assertThat(readBarCount(17)).isEqualTo(17);
        assertThat(readBarCount(50)).isEqualTo(50);
        assertThat(readBarCount(99)).isEqualTo(64);
    }

    @Test
    public void setPulseBarCount_writesClampedValueSnappedToStep() {
        assertThat(writeBarCount(0)).isEqualTo(16);
        assertThat(writeBarCount(17)).isEqualTo(16);
        assertThat(writeBarCount(18)).isEqualTo(20);
        assertThat(writeBarCount(30)).isEqualTo(32);
        assertThat(writeBarCount(64)).isEqualTo(64);
        assertThat(writeBarCount(200)).isEqualTo(64);
    }

    @Test
    public void normalizeBarCountForWrite_putsOffGridReadOnGrid() {
        assertThat(YrrpSettingsStore.normalizeBarCountForWrite(readBarCount(50))).isEqualTo(52);
    }

    @Test
    public void getPulseBarGapPercent_clampsWithoutSnapping() {
        assertThat(readBarGap(-5)).isEqualTo(0);
        assertThat(readBarGap(33)).isEqualTo(33);
        assertThat(readBarGap(81)).isEqualTo(80);
    }

    @Test
    public void setPulseBarGapPercent_writesClampedValueSnappedToStep() {
        assertThat(writeBarGap(-1)).isEqualTo(0);
        assertThat(writeBarGap(2)).isEqualTo(0);
        assertThat(writeBarGap(3)).isEqualTo(5);
        assertThat(writeBarGap(33)).isEqualTo(35);
        assertThat(writeBarGap(80)).isEqualTo(80);
        assertThat(writeBarGap(99)).isEqualTo(80);
    }

    @Test
    public void normalizeBarGapForWrite_putsOffGridReadOnGrid() {
        assertThat(YrrpSettingsStore.normalizeBarGapForWrite(readBarGap(33))).isEqualTo(35);
    }

    @Test
    public void isPulseEnabled_readsAnyNonZeroAsTrue() {
        mBackend.put(KEY_ENABLED, 7, USER);

        assertThat(mStore.isPulseEnabled()).isTrue();
    }

    @Test
    public void setPulseEnabled_writesOneOrZero() {
        mStore.setPulseEnabled(true);
        assertThat(mBackend.get(KEY_ENABLED, USER)).isEqualTo(1);

        mStore.setPulseEnabled(false);
        assertThat(mBackend.get(KEY_ENABLED, USER)).isEqualTo(0);
    }

    @Test
    public void getScreenOffAnimation_mapsOnlyOneToCrt() {
        assertThat(readAnimation(1)).isEqualTo(YrrpSettingsStore.SCREEN_OFF_CRT);
        assertThat(readAnimation(0)).isEqualTo(YrrpSettingsStore.SCREEN_OFF_STOCK);
        assertThat(readAnimation(2)).isEqualTo(YrrpSettingsStore.SCREEN_OFF_STOCK);
        assertThat(readAnimation(99)).isEqualTo(YrrpSettingsStore.SCREEN_OFF_STOCK);
        assertThat(readAnimation(-1)).isEqualTo(YrrpSettingsStore.SCREEN_OFF_STOCK);
    }

    @Test
    public void setScreenOffAnimation_writesStockOrCrt() {
        assertThat(mStore.setScreenOffAnimation(1)).isTrue();
        assertThat(mBackend.get(KEY_ANIMATION, USER)).isEqualTo(1);

        assertThat(mStore.setScreenOffAnimation(0)).isTrue();
        assertThat(mBackend.get(KEY_ANIMATION, USER)).isEqualTo(0);
    }

    @Test
    public void setScreenOffAnimation_rejectsOtherValuesWithoutWriting() {
        assertThat(mStore.setScreenOffAnimation(2)).isFalse();
        assertThat(mStore.setScreenOffAnimation(-1)).isFalse();

        assertThat(mBackend.mWrites).isEqualTo(0);
    }

    @Test
    public void getPulseColorMode_mapsOnlyOneToMatchTheme() {
        assertThat(readColorMode(1)).isEqualTo(YrrpSettingsStore.PULSE_COLOR_MODE_MATCH_THEME);
        assertThat(readColorMode(0)).isEqualTo(YrrpSettingsStore.PULSE_COLOR_MODE_SOLID);
        assertThat(readColorMode(2)).isEqualTo(YrrpSettingsStore.PULSE_COLOR_MODE_SOLID);
        assertThat(readColorMode(-1)).isEqualTo(YrrpSettingsStore.PULSE_COLOR_MODE_SOLID);
    }

    @Test
    public void setPulseColorMode_writesSolidOrMatchTheme() {
        assertThat(mStore.setPulseColorMode(1)).isTrue();
        assertThat(mBackend.get(KEY_COLOR_MODE, USER)).isEqualTo(1);

        assertThat(mStore.setPulseColorMode(0)).isTrue();
        assertThat(mBackend.get(KEY_COLOR_MODE, USER)).isEqualTo(0);
    }

    @Test
    public void setPulseColorMode_rejectsOtherValuesWithoutWriting() {
        assertThat(mStore.setPulseColorMode(2)).isFalse();
        assertThat(mStore.setPulseColorMode(-1)).isFalse();

        assertThat(mBackend.mWrites).isEqualTo(0);
    }

    @Test
    public void formatRgb_formatsUppercaseSixDigitsWithoutAlpha() {
        assertThat(YrrpSettingsStore.formatRgb(0xFFFFFF)).isEqualTo("#FFFFFF");
        assertThat(YrrpSettingsStore.formatRgb(0xD90A0B0C)).isEqualTo("#0A0B0C");
    }

    @Test
    public void setters_returnFalseWhenWriteFails() {
        mBackend.mFailWrites = true;

        assertThat(mStore.setPulseEnabled(true)).isFalse();
        assertThat(mStore.setPulseColor(0x123456)).isFalse();
        assertThat(mStore.setPulseAlpha(128)).isFalse();
        assertThat(mStore.setPulseHeightDp(48)).isFalse();
        assertThat(mStore.setPulseColorMode(1)).isFalse();
        assertThat(mStore.setPulseBoost(20)).isFalse();
        assertThat(mStore.setPulseBarCount(32)).isFalse();
        assertThat(mStore.setPulseBarGapPercent(30)).isFalse();
        assertThat(mStore.setScreenOffAnimation(YrrpSettingsStore.SCREEN_OFF_CRT)).isFalse();
    }

    @Test
    public void setters_returnTrueWhenWriteSucceeds() {
        assertThat(mStore.setPulseEnabled(true)).isTrue();
        assertThat(mStore.setPulseColor(0x123456)).isTrue();
        assertThat(mStore.setPulseAlpha(128)).isTrue();
        assertThat(mStore.setPulseHeightDp(48)).isTrue();
        assertThat(mStore.setPulseColorMode(1)).isTrue();
        assertThat(mStore.setPulseBoost(20)).isTrue();
        assertThat(mStore.setPulseBarCount(32)).isTrue();
        assertThat(mStore.setPulseBarGapPercent(30)).isTrue();
        assertThat(mStore.setScreenOffAnimation(YrrpSettingsStore.SCREEN_OFF_CRT)).isTrue();
    }

    @Test
    public void toOpaqueColor_masksRgbAndSetsFullAlpha() {
        assertThat(YrrpSettingsStore.toOpaqueColor(0x123456)).isEqualTo(0xFF123456);
        assertThat(YrrpSettingsStore.toOpaqueColor(0x40ABCDEF)).isEqualTo(0xFFABCDEF);
    }

    private void readAll() {
        mStore.isPulseEnabled();
        mStore.getPulseColor();
        mStore.getPulseAlpha();
        mStore.getPulseHeightDp();
        mStore.getPulseColorMode();
        mStore.getPulseBoost();
        mStore.getPulseBarCount();
        mStore.getPulseBarGapPercent();
        mStore.getScreenOffAnimation();
    }

    private int readAlpha(int raw) {
        mBackend.put(KEY_ALPHA, raw, USER);
        return mStore.getPulseAlpha();
    }

    private int writeAlpha(int value) {
        mStore.setPulseAlpha(value);
        return mBackend.get(KEY_ALPHA, USER);
    }

    private int readHeight(int raw) {
        mBackend.put(KEY_HEIGHT, raw, USER);
        return mStore.getPulseHeightDp();
    }

    private int writeHeight(int value) {
        mStore.setPulseHeightDp(value);
        return mBackend.get(KEY_HEIGHT, USER);
    }

    private int readColorMode(int raw) {
        mBackend.put(KEY_COLOR_MODE, raw, USER);
        return mStore.getPulseColorMode();
    }

    private int readBoost(int raw) {
        mBackend.put(KEY_BOOST, raw, USER);
        return mStore.getPulseBoost();
    }

    private int writeBoost(int value) {
        mStore.setPulseBoost(value);
        return mBackend.get(KEY_BOOST, USER);
    }

    private int readBarCount(int raw) {
        mBackend.put(KEY_BAR_COUNT, raw, USER);
        return mStore.getPulseBarCount();
    }

    private int writeBarCount(int value) {
        mStore.setPulseBarCount(value);
        return mBackend.get(KEY_BAR_COUNT, USER);
    }

    private int readBarGap(int raw) {
        mBackend.put(KEY_BAR_GAP, raw, USER);
        return mStore.getPulseBarGapPercent();
    }

    private int writeBarGap(int value) {
        mStore.setPulseBarGapPercent(value);
        return mBackend.get(KEY_BAR_GAP, USER);
    }

    private int readAnimation(int raw) {
        mBackend.put(KEY_ANIMATION, raw, USER);
        return mStore.getScreenOffAnimation();
    }

    /** In-memory per-user settings that count writes and can be made to fail. */
    private static final class FakeBackend implements YrrpSettingsStore.Backend {
        private final Map<Integer, Map<String, Integer>> mValues = new HashMap<>();
        int mWrites;
        boolean mFailWrites;

        @Override
        public int getIntForUser(String key, int defaultValue, int userId) {
            return values(userId).getOrDefault(key, defaultValue);
        }

        @Override
        public boolean putIntForUser(String key, int value, int userId) {
            mWrites++;
            if (mFailWrites) {
                return false;
            }
            put(key, value, userId);
            return true;
        }

        /** Seeds a raw value without counting it as a store write. */
        void put(String key, int value, int userId) {
            values(userId).put(key, value);
        }

        Integer get(String key, int userId) {
            return values(userId).get(key);
        }

        Set<String> keys(int userId) {
            return values(userId).keySet();
        }

        private Map<String, Integer> values(int userId) {
            return mValues.computeIfAbsent(userId, u -> new HashMap<>());
        }
    }
}
