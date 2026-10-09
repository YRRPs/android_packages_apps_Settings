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

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

/** Shows the Pulse RGB picker for a host page and writes only a confirmed, masked color. */
final class YrrpPulseColorPicker {

    private static final String TAG = "YrrpPulseColorPicker";

    private final YrrpSettingsStore mStore;
    private final Runnable mOnResult;
    private @Nullable Fragment mHost;

    /** {@code onResult} runs after every confirmed result, written or not, to re-read state. */
    YrrpPulseColorPicker(@NonNull YrrpSettingsStore store, @NonNull Runnable onResult) {
        mStore = store;
        mOnResult = onResult;
    }

    /**
     * Binds the hosting page, which shows the picker and delivers its result. Call from the host's
     * {@code onAttach()} so the listener is registered again after recreation and a pending
     * confirmed result still arrives.
     */
    void init(@NonNull Fragment host) {
        mHost = host;
        host.getChildFragmentManager()
                .setFragmentResultListener(
                        YrrpColorPickerDialogFragment.RESULT_KEY,
                        host,
                        (requestKey, result) -> onColorConfirmed(result));
    }

    /** Opens the picker at the stored color, unless the host is gone or a picker is showing. */
    void show() {
        if (mHost == null) {
            Log.w(TAG, "init(Fragment) not called");
            return;
        }
        if (!mHost.isAdded()) {
            return;
        }
        final FragmentManager manager = mHost.getChildFragmentManager();
        if (manager.isStateSaved()
                || manager.findFragmentByTag(YrrpColorPickerDialogFragment.TAG) != null) {
            return;
        }
        YrrpColorPickerDialogFragment.newInstance(mStore.getPulseColor())
                .showNow(manager, YrrpColorPickerDialogFragment.TAG);
    }

    /** The result bundle is untrusted input: the RGB key is required and its bits are masked. */
    private void onColorConfirmed(@NonNull Bundle result) {
        if (result.containsKey(YrrpColorPickerDialogFragment.RESULT_RGB)) {
            mStore.setPulseColor(
                    YrrpSettingsStore.normalizeColor(
                            result.getInt(YrrpColorPickerDialogFragment.RESULT_RGB)));
        }
        // On failure the store logs the key; either way the row re-reads the persisted value.
        mOnResult.run();
    }
}
