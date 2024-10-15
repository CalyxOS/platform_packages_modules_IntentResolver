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

package com.android.intentresolver

import com.android.intentresolver.Flags.FLAG_ENABLE_PRIVATE_PROFILE
import com.android.intentresolver.annotation.JavaInterop
import com.android.intentresolver.data.repository.FakeUserRepository
import com.android.intentresolver.domain.interactor.UserInteractor
import com.android.intentresolver.inject.FakeIntentResolverFlags
import com.android.intentresolver.shared.model.Profile
import com.android.intentresolver.shared.model.User
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(JavaInterop::class)
class ProfileHelperTest {

    private val personalUser = User(0, User.Role.PERSONAL)
    private val cloneUser1 = User(10, User.Role.CLONE)
    private val cloneUser2 = User(20, User.Role.CLONE)
    private val cloneUsers = listOf(cloneUser1, cloneUser2)

    private val personalProfile = Profile(Profile.Type.PERSONAL, personalUser)
    private val personalWithCloneProfile = Profile(Profile.Type.PERSONAL, personalUser, cloneUsers)

    private val workUser1 = User(11, User.Role.WORK)
    private val workUser2 = User(21, User.Role.WORK)
    private val workProfile1 = Profile(Profile.Type.WORK, workUser1)
    private val workProfile2 = Profile(Profile.Type.WORK, workUser2)
    private val workUsers = listOf(workUser1, workUser2)
    private val workProfiles = listOf(workProfile1, workProfile2)

    private val privateUser1 = User(12, User.Role.PRIVATE)
    private val privateUser2 = User(22, User.Role.PRIVATE)
    private val privateProfile1 = Profile(Profile.Type.PRIVATE, privateUser1)
    private val privateProfile2 = Profile(Profile.Type.PRIVATE, privateUser2)
    private val privateUsers = listOf(privateUser1, privateUser2)
    private val privateProfiles = listOf(privateProfile1, privateProfile2)

    private val flags =
        FakeIntentResolverFlags().apply { setFlag(FLAG_ENABLE_PRIVATE_PROFILE, true) }

    private fun assertProfiles(
        helper: ProfileHelper,
        personalProfile: Profile,
        workProfiles: List<Profile> = listOf(),
        privateProfiles: List<Profile> = listOf()
    ) {
        assertThat(helper.personalProfile).isEqualTo(personalProfile)
        assertThat(helper.personalHandle).isEqualTo(personalProfile.primary.handle)

        if (personalProfile.clones.isNotEmpty()) {
            assertThat(helper.cloneUserPresent).isTrue()
            assertThat(helper.cloneHandles)
                .containsExactlyElementsIn(
                    personalProfile.clones.stream().map { user -> user.handle }.toList()
                )
        } else {
            assertThat(helper.cloneUserPresent).isFalse()
            assertThat(helper.cloneHandles).isEmpty()
        }

        if (workProfiles.isNotEmpty()) {
            assertThat(helper.workProfilePresent).isTrue()
            assertThat(helper.workProfiles).containsExactlyElementsIn(workProfiles)
            assertThat(helper.workHandles).containsExactlyElementsIn(
                workProfiles.stream().map { profile -> profile.primary.handle }.toList()
            )
        } else {
            assertThat(helper.workProfilePresent).isFalse()
            assertThat(helper.workHandles).isEmpty()
        }

        if (privateProfiles.isNotEmpty()) {
            assertThat(helper.privateProfilePresent).isTrue()
            assertThat(helper.privateProfiles).containsExactlyElementsIn(privateProfiles)
            assertThat(helper.privateHandles).containsExactlyElementsIn(
                privateProfiles.stream().map { profile -> profile.primary.handle }.toList()
            )
        } else {
            assertThat(helper.privateProfilePresent).isFalse()
            assertThat(helper.privateHandles).isEmpty()
        }
    }

