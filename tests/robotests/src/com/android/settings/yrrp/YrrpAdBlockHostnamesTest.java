/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Plain JUnit; no Robolectric. */
public class YrrpAdBlockHostnamesTest {
    @Test
    public void acceptsOrdinaryNames() {
        assertTrue(YrrpAdBlockHostnames.isBlockable("ads.example.com"));
        assertTrue(YrrpAdBlockHostnames.isBlockable("a-b.c_d.example"));
        assertTrue(YrrpAdBlockHostnames.isBlockable("xn--bcher-kva.example"));
        assertTrue(YrrpAdBlockHostnames.isBlockable("1x.2y.example"));
    }

    @Test
    public void rejectsReservedNames() {
        for (String name : new String[] {"localhost", "localhost.localdomain", "local",
                "broadcasthost", "ip6-localhost", "ip6-loopback", "ip6-allnodes", "0.0.0.0",
                "LOCALHOST.localdomain"}) {
            assertFalse(name, YrrpAdBlockHostnames.isBlockable(name));
        }
    }

    @Test
    public void rejectsMalformedNames() {
        for (String name : new String[] {"", "nodot", ".example.com", "example.com.",
                "-a.example", "a-.example", "a..example", "exa mple.com", "ex*ample.com",
                "1.2.3.4", "::1", "fe80::1", "a.b/c", "nul.example\u0000", "bücher.example",
                "a.example\n"}) {
            assertFalse(name, YrrpAdBlockHostnames.isBlockable(name));
        }
        assertFalse(YrrpAdBlockHostnames.isBlockable("a." + "b".repeat(64) + ".com"));
        assertFalse(YrrpAdBlockHostnames.isBlockable("abcdefghi.".repeat(26) + "com"));
    }

    @Test
    public void normalizesCase() {
        assertEquals("ads.example.com", YrrpAdBlockHostnames.normalize("ADS.Example.COM"));
        assertNull(YrrpAdBlockHostnames.normalize("localhost"));
        assertNull(YrrpAdBlockHostnames.normalize("1.2.3.4"));
    }
}
