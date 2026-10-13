/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.android.settings.yrrp.YrrpHotspotClients.Detail;
import com.android.settings.yrrp.YrrpHotspotClients.Row;

import java.util.List;

import org.junit.Test;

/** Plain JUnit; no Robolectric. */
public class YrrpHotspotClientsTest {
    private static final String A = "aa:aa:aa:aa:aa:aa";
    private static final String B = "bb:bb:bb:bb:bb:bb";
    private static final String C = "cc:cc:cc:cc:cc:cc";

    @Test
    public void macOnlyClientHasNoNameOrAddress() {
        final List<Row> rows = YrrpHotspotClients.merge(List.of(A), List.of());
        assertEquals(1, rows.size());
        assertEquals(A, rows.get(0).mac);
        assertNull(rows.get(0).hostname);
        assertNull(rows.get(0).ipv4);
    }

    @Test
    public void joinsDetailByMacIgnoringCase() {
        final List<Row> rows = YrrpHotspotClients.merge(List.of("AA:AA:AA:AA:AA:AA"),
                List.of(new Detail("AA:aa:AA:aa:AA:aa", "pixel", "192.168.43.12/24")));
        assertEquals("pixel", rows.get(0).hostname);
        assertEquals("192.168.43.12", rows.get(0).ipv4);
    }

    @Test
    public void picksFirstIpv4AndFirstNonEmptyHostname() {
        final List<Row> rows = YrrpHotspotClients.merge(List.of(A), List.of(
                new Detail(A, "", "fe80::1/64"),
                new Detail(A, null, "192.168.43.7/24"),
                new Detail(A, "laptop", "192.168.43.8/24")));
        assertEquals("laptop", rows.get(0).hostname);
        assertEquals("192.168.43.7", rows.get(0).ipv4);
    }

    @Test
    public void detailForDisconnectedMacIsIgnored() {
        final List<Row> rows = YrrpHotspotClients.merge(List.of(A),
                List.of(new Detail(B, "gone", "192.168.43.9/24")));
        assertEquals(1, rows.size());
        assertNull(rows.get(0).hostname);
    }

    @Test
    public void namedClientsFirstThenByMacWithoutDuplicates() {
        final List<Row> rows = YrrpHotspotClients.merge(List.of(C, A, B, A, "junk"), List.of(
                new Detail(B, "zeta", null),
                new Detail(C, "Alpha", null)));
        assertEquals(List.of(C, B, A), List.of(rows.get(0).mac, rows.get(1).mac, rows.get(2).mac));
        assertEquals(3, rows.size());
    }

    /** Equal rows let the page skip a rebuild that would strand the visible rows. */
    @Test
    public void rowsWithSameMacNameAndAddressAreEqual() {
        final List<Row> first = YrrpHotspotClients.merge(List.of(A),
                List.of(new Detail(A, "pixel", "192.168.43.12/24")));
        final List<Row> second = YrrpHotspotClients.merge(List.of(A),
                List.of(new Detail(A, "pixel", "192.168.43.12/24")));
        assertEquals(first, second);
        assertEquals(first.get(0).hashCode(), second.get(0).hashCode());
    }

    @Test
    public void rowsDifferWhenAddressChanges() {
        final List<Row> first = YrrpHotspotClients.merge(List.of(A),
                List.of(new Detail(A, "pixel", "192.168.43.12/24")));
        final List<Row> second = YrrpHotspotClients.merge(List.of(A),
                List.of(new Detail(A, "pixel", "192.168.43.13/24")));
        org.junit.Assert.assertNotEquals(first, second);
    }
}