    @Test
    fun launchedByPersonal() = runTest {
        val repository = FakeUserRepository(listOf(personalUser))
        val interactor = UserInteractor(repository, launchedAs = personalUser.handle)

        val helper =
            ProfileHelper(
                interactor = interactor,
                scope = this,
                background = Dispatchers.Unconfined,
                flags = flags
            )

        assertProfiles(helper, personalProfile)

        assertThat(helper.isLaunchedAsCloneProfile).isFalse()
        assertThat(helper.launchedAsProfileType).isEqualTo(Profile.Type.PERSONAL)
        assertThat(helper.getQueryIntentsHandle(personalUser.handle))
            .isEqualTo(personalProfile.primary.handle)
        assertThat(helper.tabOwnerUserHandleForLaunch).isEqualTo(personalProfile.primary.handle)
    }

    @Test
    fun launchedByPersonal_withClone() = runTest {
        val repository = FakeUserRepository(listOf(personalUser) + cloneUsers)
        val interactor = UserInteractor(repository, launchedAs = personalUser.handle)

        val helper =
            ProfileHelper(
                interactor = interactor,
                scope = this,
                background = Dispatchers.Unconfined,
                flags = flags
            )

        assertProfiles(helper, personalWithCloneProfile)

        assertThat(helper.isLaunchedAsCloneProfile).isFalse()
        assertThat(helper.launchedAsProfileType).isEqualTo(Profile.Type.PERSONAL)
        assertThat(helper.getQueryIntentsHandle(personalUser.handle)).isEqualTo(personalUser.handle)
        assertThat(helper.tabOwnerUserHandleForLaunch).isEqualTo(personalProfile.primary.handle)
    }

    @Test
    fun launchedByClone() = runTest {
        val repository = FakeUserRepository(listOf(personalUser) + cloneUsers)
        val interactor = UserInteractor(repository, launchedAs = cloneUsers[0].handle)

        val helper =
            ProfileHelper(
                interactor = interactor,
                scope = this,
                background = Dispatchers.Unconfined,
                flags = flags
            )

        assertProfiles(helper, personalWithCloneProfile)

        assertThat(helper.isLaunchedAsCloneProfile).isTrue()
        assertThat(helper.launchedAsProfileType).isEqualTo(Profile.Type.PERSONAL)
        assertThat(personalWithCloneProfile.clones.stream().map { it.handle }.toList())
            .contains(helper.getQueryIntentsHandle(personalWithCloneProfile.primary.handle))
        assertThat(helper.tabOwnerUserHandleForLaunch)
            .isEqualTo(personalWithCloneProfile.primary.handle)
    }

    @Test
    fun launchedByPersonal_withWork() = runTest {
        val repository = FakeUserRepository(listOf(personalUser) + workUsers)
        val interactor = UserInteractor(repository, launchedAs = personalUser.handle)

        val helper =
            ProfileHelper(
                interactor = interactor,
                scope = this,
                background = Dispatchers.Unconfined,
                flags = flags
            )

        assertProfiles(helper,
            personalProfile = personalProfile,
            workProfiles = workProfiles
        )

        assertThat(helper.launchedAsProfileType).isEqualTo(Profile.Type.PERSONAL)
        assertThat(helper.isLaunchedAsCloneProfile).isFalse()
        assertThat(helper.getQueryIntentsHandle(personalUser.handle))
            .isEqualTo(personalProfile.primary.handle)
        assertThat(workUsers.stream().map { helper.getQueryIntentsHandle(it.handle) }.toList())
            .containsExactlyElementsIn(workProfiles.stream().map { it.primary.handle }.toList())
            .inOrder()
        assertThat(helper.tabOwnerUserHandleForLaunch).isEqualTo(personalProfile.primary.handle)
    }

    @Test
    fun launchedByWork() = runTest {
        val repository = FakeUserRepository(listOf(personalUser) + workUsers)
        val interactor = UserInteractor(repository, launchedAs = workUser1.handle)

        val helper =
            ProfileHelper(
                interactor = interactor,
                scope = this,
                background = Dispatchers.Unconfined,
                flags = flags
            )

        assertProfiles(helper, personalProfile = personalProfile, workProfiles = workProfiles)

        assertThat(helper.isLaunchedAsCloneProfile).isFalse()
        assertThat(helper.launchedAsProfileType).isEqualTo(Profile.Type.WORK)
        assertThat(helper.getQueryIntentsHandle(personalProfile.primary.handle))
            .isEqualTo(personalProfile.primary.handle)
        val workProfileQueryIntentsHandles =
            workProfiles.stream().map { helper.getQueryIntentsHandle(it.primary.handle) }.toList()
        val workProfileHandles = workProfiles.stream().map { it.primary.handle }.toList()
        assertThat(workProfileQueryIntentsHandles)
            .containsExactlyElementsIn(workProfileHandles)
            .inOrder()
        assertThat(helper.tabOwnerUserHandleForLaunch).isEqualTo(workProfile1.primary.handle)
    }

