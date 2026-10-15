/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static com.google.common.truth.Truth.assertThat;

import android.app.Service;
import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.PrintWriter;
import java.io.StringWriter;

@RunWith(RobolectricTestRunner.class)
public class YrrpFeatureInventoryStatusServiceTest {
    @Test
    public void coldStartDumpAndDestroy_useProductionLifecycle() {
        final YrrpFeatureInventoryStatusService service =
                Robolectric.setupService(YrrpFeatureInventoryStatusService.class);

        assertThat(service.onStartCommand(new Intent(), 0, 1))
                .isEqualTo(Service.START_NOT_STICKY);
        final String running = dump(service);
        assertThat(running).contains(
                "feature=yrrp_feature_pulse available=true"
                        + " reasonCode=available blockedBy=none probeStatus=known");
        assertThat(running).contains("gate=hingeSensorPresent value=");
        assertThat(running).doesNotContain("probeStatus=pending");

        service.onDestroy();

        final String stopped = dumpState();
        assertThat(stopped).contains("started=false");
        assertThat(stopped).contains("callbackRegistered=false");
    }

    private static String dump(YrrpFeatureInventoryStatusService service) {
        final StringWriter output = new StringWriter();
        service.dump(null, new PrintWriter(output), new String[0]);
        return output.toString();
    }

    private static String dumpState() {
        final StringWriter output = new StringWriter();
        YrrpFeatureInventoryDump.INSTANCE.format(new PrintWriter(output));
        return output.toString();
    }
}
