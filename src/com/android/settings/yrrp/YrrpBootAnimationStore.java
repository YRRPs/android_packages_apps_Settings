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

    /** System property access, replaceable in tests. {@code get} returns "" when unset. */
    interface Backend {
        String get(String key);

        boolean set(String key, String value);
    }

    private final Backend mBackend;

    YrrpBootAnimationStore(Backend backend) {
        mBackend = backend;
    }

    /** The production store, backed by {@code android.os.SystemProperties}. */
    public static YrrpBootAnimationStore create() {
        return new YrrpBootAnimationStore(new YrrpSystemPropertiesBackend());
    }

    /** Exact "lineage" reads as LineageOS; anything else, unset included, as Matrix. */
    public String getStyle() {
        return STYLE_LINEAGE.equals(mBackend.get(STYLE_PROPERTY)) ? STYLE_LINEAGE : STYLE_MATRIX;
    }

    /** Writes a known style; rejects anything else. Returns false if nothing was written. */
    public boolean setStyle(String style) {
        if (!STYLE_MATRIX.equals(style) && !STYLE_LINEAGE.equals(style)) {
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
