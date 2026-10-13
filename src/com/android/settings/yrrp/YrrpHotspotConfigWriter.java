/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.net.MacAddress;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Writes the blocked list and the client limit straight to {@link WifiManager}, like
 * {@code WifiTetherAutoOffPreferenceController}. It never uses
 * {@code WifiHotspotRepository.setSoftApConfiguration}, which restarts tethering and drops every
 * client. The Wi-Fi module applies both fields to a running hotspot and deauthenticates blocked or
 * surplus clients itself. Each write re-reads the current config first, so it keeps edits made
 * elsewhere on the page.
 */
final class YrrpHotspotConfigWriter {
    private static final String TAG = "YrrpHotspot";

    private final WifiManager mWifiManager;
    private final YrrpHotspotEventLog mLog;
    private final Runnable mOnWritten;

    YrrpHotspotConfigWriter(WifiManager wifiManager, YrrpHotspotEventLog log, Runnable onWritten) {
        mWifiManager = wifiManager;
        mLog = log;
        mOnWritten = onWritten;
    }

    boolean block(String mac) {
        return writeBlocked("block", blocked -> YrrpHotspotBlockList.add(blocked, mac));
    }

    boolean unblock(String mac) {
        return writeBlocked("unblock", blocked -> YrrpHotspotBlockList.remove(blocked, mac));
    }

    /** Stores {@code value}; 0 means no user limit. */
    boolean setMaxClients(int value) {
        return write("maxClients value=" + value,
                builder -> builder.setMaxNumberOfClients(Math.max(0, value)));
    }

    private boolean writeBlocked(String action, UnaryOperator<List<String>> edit) {
        final SoftApConfiguration current = read();
        if (current == null) {
            return false;
        }
        final List<String> before = new ArrayList<>();
        for (MacAddress mac : current.getBlockedClientList()) {
            before.add(mac.toString());
        }
        final List<String> after = edit.apply(before);
        final List<MacAddress> macs = new ArrayList<>(after.size());
        for (String mac : after) {
            macs.add(MacAddress.fromString(mac));
        }
        return write(action + " blocked=" + after.size(),
                builder -> builder.setBlockedClientList(macs));
    }

    private boolean write(String what, UnaryOperator<SoftApConfiguration.Builder> edit) {
        boolean ok = false;
        try {
            final SoftApConfiguration current = read();
            if (current != null) {
                ok = mWifiManager.setSoftApConfiguration(
                        edit.apply(new SoftApConfiguration.Builder(current)).build());
            }
            mLog.record("write " + what + " ok=" + ok);
        } catch (RuntimeException e) {
            Log.w(TAG, "write failed error=" + e.getClass().getSimpleName());
            mLog.record("write " + what + " error=" + e.getClass().getSimpleName());
        }
        mOnWritten.run();
        return ok;
    }

    private SoftApConfiguration read() {
        try {
            return mWifiManager == null ? null : mWifiManager.getSoftApConfiguration();
        } catch (RuntimeException e) {
            mLog.record("read config error=" + e.getClass().getSimpleName());
            return null;
        }
    }
}
