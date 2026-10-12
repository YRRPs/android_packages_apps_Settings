/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

/** System property access, replaceable in tests. {@code get} returns "" when unset. */
interface YrrpPropertyBackend {
    String get(String key);

    /** Returns false if the write was refused. */
    boolean set(String key, String value);
}
