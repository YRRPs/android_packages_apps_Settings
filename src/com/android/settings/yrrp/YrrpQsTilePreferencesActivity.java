/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.service.quicksettings.TileService;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.core.SubSettingLauncher;

/**
 * Handles a long-press on any Settings Quick Settings tile. SystemUI resolves one
 * QS_TILE_PREFERENCES activity per package before it knows the tile, so this activity outranks
 * Developer options and routes on the tile's component: the Ad blocking tile opens the Ad
 * blocking page, and every other tile goes on to Developer options as before.
 *
 * <p>The activity is exported, so the incoming intent is untrusted. It is never forwarded:
 * Developer options gets a new explicit intent that carries only the tile component.
 */
public class YrrpQsTilePreferencesActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ComponentName tile = getIntent().getParcelableExtra(
                Intent.EXTRA_COMPONENT_NAME, ComponentName.class);
        final String tilePackage = tile == null ? null : tile.getPackageName();
        final String tileClass = tile == null ? null : tile.getClassName();
        if (YrrpAdBlockTileRoute.opensAdBlockPage(getPackageName(),
                YrrpAdBlockTileService.class.getName(), tilePackage, tileClass)) {
            new SubSettingLauncher(this)
                    .setDestination(YrrpAdBlockSettings.class.getName())
                    .setTitleRes(R.string.yrrp_ad_block_title)
                    .setSourceMetricsCategory(SettingsEnums.PAGE_UNKNOWN)
                    .launch();
        } else {
            final Intent developerOptions =
                    new Intent(TileService.ACTION_QS_TILE_PREFERENCES)
                            .setClass(this, Settings.DevelopmentSettingsActivity.class);
            if (tile != null) {
                developerOptions.putExtra(Intent.EXTRA_COMPONENT_NAME, tile);
            }
            startActivity(developerOptions);
        }
        finish();
    }
}
