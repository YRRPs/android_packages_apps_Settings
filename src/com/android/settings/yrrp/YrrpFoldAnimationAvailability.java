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

import android.content.res.Resources;

import androidx.annotation.VisibleForTesting;

import java.util.function.BooleanSupplier;

/** Whether the device runs SystemUI's unfold framework, which the fold animation needs. */
final class YrrpFoldAnimationAvailability {
    private YrrpFoldAnimationAvailability() {}

    /** Where {@link #SYSTEM} reads; tests replace it and must restore {@link #READ_RESOURCE}. */
    @VisibleForTesting static BooleanSupplier sSource = YrrpFoldAnimationAvailability::readResource;

    /** Follows {@link #sSource}, so controllers and the search provider share one answer. */
    static final BooleanSupplier SYSTEM = () -> sSource.getAsBoolean();

    /** The production source. */
    @VisibleForTesting
    static final BooleanSupplier READ_RESOURCE = YrrpFoldAnimationAvailability::readResource;

    /** Reads the same resource as SystemUI's ResourceUnfoldTransitionConfig. */
    private static boolean readResource() {
        Resources res = Resources.getSystem();
        int id = res.getIdentifier("config_unfoldTransitionEnabled", "bool", "android");
        return id != 0 && res.getBoolean(id);
    }
}
