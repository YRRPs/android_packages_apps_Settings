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

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

/**
 * Prints the ad-block decision inputs for {@code dumpsys}. Guarded by DUMP, like
 * SettingsDumpService. dumpsys reaches only a running service, so start it first; it stops
 * itself after five minutes:
 *
 * <pre>
 * adb shell am start-service -n com.android.settings/.yrrp.YrrpAdBlockStatusService
 * adb shell dumpsys activity service com.android.settings/.yrrp.YrrpAdBlockStatusService
 * </pre>
 */
public class YrrpAdBlockStatusService extends Service {
    private static final long LIFETIME_MILLIS = TimeUnit.MINUTES.toMillis(5);

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
        super.onDestroy();
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        YrrpAdBlockDump.format(writer, YrrpAdBlockAndroid.snapshot(this));
    }
}
