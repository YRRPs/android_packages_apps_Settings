/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * The last {@link #CAPACITY} ad-block transitions, kept in a small file so they survive the
 * Settings process dying between an update and a dump. Callers record only enum names, counts,
 * and source hostnames: never full URLs, allowlist entries, or looked-up names.
 */
final class YrrpAdBlockEventLog {
    static final int CAPACITY = 50;

    private final File mFile;
    private final LongSupplier mClock;

    YrrpAdBlockEventLog(File file, LongSupplier clock) {
        mFile = file;
        mClock = clock;
    }

    /** Appends one event; a failed write is dropped, because the log must never break a flow. */
    synchronized void record(String event) {
        final List<String> lines = new ArrayList<>(lines());
        lines.add(mClock.getAsLong() + " " + event.replaceAll("[\\r\\n]+", " "));
        final List<String> kept = lines.subList(Math.max(0, lines.size() - CAPACITY), lines.size());
        try {
            mFile.getParentFile().mkdirs();
            YrrpAdBlockFiles.writeAtomically(mFile, out -> {
                for (String line : kept) {
                    out.write(line);
                    out.write('\n');
                }
            });
        } catch (IOException e) {
            // Observability only.
        }
    }

    /** Oldest first; empty when the file is missing or unreadable. */
    synchronized List<String> lines() {
        try {
            return Files.readAllLines(mFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return List.of();
        }
    }
}
