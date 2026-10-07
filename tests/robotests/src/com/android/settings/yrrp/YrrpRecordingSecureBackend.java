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

import android.content.ContentResolver;
import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A {@link YrrpSettingsStore.Backend} over Robolectric's secure settings that records every write
 * attempt and can fail writes to chosen keys without storing them.
 *
 * <p>Successful writes go through {@link Settings.Secure}. Robolectric's secure settings notify
 * registered content observers only when the stored value changes; writing the value already stored
 * notifies nobody.
 */
final class YrrpRecordingSecureBackend implements YrrpSettingsStore.Backend {
    /** Every write attempt, failed ones included, as {@code key=value} in call order. */
    final List<String> mWrites = new ArrayList<>();

    /** Keys whose writes return false and store nothing. */
    final Set<String> mFailingKeys = new HashSet<>();

    private final ContentResolver mContentResolver;

    YrrpRecordingSecureBackend(Context context) {
        mContentResolver = context.getContentResolver();
    }

    /** A store over this backend for the calling user. */
    YrrpSettingsStore newStore() {
        return new YrrpSettingsStore(this, UserHandle::myUserId);
    }

    @Override
    public int getIntForUser(String key, int defaultValue, int userId) {
        return Settings.Secure.getIntForUser(mContentResolver, key, defaultValue, userId);
    }

    @Override
    public boolean putIntForUser(String key, int value, int userId) {
        mWrites.add(key + "=" + value);
        if (mFailingKeys.contains(key)) {
            return false;
        }
        return Settings.Secure.putIntForUser(mContentResolver, key, value, userId);
    }
}
