<<<<<<< HEAD   (dce59d Add git-review configuration)
=======
package com.android.intentresolver.v2

import android.os.UserHandle
import androidx.activity.ComponentActivity
import androidx.annotation.OpenForTesting

/**
 * Activity logic for [ChooserActivity].
 *
 * TODO: Make this class no longer open once [ChooserActivity] no longer needs to cast to access
 *   [chooserRequest]. For now, this class being open is better than using reflection there.
 */
@OpenForTesting
open class ChooserActivityLogic(
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
