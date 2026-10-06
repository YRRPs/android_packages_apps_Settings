/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.yrrp;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.accessibility.ColorPreference;

/**
 * A {@link ColorPreference} row (swatch preview and its content description) that never opens the
 * preset color grid.
 *
 * <p>androidx {@code Preference.performClick()} calls {@code onClick()}, which for a {@code
 * DialogPreference} shows the dialog, before any click listener or controller sees the click. The
 * grid is therefore suppressed here, and the owning controller opens the RGB picker from {@code
 * handlePreferenceTreeClick}.
 */
public class YrrpColorPreference extends ColorPreference {

    public YrrpColorPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setPreviewEnabled(true);
    }

    @Override
    protected void onClick() {
        // Intentionally empty: see the class comment.
    }
}
