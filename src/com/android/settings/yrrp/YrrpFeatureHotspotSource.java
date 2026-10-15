/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import androidx.annotation.NonNull;

/** Narrow hotspot callback boundary used by the feature inventory. */
interface YrrpFeatureHotspotSource {
    interface Listener {
        void onHotspotChanged();
    }

    void addListener(@NonNull Listener listener);
    void removeListener(@NonNull Listener listener);
}
