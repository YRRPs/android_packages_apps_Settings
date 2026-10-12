/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Downloads one hosts source as a conditional GET. Settings allows cleartext traffic, so this
 * class enforces https itself, on the first URL and on every redirect hop, which it follows by
 * hand. TLS uses the platform's default trust; there is no custom TrustManager. The body is
 * capped, and the destination changes only when a complete body arrived.
 */
final class YrrpAdBlockFetcher {
    static final long MAX_BYTES = 10L * 1024 * 1024;
    static final int MAX_REDIRECTS = 5;
    static final int CONNECT_TIMEOUT_MS = 15_000;
    static final int READ_TIMEOUT_MS = 30_000;

    enum Status { OK, NOT_MODIFIED, NOT_HTTPS, TOO_MANY_REDIRECTS, HTTP_ERROR, TOO_LARGE, IO_ERROR }

    /** Opens a connection; replaced in tests. */
    interface Opener {
        HttpURLConnection open(URL url) throws IOException;
    }

    /** The outcome for one source. {@code etag} and {@code lastModified} are set on OK. */
    static final class Result {
        final Status status;
        final int httpCode;
        final String etag;
        final String lastModified;

        Result(Status status, int httpCode, String etag, String lastModified) {
            this.status = status;
            this.httpCode = httpCode;
            this.etag = etag;
            this.lastModified = lastModified;
        }

        static Result of(Status status, int httpCode) {
            return new Result(status, httpCode, null, null);
        }
    }

    private final Opener mOpener;

    YrrpAdBlockFetcher(Opener opener) {
        mOpener = opener;
    }

    static YrrpAdBlockFetcher create() {
        return new YrrpAdBlockFetcher(url -> (HttpURLConnection) url.openConnection());
    }

    /** Writes the body to {@code dest} only on OK; {@code dest} is untouched otherwise. */
    Result fetch(String url, String etag, String lastModified, File dest) {
        try {
            URL current = new URL(url);
            for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
                if (!"https".equals(current.getProtocol())) {
                    return Result.of(Status.NOT_HTTPS, 0);
                }
                final HttpURLConnection c = mOpener.open(current);
                try {
                    configure(c, hop == 0 ? etag : null, hop == 0 ? lastModified : null);
                    final int code = c.getResponseCode();
                    if (isRedirect(code)) {
                        final String location = c.getHeaderField("Location");
                        if (location == null) {
                            return Result.of(Status.HTTP_ERROR, code);
                        }
                        current = current.toURI().resolve(URI.create(location)).toURL();
                        continue;
                    }
                    return receive(c, code, etag, lastModified, dest);
                } finally {
                    c.disconnect();
                }
            }
            return Result.of(Status.TOO_MANY_REDIRECTS, 0);
        } catch (IOException | IllegalArgumentException | URISyntaxException e) {
            return Result.of(Status.IO_ERROR, 0);
        }
    }

    private static void configure(HttpURLConnection c, String etag, String lastModified) {
        c.setInstanceFollowRedirects(false);
        c.setConnectTimeout(CONNECT_TIMEOUT_MS);
        c.setReadTimeout(READ_TIMEOUT_MS);
        c.setUseCaches(false);
        if (etag != null && !etag.isEmpty()) {
            c.setRequestProperty("If-None-Match", etag);
        }
        if (lastModified != null && !lastModified.isEmpty()) {
            c.setRequestProperty("If-Modified-Since", lastModified);
        }
    }

    private static boolean isRedirect(int code) {
        return code == 301 || code == 302 || code == 303 || code == 307 || code == 308;
    }

    private static Result receive(HttpURLConnection c, int code, String etag,
            String lastModified, File dest) throws IOException {
        if (code == HttpURLConnection.HTTP_NOT_MODIFIED) {
            return new Result(Status.NOT_MODIFIED, code, etag, lastModified);
        }
        if (code != HttpURLConnection.HTTP_OK) {
            return Result.of(Status.HTTP_ERROR, code);
        }
        if (c.getContentLengthLong() > MAX_BYTES || !copyCapped(c.getInputStream(), dest)) {
            return Result.of(Status.TOO_LARGE, code);
        }
        return new Result(Status.OK, code,
                c.getHeaderField("ETag"), c.getHeaderField("Last-Modified"));
    }

    /** Copies to dest.tmp and renames; over the cap, deletes the tmp file and returns false. */
    private static boolean copyCapped(InputStream in, File dest) throws IOException {
        dest.getParentFile().mkdirs();
        final File tmp = new File(dest.getPath() + ".tmp");
        try {
            long total = 0;
            try (InputStream body = in; OutputStream out = new FileOutputStream(tmp)) {
                final byte[] buf = new byte[64 * 1024];
                for (int n = body.read(buf); n >= 0; n = body.read(buf)) {
                    total += n;
                    if (total > MAX_BYTES) {
                        return false;
                    }
                    out.write(buf, 0, n);
                }
            }
            if (!tmp.renameTo(dest)) {
                throw new IOException("rename failed");
            }
            return true;
        } finally {
            tmp.delete();
        }
    }
}
