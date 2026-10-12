/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/** Plain JUnit; no Robolectric. */
public class YrrpAdBlockStateTest {
    private final YrrpRecordingPropertyBackend mBackend = new YrrpRecordingPropertyBackend();
    private final YrrpAdBlockState mState = new YrrpAdBlockState(mBackend);

    @Test
    public void onlyOneOrTrueReadsAsEnabled() {
        assertFalse(mState.isEnabled());
        for (String on : new String[] {"1", "true"}) {
            mBackend.values.put(YrrpAdBlockState.ENABLED_PROPERTY, on);
            assertTrue(on, mState.isEnabled());
        }
        for (String off : new String[] {"0", "false", "yes", "2", ""}) {
            mBackend.values.put(YrrpAdBlockState.ENABLED_PROPERTY, off);
            assertFalse(off, mState.isEnabled());
        }
    }

    @Test
    public void setEnabledWritesOneOrZero() {
        assertTrue(mState.setEnabled(true));
        assertTrue(mState.setEnabled(false));
        assertEquals(List.of("persist.yrrp.adblock.enabled=1", "persist.yrrp.adblock.enabled=0"),
                mBackend.writes);
    }

    @Test
    public void bumpApplyCountsUpFromAnyStart() {
        assertTrue(mState.bumpApply());
        assertEquals("1", mBackend.values.get(YrrpAdBlockState.APPLY_PROPERTY));
        assertTrue(mState.bumpApply());
        assertEquals("2", mBackend.values.get(YrrpAdBlockState.APPLY_PROPERTY));
        mBackend.values.put(YrrpAdBlockState.APPLY_PROPERTY, "junk");
        assertTrue(mState.bumpApply());
        assertEquals("1", mBackend.values.get(YrrpAdBlockState.APPLY_PROPERTY));
    }

    @Test
    public void refusedWritesReturnFalse() {
        mBackend.failWrites = true;
        assertFalse(mState.setEnabled(true));
        assertFalse(mState.bumpApply());
    }
}
