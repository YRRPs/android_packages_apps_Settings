/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import java.net.URI;

/** One hosts list and the result of its last update. Mutable; owned by a store state. */
final class YrrpAdBlockSource {
    static final String STATUS_NEVER = "NEVER";

    final String url;
    boolean enabled = true;
    String etag = "";
    String lastModified = "";
    /** A {@link YrrpAdBlockFetcher.Status} name, or {@link #STATUS_NEVER}. */
    String status = STATUS_NEVER;
    int entries;
    int redirectsDropped;
    long lastSuccessMillis;

    YrrpAdBlockSource(String url) {
        this.url = url;
    }

    /** The host part only, for display, logs and dumps; never the path or query. */
    String host() {
        try {
            final String host = URI.create(url).getHost();
            return host == null ? "?" : host;
        } catch (IllegalArgumentException e) {
            return "?";
        }
    }
}
