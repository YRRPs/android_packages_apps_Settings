/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * The Ad blocking Quick Settings tile. It reads and writes the same property as the Settings
 * switch, through the same path, so the two cannot disagree.
 */
public class YrrpAdBlockTileService extends TileService {
    @Override
    public void onStartListening() {
        super.onStartListening();
        refresh();
    }

    @Override
    public void onClick() {
        final Tile tile = getQsTile();
        if (tile == null || tile.getState() == Tile.STATE_UNAVAILABLE) {
            return;
        }
        YrrpAdBlockAndroid.setEnabled(this, tile.getState() == Tile.STATE_INACTIVE);
        refresh();
    }

    private void refresh() {
        final Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        if (!YrrpAdBlockAndroid.isPrimaryUser(this)) {
            tile.setState(Tile.STATE_UNAVAILABLE);
        } else {
            tile.setState(YrrpAdBlockAndroid.state().isEnabled()
                    ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        }
        tile.updateTile();
    }
}
