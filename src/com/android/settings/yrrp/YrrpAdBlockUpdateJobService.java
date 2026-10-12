/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

import com.android.settingslib.utils.ThreadUtils;

import java.util.concurrent.TimeUnit;

/** Weekly ad-block list update on an unmetered network while the device is idle. */
public class YrrpAdBlockUpdateJobService extends JobService {
    /** "YRBP". */
    static final int JOB_ID = 0x59524250;
    static final long PERIOD_MILLIS = TimeUnit.DAYS.toMillis(7);

    /** Schedules the job while blocking and auto-update are both on; cancels it otherwise. */
    static void sync(Context context) {
        ThreadUtils.postOnBackgroundThread(() -> {
            final boolean wanted = YrrpAdBlockAndroid.isPrimaryUser(context)
                    && YrrpAdBlockAndroid.state().isEnabled()
                    && YrrpAdBlockAndroid.readState(context).autoUpdate;
            final JobScheduler scheduler = context.getSystemService(JobScheduler.class);
            if (!wanted) {
                scheduler.cancel(JOB_ID);
            } else if (scheduler.getPendingJob(JOB_ID) == null) {
                scheduler.schedule(new JobInfo.Builder(JOB_ID,
                        new ComponentName(context, YrrpAdBlockUpdateJobService.class))
                        .setPeriodic(PERIOD_MILLIS)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                        .setRequiresDeviceIdle(true)
                        .setPersisted(true)
                        .build());
            }
            YrrpAdBlockAndroid.log(context).record("job wanted=" + wanted);
        });
    }

    static boolean isScheduled(Context context) {
        return context.getSystemService(JobScheduler.class).getPendingJob(JOB_ID) != null;
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        YrrpAdBlockAndroid.record(this, "job run");
        YrrpAdBlockAndroid.runUpdate(this, true, outcome -> jobFinished(params, false));
        return true;
    }

    /** Reschedule when stopped early; the updater keeps the last good file meanwhile. */
    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
