/*
 * SPDX-FileCopyrightText: Yim's Riced ROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.yrrp;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.utils.ThreadUtils;

import java.io.IOException;
import java.util.List;

/**
 * A category of removable rows with an Add row at the end: the hosts lists and the allowed
 * hosts. Edits are saved off the main thread under the update lock, then the blocklist is
 * rebuilt from the caches with no download, and the rows are redrawn. While the page is
 * started, the rows also redraw after every update, so new list statuses show at once.
 */
public abstract class YrrpAdBlockListPreferenceController extends BasePreferenceController
        implements DefaultLifecycleObserver {
    private static final String TAG = "YrrpAdBlock";
    private static final String ADD_KEY_SUFFIX = "_add";

    /** A change saved under the update lock; OK triggers a rebuild. */
    interface Change {
        YrrpAdBlockStore.AddResult apply(YrrpAdBlockUpdater updater) throws IOException;
    }

    private final Runnable mRefresh = this::refreshRows;
    private Fragment mHost;
    private PreferenceCategory mCategory;

    YrrpAdBlockListPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    /** The key the text dialog publishes its result under. */
    abstract String requestKey();

    abstract YrrpAdBlockTextInputDialogFragment newAddDialog();

    @StringRes
    abstract int addTitle();

    /** Builds one row per entry, keyed by the entry; rows must not use the Add key. */
    abstract List<Preference> rows(YrrpAdBlockStore.State state);

    /** Adds the typed text to the state. */
    abstract YrrpAdBlockStore.AddResult add(YrrpAdBlockStore.State state, String text);

    /** Removes the entry the row key names. Called off the main thread. */
    abstract void remove(YrrpAdBlockUpdater updater, String rowKey) throws IOException;

    /** Binds the hosting page; call from its {@code onAttach()}, like the Finish intro gear. */
    public void init(@NonNull Fragment host) {
        mHost = host;
        host.getChildFragmentManager().setFragmentResultListener(requestKey(), host,
                (key, result) -> onAddConfirmed(
                        result.getString(YrrpAdBlockTextInputDialogFragment.RESULT_TEXT, "")));
    }

    @Override
    public int getAvailabilityStatus() {
        return YrrpAdBlockAndroid.isPrimaryUser(mContext) ? AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mCategory = screen.findPreference(getPreferenceKey());
        refreshRows();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        YrrpAdBlockAndroid.updateObservers().add(mRefresh);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        YrrpAdBlockAndroid.updateObservers().remove(mRefresh);
    }

    /** Redraws on resume, so list statuses from an update made meanwhile show. */
    @Override
    public void updateState(@NonNull Preference preference) {
        super.updateState(preference);
        refreshRows();
    }

    @Override
    public boolean handlePreferenceTreeClick(@NonNull Preference preference) {
        final String key = preference.getKey();
        if (key == null || mCategory == null || mCategory.findPreference(key) != preference) {
            return false;
        }
        if (key.equals(addKey())) {
            showAddDialog();
        } else {
            confirmRemove(key, preference.getTitle());
        }
        return true;
    }

    /** Rebuilds the rows from the saved state. */
    void refreshRows() {
        if (mCategory == null) {
            return;
        }
        mCategory.removeAll();
        for (Preference row : rows(YrrpAdBlockAndroid.readState(mContext))) {
            mCategory.addPreference(row);
        }
        final Preference add = new Preference(mContext);
        add.setKey(addKey());
        add.setTitle(addTitle());
        add.setIcon(R.drawable.ic_add_24dp);
        mCategory.addPreference(add);
    }

    /** Saves a change, rebuilds the blocklist from the caches, then redraws. */
    void saveAndRebuild(Change change) {
        final Context app = mContext.getApplicationContext();
        ThreadUtils.postOnBackgroundThread(() -> {
            YrrpAdBlockStore.AddResult result;
            try {
                result = change.apply(YrrpAdBlockAndroid.updater(app));
            } catch (IOException e) {
                Log.w(TAG, "save failed error=" + e.getClass().getSimpleName());
                ThreadUtils.postOnMainThread(() -> toast(R.string.yrrp_ad_block_error_save));
                return;
            }
            if (result == YrrpAdBlockStore.AddResult.OK) {
                YrrpAdBlockAndroid.updater(app).update(false);
            }
            final YrrpAdBlockStore.AddResult shown = result;
            ThreadUtils.postOnMainThread(() -> {
                if (shown != YrrpAdBlockStore.AddResult.OK) {
                    toast(errorText(shown));
                }
                refreshRows();
            });
        });
    }

    private void onAddConfirmed(String text) {
        saveAndRebuild(updater -> updater.edit(state -> add(state, text)));
    }

    private void showAddDialog() {
        if (mHost == null || !mHost.isAdded()) {
            Log.w(TAG, "init(Fragment) not called or host gone");
            return;
        }
        final FragmentManager manager = mHost.getChildFragmentManager();
        if (manager.isStateSaved() || manager.findFragmentByTag(requestKey()) != null) {
            return;
        }
        newAddDialog().show(manager, requestKey());
    }

    private void confirmRemove(String key, CharSequence title) {
        if (mHost == null || !mHost.isAdded()) {
            return;
        }
        new AlertDialog.Builder(mHost.requireContext())
                .setTitle(mContext.getString(R.string.yrrp_ad_block_remove_title, title))
                .setPositiveButton(R.string.yrrp_ad_block_remove,
                        (dialog, which) -> saveAndRebuild(updater -> {
                            remove(updater, key);
                            return YrrpAdBlockStore.AddResult.OK;
                        }))
                .setNegativeButton(R.string.cancel, /* listener= */ null)
                .show();
    }

    private String addKey() {
        return getPreferenceKey() + ADD_KEY_SUFFIX;
    }

    private void toast(@StringRes int text) {
        Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
    }

    @StringRes
    private static int errorText(YrrpAdBlockStore.AddResult result) {
        switch (result) {
            case NOT_HTTPS:
                return R.string.yrrp_ad_block_error_not_https;
            case DUPLICATE:
                return R.string.yrrp_ad_block_error_duplicate;
            case LIMIT:
                return R.string.yrrp_ad_block_error_limit;
            case INVALID_HOST:
                return R.string.yrrp_ad_block_error_invalid_host;
            case MALFORMED:
            default:
                return R.string.yrrp_ad_block_error_malformed;
        }
    }
}
