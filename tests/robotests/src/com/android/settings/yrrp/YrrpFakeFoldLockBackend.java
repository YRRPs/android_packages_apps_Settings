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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** An in-memory {@link YrrpFoldLockStore.Backend} that records every write and delete attempt. */
final class YrrpFakeFoldLockBackend implements YrrpFoldLockStore.Backend {
    /**
     * Every attempt in call order: {@code system:key=value}, {@code secure:key=value}, or
     * {@code delete:key}.
     */
    final List<String> mWrites = new ArrayList<>();

    /** Keys whose writes and deletes fail and change nothing. */
    final Set<String> mFailingKeys = new HashSet<>();

    final Map<String, String> mSystem = new HashMap<>();
    final Map<String, String> mSecure = new HashMap<>();

    YrrpFoldLockStore newStore() {
        return new YrrpFoldLockStore(this, () -> 0);
    }

    @Override
    public String getSystemString(String key, int userId) {
        return mSystem.get(key);
    }

    @Override
    public boolean putSystemString(String key, String value, int userId) {
        mWrites.add("system:" + key + "=" + value);
        if (mFailingKeys.contains(key)) {
            return false;
        }
        mSystem.put(key, value);
        return true;
    }

    @Override
    public String getSecureString(String key, int userId) {
        return mSecure.get(key);
    }

    @Override
    public boolean putSecureString(String key, String value, int userId) {
        mWrites.add("secure:" + key + "=" + value);
        if (mFailingKeys.contains(key)) {
            return false;
        }
        mSecure.put(key, value);
        return true;
    }

    @Override
    public boolean deleteSecure(String key, int userId) {
        mWrites.add("delete:" + key);
        if (mFailingKeys.contains(key)) {
            return false;
        }
        mSecure.remove(key);
        return true;
    }
}
