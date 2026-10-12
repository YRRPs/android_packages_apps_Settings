/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/** Plain JUnit; no Robolectric. */
public class YrrpAdBlockEnabledObserversTest {
    private final YrrpAdBlockEnabledObservers mObservers = new YrrpAdBlockEnabledObservers();

    @Test
    public void notifiesEveryAddedObserver() {
        final AtomicInteger page = new AtomicInteger();
        final AtomicInteger other = new AtomicInteger();
        mObservers.add(page::incrementAndGet);
        mObservers.add(other::incrementAndGet);
        mObservers.notifyChanged();
        assertEquals(1, page.get());
        assertEquals(1, other.get());
    }

    @Test
    public void removedObserverIsNotNotified() {
        final AtomicInteger page = new AtomicInteger();
        final Runnable observer = page::incrementAndGet;
        mObservers.add(observer);
        mObservers.remove(observer);
        mObservers.notifyChanged();
        assertEquals(0, page.get());
    }

    @Test
    public void addingTwiceNotifiesOnce() {
        final AtomicInteger page = new AtomicInteger();
        final Runnable observer = page::incrementAndGet;
        mObservers.add(observer);
        mObservers.add(observer);
        mObservers.notifyChanged();
        assertEquals(1, page.get());
    }

    @Test
    public void observerMayRemoveItselfWhileNotified() {
        final AtomicInteger page = new AtomicInteger();
        final Runnable[] self = new Runnable[1];
        self[0] = () -> {
            page.incrementAndGet();
            mObservers.remove(self[0]);
        };
        mObservers.add(self[0]);
        mObservers.notifyChanged();
        mObservers.notifyChanged();
        assertEquals(1, page.get());
    }
}
