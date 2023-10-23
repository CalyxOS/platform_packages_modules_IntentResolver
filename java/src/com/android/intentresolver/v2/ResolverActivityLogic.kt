<<<<<<< HEAD   (dce59d Add git-review configuration)
=======
package com.android.intentresolver.v2

import android.os.UserHandle
import androidx.activity.ComponentActivity
import androidx.annotation.OpenForTesting

/** Activity logic for [ResolverActivity]. */
@OpenForTesting
open class ResolverActivityLogic(
    tag: String,
    activity: ComponentActivity,
    onWorkProfileStatusUpdated: (UserHandle) -> Unit,
) :
    ActivityLogic,
    CommonActivityLogic by CommonActivityLogicImpl(
        tag,
        activity,
        onWorkProfileStatusUpdated,
    )
>>>>>>> CHANGE (b99219 Support sharing to non-first work profiles)
