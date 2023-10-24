/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.intentresolver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.ArraySet;

import androidx.annotation.VisibleForTesting;

import com.google.common.collect.ImmutableList;

import java.util.Set;
import java.util.function.Consumer;

/** Monitor for runtime conditions that may disable work profile display. */
public class WorkProfileAvailabilityManager {
    private final UserManager mUserManager;
    private final ImmutableList<UserHandle> mWorkProfileUserHandles;
    private final Consumer<UserHandle> mOnWorkProfileStateUpdated;

    private BroadcastReceiver mWorkProfileStateReceiver;

    private Set<UserHandle> mIsWaitingToEnableWorkProfile = new ArraySet<>();
    private Set<UserHandle> mWorkProfileHasBeenEnabled = new ArraySet<>();

    public WorkProfileAvailabilityManager(
            UserManager userManager,
            ImmutableList<UserHandle> workProfileUserHandles,
            Consumer<UserHandle> onWorkProfileStateUpdated) {
        mUserManager = userManager;
        mWorkProfileUserHandles = workProfileUserHandles;
        for (UserHandle workProfileUserHandle : workProfileUserHandles) {
            if (isWorkProfileEnabled(workProfileUserHandle)) {
                mWorkProfileHasBeenEnabled.add(workProfileUserHandle);
            }
        }
        mOnWorkProfileStateUpdated = onWorkProfileStateUpdated;
    }

    /**
     * Register a {@link BroadcastReceiver}, if we haven't already, to be notified about work
     * profile availability changes.
     *
     * TODO: this takes the context for testing, because we don't have a context on hand when we
     * set up this component's default "override" in {@link ChooserActivityOverrideData#reset()}.
     * The use of these overrides in our testing design is questionable and can hopefully be
     * improved someday; then this context should be injected in our constructor & held as `final`.
     *
     * TODO: consider injecting an optional `Lifecycle` so that this component can automatically
     * manage its own registration/unregistration. (This would be optional because registration of
     * the receiver is conditional on having `shouldShowTabs()` in our session.)
     */
    public void registerWorkProfileStateReceiver(Context context) {
        if (mWorkProfileStateReceiver != null) {
            return;
        }
        mWorkProfileStateReceiver = createWorkProfileStateReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_UNLOCKED);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE);
        filter.addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE);
        context.registerReceiverAsUser(
                mWorkProfileStateReceiver, UserHandle.ALL, filter, null, null);
    }

    /**
     * Unregister any {@link BroadcastReceiver} currently waiting for a work-enabled broadcast.
     *
     * TODO: this takes the context for testing, because we don't have a context on hand when we
     * set up this component's default "override" in {@link ChooserActivityOverrideData#reset()}.
     * The use of these overrides in our testing design is questionable and can hopefully be
     * improved someday; then this context should be injected in our constructor & held as `final`.
     */
    public void unregisterWorkProfileStateReceiver(Context context) {
        if (mWorkProfileStateReceiver == null) {
            return;
        }
        context.unregisterReceiver(mWorkProfileStateReceiver);
        mWorkProfileStateReceiver = null;
    }

    public boolean isQuietModeEnabled(UserHandle workProfileUserHandle) {
        return mUserManager.isQuietModeEnabled(workProfileUserHandle);
    }

    // TODO: why do clients only care about the result of `isQuietModeEnabled()`, even though
    // internally (in `isWorkProfileEnabled()`) we also check this 'unlocked' condition?
    @VisibleForTesting
    public boolean isWorkProfileUserUnlocked(UserHandle workProfileUserHandle) {
        return mUserManager.isUserUnlocked(workProfileUserHandle);
    }

    public void requestQuietModeEnabled(UserHandle workProfileUserHandle, boolean enabled) {
        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
                mUserManager.requestQuietModeEnabled(enabled, workProfileUserHandle);
        });
        mIsWaitingToEnableWorkProfile.add(workProfileUserHandle);
    }

    /**
     * Request that quiet mode be enabled (or disabled) for the work profile.
     * TODO: this is only used to disable quiet mode; should that be hard-coded?
     */
    private void requestQuietModeEnabled(boolean enabled) {
        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
            for (UserHandle workProfileUserHandle : mWorkProfileUserHandles) {
                mUserManager.requestQuietModeEnabled(enabled, workProfileUserHandle);
                mIsWaitingToEnableWorkProfile.add(workProfileUserHandle);
             }
        });
    }

    /**
     * Stop waiting for a work-enabled broadcast.
     * TODO: this seems strangely low-level to include as part of the public API. Maybe some
     * responsibilities need to be pulled over from the client?
     */
    public void markWorkProfileEnabledBroadcastReceived(UserHandle workProfileUserHandle) {
        mIsWaitingToEnableWorkProfile.remove(workProfileUserHandle);
    }

    public boolean isWaitingToEnableWorkProfile(UserHandle workProfileUserHandle) {
        return mIsWaitingToEnableWorkProfile.contains(workProfileUserHandle);
    }

    private boolean isWorkProfileEnabled(UserHandle workProfileUserHandle) {
        return !isQuietModeEnabled(workProfileUserHandle)
                && isWorkProfileUserUnlocked(workProfileUserHandle);
    }

    private BroadcastReceiver createWorkProfileStateReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (!TextUtils.equals(action, Intent.ACTION_USER_UNLOCKED)
                        && !TextUtils.equals(action, Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
                        && !TextUtils.equals(action, Intent.ACTION_MANAGED_PROFILE_AVAILABLE)) {
                    return;
                }

                final int intentUserId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -1);
                boolean workProfileIsEnabled = false;
                UserHandle workProfileUserHandle = null;
                for (UserHandle userHandle : mWorkProfileUserHandles) {
                    final int workProfileUserId = userHandle.getIdentifier();
                    if (intentUserId == workProfileUserId) {
                        workProfileUserHandle = userHandle;
                        break;
                    }
                }
                if (workProfileUserHandle == null) {
                    return;
                }

                if (isWorkProfileEnabled(workProfileUserHandle)) {
                    if (mWorkProfileHasBeenEnabled.contains(workProfileUserHandle)) {
                        return;
                    }
                    mWorkProfileHasBeenEnabled.add(workProfileUserHandle);
                    mIsWaitingToEnableWorkProfile.remove(workProfileUserHandle);
                } else {
                    // Must be an UNAVAILABLE broadcast, so we watch for the next availability.
                    // TODO: confirm the above reasoning (& handling of "UNAVAILABLE" in general).
                    mWorkProfileHasBeenEnabled.remove(workProfileUserHandle);
                }

                mOnWorkProfileStateUpdated.accept(workProfileUserHandle);
            }
        };
    }
}
