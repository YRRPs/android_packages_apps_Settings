/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.android.settings.yrrp.YrrpFeatureAvailability.HotspotControls;
import com.android.settings.yrrp.YrrpFeatureCatalog.Id;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** DUMP-protected, short-lived dumpsys surface for bounded inventory state. */
public class YrrpFeatureInventoryStatusService extends Service {
    private static final long LIFETIME_MILLIS = TimeUnit.MINUTES.toMillis(5);

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Nullable private YrrpHotspotMonitor mMonitor;
    @Nullable private YrrpFeatureInventoryController mController;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ensureRuntime();
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(this::stopSelf, LIFETIME_MILLIS);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        final YrrpHotspotMonitor monitor = mMonitor;
        if (monitor != null) {
            monitor.stop();
            mMonitor = null;
        }
        final YrrpFeatureInventoryController controller = mController;
        if (controller != null) {
            controller.stop();
            mController = null;
        }
        super.onDestroy();
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        ensureRuntime();
        YrrpFeatureInventoryDump.INSTANCE.format(writer);
    }

    static void resolveForDump(
            YrrpFeatureEnvironment environment, YrrpFeatureInventoryDump dump) {
        final YrrpFeatureEnvironment.Snapshot gates =
                YrrpFeatureEnvironment.Snapshot.capture(environment);
        final Map<Id, YrrpFeatureAvailability> results = new EnumMap<>(Id.class);
        for (Id id : YrrpFeatureCatalog.ids()) {
            results.put(id, YrrpFeatureCatalog.resolve(id, gates));
        }
        dump.update(results, gates, false, false);
    }

    private void ensureRuntime() {
        if (mController != null) {
            return;
        }
        final YrrpHotspotMonitor monitor = new YrrpHotspotMonitor(this);
        mMonitor = monitor;
        final YrrpFeatureEnvironment environment =
                YrrpFeatureEnvironment.AndroidEnvironment.create(
                        this, () -> hotspotControls(monitor.snapshot()));
        final YrrpFeatureInventoryController controller =
                new YrrpFeatureInventoryController(
                        this, environment, monitor, YrrpFeatureInventoryDump.INSTANCE);
        mController = controller;
        controller.resolveAll();
        controller.start();
        monitor.start();
        controller.onHotspotChanged();
    }

    private static HotspotControls hotspotControls(YrrpHotspotDump.Snapshot snapshot) {
        if (!snapshot.capabilityKnown) {
            return HotspotControls.UNDETERMINED;
        }
        return snapshot.forceDisconnectSupported
                ? HotspotControls.SUPPORTED : HotspotControls.UNSUPPORTED;
    }
}
