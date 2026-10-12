/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/** Opens the ad blocking page and shows On or Off; refreshed on resume. */
public class YrrpAdBlockEntryPreferenceController extends BasePreferenceController {
    public YrrpAdBlockEntryPreferenceController(
            @NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    /** Device-wide, so only the primary user controls it. */
    @Override
    public int getAvailabilityStatus() {
        return YrrpAdBlockAndroid.isPrimaryUser(mContext) ? AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getText(YrrpAdBlockAndroid.state().isEnabled()
                ? R.string.yrrp_ad_block_on : R.string.yrrp_ad_block_off);
    }
}
