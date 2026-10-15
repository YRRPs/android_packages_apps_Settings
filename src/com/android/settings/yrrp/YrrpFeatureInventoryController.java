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

    @Nullable private YrrpFeatureEnvironment.Snapshot mGates;
    private boolean mStarted;

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
        final YrrpFeatureEnvironment.Snapshot gates =
                YrrpFeatureEnvironment.Snapshot.capture(mEnvironment);
        mGates = gates;
        for (Id id : YrrpFeatureCatalog.ids()) {
            resolve(id, gates);
        }
        publish();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        start();
    }

    void start() {
        if (mStarted) {
            return;
        }
        mStarted = true;
        mHotspot.addListener(this);
        refreshHotspot();
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        stop();
    }

    void stop() {
        if (!mStarted) {
            return;
        }
        mStarted = false;
        mHotspot.removeListener(this);
        publish();
    }

    @Override
    public void onHotspotChanged() {
        if (mStarted) {
            refreshHotspot();
        }
    }

    @Nullable
    YrrpFeatureAvailability result(Id id) {
        return mResults.get(id);
    }

    Map<Id, YrrpFeatureAvailability> results() {
        return Collections.unmodifiableMap(new EnumMap<>(mResults));
    }

    private void refreshHotspot() {
        final YrrpFeatureEnvironment.Snapshot previous = mGates;
        final YrrpFeatureEnvironment.Snapshot gates = previous == null
                ? YrrpFeatureEnvironment.Snapshot.capture(mEnvironment)
                : previous.withHotspot(mEnvironment.hotspotControls());
        mGates = gates;
        resolve(Id.HOTSPOT_CLIENT_MANAGEMENT, gates);
        publish();
    }

    private void resolve(Id id, YrrpFeatureEnvironment.Snapshot gates) {
        final YrrpFeatureAvailability next = YrrpFeatureCatalog.resolve(id, gates);
        final YrrpFeatureAvailability previous = mResults.get(id);
        if (sameDecision(previous, next)) {
            return;
        }
        mResults.put(id, next);
        final YrrpFeaturePreference row = mRows.get(id);
        if (row != null) {
            row.bind(next);
        }
    }

    private void publish() {
        if (mGates != null) {
            mDump.update(
                    mResults,
                    mGates,
                    mStarted,
                    mStarted && mHotspot.callbacksRegistered());
        }
    }

    private static boolean sameDecision(
            @Nullable YrrpFeatureAvailability left,
            @NonNull YrrpFeatureAvailability right) {
        return left != null
                && left.statusRes == right.statusRes
                && left.diagnosticFields().equals(right.diagnosticFields());
    }
}
