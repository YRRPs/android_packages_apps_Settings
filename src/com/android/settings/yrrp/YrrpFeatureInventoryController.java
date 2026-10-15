/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.yrrp.YrrpFeatureCatalog.Id;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Resolves and renders all feature rows while the inventory page is active. */
final class YrrpFeatureInventoryController extends BasePreferenceController
        implements DefaultLifecycleObserver, YrrpFeatureHotspotSource.Listener {
    private static final String SCREEN_KEY = "yrrp_feature_inventory_settings_screen";

    private final YrrpFeatureEnvironment mEnvironment;
    private final YrrpFeatureHotspotSource mHotspot;
    private final YrrpFeatureInventoryDump mDump;
    private final Map<Id, YrrpFeaturePreference> mRows = new EnumMap<>(Id.class);
    private final Map<Id, YrrpFeatureAvailability> mResults = new EnumMap<>(Id.class);

    private boolean mStarted;
    private long mResolutionEpoch;

    YrrpFeatureInventoryController(
            @NonNull Context context,
            @NonNull YrrpFeatureEnvironment environment,
            @NonNull YrrpFeatureHotspotSource hotspot) {
        this(context, environment, hotspot, YrrpFeatureInventoryDump.INSTANCE);
    }

    YrrpFeatureInventoryController(
            @NonNull Context context,
            @NonNull YrrpFeatureEnvironment environment,
            @NonNull YrrpFeatureHotspotSource hotspot,
            @NonNull YrrpFeatureInventoryDump dump) {
        super(context, SCREEN_KEY);
        mEnvironment = environment;
        mHotspot = hotspot;
        mDump = dump;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mRows.clear();
        for (Id id : YrrpFeatureCatalog.ids()) {
            final Preference preference = screen.findPreference(id.key);
            if (preference instanceof YrrpFeaturePreference) {
                mRows.put(id, (YrrpFeaturePreference) preference);
            }
        }
        resolveAll();
    }

    void resolveAll() {
        boolean changed = false;
        for (Id id : YrrpFeatureCatalog.ids()) {
            changed |= resolve(id);
        }
        publish(changed);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mStarted) {
            return;
        }
        mStarted = true;
        mHotspot.addListener(this);
        publish(resolve(Id.HOTSPOT_CLIENT_MANAGEMENT));
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (!mStarted) {
            return;
        }
        mStarted = false;
        mHotspot.removeListener(this);
        publish(false);
    }

    @Override
    public void onHotspotChanged() {
        if (mStarted) {
            publish(resolve(Id.HOTSPOT_CLIENT_MANAGEMENT));
        }
    }

    @Nullable
    YrrpFeatureAvailability result(Id id) {
        return mResults.get(id);
    }

    Map<Id, YrrpFeatureAvailability> results() {
        return Collections.unmodifiableMap(new EnumMap<>(mResults));
    }

    private boolean resolve(Id id) {
        final YrrpFeatureAvailability next = YrrpFeatureCatalog.resolve(id, mEnvironment);
        final YrrpFeatureAvailability previous = mResults.get(id);
        if (sameDecision(previous, next)) {
            return false;
        }
        mResults.put(id, next);
        mDump.recordTransition(id, next);
        final YrrpFeaturePreference row = mRows.get(id);
        if (row != null) {
            row.bind(next);
        }
        return true;
    }

    private void publish(boolean changed) {
        if (changed) {
            mResolutionEpoch++;
        }
        mDump.update(
                mResults,
                mStarted,
                mResolutionEpoch,
                mEnvironment.hotspotControls(),
                mStarted);
    }

    private static boolean sameDecision(
            @Nullable YrrpFeatureAvailability left,
            @NonNull YrrpFeatureAvailability right) {
        return left != null
                && left.available == right.available
                && left.statusRes == right.statusRes
                && left.reasonCode.equals(right.reasonCode)
                && left.blockedBy.equals(right.blockedBy)
                && left.probeStatus == right.probeStatus;
    }
}