    @Test
    fun launchedByPersonal_withPrivate() = runTest {
        val repository = FakeUserRepository(listOf(personalUser) + privateUsers)
        val interactor = UserInteractor(repository, launchedAs = personalUser.handle)

        val helper =
            ProfileHelper(
                interactor = interactor,
                scope = this,
                background = Dispatchers.Unconfined,
                flags = flags
            )

        assertProfiles(helper, personalProfile = personalProfile, privateProfiles = privateProfiles)

        assertThat(helper.isLaunchedAsCloneProfile).isFalse()
        assertThat(helper.launchedAsProfileType).isEqualTo(Profile.Type.PERSONAL)
        assertThat(helper.getQueryIntentsHandle(personalProfile.primary.handle))
            .isEqualTo(personalProfile.primary.handle)
        val privateProfileQueryIntentsHandles =
            privateProfiles.stream().map {
                helper.getQueryIntentsHandle(it.primary.handle)
            }.toList()
        val privateProfileHandles = privateProfiles.stream().map { it.primary.handle }.toList()
        assertThat(privateProfileQueryIntentsHandles)
            .containsExactlyElementsIn(privateProfileHandles)
            .inOrder()
        assertThat(helper.tabOwnerUserHandleForLaunch).isEqualTo(personalProfile.primary.handle)
    }

    @Test
    fun launchedByPrivate() = runTest {
        val repository = FakeUserRepository(listOf(personalUser) + privateUsers)
        val interactor = UserInteractor(repository, launchedAs = privateUser1.handle)

        val helper =
            ProfileHelper(
                interactor = interactor,
                scope = this,
                background = Dispatchers.Unconfined,
                flags = flags
            )

        assertProfiles(helper, personalProfile = personalProfile, privateProfiles = privateProfiles)

        assertThat(helper.isLaunchedAsCloneProfile).isFalse()
        assertThat(helper.launchedAsProfileType).isEqualTo(Profile.Type.PRIVATE)
        assertThat(helper.getQueryIntentsHandle(personalProfile.primary.handle))
            .isEqualTo(personalProfile.primary.handle)
        val privateProfileQueryIntentsHandles =
            privateProfiles.stream().map {
                helper.getQueryIntentsHandle(it.primary.handle)
            }.toList()
        val privateProfileHandles = privateProfiles.stream().map { it.primary.handle }.toList()
        assertThat(privateProfileQueryIntentsHandles)
            .containsExactlyElementsIn(privateProfileHandles)
            .inOrder()
        assertThat(helper.tabOwnerUserHandleForLaunch).isEqualTo(privateProfile1.primary.handle)
    }

    @Test
    fun launchedByPersonal_withPrivate_privateDisabled() = runTest {
        flags.setFlag(FLAG_ENABLE_PRIVATE_PROFILE, false)

        val repository = FakeUserRepository(listOf(personalUser) + privateUsers)
        val interactor = UserInteractor(repository, launchedAs = personalUser.handle)

        val helper =
            ProfileHelper(
                interactor = interactor,
                scope = this,
                background = Dispatchers.Unconfined,
                flags = flags
            )

        assertProfiles(helper, personalProfile = personalProfile, privateProfiles = listOf())

        assertThat(helper.isLaunchedAsCloneProfile).isFalse()
        assertThat(helper.launchedAsProfileType).isEqualTo(Profile.Type.PERSONAL)
        assertThat(helper.getQueryIntentsHandle(personalProfile.primary.handle))
            .isEqualTo(personalProfile.primary.handle)
        assertThat(helper.tabOwnerUserHandleForLaunch).isEqualTo(personalProfile.primary.handle)
    }
}
