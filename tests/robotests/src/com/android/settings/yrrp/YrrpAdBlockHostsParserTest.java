/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Set;

/** Plain JUnit; no Robolectric. */
public class YrrpAdBlockHostsParserTest {
    private static YrrpAdBlockHostsParser.Result parse(String text) throws IOException {
        return YrrpAdBlockHostsParser.parse(new StringReader(text));
    }

    @Test
    public void acceptsEveryBlockAddressForm() throws IOException {
        final YrrpAdBlockHostsParser.Result r = parse(
                "0.0.0.0 a.example\n127.0.0.1\tb.example\n:: c.example\r\n"
                        + "0.0.0.0 d.example e.example # two names\n");
        assertEquals(Set.of("a.example", "b.example", "c.example", "d.example", "e.example"),
                r.hosts);
        assertEquals(0, r.redirectsDropped);
        assertEquals(0, r.invalidDropped);
    }

    @Test
    public void dropsRedirectEntries() throws IOException {
        final YrrpAdBlockHostsParser.Result r = parse(
                "1.2.3.4 bank.example\n192.168.0.1 x.example\n::1 y.example\n0.0.0.0 ok.example\n");
        assertEquals(Set.of("ok.example"), r.hosts);
        assertEquals(3, r.redirectsDropped);
    }

    @Test
    public void dropsInvalidAndReservedNames() throws IOException {
        final YrrpAdBlockHostsParser.Result r = parse(
                "0.0.0.0 localhost\n127.0.0.1 localhost.localdomain\n0.0.0.0 1.2.3.4\n"
                        + "0.0.0.0 " + "a".repeat(1020) + ".com\n0.0.0.0\n");
        assertEquals(Set.of(), r.hosts);
        assertEquals(0, r.redirectsDropped);
        assertEquals(5, r.invalidDropped);
    }

    @Test
    public void ignoresBlanksAndCommentsAndCollapsesDuplicates() throws IOException {
        final YrrpAdBlockHostsParser.Result r = parse(
                "# header\n\n   \n0.0.0.0 ADS.Example\n0.0.0.0 ads.example\n"
                        + "#0.0.0.0 off.example\n");
        assertEquals(Set.of("ads.example"), r.hosts);
        assertEquals(0, r.invalidDropped);
    }
}
