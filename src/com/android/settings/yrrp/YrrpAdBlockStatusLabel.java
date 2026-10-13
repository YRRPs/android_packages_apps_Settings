/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import java.util.Map;

/**
 * What a list row says about its last download. Each saved status (a
 * {@link YrrpAdBlockFetcher.Status} name, {@link YrrpAdBlockSource#STATUS_NEVER}, or
 * {@link YrrpAdBlockUpdater#STATUS_EMPTY}) maps to one label; the controller maps the label
 * to a string. The dump keeps the raw status names.
 */
enum YrrpAdBlockStatusLabel {
    NOT_DOWNLOADED, UP_TO_DATE, UNCHANGED, NOT_HTTPS, TOO_MANY_REDIRECTS, SERVER_ERROR,
    TOO_LARGE, TIMED_OUT, CONNECTION_FAILED, EMPTY, UNKNOWN;

    private static final Map<String, YrrpAdBlockStatusLabel> BY_STATUS = Map.ofEntries(
            Map.entry(YrrpAdBlockSource.STATUS_NEVER, NOT_DOWNLOADED),
            Map.entry(YrrpAdBlockUpdater.STATUS_EMPTY, EMPTY),
            Map.entry(YrrpAdBlockFetcher.Status.OK.name(), UP_TO_DATE),
            Map.entry(YrrpAdBlockFetcher.Status.NOT_MODIFIED.name(), UNCHANGED),
            Map.entry(YrrpAdBlockFetcher.Status.NOT_HTTPS.name(), NOT_HTTPS),
            Map.entry(YrrpAdBlockFetcher.Status.TOO_MANY_REDIRECTS.name(), TOO_MANY_REDIRECTS),
            Map.entry(YrrpAdBlockFetcher.Status.HTTP_ERROR.name(), SERVER_ERROR),
            Map.entry(YrrpAdBlockFetcher.Status.TOO_LARGE.name(), TOO_LARGE),
            Map.entry(YrrpAdBlockFetcher.Status.TIMEOUT.name(), TIMED_OUT),
            Map.entry(YrrpAdBlockFetcher.Status.IO_ERROR.name(), CONNECTION_FAILED));

    /**
     * Whether the last download failed, so the list kept its last good copy. Matches the
     * updater's failure count; an unknown status is not counted.
     */
    boolean isFailure() {
        switch (this) {
            case NOT_DOWNLOADED:
            case UP_TO_DATE:
            case UNCHANGED:
            case UNKNOWN:
                return false;
            default:
                return true;
        }
    }

    /** UNKNOWN for a status this build does not know, such as one saved by a later build. */
    static YrrpAdBlockStatusLabel of(String status) {
        return status == null ? UNKNOWN : BY_STATUS.getOrDefault(status, UNKNOWN);
    }
}
