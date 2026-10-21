/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

/**
 * One boot animation style radio row. Properties have no change observer, so rows refresh on
 * page resume (DashboardFragment calls updateState) and after their own writes.
 */
public class YrrpBootAnimationStylePreferenceController extends BasePreferenceController
        implements SelectorWithWidgetPreference.OnClickListener {

    // Must match the radio keys in R.xml.yrrp_boot_animation_settings.
    static final String KEY_MATRIX = "yrrp_boot_animation_matrix";
    static final String KEY_LINEAGE = "yrrp_boot_animation_lineage";
    static final String KEY_XP = "yrrp_boot_animation_xp";
    private static final String[] KEYS = {KEY_MATRIX, KEY_LINEAGE, KEY_XP};

    protected final YrrpBootAnimationStore mStore;
    private final String mStyle;
    private @Nullable PreferenceScreen mScreen;

    public YrrpBootAnimationStylePreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        this(context, preferenceKey, YrrpBootAnimationStore.create());
    }

    /** Injects the store, so tests can count writes and make them fail. */
    @VisibleForTesting
    YrrpBootAnimationStylePreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey,
            @NonNull YrrpBootAnimationStore store) {
        super(context, preferenceKey);
        mStyle = styleFor(preferenceKey);
        mStore = store;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;
        final SelectorWithWidgetPreference preference = screen.findPreference(getPreferenceKey());
        if (preference != null) {
            preference.setOnClickListener(this);
        }
    }

    /** Checks this row when it matches the normalized stored style; the value is not rewritten. */
    @Override
    public void updateState(@Nullable Preference preference) {
        if (preference instanceof SelectorWithWidgetPreference) {
            ((SelectorWithWidgetPreference) preference)
                    .setChecked(mStore.getStyle().equals(mStyle));
        }
    }

    /**
     * Writes this row's style, then re-reads it into every row. A failed write is logged by the
     * backend and leaves the selection unchanged.
     */
    @Override
    public void onRadioButtonClicked(@NonNull SelectorWithWidgetPreference preference) {
        mStore.setStyle(mStyle);
        refreshRows();
    }

    private void refreshRows() {
        if (mScreen == null) {
            return;
        }
        final String stored = mStore.getStyle();
        for (String key : KEYS) {
            final Preference row = mScreen.findPreference(key);
            if (row instanceof SelectorWithWidgetPreference) {
                ((SelectorWithWidgetPreference) row).setChecked(stored.equals(styleFor(key)));
            }
        }
    }

    private static String styleFor(@NonNull String key) {
        switch (key) {
            case KEY_MATRIX:
                return YrrpBootAnimationStore.STYLE_MATRIX;
            case KEY_LINEAGE:
                return YrrpBootAnimationStore.STYLE_LINEAGE;
            case KEY_XP:
                return YrrpBootAnimationStore.STYLE_XP;
            default:
                throw new IllegalArgumentException("unknown boot animation key");
        }
    }
}
