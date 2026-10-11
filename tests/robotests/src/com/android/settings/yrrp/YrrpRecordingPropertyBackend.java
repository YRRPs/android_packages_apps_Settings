/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** In-memory property backend that records writes and can refuse them. */
final class YrrpRecordingPropertyBackend implements YrrpBootAnimationStore.Backend {
    final Map<String, String> values = new HashMap<>();
    final List<String> writes = new ArrayList<>();
    boolean failWrites;

    @Override
    public String get(String key) {
        return values.getOrDefault(key, "");
    }

    @Override
    public boolean set(String key, String value) {
        writes.add(key + "=" + value);
        if (failWrites) {
            return false;
        }
        values.put(key, value);
        return true;
    }

    YrrpBootAnimationStore newStore() {
        return new YrrpBootAnimationStore(this);
    }
}
