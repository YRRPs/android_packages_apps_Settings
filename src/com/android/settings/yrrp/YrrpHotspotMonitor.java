/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.content.Context;
import android.net.MacAddress;
import android.net.TetheredClient;
import android.net.TetheringManager;
import android.net.wifi.SoftApCapability;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiClient;
import android.net.wifi.WifiManager;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Live hotspot state for the client controls: AP state, capability, connected MACs from the Wi-Fi
 * module, and DHCP details from tethering. Callbacks run on the main thread between onStart and
 * onStop; the module replays its current state to each new registration.
 */
final class YrrpHotspotMonitor implements DefaultLifecycleObserver {
    private static final String TAG = "YrrpHotspot";

    /** Notified on the main thread after any input or a write changes. */
    interface Listener {
        void onHotspotChanged();
    }

    private final Context mContext;
    private final WifiManager mWifiManager;
    private final TetheringManager mTetheringManager;
    private final YrrpHotspotConfigWriter mWriter;
    private final YrrpHotspotEventLog mLog = YrrpHotspotEventLog.INSTANCE;
    private final List<Listener> mListeners = new ArrayList<>();
    private final SoftApCallback mSoftApCallback = new SoftApCallback();
    private final TetheringCallback mTetheringCallback = new TetheringCallback();

    private boolean mApEnabled;
    private SoftApCapability mCapability;
    private List<String> mConnected = List.of();
    private List<YrrpHotspotClients.Detail> mDetails = List.of();
    private int mCarrierMax;
    private boolean mStarted;

    YrrpHotspotMonitor(@NonNull Context context) {
        mContext = context.getApplicationContext();
        mWifiManager = mContext.getSystemService(WifiManager.class);
        mTetheringManager = mContext.getSystemService(TetheringManager.class);
        mWriter = new YrrpHotspotConfigWriter(mWifiManager, mLog, this::notifyListeners);
    }

