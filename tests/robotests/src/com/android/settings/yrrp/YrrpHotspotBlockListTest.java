/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/** Plain JUnit; no Robolectric. */
public class YrrpHotspotBlockListTest {
    private static final String A = "aa:aa:aa:aa:aa:aa";
    private static final String B = "bb:bb:bb:bb:bb:bb";

    @Test
    public void addAppendsNormalizedMac() {
        assertEquals(List.of(A, B), YrrpHotspotBlockList.add(List.of(A), "BB:BB:BB:BB:BB:BB"));
    }

    @Test
    public void addIgnoresDuplicate() {
        assertEquals(List.of(A), YrrpHotspotBlockList.add(List.of(A), "AA:AA:AA:AA:AA:AA"));
    }

    @Test
    public void addIgnoresMalformed() {
        assertEquals(List.of(A), YrrpHotspotBlockList.add(List.of(A), "nonsense"));
    }

    @Test
    public void removeDropsMacAndIsIdempotent() {
        assertEquals(List.of(B), YrrpHotspotBlockList.remove(List.of(A, B), "AA:AA:AA:AA:AA:AA"));
        assertEquals(List.of(B), YrrpHotspotBlockList.remove(List.of(B), A));
    }

    @Test
    public void neverMutatesInput() {
        final List<String> input = new ArrayList<>(List.of(A));
        YrrpHotspotBlockList.add(input, B);
        YrrpHotspotBlockList.remove(input, A);
        assertEquals(List.of(A), input);
    }

    @Test
    public void normalizeLowercasesDedupesAndDropsMalformed() {
        assertEquals(List.of(A, B), YrrpHotspotBlockList.normalize(
                List.of("AA:AA:AA:AA:AA:AA", "junk", B, A)));
    }

    @Test
    public void containsMatchesCaseInsensitively() {
        assertTrue(YrrpHotspotBlockList.contains(List.of(A), "AA:AA:AA:AA:AA:AA"));
        assertFalse(YrrpHotspotBlockList.contains(List.of(A), B));
    }
}
