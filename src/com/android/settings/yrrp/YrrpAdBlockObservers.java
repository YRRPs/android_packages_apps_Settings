/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Callbacks an open page registers to follow a change made elsewhere: a switch write from the
 * Quick Settings tile, or an update that finished. The tile, the job, and the page share the
 * Settings process.
 */
final class YrrpAdBlockObservers {
    private final CopyOnWriteArraySet<Runnable> mObservers = new CopyOnWriteArraySet<>();

    void add(Runnable observer) {
        mObservers.add(observer);
    }

    void remove(Runnable observer) {
        mObservers.remove(observer);
    }

    /** Runs each observer once; an observer may remove itself while it runs. */
    void notifyChanged() {
        for (Runnable observer : mObservers) {
            observer.run();
        }
    }
}
