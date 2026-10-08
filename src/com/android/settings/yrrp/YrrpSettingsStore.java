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

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.Locale;
import java.util.function.IntSupplier;

/**
 * Reads and writes the private YRRPs secure settings for the current foreground user.
 *
 * <p>Other privileged writers can store any integer in these keys, so every read is normalized for
 * display without writing back. Setters normalize or reject their input before writing, so only
 * values SystemUI reads identically are stored.
 */
public class YrrpSettingsStore {
    private static final String TAG = "YrrpSettingsStore";

    static final String PULSE_ENABLED = "lineage_pulse_enabled";
    static final String PULSE_COLOR = "lineage_pulse_color";
    static final String PULSE_ALPHA = "lineage_pulse_alpha";
    static final String PULSE_HEIGHT_DP = "lineage_pulse_height_dp";
    static final String PULSE_COLOR_MODE = "lineage_pulse_color_mode";
    static final String PULSE_BOOST = "lineage_pulse_log_boost";
    static final String SCREEN_OFF_ANIMATION = "lineage_screen_off_animation";

    static final int PULSE_COLOR_DEFAULT = 0xFFFFFF;
    static final int PULSE_ALPHA_DEFAULT = 217;
    static final int PULSE_ALPHA_MIN = 26;
    static final int PULSE_ALPHA_MAX = 255;
    static final int PULSE_HEIGHT_DEFAULT_DP = 48;
    static final int PULSE_HEIGHT_MIN_DP = 8;
    static final int PULSE_HEIGHT_MAX_DP = 96;
    static final int PULSE_HEIGHT_STEP_DP = 4;
    static final int PULSE_COLOR_MODE_SOLID = 0;
    static final int PULSE_COLOR_MODE_MATCH_THEME = 1;
    /** Strength k of SystemUI's PulseHeightCurve; 0 draws bar heights linearly. */
    static final int PULSE_BOOST_DEFAULT = 20;
    static final int PULSE_BOOST_MIN = 0;
    static final int PULSE_BOOST_MAX = 100;
    static final int SCREEN_OFF_STOCK = 0;
    static final int SCREEN_OFF_CRT = 1;

    /** Per-user access to {@link Settings.Secure}, replaceable in tests. */
    interface Backend {
        int getIntForUser(String key, int defaultValue, int userId);

        boolean putIntForUser(String key, int value, int userId);
    }

    private final Backend mBackend;
    private final IntSupplier mCurrentUser;

    public YrrpSettingsStore(@NonNull Context context) {
        this(
                new SecureSettingsBackend(context.getContentResolver()),
                ActivityManager::getCurrentUser);
    }

    @VisibleForTesting
    YrrpSettingsStore(@NonNull Backend backend, @NonNull IntSupplier currentUser) {
        mBackend = backend;
        mCurrentUser = currentUser;
    }

    /** Missing reads as off and any non-zero value as on, as SystemUI reads it. */
    public boolean isPulseEnabled() {
        return getInt(PULSE_ENABLED, 0) != 0;
    }

    /** Writes 1 or 0. Returns false if the write failed. */
    public boolean setPulseEnabled(boolean enabled) {
        return putInt(PULSE_ENABLED, enabled ? 1 : 0);
    }

    /** Returns the stored Pulse color as 0xRRGGBB. */
    public int getPulseColor() {
        return normalizeColor(getInt(PULSE_COLOR, PULSE_COLOR_DEFAULT));
    }

    /** Writes the RGB bits of {@code rgb}. Returns false if the write failed. */
    public boolean setPulseColor(int rgb) {
        return putInt(PULSE_COLOR, normalizeColor(rgb));
    }

    /** Returns the stored Pulse opacity (alpha) clamped to the supported range. */
    public int getPulseAlpha() {
        return normalizeAlpha(getInt(PULSE_ALPHA, PULSE_ALPHA_DEFAULT));
    }

    /** Writes {@code alpha} clamped to the supported range. Returns false if the write failed. */
    public boolean setPulseAlpha(int alpha) {
        return putInt(PULSE_ALPHA, normalizeAlpha(alpha));
    }

    /** Returns the stored Pulse height clamped to the drawable range, as SystemUI draws it. */
    public int getPulseHeightDp() {
        return normalizeHeightForDisplay(getInt(PULSE_HEIGHT_DP, PULSE_HEIGHT_DEFAULT_DP));
    }

    /** Writes the height clamped and snapped to the slider step. Returns false on failure. */
    public boolean setPulseHeightDp(int heightDp) {
        return putInt(PULSE_HEIGHT_DP, normalizeHeightForWrite(heightDp));
    }

