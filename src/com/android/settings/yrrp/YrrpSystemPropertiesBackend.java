/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.os.SystemProperties;
import android.util.Log;

/** Reads and writes YRRP system properties; a refused write is logged and reported. */
final class YrrpSystemPropertiesBackend implements YrrpBootAnimationStore.Backend {
    private static final String TAG = "YrrpSystemProperties";

    @Override
    public String get(String key) {
        return SystemProperties.get(key, "");
    }

    @Override
    public boolean set(String key, String value) {
        try {
            SystemProperties.set(key, value);
            return true;
        } catch (RuntimeException e) {
            // SELinux denial or a full property area; the class name carries no user data.
            Log.e(TAG, "write failed key=" + key + " error=" + e.getClass().getSimpleName());
            return false;
        }
    }
}
