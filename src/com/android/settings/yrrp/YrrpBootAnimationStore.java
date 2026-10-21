/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

/**
 * The boot animation choice, stored in device-wide persistent system properties because
 * bootanimation starts before any settings provider. A documented exception to the per-user
 * {@code Settings.Secure} rule. Parsing matches YrrpMatrixConfig in frameworks/base
 * cmds/bootanimation exactly; reads never rewrite a stored value.
 */
public final class YrrpBootAnimationStore {
    static final String STYLE_PROPERTY = "persist.yrrp.bootanim.style";
    static final String FULL_INTRO_PROPERTY = "persist.yrrp.bootanim.full_intro";
    static final String STYLE_MATRIX = "matrix";
    static final String STYLE_LINEAGE = "lineage";
    static final String STYLE_XP = "xp";

    /** System property access for this store; see {@link YrrpPropertyBackend}. */
    interface Backend extends YrrpPropertyBackend {}

    private final Backend mBackend;

    // No @VisibleForTesting: androidx imports would break the plain-JUnit runner.
    YrrpBootAnimationStore(Backend backend) {
        mBackend = backend;
    }

    /** The production store, backed by {@code android.os.SystemProperties}. */
    public static YrrpBootAnimationStore create() {
        return new YrrpBootAnimationStore(new YrrpSystemPropertiesBackend());
    }

    /** Exact "lineage" or "xp" read as that style; anything else, unset included, as Matrix. */
    public String getStyle() {
        final String value = mBackend.get(STYLE_PROPERTY);
        return STYLE_LINEAGE.equals(value) || STYLE_XP.equals(value) ? value : STYLE_MATRIX;
    }

    /** Writes a known style; rejects anything else. Returns false if nothing was written. */
    public boolean setStyle(String style) {
        if (!STYLE_MATRIX.equals(style) && !STYLE_LINEAGE.equals(style)
                && !STYLE_XP.equals(style)) {
            return false;
        }
        return mBackend.set(STYLE_PROPERTY, style);
    }

    /** "false" or "0" read as off; anything else, unset included, as on. */
    public boolean isFullIntro() {
        final String value = mBackend.get(FULL_INTRO_PROPERTY);
        return !("false".equals(value) || "0".equals(value));
    }

    /** Writes "true" or "false". Returns false if the write failed. */
    public boolean setFullIntro(boolean fullIntro) {
        return mBackend.set(FULL_INTRO_PROPERTY, fullIntro ? "true" : "false");
    }
}
