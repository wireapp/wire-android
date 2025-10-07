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
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.WireTestTheme
import com.wire.android.ui.common.avatar.LEGAL_HOLD_INDICATOR_TEST_TAG
import com.wire.android.ui.common.avatar.STATUS_INDICATOR_TEST_TAG
import com.wire.android.ui.common.avatar.TEMP_USER_INDICATOR_TEST_TAG
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.avatar.UserProfileAvatarType
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.hours

class UserProfileAvatarTest {
    @get:Rule
    val composeTestRule by lazy { createComposeRule() }

    private fun ComposeTestRule.assertNode(tag: String, isDisplayed: Boolean) =
        this.onNodeWithTag(tag).let { if (isDisplayed) it.assertIsDisplayed() else it.assertDoesNotExist() }

    private fun testIndicators(
        userAvailabilityStatus: UserAvailabilityStatus,
        type: UserProfileAvatarType,
        shouldStatusIndicatorBeVisible: Boolean,
        shouldLegalHoldIndicatorBeVisible: Boolean,
        shouldTemporaryUserIndicatorBeVisible: Boolean,
    ) = runTest {
        composeTestRule.setContent {
            WireTestTheme {
                UserProfileAvatar(
                    size = dimensions().avatarDefaultBigSize,
                    avatarData = UserAvatarData(availabilityStatus = userAvailabilityStatus),
                    type = type,
                )
            }
        }
        composeTestRule.assertNode(tag = STATUS_INDICATOR_TEST_TAG, isDisplayed = shouldStatusIndicatorBeVisible)
        composeTestRule.assertNode(tag = LEGAL_HOLD_INDICATOR_TEST_TAG, isDisplayed = shouldLegalHoldIndicatorBeVisible)
        composeTestRule.assertNode(tag = TEMP_USER_INDICATOR_TEST_TAG, isDisplayed = shouldTemporaryUserIndicatorBeVisible)
    }

    @Test
    fun givenTypeWithoutIndicators_thenShouldNotShowAnyIndicator() = testIndicators(
        userAvailabilityStatus = UserAvailabilityStatus.AVAILABLE,
        type = UserProfileAvatarType.WithoutIndicators,
        shouldStatusIndicatorBeVisible = false,
        shouldLegalHoldIndicatorBeVisible = false,
        shouldTemporaryUserIndicatorBeVisible = false,
    )

    @Test
    fun givenTypeWithIndicators_andRegularUserWithoutStatusAndWithoutLegalHold_thenShouldNotShowAnyIndicator() = testIndicators(
        userAvailabilityStatus = UserAvailabilityStatus.NONE,
        type = UserProfileAvatarType.WithIndicators.RegularUser(legalHoldIndicatorVisible = false),
        shouldStatusIndicatorBeVisible = false,
        shouldLegalHoldIndicatorBeVisible = false,
        shouldTemporaryUserIndicatorBeVisible = false,
    )

    @Test
    fun givenTypeWithIndicators_andRegularUserWithStatusAndWithoutLegalHold_thenShouldShowStatusIndicator() = testIndicators(
        userAvailabilityStatus = UserAvailabilityStatus.AVAILABLE,
        type = UserProfileAvatarType.WithIndicators.RegularUser(legalHoldIndicatorVisible = false),
        shouldStatusIndicatorBeVisible = true,
        shouldLegalHoldIndicatorBeVisible = false,
        shouldTemporaryUserIndicatorBeVisible = false,
    )

    @Test
    fun givenTypeWithIndicators_andRegularUserWithoutStatusAndWithLegalHold_thenShouldShowLegalHoldIndicator() = testIndicators(
        userAvailabilityStatus = UserAvailabilityStatus.NONE,
        type = UserProfileAvatarType.WithIndicators.RegularUser(legalHoldIndicatorVisible = true),
        shouldStatusIndicatorBeVisible = false,
        shouldLegalHoldIndicatorBeVisible = true,
        shouldTemporaryUserIndicatorBeVisible = false,
    )

    @Test
    fun givenTypeWithIndicators_andRegularUserWithStatusAndWithLegalHold_thenShouldShowStatusAndLegalHoldIndicators() = testIndicators(
        userAvailabilityStatus = UserAvailabilityStatus.AVAILABLE,
        type = UserProfileAvatarType.WithIndicators.RegularUser(legalHoldIndicatorVisible = true),
        shouldStatusIndicatorBeVisible = true,
        shouldLegalHoldIndicatorBeVisible = true,
        shouldTemporaryUserIndicatorBeVisible = false,
    )

    @Test
    fun givenTypeWithIndicators_andTemporaryGuestUser_thenShouldShowTemporaryUserIndicator() = testIndicators(
        userAvailabilityStatus = UserAvailabilityStatus.AVAILABLE,
        type = UserProfileAvatarType.WithIndicators.TemporaryUser(expiresAt = Clock.System.now().plus(24.hours)),
        shouldStatusIndicatorBeVisible = false,
        shouldLegalHoldIndicatorBeVisible = false,
        shouldTemporaryUserIndicatorBeVisible = true,
    )
}
