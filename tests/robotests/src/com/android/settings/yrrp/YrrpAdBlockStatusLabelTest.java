/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.EnumSet;

import org.junit.Test;

/** Plain JUnit; no Robolectric. */
public class YrrpAdBlockStatusLabelTest {
    @Test
    public void everyFetcherStatusHasItsOwnLabel() {
        for (YrrpAdBlockFetcher.Status status : YrrpAdBlockFetcher.Status.values()) {
            assertNotEquals(status.name(), YrrpAdBlockStatusLabel.UNKNOWN,
                    YrrpAdBlockStatusLabel.of(status.name()));
        }
    }

    @Test
    public void mapsEachStatusToTheLabelAUserReads() {
        assertEquals(YrrpAdBlockStatusLabel.NOT_DOWNLOADED,
                YrrpAdBlockStatusLabel.of(YrrpAdBlockSource.STATUS_NEVER));
        assertEquals(YrrpAdBlockStatusLabel.EMPTY,
                YrrpAdBlockStatusLabel.of(YrrpAdBlockUpdater.STATUS_EMPTY));
        assertEquals(YrrpAdBlockStatusLabel.UP_TO_DATE, YrrpAdBlockStatusLabel.of("OK"));
        assertEquals(YrrpAdBlockStatusLabel.UNCHANGED, YrrpAdBlockStatusLabel.of("NOT_MODIFIED"));
        assertEquals(YrrpAdBlockStatusLabel.NOT_HTTPS, YrrpAdBlockStatusLabel.of("NOT_HTTPS"));
        assertEquals(YrrpAdBlockStatusLabel.TOO_MANY_REDIRECTS,
                YrrpAdBlockStatusLabel.of("TOO_MANY_REDIRECTS"));
        assertEquals(YrrpAdBlockStatusLabel.SERVER_ERROR, YrrpAdBlockStatusLabel.of("HTTP_ERROR"));
        assertEquals(YrrpAdBlockStatusLabel.TOO_LARGE, YrrpAdBlockStatusLabel.of("TOO_LARGE"));
        assertEquals(YrrpAdBlockStatusLabel.TIMED_OUT, YrrpAdBlockStatusLabel.of("TIMEOUT"));
        assertEquals(YrrpAdBlockStatusLabel.CONNECTION_FAILED,
                YrrpAdBlockStatusLabel.of("IO_ERROR"));
    }

    @Test
    public void unknownOrMissingStatusIsUnknown() {
        // state.tsv written by a later build, or a damaged row.
        assertEquals(YrrpAdBlockStatusLabel.UNKNOWN, YrrpAdBlockStatusLabel.of("SOMETHING_NEW"));
        assertEquals(YrrpAdBlockStatusLabel.UNKNOWN, YrrpAdBlockStatusLabel.of(""));
        assertEquals(YrrpAdBlockStatusLabel.UNKNOWN, YrrpAdBlockStatusLabel.of(null));
    }

    @Test
    public void onlyAFailedDownloadIsAFailure() {
        final EnumSet<YrrpAdBlockStatusLabel> notFailures = EnumSet.of(
                YrrpAdBlockStatusLabel.NOT_DOWNLOADED, YrrpAdBlockStatusLabel.UP_TO_DATE,
                YrrpAdBlockStatusLabel.UNCHANGED, YrrpAdBlockStatusLabel.UNKNOWN);
        for (YrrpAdBlockStatusLabel label : YrrpAdBlockStatusLabel.values()) {
            assertEquals(label.name(), !notFailures.contains(label), label.isFailure());
        }
    }
}
