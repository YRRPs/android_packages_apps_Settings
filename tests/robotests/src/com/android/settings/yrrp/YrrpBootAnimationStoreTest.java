/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class YrrpBootAnimationStoreTest {
    private final YrrpRecordingPropertyBackend mBackend = new YrrpRecordingPropertyBackend();
    private final YrrpBootAnimationStore mStore = mBackend.newStore();

    @Test
    public void propertyNames_matchBootanimation() {
        assertEquals("persist.yrrp.bootanim.style", YrrpBootAnimationStore.STYLE_PROPERTY);
        assertEquals("persist.yrrp.bootanim.full_intro", YrrpBootAnimationStore.FULL_INTRO_PROPERTY);
    }

    @Test
    public void xpStyleValue_matchesBootanimation() {
        assertEquals("xp", YrrpBootAnimationStore.STYLE_XP);
    }

    @Test
    public void unset_readsMatrixAndFullIntro() {
        assertEquals(YrrpBootAnimationStore.STYLE_MATRIX, mStore.getStyle());
        assertTrue(mStore.isFullIntro());
    }

    @Test
    public void style_onlyExactLineageOrXpReadAsThemselves() {
        mBackend.values.put(YrrpBootAnimationStore.STYLE_PROPERTY, "lineage");
        assertEquals(YrrpBootAnimationStore.STYLE_LINEAGE, mStore.getStyle());
        mBackend.values.put(YrrpBootAnimationStore.STYLE_PROPERTY, "xp");
        assertEquals(YrrpBootAnimationStore.STYLE_XP, mStore.getStyle());
        for (String value :
                new String[] {"LINEAGE", "garbage", " lineage", "matrix", "XP", "xp "}) {
            mBackend.values.put(YrrpBootAnimationStore.STYLE_PROPERTY, value);
            assertEquals(value, YrrpBootAnimationStore.STYLE_MATRIX, mStore.getStyle());
        }
    }

    @Test
    public void read_neverWrites() {
        mBackend.values.put(YrrpBootAnimationStore.STYLE_PROPERTY, "garbage");
        mBackend.values.put(YrrpBootAnimationStore.FULL_INTRO_PROPERTY, "maybe");
        mStore.getStyle();
        mStore.isFullIntro();
        assertEquals(Collections.emptyList(), mBackend.writes);
        assertEquals("garbage", mBackend.values.get(YrrpBootAnimationStore.STYLE_PROPERTY));
    }

    @Test
    public void fullIntro_falseOrZeroIsOffAnythingElseOn() {
        for (String off : new String[] {"false", "0"}) {
            mBackend.values.put(YrrpBootAnimationStore.FULL_INTRO_PROPERTY, off);
            assertFalse(off, mStore.isFullIntro());
        }
        for (String on : new String[] {"true", "1", "maybe", ""}) {
            mBackend.values.put(YrrpBootAnimationStore.FULL_INTRO_PROPERTY, on);
            assertTrue(on, mStore.isFullIntro());
        }
    }

    @Test
    public void setStyle_writesKnownStylesAndRejectsOthers() {
        assertTrue(mStore.setStyle(YrrpBootAnimationStore.STYLE_LINEAGE));
        assertTrue(mStore.setStyle(YrrpBootAnimationStore.STYLE_MATRIX));
        assertTrue(mStore.setStyle(YrrpBootAnimationStore.STYLE_XP));
        assertFalse(mStore.setStyle("garbage"));
        assertFalse(mStore.setStyle(null));
        assertEquals(
                Arrays.asList(
                        "persist.yrrp.bootanim.style=lineage",
                        "persist.yrrp.bootanim.style=matrix",
                        "persist.yrrp.bootanim.style=xp"),
                mBackend.writes);
    }

    @Test
    public void setFullIntro_writesTrueOrFalse() {
        assertTrue(mStore.setFullIntro(false));
        assertTrue(mStore.setFullIntro(true));
        assertEquals(
                Arrays.asList(
                        "persist.yrrp.bootanim.full_intro=false",
                        "persist.yrrp.bootanim.full_intro=true"),
                mBackend.writes);
    }

    @Test
    public void failedWrite_returnsFalseAndKeepsValue() {
        mBackend.failWrites = true;
        assertFalse(mStore.setStyle(YrrpBootAnimationStore.STYLE_LINEAGE));
        assertEquals(YrrpBootAnimationStore.STYLE_MATRIX, mStore.getStyle());
    }
}
