/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleObserver;

import java.io.PrintWriter;

/**
 * The YRRPs client controls on the stock Wi-Fi hotspot page, behind one public entry point so
 * {@code WifiTetherSettings} needs only a few lines: create in {@code onAttach}, observe the
 * page lifecycle, bind the controllers, and print the dump.
 */
public final class YrrpHotspotControls {
    private final YrrpHotspotMonitor mMonitor;

    public YrrpHotspotControls(@NonNull Context context) {
        mMonitor = new YrrpHotspotMonitor(context);
    }

    /** Registers the Wi-Fi and tethering callbacks between the page's onStart and onStop. */
    @NonNull
    public LifecycleObserver lifecycleObserver() {
        return mMonitor;
    }

    /** Binds whichever controllers the page created; a missing one is skipped. */
    public void bind(@NonNull Fragment host,
            @Nullable YrrpHotspotClientsPreferenceController clients,
            @Nullable YrrpHotspotMaxClientsPreferenceController maxClients,
            @Nullable YrrpHotspotBlockedEntryPreferenceController blockedEntry) {
        if (clients != null) {
            clients.init(host, mMonitor);
        }
        if (maxClients != null) {
            maxClients.init(mMonitor);
        }
        if (blockedEntry != null) {
            blockedEntry.init(mMonitor);
        }
    }

    /** Prints gates, counts, and recent transitions; never a MAC, hostname, or address. */
    public void dump(@NonNull String prefix, @NonNull PrintWriter writer) {
        writer.print(prefix);
        YrrpHotspotDump.format(writer, mMonitor.snapshot());
    }
}
