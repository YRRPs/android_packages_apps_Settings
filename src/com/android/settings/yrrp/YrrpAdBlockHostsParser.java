/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

/**
 * Reads a hosts-format list and keeps only block entries. Lists come from the internet, so
 * a line that maps a name to any address other than 0.0.0.0, 127.0.0.1, or :: is dropped: a
 * poisoned list must not be able to send a name to an attacker's address.
 */
final class YrrpAdBlockHostsParser {
    static final int MAX_LINE = 1024;

    /** Parsed names plus what was dropped, for the per-source status. */
    static final class Result {
        final Set<String> hosts = new HashSet<>();
        int redirectsDropped;
        int invalidDropped;
    }

    private YrrpAdBlockHostsParser() {}

    static Result parse(Reader in) throws IOException {
        final Result result = new Result();
        final BufferedReader reader = new BufferedReader(in);
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            parseLine(line, result);
        }
        return result;
    }

    private static void parseLine(String raw, Result result) {
        if (raw.length() > MAX_LINE) {
            result.invalidDropped++;
            return;
        }
        final int hash = raw.indexOf('#');
        final String line = (hash >= 0 ? raw.substring(0, hash) : raw).trim();
        if (line.isEmpty()) {
            return;
        }
        final String[] fields = line.split("\\s+");
        if (fields.length < 2) {
            result.invalidDropped++;
            return;
        }
        if (!isBlockAddress(fields[0])) {
            result.redirectsDropped++;
            return;
        }
        for (int i = 1; i < fields.length; i++) {
            final String host = YrrpAdBlockHostnames.normalize(fields[i]);
            if (host == null) {
                result.invalidDropped++;
            } else {
                result.hosts.add(host);
            }
        }
    }

    private static boolean isBlockAddress(String address) {
        return "0.0.0.0".equals(address) || "127.0.0.1".equals(address) || "::".equals(address);
    }
}
