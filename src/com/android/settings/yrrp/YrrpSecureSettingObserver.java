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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

/**
 * Content and lifecycle observer that refreshes its owning preference controller when any of the
 * owning controller's YRRPs secure settings change.
 *
 * <p>Composed into toggle and list controllers rather than subclassed. The owner forwards {@link
 * #displayPreference}, {@link #onStart} and {@link #onStop}.
 */
public class YrrpSecureSettingObserver extends ContentObserver implements DefaultLifecycleObserver {

    private final ContentResolver mContentResolver;
    private final AbstractPreferenceController mController;
    private final List<Uri> mUris = new ArrayList<>();
    private @Nullable Preference mPreference;

    public YrrpSecureSettingObserver(
            @NonNull Context context,
            @NonNull AbstractPreferenceController controller,
            @NonNull YrrpSettingsStore store,
            @NonNull String... keys) {
        super(new Handler(Looper.getMainLooper()));
        if (keys.length == 0) {
            throw new IllegalArgumentException("no keys");
        }
        mContentResolver = context.getContentResolver();
        mController = controller;
        for (String key : keys) {
            mUris.add(store.getUriFor(key));
        }
    }

    /** Binds the owner's preference so observed changes can refresh it. */
    public void displayPreference(@NonNull PreferenceScreen screen) {
        mPreference = screen.findPreference(mController.getPreferenceKey());
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        for (Uri uri : mUris) {
            mContentResolver.registerContentObserver(
                    uri, /* notifyForDescendants= */ false, this, UserHandle.USER_CURRENT);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        mContentResolver.unregisterContentObserver(this);
    }

    @Override
    public void onChange(boolean selfChange, @Nullable Uri uri) {
        if (mPreference != null && (uri == null || mUris.contains(uri))) {
            mController.updateState(mPreference);
        }
    }
}
