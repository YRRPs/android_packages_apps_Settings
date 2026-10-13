/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Joins the hotspot's connected MACs (from the Wi-Fi module) with DHCP details (from tethering).
 * The Wi-Fi list decides who is connected; a detail for any other MAC is stale and ignored.
 */
final class YrrpHotspotClients {
    /** One tethering address entry: a MAC, its DHCP hostname, and a "a.b.c.d/nn" address. */
    static final class Detail {
        final String mac;
        final String hostname;
        final String address;

        Detail(String mac, String hostname, String address) {
            this.mac = mac;
            this.hostname = hostname;
            this.address = address;
        }
    }

    /** One connected client; hostname and ipv4 are null when DHCP has not supplied them. */
    static final class Row {
        final String mac;
        String hostname;
        String ipv4;

        Row(String mac) {
            this.mac = mac;
        }

        /** Equal rows render identically, so the page can skip rebuilding them. */
        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Row)) {
                return false;
            }
            final Row row = (Row) other;
            return mac.equals(row.mac) && Objects.equals(hostname, row.hostname)
                    && Objects.equals(ipv4, row.ipv4);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mac, hostname, ipv4);
        }
    }

    private static final Comparator<Row> ORDER =
            Comparator.comparing((Row row) -> row.hostname == null)
                    .thenComparing(row -> row.hostname == null
                            ? "" : row.hostname.toLowerCase(Locale.ROOT))
                    .thenComparing(row -> row.mac);

    private YrrpHotspotClients() {}

    static List<Row> merge(List<String> connectedMacs, List<Detail> details) {
        final Map<String, Row> rows = new LinkedHashMap<>();
        for (String mac : connectedMacs) {
            final String normalized = YrrpHotspotMac.normalize(mac);
            if (normalized != null) {
                rows.putIfAbsent(normalized, new Row(normalized));
            }
        }
        for (Detail detail : details) {
            final Row row = rows.get(YrrpHotspotMac.normalize(detail.mac));
            if (row != null) {
                fill(row, detail);
            }
        }
        final List<Row> sorted = new ArrayList<>(rows.values());
        sorted.sort(ORDER);
        return sorted;
    }

    private static void fill(Row row, Detail detail) {
        if (row.hostname == null && detail.hostname != null && !detail.hostname.isEmpty()) {
            row.hostname = detail.hostname;
        }
        if (row.ipv4 == null) {
            row.ipv4 = ipv4(detail.address);
        }
    }

    /** The bare IPv4 address of "a.b.c.d/nn", or null for IPv6 and missing addresses. */
    private static String ipv4(String address) {
        if (address == null || address.indexOf(':') >= 0 || address.indexOf('.') < 0) {
            return null;
        }
        final int slash = address.indexOf('/');
        return slash < 0 ? address : address.substring(0, slash);
    }
}
