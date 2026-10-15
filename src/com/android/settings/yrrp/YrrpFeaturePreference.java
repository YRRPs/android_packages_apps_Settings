/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

/** Enabled, non-selectable row showing one feature description and availability status. */
public final class YrrpFeaturePreference extends Preference {
    private static final float AVAILABLE_ALPHA = 1f;
    private static final float UNAVAILABLE_ALPHA = 0.6f;

    @Nullable private YrrpFeatureAvailability mAvailability;

    public YrrpFeaturePreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.yrrp_feature_preference);
        setSelectable(false);
    }

    void bind(@NonNull YrrpFeatureAvailability availability) {
        mAvailability = availability;
        notifyChanged();
    }

    @Nullable
    YrrpFeatureAvailability availability() {
        return mAvailability;
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final YrrpFeatureAvailability availability = mAvailability;
        if (availability == null) {
            return;
        }
        final TextView status = (TextView) holder.findViewById(R.id.yrrp_feature_status);
        final ImageView icon = (ImageView) holder.findViewById(android.R.id.icon);
        final float alpha = availability.available ? AVAILABLE_ALPHA : UNAVAILABLE_ALPHA;
        status.setText(availability.statusRes);
        status.setAlpha(alpha);
        if (icon != null) {
            icon.setAlpha(alpha);
        }
        final View item = holder.itemView;
        item.setEnabled(true);
    }
}
