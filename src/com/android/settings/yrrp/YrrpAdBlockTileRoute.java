/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

/**
 * Decides where a Quick Settings tile long-press lands. Settings has one activity per
 * QS_TILE_PREFERENCES request, so the Ad blocking tile is picked out by its component and every
 * other tile keeps Developer options.
 */
final class YrrpAdBlockTileRoute {
    private YrrpAdBlockTileRoute() {}

    /** True only for {@code adBlockTileClass} in this Settings package. */
    static boolean opensAdBlockPage(String ownPackage, String adBlockTileClass,
            String tilePackage, String tileClass) {
        return ownPackage.equals(tilePackage) && adBlockTileClass.equals(tileClass);
    }
}
