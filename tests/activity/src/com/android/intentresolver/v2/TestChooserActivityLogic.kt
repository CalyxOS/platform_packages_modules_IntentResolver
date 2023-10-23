package com.android.intentresolver.v2

import android.os.UserHandle
import androidx.activity.ComponentActivity
import com.android.intentresolver.AnnotatedUserHandles
import com.android.intentresolver.WorkProfileAvailabilityManager

/** Activity logic for use when testing [ChooserActivity]. */
class TestChooserActivityLogic(
    tag: String,
    activity: ComponentActivity,
    onWorkProfileStatusUpdated: (UserHandle) -> Unit,
    private val annotatedUserHandlesOverride: AnnotatedUserHandles?,
    private val workProfileAvailabilityOverride: WorkProfileAvailabilityManager?,
) :
    ChooserActivityLogic(
        tag,
        activity,
        onWorkProfileStatusUpdated,
    ) {
    override val annotatedUserHandles: AnnotatedUserHandles?
        get() = annotatedUserHandlesOverride ?: super.annotatedUserHandles

    override val workProfileAvailabilityManager: WorkProfileAvailabilityManager
        get() = workProfileAvailabilityOverride ?: super.workProfileAvailabilityManager
}
