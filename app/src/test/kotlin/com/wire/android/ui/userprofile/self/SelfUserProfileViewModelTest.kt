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
package com.wire.android.ui.userprofile.self

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.ui.legalhold.banner.LegalHoldUIState
import com.wire.kalium.logic.feature.legalhold.LegalHoldStateForSelfUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class SelfUserProfileViewModelTest {

    @Test
    fun `given legal hold request pending, then isUnderLegalHold is pending`() = runTest {
        // given
        val (_, viewModel) = SelfUserProfileViewModelArrangement()
            .withLegalHoldStatus(LegalHoldStateForSelfUser.PendingRequest)
            .arrange()
        // then
        assertEquals(LegalHoldUIState.Pending, viewModel.userProfileState.legalHoldStatus)
    }

    @Test
    fun `given legal hold enabled, then isUnderLegalHold is active`() = runTest {
        // given
        val (_, viewModel) = SelfUserProfileViewModelArrangement()
            .withLegalHoldStatus(LegalHoldStateForSelfUser.Enabled)
            .arrange()
        // then
        assertEquals(LegalHoldUIState.Active, viewModel.userProfileState.legalHoldStatus)
    }

    @Test
    fun `given legal hold disabled and no request available, then isUnderLegalHold is none`() =
        runTest {
            // given
            val (_, viewModel) = SelfUserProfileViewModelArrangement()
                .withLegalHoldStatus(LegalHoldStateForSelfUser.Disabled)
                .arrange()
            // then
            assertEquals(LegalHoldUIState.None, viewModel.userProfileState.legalHoldStatus)
        }
}
