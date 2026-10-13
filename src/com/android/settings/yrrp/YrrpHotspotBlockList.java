/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import java.util.ArrayList;
import java.util.List;

/**
 * Edits of the hotspot blocked-client list as normalized MAC strings. Every edit returns a new
 * list and leaves its input untouched, because the input is read from the live configuration.
 */
final class YrrpHotspotBlockList {
    private YrrpHotspotBlockList() {}

    /** Appends {@code mac}; a duplicate or malformed MAC leaves the list unchanged. */
    static List<String> add(List<String> blocked, String mac) {
        final List<String> result = normalizedCopy(blocked);
        final String normalized = YrrpHotspotMac.normalize(mac);
        if (normalized != null && !result.contains(normalized)) {
            result.add(normalized);
        }
        return result;
    }

    /** Removes {@code mac}; removing an absent MAC leaves the list unchanged. */
    static List<String> remove(List<String> blocked, String mac) {
        final List<String> result = normalizedCopy(blocked);
        result.remove(YrrpHotspotMac.normalize(mac));
        return result;
    }

    static boolean contains(List<String> blocked, String mac) {
        final String normalized = YrrpHotspotMac.normalize(mac);
        return normalized != null && normalizedCopy(blocked).contains(normalized);
    }

    private static List<String> normalizedCopy(List<String> blocked) {
        final List<String> result = new ArrayList<>(blocked.size());
        for (String entry : blocked) {
            final String normalized = YrrpHotspotMac.normalize(entry);
            if (normalized != null && !result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }
}
