/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Callbacks run after the switch path writes the enabled property, so an open page follows a
 * change made from the Quick Settings tile. The tile and the page share the Settings process.
 */
final class YrrpAdBlockEnabledObservers {
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
