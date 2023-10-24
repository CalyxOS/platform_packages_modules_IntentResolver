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

package com.android.intentresolver

import android.os.UserHandle

import com.google.common.truth.Truth.assertThat
import com.google.common.collect.ImmutableList

import org.junit.Test

class AnnotatedUserHandlesTest {

    @Test
    fun testBasicProperties() {  // Fields that are reflected back w/o logic.
        val info = AnnotatedUserHandles.newBuilder()
            .setUserIdOfCallingApp(42)
            .setUserHandleSharesheetLaunchedAs(UserHandle.of(116))
            .setPersonalProfileUserHandle(UserHandle.of(117))
            .setWorkProfileUserHandles(ImmutableList.of(UserHandle.of(118), UserHandle.of(119)))
            .setCloneProfileUserHandles(ImmutableList.of(UserHandle.of(120)))
            .build()

        assertThat(info.userIdOfCallingApp).isEqualTo(42)
        assertThat(info.userHandleSharesheetLaunchedAs.identifier).isEqualTo(116)
        assertThat(info.personalProfileUserHandle.identifier).isEqualTo(117)
        assertThat(info.workProfileUserHandles.get(0).identifier).isEqualTo(118)
        assertThat(info.workProfileUserHandles.get(1).identifier).isEqualTo(119)
        assertThat(info.workProfileUserHandles.size).isEqualTo(2)
        assertThat(info.cloneProfileUserHandles.get(0).identifier).isEqualTo(120)
        assertThat(info.cloneProfileUserHandles.size).isEqualTo(1)
    }

    @Test
    fun testWorkTabInitiallySelectedWhenLaunchedFromWorkProfile() {
        val info = AnnotatedUserHandles.newBuilder()
            .setUserIdOfCallingApp(42)
            .setPersonalProfileUserHandle(UserHandle.of(101))
            .setWorkProfileUserHandles(ImmutableList.of(UserHandle.of(202), UserHandle.of(203)))
            .setUserHandleSharesheetLaunchedAs(UserHandle.of(202))
            .build()

        assertThat(info.tabOwnerUserHandleForLaunch.identifier).isEqualTo(202)
    }

    @Test
    fun testPersonalTabInitiallySelectedWhenLaunchedFromPersonalProfile() {
        val info = AnnotatedUserHandles.newBuilder()
            .setUserIdOfCallingApp(42)
            .setPersonalProfileUserHandle(UserHandle.of(101))
            .setWorkProfileUserHandles(ImmutableList.of(UserHandle.of(202), UserHandle.of(203)))
            .setUserHandleSharesheetLaunchedAs(UserHandle.of(101))
            .build()

        assertThat(info.tabOwnerUserHandleForLaunch.identifier).isEqualTo(101)
    }

    @Test
    fun testPersonalTabInitiallySelectedWhenLaunchedFromOtherProfile() {
        val info = AnnotatedUserHandles.newBuilder()
            .setUserIdOfCallingApp(42)
            .setPersonalProfileUserHandle(UserHandle.of(101))
            .setWorkProfileUserHandles(ImmutableList.of(UserHandle.of(202), UserHandle.of(203)))
            .setUserHandleSharesheetLaunchedAs(UserHandle.of(303))
            .build()

        assertThat(info.tabOwnerUserHandleForLaunch.identifier).isEqualTo(101)
    }
}
