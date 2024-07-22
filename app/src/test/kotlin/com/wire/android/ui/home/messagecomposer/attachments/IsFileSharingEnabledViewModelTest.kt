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
package com.wire.android.ui.home.messagecomposer.attachments

import com.wire.kalium.logic.configuration.FileSharingStatus
import com.wire.kalium.logic.feature.user.IsFileSharingEnabledUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsFileSharingEnabledViewModelTest {

    @Test
    fun `given fileSharing is allowed, then state should be true`() {
        val (arrangement, viewModel) = Arrangement().arrange {
            withFileSharingStatus(FileSharingStatus.Value.EnabledAll)
        }

        assertTrue(viewModel.isFileSharingEnabled())
        coVerify(exactly = 1) {
            arrangement.isFileSharingEnabledUseCase()
        }
    }

    @Test
    fun `given fileSharing is disabled, then state should be false`() {
        val (arrangement, viewModel) = Arrangement().arrange {
            withFileSharingStatus(FileSharingStatus.Value.Disabled)
        }

        assertFalse(viewModel.isFileSharingEnabled())
        coVerify(exactly = 1) {
            arrangement.isFileSharingEnabledUseCase()
        }
    }

    @Test
    fun `given fileSharing is allowed for some, then state should be true`() {
        val (arrangement, viewModel) = Arrangement().arrange {
            withFileSharingStatus(FileSharingStatus.Value.EnabledSome(emptyList()))
        }

        assertTrue(viewModel.isFileSharingEnabled())
        coVerify(exactly = 1) {
            arrangement.isFileSharingEnabledUseCase()
        }
    }

    private class Arrangement {

        @MockK
        lateinit var isFileSharingEnabledUseCase: IsFileSharingEnabledUseCase
        private val viewModel: IsFileSharingEnabledViewModel = IsFileSharingEnabledViewModelImpl(
            isFileSharingEnabledUseCase
        )

        fun withFileSharingStatus(result: FileSharingStatus.Value) = apply {
            every { isFileSharingEnabledUseCase() } returns FileSharingStatus (
                result,
                true
            )
        }

        fun arrange(block: Arrangement.() -> Unit) = apply(block).let {
            this to viewModel
        }

    }
}
