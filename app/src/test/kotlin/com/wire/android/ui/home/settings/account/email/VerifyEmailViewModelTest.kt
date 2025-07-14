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
package com.wire.android.ui.home.settings.account.email

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.ui.home.settings.account.email.verifyEmail.VerifyEmailNavArgs
import com.wire.android.ui.home.settings.account.email.verifyEmail.VerifyEmailViewModel
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.feature.user.UpdateEmailUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(NavigationTestExtension::class)
class VerifyEmailViewModelTest {

    @Test
    fun `given updateEmail returns Success NoChange, when onVerifyEmail is called, then change state noChange to true`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNewEmail("newEmail")
            .withUpdateEmailResult(UpdateEmailUseCase.Result.Success.NoChange)
            .arrange()

        viewModel.onResendVerificationEmailClicked()

        assertEquals(true, viewModel.state.noChange)
        coVerify(exactly = 1) {
            arrangement.updateEmail(any())
        }
    }

    private class Arrangement {

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var updateEmail: UpdateEmailUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withNewEmail(email: String) = apply {
            every { savedStateHandle.navArgs<VerifyEmailNavArgs>() } returns VerifyEmailNavArgs(newEmail = email)
        }

        fun withUpdateEmailResult(result: UpdateEmailUseCase.Result) = apply {
            coEvery { updateEmail(any()) } returns result
        }

        fun arrange() = this to VerifyEmailViewModel(
            updateEmail,
            savedStateHandle
        )
    }
}
