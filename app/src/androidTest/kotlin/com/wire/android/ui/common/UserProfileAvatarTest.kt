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
package com.wire.android.ui.common

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.WireTestTheme
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.hours

class UserProfileAvatarTest {
    @get:Rule
    val composeTestRule by lazy { createComposeRule() }

    @Test
    fun givenARegularUserNotUnderLegalHold_thenShouldShowStatusIndicator() = runTest {
        composeTestRule.setContent {
            WireTestTheme {
                UserProfileAvatar(
                    size = dimensions().avatarDefaultBigSize,
                    avatarData = UserAvatarData(),
                    type = UserProfileAvatarType.WithoutIndicators
                )
            }
        }

        composeTestRule.onNodeWithTag(STATUS_INDICATOR_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(LEGAL_HOLD_INDICATOR_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TEMP_USER_INDICATOR_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun givenARegularUserUnderLegalHold_thenShouldShowStatusAndLegalHoldIndicators() = runTest {
        composeTestRule.setContent {
            WireTestTheme {
                UserProfileAvatar(
                    size = dimensions().avatarDefaultBigSize,
                    avatarData = UserAvatarData(availabilityStatus = UserAvailabilityStatus.AVAILABLE),
                    type = UserProfileAvatarType.WithIndicators.RegularUser(true)
                )
            }
        }

        composeTestRule.onNodeWithTag(STATUS_INDICATOR_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(LEGAL_HOLD_INDICATOR_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEMP_USER_INDICATOR_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun givenATempGuestUser_ShouldShowTempUserIndicators() = runTest {
        composeTestRule.setContent {
            WireTestTheme {
                UserProfileAvatar(
                    size = dimensions().avatarDefaultBigSize,
                    avatarData = UserAvatarData(),
                    type = UserProfileAvatarType.WithIndicators.TemporaryUser(expiresAt = Clock.System.now().plus(24.hours))
                )
            }
        }

        composeTestRule.onNodeWithTag(STATUS_INDICATOR_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(LEGAL_HOLD_INDICATOR_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TEMP_USER_INDICATOR_TEST_TAG).assertExists()
    }
}
