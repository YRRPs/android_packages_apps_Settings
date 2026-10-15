/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.yrrp.YrrpFeatureAvailability.HotspotControls;
import com.android.settings.yrrp.YrrpFeatureAvailability.Probe;
import com.android.settings.yrrp.YrrpFeatureCatalog.Id;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class YrrpFeatureInventoryControllerTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final LifecycleOwner mOwner = mock(LifecycleOwner.class);
    private FakeEnvironment mEnvironment;
    private FakeHotspotSource mHotspot;
    private YrrpFeatureInventoryController mController;

    @Before
    public void setUp() {
        mEnvironment = new FakeEnvironment();
        mHotspot = new FakeHotspotSource();
        mController = new YrrpFeatureInventoryController(mContext, mEnvironment, mHotspot);
    }

    @Test
    public void displayPreference_resolvesAndBindsEveryFeature() {
        final Map<String, YrrpFeaturePreference> rows = new HashMap<>();
        for (Id id : YrrpFeatureCatalog.ids()) {
            rows.put(id.key, new YrrpFeaturePreference(mContext, null));
        }
        final PreferenceScreen screen = mock(PreferenceScreen.class);
        when(screen.findPreference(anyString())).thenAnswer(
                invocation -> rows.get(invocation.getArgument(0)));

        mController.displayPreference(screen);

        assertThat(mController.results()).hasSize(11);
        for (Id id : YrrpFeatureCatalog.ids()) {
            assertThat(rows.get(id.key).availability()).isNotNull();
        }
    }

    @Test
    public void lifecycle_balancesListenerAndIgnoresLateCallback() {
        mController.resolveAll();
        mController.onStart(mOwner);
        mController.onStart(mOwner);
        assertThat(mHotspot.addCount).isEqualTo(1);

        mEnvironment.hotspot = HotspotControls.SUPPORTED;
        mHotspot.fire();
        assertThat(mController.result(Id.HOTSPOT_CLIENT_MANAGEMENT).reasonCode)
                .isEqualTo("available");

        mController.onStop(mOwner);
        assertThat(mHotspot.removeCount).isEqualTo(1);
        mEnvironment.hotspot = HotspotControls.UNSUPPORTED;
        mHotspot.fireLate();
        assertThat(mController.result(Id.HOTSPOT_CLIENT_MANAGEMENT).reasonCode)
                .isEqualTo("available");
    }

    @Test
    public void hotspotCallback_updatesOnlyHotspotResult() {
        mController.resolveAll();
        final YrrpFeatureAvailability pulseBefore = mController.result(Id.PULSE);
        mEnvironment.hotspot = HotspotControls.UNDETERMINED;
        mController.onStart(mOwner);
        mHotspot.fire();

        assertThat(mController.result(Id.PULSE)).isSameInstanceAs(pulseBefore);
        assertThat(mController.result(Id.HOTSPOT_CLIENT_MANAGEMENT).available).isTrue();
        assertThat(mController.result(Id.HOTSPOT_CLIENT_MANAGEMENT).reasonCode)
                .isEqualTo("hotspot_controls_unknown");

        mEnvironment.hotspot = HotspotControls.UNSUPPORTED;
        mHotspot.fire();
        assertThat(mController.result(Id.HOTSPOT_CLIENT_MANAGEMENT).available).isTrue();
        assertThat(mController.result(Id.HOTSPOT_CLIENT_MANAGEMENT).reasonCode)
                .isEqualTo("hotspot_controls_unavailable");
    }

    @Test
    public void failedProbe_doesNotOverwriteIndependentRows() {
        mEnvironment.primaryUser = Probe.UNKNOWN;
        mController.resolveAll();

        assertThat(mController.result(Id.AD_BLOCKING).reasonCode).isEqualTo("probe_unknown");
        assertThat(mController.result(Id.PULSE).available).isTrue();
        assertThat(mController.result(Id.ONEPLUS_CAMERA).available).isTrue();
    }

    @Test
    public void changedResult_incrementsEpochAndLogsWhileUnchangedRerenderDoesNot() {
        final YrrpFeatureInventoryEventLog log = new YrrpFeatureInventoryEventLog(() -> 4L);
        final YrrpFeatureInventoryDump dump = new YrrpFeatureInventoryDump(log);
        mController = new YrrpFeatureInventoryController(
                mContext, mEnvironment, mHotspot, dump);

        mController.resolveAll();
        assertThat(dump.resolutionEpoch()).isEqualTo(1L);
        assertThat(log.lines()).hasSize(11);

        mController.resolveAll();
        assertThat(dump.resolutionEpoch()).isEqualTo(1L);
        assertThat(log.lines()).hasSize(11);

        mController.onStart(mOwner);
        mEnvironment.hotspot = HotspotControls.UNSUPPORTED;
        mHotspot.fire();
        assertThat(dump.resolutionEpoch()).isEqualTo(2L);
        assertThat(log.lines()).hasSize(12);
    }

    @Test
    public void recreationWithSameFacts_restoresSameResults() {
        mController.resolveAll();
        final YrrpFeatureInventoryController recreated =
                new YrrpFeatureInventoryController(mContext, mEnvironment, mHotspot);
        recreated.resolveAll();

        for (Id id : YrrpFeatureCatalog.ids()) {
            assertThat(recreated.result(id).available)
                    .isEqualTo(mController.result(id).available);
            assertThat(recreated.result(id).reasonCode)
                    .isEqualTo(mController.result(id).reasonCode);
            assertThat(recreated.result(id).blockedBy)
                    .isEqualTo(mController.result(id).blockedBy);
        }
    }

    private static final class FakeHotspotSource implements YrrpFeatureHotspotSource {
        int addCount;
        int removeCount;
        Listener listener;
        Listener lastListener;

        @Override
        public void addListener(Listener listener) {
            addCount++;
            this.listener = listener;
            lastListener = listener;
        }

        @Override
        public void removeListener(Listener listener) {
            removeCount++;
            if (this.listener == listener) {
                this.listener = null;
            }
        }

        @Override
        public YrrpHotspotDump.Snapshot snapshot() {
            return new YrrpHotspotDump.Snapshot();
        }

        void fire() {
            if (listener != null) {
                listener.onHotspotChanged();
            }
        }

        void fireLate() {
            if (lastListener != null) {
                lastListener.onHotspotChanged();
            }
        }
    }

    private static final class FakeEnvironment implements YrrpFeatureEnvironment {
        Probe primaryUser = Probe.YES;
        HotspotControls hotspot = HotspotControls.SUPPORTED;

        @Override
        public YrrpFoldAnimationAvailability.Snapshot fold() {
            return new YrrpFoldAnimationAvailability.Snapshot(Probe.YES, Probe.YES, Probe.YES);
        }

        @Override
        public Probe primaryUser() {
            return primaryUser;
        }

        @Override
        public Probe salamiDevice() {
            return Probe.YES;
        }

        @Override
        public Probe cameraPackage() {
            return Probe.YES;
        }

        @Override
        public Probe galleryPackage() {
            return Probe.YES;
        }

        @Override
        public Probe gappsBuild() {
            return Probe.YES;
        }

        @Override
        public HotspotControls hotspotControls() {
            return hotspot;
        }
    }
}
