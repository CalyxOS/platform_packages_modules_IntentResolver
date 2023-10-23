<<<<<<< HEAD   (dce59d Add git-review configuration)
=======
package com.android.intentresolver.v2

import android.os.UserHandle
import androidx.activity.ComponentActivity
import com.android.intentresolver.AnnotatedUserHandles
import com.android.intentresolver.WorkProfileAvailabilityManager

/** Activity logic for use when testing [ResolverActivity]. */
class TestResolverActivityLogic(
    tag: String,
    activity: ComponentActivity,
    onWorkProfileStatusUpdated: (UserHandle) -> Unit,
    private val overrideData: ResolverWrapperActivity.OverrideData,
) : ResolverActivityLogic(tag, activity, onWorkProfileStatusUpdated) {

    override val annotatedUserHandles: AnnotatedUserHandles? by lazy {
        overrideData.annotatedUserHandles
    }

    override val workProfileAvailabilityManager: WorkProfileAvailabilityManager by lazy {
        overrideData.mWorkProfileAvailability ?: super.workProfileAvailabilityManager
    }
}
>>>>>>> CHANGE (b99219 Support sharing to non-first work profiles)
