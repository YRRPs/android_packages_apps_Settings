/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import java.util.ArrayList;
import java.util.List;

/**
 * The items behind a list of generated preference rows, keyed by index.
 *
 * <p>Keys are indexes, never item text, because Settings logs the key of every clicked
 * preference. A tap resolves by key alone: after a rebuild, {@code PreferenceGroup} reuses a row's
 * id by key and DashboardFragment's comparison callback keeps the old view bound to the old,
 * detached Preference, so the tapped object is not the one in the group.
 *
 * <p>{@link #update} reports whether the items changed, so callers rebuild only then.
 */
final class YrrpHotspotRowKeys<T> {
    private final String mPrefix;
    private List<T> mItems;

    YrrpHotspotRowKeys(String prefix) {
        mPrefix = prefix;
    }

    /** Stores the items; true when the rows must be rebuilt (first call, reset, or a change). */
    boolean update(List<T> items) {
        if (mItems != null && mItems.equals(items)) {
            return false;
        }
        mItems = new ArrayList<>(items);
        return true;
    }

    /** Forgets the items, so the next {@link #update} rebuilds and no key resolves. */
    void reset() {
        mItems = null;
    }

    String key(int index) {
        return mPrefix + index;
    }

    /** The current item for a row key, or null for a key this list did not issue. */
    T get(String key) {
        if (mItems == null || key == null || !key.startsWith(mPrefix)) {
            return null;
        }
        final int index;
        try {
            index = Integer.parseInt(key.substring(mPrefix.length()));
        } catch (NumberFormatException e) {
            return null;
        }
        return index >= 0 && index < mItems.size() ? mItems.get(index) : null;
    }
}
