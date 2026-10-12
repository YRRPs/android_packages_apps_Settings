/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Replaces a file through a temp file, fsync, and rename, so a reader or a crash sees either
 * the old or the new file, never part of one. Pure Java rather than android.util.AtomicFile, so
 * the plain-JUnit tests can run it.
 */
final class YrrpAdBlockFiles {
    /** Writes the new content. */
    interface Content {
        void writeTo(Writer out) throws IOException;
    }

    private YrrpAdBlockFiles() {}

    static void writeAtomically(File target, Content content) throws IOException {
        final File tmp = new File(target.getPath() + ".tmp");
        try {
            try (FileOutputStream stream = new FileOutputStream(tmp)) {
                final Writer out = new BufferedWriter(
                        new OutputStreamWriter(stream, StandardCharsets.UTF_8), 64 * 1024);
                content.writeTo(out);
                out.flush();
                stream.getFD().sync();
            }
            if (!tmp.renameTo(target)) {
                throw new IOException("rename failed: " + target.getName());
            }
        } finally {
            tmp.delete();
        }
    }
}
