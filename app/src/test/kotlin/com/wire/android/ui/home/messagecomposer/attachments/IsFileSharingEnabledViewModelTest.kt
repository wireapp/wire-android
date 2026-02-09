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

import com.wire.android.config.CoroutineTestExtension
import com.wire.kalium.logic.configuration.FileSharingStatus
import com.wire.kalium.logic.feature.user.IsFileSharingEnabledUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
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

        private lateinit var viewModel: IsFileSharingEnabledViewModel

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withFileSharingStatus(result: FileSharingStatus.Value) = apply {
            coEvery { isFileSharingEnabledUseCase() } returns FileSharingStatus(
                result,
                true
            )
        }

        fun arrange(block: Arrangement.() -> Unit) = apply(block).let {
            viewModel = IsFileSharingEnabledViewModelImpl(
                isFileSharingEnabledUseCase
            )
            this to viewModel
        }
    }
}
