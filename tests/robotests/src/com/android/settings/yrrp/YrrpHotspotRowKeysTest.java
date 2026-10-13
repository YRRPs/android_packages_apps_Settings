/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

/** Plain JUnit; no Robolectric. */
public class YrrpHotspotRowKeysTest {
    private static final String A = "aa:aa:aa:aa:aa:aa";
    private static final String B = "bb:bb:bb:bb:bb:bb";

    @Test
    public void firstUpdateAsksForRebuild() {
        assertTrue(new YrrpHotspotRowKeys<String>("row_").update(List.of()));
    }

    @Test
    public void unchangedItemsSkipRebuild() {
        final YrrpHotspotRowKeys<String> keys = new YrrpHotspotRowKeys<>("row_");
        keys.update(List.of(A, B));
        assertFalse(keys.update(List.of(A, B)));
    }

    @Test
    public void changedItemsAskForRebuild() {
        final YrrpHotspotRowKeys<String> keys = new YrrpHotspotRowKeys<>("row_");
        keys.update(List.of(A, B));
        assertTrue(keys.update(List.of(B)));
    }

    @Test
    public void resetForcesNextRebuild() {
        final YrrpHotspotRowKeys<String> keys = new YrrpHotspotRowKeys<>("row_");
        keys.update(List.of(A));
        keys.reset();
        assertTrue(keys.update(List.of(A)));
    }

    /** A tap reaches the controller with a stale Preference object; only its key may matter. */
    @Test
    public void keyResolvesToCurrentItem() {
        final YrrpHotspotRowKeys<String> keys = new YrrpHotspotRowKeys<>("row_");
        keys.update(List.of(A, B));
        assertEquals("row_1", keys.key(1));
        assertEquals(B, keys.get("row_1"));
    }

    @Test
    public void foreignOrStaleKeyResolvesToNull() {
        final YrrpHotspotRowKeys<String> keys = new YrrpHotspotRowKeys<>("row_");
        keys.update(List.of(A));
        assertNull(keys.get("row_1"));
        assertNull(keys.get("other_0"));
        assertNull(keys.get("row_x"));
        assertNull(keys.get(null));
        keys.reset();
        assertNull(keys.get("row_0"));
    }

    @Test
    public void keysNeverContainItemText() {
        final YrrpHotspotRowKeys<String> keys = new YrrpHotspotRowKeys<>("row_");
        keys.update(List.of(A));
        assertFalse(keys.key(0).contains("aa"));
    }
}
