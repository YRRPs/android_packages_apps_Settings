/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.TetheringManager;
import android.net.wifi.WifiManager;

import androidx.lifecycle.LifecycleOwner;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Executor;

public class YrrpHotspotMonitorLifecycleTest {
    private final Context mContext = mock(Context.class);
    private final WifiManager mWifi = mock(WifiManager.class);
    private final TetheringManager mTethering = mock(TetheringManager.class);
    private final LifecycleOwner mOwner = mock(LifecycleOwner.class);
    private final Executor mExecutor = Runnable::run;

    @Before
    public void setUp() {
        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getSystemService(WifiManager.class)).thenReturn(mWifi);
        when(mContext.getSystemService(TetheringManager.class)).thenReturn(mTethering);
        when(mContext.getMainExecutor()).thenReturn(mExecutor);
    }

    @Test
    public void wifiRegistrationFailure_keepsUnknownAndBalancesTethering() {
        doThrow(new IllegalStateException("private-value"))
                .when(mWifi).registerSoftApCallback(any(), any());
        final YrrpHotspotMonitor monitor = new YrrpHotspotMonitor(mContext);

        monitor.onStart(mOwner);
        monitor.onStop(mOwner);

        assertThat(monitor.snapshot().capabilityKnown).isFalse();
        verify(mWifi, never()).unregisterSoftApCallback(any());
        verify(mTethering).registerTetheringEventCallback(any(), any());
        verify(mTethering).unregisterTetheringEventCallback(any());
    }

    @Test
    public void tetheringRegistrationFailure_cleansUpWifiOnly() {
        doThrow(new IllegalStateException("private-value"))
                .when(mTethering).registerTetheringEventCallback(any(), any());
        final YrrpHotspotMonitor monitor = new YrrpHotspotMonitor(mContext);

        monitor.onStart(mOwner);
        monitor.onStop(mOwner);

        verify(mWifi).unregisterSoftApCallback(any());
        verify(mTethering, never()).unregisterTetheringEventCallback(any());
    }

    @Test
    public void oneUnregisterFailure_doesNotSkipOtherCleanup() {
        doThrow(new IllegalStateException("private-value"))
                .when(mWifi).unregisterSoftApCallback(any());
        final YrrpHotspotMonitor monitor = new YrrpHotspotMonitor(mContext);
        monitor.onStart(mOwner);

        monitor.onStop(mOwner);

        verify(mTethering).unregisterTetheringEventCallback(any());
    }
}
