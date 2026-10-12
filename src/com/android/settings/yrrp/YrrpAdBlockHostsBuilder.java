/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/** Merges parsed sources into hosts-file text. The stock localhost lines always come first. */
final class YrrpAdBlockHostsBuilder {
    /** Bounds Settings' heap: the merged set is held in memory while the file is built. */
    static final int MAX_ENTRIES = 1_000_000;
    /** Matches AOSP system/core/rootdir/etc/hosts. */
    static final String STOCK_HEADER = "127.0.0.1 localhost\n::1 ip6-localhost\n";

    private YrrpAdBlockHostsBuilder() {}

    /**
     * Writes the file and returns the number of block entries. Throws before writing anything
     * when the merged list exceeds {@code max}.
     */
    static int write(Writer out, Collection<Set<String>> sources, Set<String> allowlist, int max)
            throws IOException {
        final TreeSet<String> merged = new TreeSet<>();
        for (Set<String> source : sources) {
            merged.addAll(source);
        }
        for (String allowed : allowlist) {
            merged.remove(allowed.toLowerCase(Locale.ROOT));
        }
        if (merged.size() > max) {
            throw new IllegalStateException("entries=" + merged.size() + " max=" + max);
        }
        return writeSorted(out, merged);
    }

    /** Writes an already merged, allowlist-free set; no copy, so a large list is held once. */
    static int writeSorted(Writer out, SortedSet<String> merged) throws IOException {
        out.write(STOCK_HEADER);
        for (String host : merged) {
            out.write("0.0.0.0 ");
            out.write(host);
            out.write('\n');
        }
        out.flush();
        return merged.size();
    }
}
