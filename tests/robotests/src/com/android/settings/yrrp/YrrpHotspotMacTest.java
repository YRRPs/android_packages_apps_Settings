/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** Plain JUnit; no Robolectric. */
public class YrrpHotspotMacTest {
    @Test
    public void lowercasesAndTrims() {
        assertEquals("aa:bb:cc:0d:ee:ff", YrrpHotspotMac.normalize(" AA:BB:CC:0D:EE:FF "));
    }

    @Test
    public void rejectsMalformed() {
        assertNull(YrrpHotspotMac.normalize(null));
        assertNull(YrrpHotspotMac.normalize(""));
        assertNull(YrrpHotspotMac.normalize("aa:bb:cc:dd:ee"));
        assertNull(YrrpHotspotMac.normalize("aa-bb-cc-dd-ee-ff"));
        assertNull(YrrpHotspotMac.normalize("gg:bb:cc:dd:ee:ff"));
        assertNull(YrrpHotspotMac.normalize("aa:bb:cc:dd:ee:ff:00"));
    }
}
