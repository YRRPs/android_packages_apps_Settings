/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/** Opens the boot animation page and names the stored style; refreshed on resume. */
public class YrrpBootAnimationEntryPreferenceController extends BasePreferenceController {
    private final YrrpBootAnimationStore mStore;

    public YrrpBootAnimationEntryPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        this(context, preferenceKey, YrrpBootAnimationStore.create());
    }

    /** Injects the store, so tests can set the stored style. */
    @VisibleForTesting
    YrrpBootAnimationEntryPreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey,
            @NonNull YrrpBootAnimationStore store) {
        super(context, preferenceKey);
        mStore = store;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    /** Names the normalized stored style; an unknown stored value shows Matrix. */
    @Override
    public CharSequence getSummary() {
        return mContext.getText(
                YrrpBootAnimationStore.STYLE_LINEAGE.equals(mStore.getStyle())
                        ? R.string.yrrp_boot_animation_lineage
                        : R.string.yrrp_boot_animation_matrix);
    }
}
