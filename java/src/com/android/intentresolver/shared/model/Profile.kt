/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.intentresolver.shared.model

/**
 * Associates [users][User] into a [Type] instance.
 *
 * This is a simple abstraction which combines a primary [user][User] with an optional
 * [cloned apps][User.Role.CLONE] user. This encapsulates the cloned app user id, while still being
 * available where needed.
 */
data class Profile(
    val type: Type,
    val primary: User,
    /**
     * List of [User]s of which contains additional instances of some applications installed for the
     * personal user. This value may only be supplied when creating the PERSONAL profile.
     */
    val clones: List<User> = listOf(),
) {

    init {
        clones.forEach { it.apply {
            require(primary.role == User.Role.PERSONAL) {
                "clone is not supported for profile=${this@Profile.type} / primary=$primary"
            }
            require(role == User.Role.CLONE) { "clone is not a clone user ($this)" }
        } }
    }

    enum class Type {
        PERSONAL,
        WORK,
        PRIVATE,
    }
}
