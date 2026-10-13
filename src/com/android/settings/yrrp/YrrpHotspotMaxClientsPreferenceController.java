/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.SliderPreferenceController;
import com.android.settingslib.widget.SliderPreference;

/**
 * Maximum connected devices: a slider from 1 to the capability cap, whose top position is
 * "No limit" and stores 0. Writes on release only, because each write can deauthenticate clients.
 * Hidden until the capability arrives, and when the module cannot force-disconnect. The limit is
 * cached in {@link #updateState}, so the label formatter never reads Wi-Fi config while dragging.
 */
public class YrrpHotspotMaxClientsPreferenceController extends SliderPreferenceController
        implements YrrpHotspotMonitor.Listener {
    private YrrpHotspotMonitor mMonitor;
    private @Nullable SliderPreference mPreference;
    private YrrpHotspotLimit mLimit = new YrrpHotspotLimit(0, 0, 0, false);
    private boolean mAllowed;

    public YrrpHotspotMaxClientsPreferenceController(
            @NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    /** Binds the page's monitor; call from the page's {@code onAttach()}. */
    void init(@NonNull YrrpHotspotMonitor monitor) {
        mMonitor = monitor;
        monitor.addListener(this);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference preference = screen.findPreference(getPreferenceKey());
        if (preference instanceof SliderPreference) {
            mPreference = (SliderPreference) preference;
            mPreference.setUpdatesContinuously(false);
            mPreference.setShowSliderValue(true);
            mPreference.setLabelFormater(value -> formatValue(Math.round(value)));
            mPreference.setTextStart(R.string.yrrp_hotspot_max_clients_one);
            mPreference.setTextEnd(R.string.yrrp_hotspot_max_clients_no_limit);
        }
        onHotspotChanged();
    }

    @Override
    public void onHotspotChanged() {
        if (mPreference != null) {
            updateState(mPreference);
        }
    }

    @Override
    public void updateState(@Nullable Preference preference) {
        if (!(preference instanceof SliderPreference) || mMonitor == null) {
            return;
        }
        final SliderPreference slider = (SliderPreference) preference;
        final YrrpHotspotDump.Snapshot snapshot = mMonitor.snapshot();
        mLimit = snapshot.limit;
        mAllowed = YrrpHotspotDump.limitAllowed(snapshot);
        slider.setVisible(mAllowed);
        if (!mAllowed) {
            return;
        }
        final YrrpHotspotLimit limit = mLimit;
        // Max before min: the defaults are 0..100 and the cap may exceed neither bound.
        slider.setMax(limit.maxSupported());
        slider.setMin(1);
        slider.setValue(limit.sliderPosition());
        slider.setSummary(formatSummary(limit));
        slider.setSliderStateDescription(formatValue(limit.sliderPosition()));
    }

    @Override
    public int getSliderPosition() {
        return mAllowed ? mLimit.sliderPosition() : getMax();
    }

    @Override
    public boolean setSliderPosition(int position) {
        if (mMonitor == null || !mAllowed) {
            return false;
        }
        final boolean written = mMonitor.writer().setMaxClients(mLimit.storedValueFor(position));
        if (!written) {
            Toast.makeText(mContext, R.string.yrrp_hotspot_write_failed, Toast.LENGTH_SHORT)
                    .show();
        }
        return written;
    }

    @Override
    public int getMax() {
        return Math.max(1, mLimit.maxSupported());
    }

    @Override
    public int getMin() {
        return 1;
    }

    /** Not exposed as a slice: changing it can disconnect clients. */
    @Override
    public boolean isSliceable() {
        return false;
    }

    private String formatValue(int position) {
        return position >= getMax()
                ? mContext.getString(R.string.yrrp_hotspot_max_clients_no_limit)
                : String.valueOf(position);
    }

    private String formatSummary(YrrpHotspotLimit limit) {
        final int effective = limit.effectiveMax();
        final String count = mContext.getResources().getQuantityString(
                R.plurals.yrrp_hotspot_devices, effective, effective);
        return mContext.getString(R.string.yrrp_hotspot_max_clients_summary, count,
                mContext.getString(sourceText(limit.source())));
    }

    private static int sourceText(YrrpHotspotLimit.Source source) {
        switch (source) {
            case USER:
                return R.string.yrrp_hotspot_limit_source_user;
            case CARRIER:
                return R.string.yrrp_hotspot_limit_source_carrier;
            case DEVICE:
            default:
                return R.string.yrrp_hotspot_limit_source_device;
        }
    }
}