    /** Adds {@code listener} once; a page may re-attach and bind its controllers again. */
    void addListener(@NonNull Listener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mStarted || mWifiManager == null) {
            return;
        }
        mStarted = true;
        mCarrierMax = readCarrierMax();
        final Executor main = mContext.getMainExecutor();
        mWifiManager.registerSoftApCallback(main, mSoftApCallback);
        if (mTetheringManager != null) {
            mTetheringManager.registerTetheringEventCallback(main, mTetheringCallback);
        }
        mLog.record("monitor started carrierMax=" + mCarrierMax);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (!mStarted) {
            return;
        }
        mStarted = false;
        mWifiManager.unregisterSoftApCallback(mSoftApCallback);
        if (mTetheringManager != null) {
            mTetheringManager.unregisterTetheringEventCallback(mTetheringCallback);
        }
        mLog.record("monitor stopped");
    }

    boolean apEnabled() {
        return mApEnabled;
    }

    YrrpHotspotConfigWriter writer() {
        return mWriter;
    }

    List<YrrpHotspotClients.Row> rows() {
        return YrrpHotspotClients.merge(mConnected, mDetails);
    }

    List<String> blocked() {
        final SoftApConfiguration config = config();
        final List<String> macs = new ArrayList<>();
        if (config != null) {
            for (MacAddress mac : config.getBlockedClientList()) {
                macs.add(mac.toString());
            }
        }
        return YrrpHotspotBlockList.normalize(macs);
    }

    YrrpHotspotDump.Snapshot snapshot() {
        final YrrpHotspotDump.Snapshot s = new YrrpHotspotDump.Snapshot();
        s.apEnabled = mApEnabled;
        s.capabilityKnown = mCapability != null;
        s.forceDisconnectSupported = forceDisconnectSupported();
        final SoftApConfiguration config = config();
        s.limit = new YrrpHotspotLimit(
                mCapability == null ? 0 : mCapability.getMaxSupportedClients(),
                config == null ? 0 : config.getMaxNumberOfClients(),
                mCarrierMax, s.forceDisconnectSupported);
        final List<YrrpHotspotClients.Row> rows = rows();
        s.connected = rows.size();
        for (YrrpHotspotClients.Row row : rows) {
            if (row.hostname != null || row.ipv4 != null) {
                s.withDetails++;
            }
        }
        s.blocked = config == null ? 0 : config.getBlockedClientList().size();
        s.events = mLog.lines();
        return s;
    }

    private boolean forceDisconnectSupported() {
        return mCapability != null && mCapability.areFeaturesSupported(
                SoftApCapability.SOFTAP_FEATURE_CLIENT_FORCE_DISCONNECT);
    }

    private SoftApConfiguration config() {
        try {
            return mWifiManager == null ? null : mWifiManager.getSoftApConfiguration();
        } catch (RuntimeException e) {
            Log.w(TAG, "read config failed error=" + e.getClass().getSimpleName());
            return null;
        }
    }

    /** The carrier's hotspot client cap for the default data SIM, or 0 when unset or unknown. */
    private int readCarrierMax() {
        final CarrierConfigManager manager = mContext.getSystemService(CarrierConfigManager.class);
        if (manager == null) {
            return 0;
        }
        try {
            final PersistableBundle bundle = manager.getConfigForSubId(
                    SubscriptionManager.getDefaultDataSubscriptionId(),
                    CarrierConfigManager.Wifi.KEY_HOTSPOT_MAX_CLIENT_COUNT);
            return bundle == null
                    ? 0 : bundle.getInt(CarrierConfigManager.Wifi.KEY_HOTSPOT_MAX_CLIENT_COUNT, 0);
        } catch (RuntimeException e) {
            Log.w(TAG, "read carrier config failed error=" + e.getClass().getSimpleName());
            return 0;
        }
    }

    private void notifyListeners() {
        for (Listener listener : new ArrayList<>(mListeners)) {
            listener.onHotspotChanged();
        }
    }

    private final class SoftApCallback implements WifiManager.SoftApCallback {
        @Override
        public void onStateChanged(int state, int failureReason) {
            final boolean enabled = state == WifiManager.WIFI_AP_STATE_ENABLED;
            if (enabled != mApEnabled) {
                mApEnabled = enabled;
                mLog.record("apEnabled=" + enabled);
            }
            if (!enabled) {
                mConnected = List.of();
            }
            notifyListeners();
        }

        @Override
        public void onConnectedClientsChanged(@NonNull List<WifiClient> clients) {
            final List<String> macs = new ArrayList<>(clients.size());
            for (WifiClient client : clients) {
                macs.add(client.getMacAddress().toString());
            }
            if (macs.size() != mConnected.size()) {
                mLog.record("connected=" + macs.size());
            }
            mConnected = macs;
            notifyListeners();
        }

        @Override
        public void onCapabilityChanged(@NonNull SoftApCapability capability) {
            mCapability = capability;
            // The module lowers the cap when carrier config changes, so re-read the carrier value.
            mCarrierMax = readCarrierMax();
            mLog.record("capability maxSupported=" + capability.getMaxSupportedClients()
                    + " forceDisconnect=" + forceDisconnectSupported());
            notifyListeners();
        }
    }

    private final class TetheringCallback implements TetheringManager.TetheringEventCallback {
        @Override
        public void onClientsChanged(@NonNull Collection<TetheredClient> clients) {
            final List<YrrpHotspotClients.Detail> details = new ArrayList<>();
            for (TetheredClient client : clients) {
                if (client.getTetheringType() != TetheringManager.TETHERING_WIFI) {
                    continue;
                }
                final String mac = client.getMacAddress().toString();
                for (TetheredClient.AddressInfo info : client.getAddresses()) {
                    details.add(new YrrpHotspotClients.Detail(
                            mac, info.getHostname(), info.getAddress().toString()));
                }
            }
            mDetails = details;
            notifyListeners();
        }
    }
}
