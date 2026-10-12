/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

/**
 * The ad-block switch and apply counter. Device-wide system properties, because init acts on
 * them (vendor/extra init/yrrp_adblock.rc): enabled copies hosts.blocked over the live hosts
 * file, disabled copies the stock file, and a new apply value re-copies while enabled.
 */
final class YrrpAdBlockState {
    static final String ENABLED_PROPERTY = "persist.yrrp.adblock.enabled";
    static final String APPLY_PROPERTY = "sys.yrrp.adblock.apply";

    private final YrrpPropertyBackend mBackend;

    YrrpAdBlockState(YrrpPropertyBackend backend) {
        mBackend = backend;
    }

    static YrrpAdBlockState create() {
        return new YrrpAdBlockState(new YrrpSystemPropertiesBackend());
    }

    /** Only "1" reads as on, because init's rc triggers act only on 1 and 0. */
    boolean isEnabled() {
        return "1".equals(mBackend.get(ENABLED_PROPERTY));
    }

    /** Returns false if the write was refused. */
    boolean setEnabled(boolean enabled) {
        return mBackend.set(ENABLED_PROPERTY, enabled ? "1" : "0");
    }

    /** Asks init to copy hosts.blocked again. Returns false if the write was refused. */
    boolean bumpApply() {
        long current;
        try {
            current = Long.parseLong(mBackend.get(APPLY_PROPERTY));
        } catch (NumberFormatException e) {
            current = 0;
        }
        return mBackend.set(APPLY_PROPERTY, Long.toString(current + 1));
    }
}
