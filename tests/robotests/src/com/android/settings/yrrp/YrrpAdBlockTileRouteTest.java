/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Plain JUnit; no Robolectric. */
public class YrrpAdBlockTileRouteTest {
    private static final String SETTINGS = "com.android.settings";
    private static final String TILE = "com.android.settings.yrrp.YrrpAdBlockTileService";

    @Test
    public void adBlockTileOpensTheAdBlockPage() {
        assertTrue(YrrpAdBlockTileRoute.opensAdBlockPage(SETTINGS, TILE, SETTINGS, TILE));
    }

    @Test
    public void developerTilesKeepDeveloperOptions() {
        assertFalse(YrrpAdBlockTileRoute.opensAdBlockPage(SETTINGS, TILE, SETTINGS,
                "com.android.settings.development.qstile.DevelopmentTiles$WirelessDebugging"));
        assertFalse(YrrpAdBlockTileRoute.opensAdBlockPage(SETTINGS, TILE, SETTINGS,
                "com.android.settings.development.qstile.DevelopmentTiles$ShowLayout"));
    }

    @Test
    public void sameClassFromAnotherPackageOrNoTileKeepsDeveloperOptions() {
        assertFalse(YrrpAdBlockTileRoute.opensAdBlockPage(SETTINGS, TILE, "com.example", TILE));
        assertFalse(YrrpAdBlockTileRoute.opensAdBlockPage(SETTINGS, TILE, null, null));
    }
}
