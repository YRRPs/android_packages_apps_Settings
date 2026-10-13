/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import java.util.Locale;
import java.util.regex.Pattern;

/** MAC address text in one canonical form, so hotspot lists compare by plain string equality. */
final class YrrpHotspotMac {
    private static final Pattern MAC = Pattern.compile("[0-9a-f]{2}(:[0-9a-f]{2}){5}");

    private YrrpHotspotMac() {}

    /** Returns lowercase {@code aa:bb:cc:dd:ee:ff}, or null if {@code text} is not a MAC. */
    static String normalize(String text) {
        if (text == null) {
            return null;
        }
        final String mac = text.trim().toLowerCase(Locale.ROOT);
        return MAC.matcher(mac).matches() ? mac : null;
    }
}
