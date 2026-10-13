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

import static com.android.settings.display.FoldLockBehaviorSettings.SETTING_VALUE_SELECTIVE_STAY_AWAKE;
import static com.android.settings.display.FoldLockBehaviorSettings.SETTING_VALUE_STAY_AWAKE_ON_FOLD;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.function.IntSupplier;

/**
 * Hands "Continue using apps on fold" to the Duo fold animation and back, for the current user.
 *
 * <p>Selecting Duo stores the user's fold-lock value in a private secure key and writes Always, so
 * folding never shows the lock screen. Leaving Duo writes the stored value back, or Swipe up to
 * continue when none was stored, and deletes the key. SystemUI keeps the value at Always while Duo
 * is on.
 */
public class YrrpFoldLockStore {
    private static final String TAG = "YrrpFoldLockStore";

    /** The framework fold-lock setting, {@link Settings.System#FOLD_LOCK_BEHAVIOR}. */
    static final String FOLD_LOCK = Settings.System.FOLD_LOCK_BEHAVIOR;
    /** Per-user secure key holding the fold-lock value from before Duo was selected. */
    static final String BACKUP_BEFORE_DUO = "yrrp_fold_lock_before_duo";

    /** Per-user access to {@link Settings.System} and {@link Settings.Secure} strings. */
    interface Backend {
        @Nullable
        String getSystemString(String key, int userId);

        boolean putSystemString(String key, String value, int userId);

        @Nullable
        String getSecureString(String key, int userId);

        boolean putSecureString(String key, String value, int userId);

        boolean deleteSecure(String key, int userId);
    }

    private final Backend mBackend;
    private final IntSupplier mCurrentUser;

    public YrrpFoldLockStore(@NonNull Context context) {
        this(new SettingsBackend(context.getContentResolver()), ActivityManager::getCurrentUser);
    }

    @VisibleForTesting
    YrrpFoldLockStore(@NonNull Backend backend, @NonNull IntSupplier currentUser) {
        mBackend = backend;
        mCurrentUser = currentUser;
    }

    /**
     * Stores the current fold-lock value unless a stored value already exists, then writes Always.
     * Returns false, leaving the fold-lock value untouched, when the backup write fails.
     */
    public boolean enterDuo() {
        final int user = mCurrentUser.getAsInt();
        if (!hasBackup()) {
            final String current = mBackend.getSystemString(FOLD_LOCK, user);
            final String backup = current != null ? current : SETTING_VALUE_SELECTIVE_STAY_AWAKE;
            if (!logged(mBackend.putSecureString(BACKUP_BEFORE_DUO, backup, user), "store")) {
                return false;
            }
        }
        return logged(
                mBackend.putSystemString(FOLD_LOCK, SETTING_VALUE_STAY_AWAKE_ON_FOLD, user),
                "force Always");
    }

    /**
     * Writes back the stored fold-lock value, or Swipe up to continue when none exists, then deletes
     * the stored value. A failed restore keeps the stored value so the next attempt can use it.
     */
    public boolean leaveDuo() {
        final int user = mCurrentUser.getAsInt();
        final String stored = mBackend.getSecureString(BACKUP_BEFORE_DUO, user);
        final String restore = stored != null ? stored : SETTING_VALUE_SELECTIVE_STAY_AWAKE;
        if (!logged(mBackend.putSystemString(FOLD_LOCK, restore, user), "restore")) {
            return false;
        }
        return logged(mBackend.deleteSecure(BACKUP_BEFORE_DUO, user), "delete backup");
    }

    /** Whether a fold-lock value from before Duo is stored. */
    public boolean hasBackup() {
        return mBackend.getSecureString(BACKUP_BEFORE_DUO, mCurrentUser.getAsInt()) != null;
    }

    private static boolean logged(boolean succeeded, String stage) {
        if (!succeeded) {
            Log.e(TAG, "Fold-lock " + stage + " failed");
        }
        return succeeded;
    }

    private static final class SettingsBackend implements Backend {
        private final ContentResolver mContentResolver;

        SettingsBackend(ContentResolver contentResolver) {
            mContentResolver = contentResolver;
        }

        @Override
        public String getSystemString(String key, int userId) {
            return Settings.System.getStringForUser(mContentResolver, key, userId);
        }

        @Override
        public boolean putSystemString(String key, String value, int userId) {
            return Settings.System.putStringForUser(mContentResolver, key, value, userId);
        }

        @Override
        public String getSecureString(String key, int userId) {
            return Settings.Secure.getStringForUser(mContentResolver, key, userId);
        }

        @Override
        public boolean putSecureString(String key, String value, int userId) {
            return Settings.Secure.putStringForUser(mContentResolver, key, value, userId);
        }

        /** Settings.Secure has no delete API, so call the provider's delete method directly. */
        @Override
        public boolean deleteSecure(String key, int userId) {
            final Bundle args = new Bundle();
            args.putInt(Settings.CALL_METHOD_USER_KEY, userId);
            try {
                mContentResolver.call(
                        Settings.Secure.CONTENT_URI, Settings.CALL_METHOD_DELETE_SECURE, key, args);
                return true;
            } catch (RuntimeException e) {
                Log.e(TAG, "Delete failed: " + e.getClass().getSimpleName());
                return false;
            }
        }
    }
}
