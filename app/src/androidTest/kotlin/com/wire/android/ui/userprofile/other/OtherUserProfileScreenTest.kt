/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.userprofile.other

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.wire.android.ui.WireTestTheme
import com.wire.android.ui.connection.CONNECTION_ACTION_BUTTONS_TEST_TAG
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.userprofile.other.OtherUserStubs.provideState
import kotlinx.coroutines.test.runTest
import kotlin.time.Instant
import org.junit.Rule
import org.junit.Test

class OtherUserProfileScreenTest {
    @get:Rule
    val composeTestRule by lazy { createComposeRule() }

    @Test
    fun givenOtherUserProfileFooter_ShouldNotShowConnectButtonForTempUsers() = runTest {
        composeTestRule.setContent {
            WireTestTheme {
                ContentFooter(
                    state = provideState(withExpireAt = Instant.DISTANT_FUTURE.toEpochMilliseconds()),
                )
            }
        }

        composeTestRule.onNodeWithTag(CONNECTION_ACTION_BUTTONS_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun givenOtherUserProfileFooter_ShouldNotShowConnectButtonForUsersWithoutMetadata() = runTest {
        composeTestRule.setContent {
            WireTestTheme {
                ContentFooter(
                    state = provideState(withUserName = "", withFullName = ""),
                )
            }
        }

        composeTestRule.onNodeWithTag(CONNECTION_ACTION_BUTTONS_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun givenOtherUserProfileFooter_ShouldNotShowConnectButtonForServices() = runTest {
        composeTestRule.setContent {
            WireTestTheme {
                ContentFooter(
                    state = provideState(withMembership = Membership.Service),
                )
            }
        }

        composeTestRule.onNodeWithTag(CONNECTION_ACTION_BUTTONS_TEST_TAG).assertDoesNotExist()
    }
}
