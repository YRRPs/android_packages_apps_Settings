/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import com.google.common.net.InternetDomainName;

import java.util.Locale;
import java.util.Set;

/**
 * Which names a hosts block entry or an allowlist entry may hold. Label syntax comes from
 * Guava's {@link InternetDomainName}, as in the Private DNS dialog; this class adds the
 * hosts-file rules: ASCII only, at least one dot, no trailing dot, not a reserved local name,
 * and not an address.
 */
final class YrrpAdBlockHostnames {
    private static final Set<String> RESERVED = Set.of(
            "localhost", "localhost.localdomain", "local", "broadcasthost", "ip6-localhost",
            "ip6-loopback", "ip6-localnet", "ip6-mcastprefix", "ip6-allnodes", "ip6-allrouters",
            "ip6-allhosts", "0.0.0.0");

    private YrrpAdBlockHostnames() {}

    /** The lowercased name when it is blockable, else null. */
    static String normalize(String name) {
        final String lower = name.toLowerCase(Locale.ROOT);
        return isBlockable(lower) ? lower : null;
    }

    /** True for a dotted ASCII DNS name that is neither reserved nor an address. */
    static boolean isBlockable(String name) {
        final String lower = name.toLowerCase(Locale.ROOT);
        return hasOnlyNameCharacters(lower)
                && lower.indexOf('.') > 0
                && !lower.endsWith(".")
                && !RESERVED.contains(lower)
                && InternetDomainName.isValid(lower);
    }

    private static boolean hasOnlyNameCharacters(String name) {
        if (name.isEmpty()) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            final boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.';
            if (!ok) {
                return false;
            }
        }
        return true;
    }
}
