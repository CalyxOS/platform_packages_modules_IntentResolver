package com.android.intentresolver.v2

import android.os.UserHandle
import androidx.activity.ComponentActivity
import com.android.intentresolver.AnnotatedUserHandles
import com.android.intentresolver.WorkProfileAvailabilityManager

/** Activity logic for use when testing [ResolverActivity]. */
class TestResolverActivityLogic(
    tag: String,
    activityProvider: () -> ComponentActivity,
    onWorkProfileStatusUpdated: (UserHandle) -> Unit,
    private val overrideData: ResolverWrapperActivity.OverrideData,
) : ResolverActivityLogic(tag, activityProvider, onWorkProfileStatusUpdated) {

    override val annotatedUserHandles: AnnotatedUserHandles? by lazy {
        overrideData.annotatedUserHandles
    }

    override val workProfileAvailabilityManager: WorkProfileAvailabilityManager by lazy {
        overrideData.mWorkProfileAvailability ?: super.workProfileAvailabilityManager
    }
}