    /**
     * Returns {@link #PULSE_COLOR_MODE_MATCH_THEME} only for an exact Match theme value, otherwise
     * Solid, as SystemUI reads it.
     */
    public int getPulseColorMode() {
        return normalizeColorMode(getInt(PULSE_COLOR_MODE, PULSE_COLOR_MODE_SOLID));
    }

    /** Rejects anything other than Solid or Match theme instead of coercing it. */
    public boolean setPulseColorMode(int mode) {
        if (mode != PULSE_COLOR_MODE_SOLID && mode != PULSE_COLOR_MODE_MATCH_THEME) {
            return false;
        }
        return putInt(PULSE_COLOR_MODE, mode);
    }

    /** Returns the stored Pulse low-level boost clamped to the supported range. */
    public int getPulseBoost() {
        return normalizeBoost(getInt(PULSE_BOOST, PULSE_BOOST_DEFAULT));
    }

    /** Writes {@code boost} clamped to the supported range. Returns false if the write failed. */
    public boolean setPulseBoost(int boost) {
        return putInt(PULSE_BOOST, normalizeBoost(boost));
    }

    /** Returns {@link #SCREEN_OFF_CRT} only for an exact CRT value, otherwise Stock. */
    public int getScreenOffAnimation() {
        return normalizeScreenOffAnimation(getInt(SCREEN_OFF_ANIMATION, SCREEN_OFF_STOCK));
    }

    /** Rejects anything other than Stock or CRT instead of coercing it. */
    public boolean setScreenOffAnimation(int mode) {
        if (mode != SCREEN_OFF_STOCK && mode != SCREEN_OFF_CRT) {
            return false;
        }
        return putInt(SCREEN_OFF_ANIMATION, mode);
    }

    /** Returns the secure settings URI to observe for {@code key}. */
    @NonNull
    public Uri getUriFor(@NonNull String key) {
        return Settings.Secure.getUriFor(key);
    }

    static int normalizeColor(int raw) {
        return raw & 0xFFFFFF;
    }

    static int normalizeAlpha(int raw) {
        return Math.max(PULSE_ALPHA_MIN, Math.min(PULSE_ALPHA_MAX, raw));
    }

    /** Returns the RGB bits of {@code rgb} with full alpha, for drawing the color opaque. */
    static int toOpaqueColor(int rgb) {
        return 0xFF000000 | normalizeColor(rgb);
    }

    /** Formats the RGB bits of {@code rgb} as {@code #RRGGBB}, uppercase and locale independent. */
    @NonNull
    static String formatRgb(int rgb) {
        return String.format(Locale.US, "#%06X", normalizeColor(rgb));
    }

    static int normalizeHeightForDisplay(int raw) {
        return Math.max(PULSE_HEIGHT_MIN_DP, Math.min(PULSE_HEIGHT_MAX_DP, raw));
    }

    static int normalizeHeightForWrite(int raw) {
        final int clamped = normalizeHeightForDisplay(raw);
        return PULSE_HEIGHT_MIN_DP
                + ((clamped - PULSE_HEIGHT_MIN_DP + PULSE_HEIGHT_STEP_DP / 2)
                                / PULSE_HEIGHT_STEP_DP)
                        * PULSE_HEIGHT_STEP_DP;
    }

    static int normalizeColorMode(int raw) {
        return raw == PULSE_COLOR_MODE_MATCH_THEME
                ? PULSE_COLOR_MODE_MATCH_THEME
                : PULSE_COLOR_MODE_SOLID;
    }

    static int normalizeBoost(int raw) {
        return Math.max(PULSE_BOOST_MIN, Math.min(PULSE_BOOST_MAX, raw));
    }

    static int normalizeScreenOffAnimation(int raw) {
        return raw == SCREEN_OFF_CRT ? SCREEN_OFF_CRT : SCREEN_OFF_STOCK;
    }

    private int getInt(String key, int defaultValue) {
        return mBackend.getIntForUser(key, defaultValue, mCurrentUser.getAsInt());
    }

    private boolean putInt(String key, int value) {
        final boolean written = mBackend.putIntForUser(key, value, mCurrentUser.getAsInt());
        if (!written) {
            Log.e(TAG, "Failed to write " + key);
        }
        return written;
    }

    private static final class SecureSettingsBackend implements Backend {
        private final ContentResolver mContentResolver;

        SecureSettingsBackend(ContentResolver contentResolver) {
            mContentResolver = contentResolver;
        }

        @Override
        public int getIntForUser(String key, int defaultValue, int userId) {
            return Settings.Secure.getIntForUser(mContentResolver, key, defaultValue, userId);
        }

        @Override
        public boolean putIntForUser(String key, int value, int userId) {
            return Settings.Secure.putIntForUser(mContentResolver, key, value, userId);
        }
    }
}
