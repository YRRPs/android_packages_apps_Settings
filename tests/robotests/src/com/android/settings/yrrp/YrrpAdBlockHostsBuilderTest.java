/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;

/** Plain JUnit; no Robolectric. */
public class YrrpAdBlockHostsBuilderTest {
    @Test
    public void mergesSortsAndAppliesAllowlist() throws IOException {
        final StringWriter out = new StringWriter();
        final int entries = YrrpAdBlockHostsBuilder.write(out,
                List.of(Set.of("b.example", "a.example"), Set.of("a.example", "c.example")),
                Set.of("C.Example"), 10);
        assertEquals(2, entries);
        assertEquals("127.0.0.1 localhost\n::1 ip6-localhost\n"
                + "0.0.0.0 a.example\n0.0.0.0 b.example\n", out.toString());
    }

    @Test
    public void emptyInputWritesOnlyLocalhost() throws IOException {
        final StringWriter out = new StringWriter();
        assertEquals(0, YrrpAdBlockHostsBuilder.write(out, List.of(), Set.of(), 10));
        assertEquals(YrrpAdBlockHostsBuilder.STOCK_HEADER, out.toString());
    }

    @Test
    public void overCapThrowsBeforeWriting() {
        final StringWriter out = new StringWriter();
        assertThrows(IllegalStateException.class, () -> YrrpAdBlockHostsBuilder.write(out,
                List.of(Set.of("a.example", "b.example", "c.example")), Set.of(), 2));
        assertEquals("", out.toString());
    }
}
