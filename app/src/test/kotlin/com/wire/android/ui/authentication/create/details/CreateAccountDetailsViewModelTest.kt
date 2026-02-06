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

package com.wire.android.ui.authentication.create.details

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.SavedStateHandle
import com.wire.android.assertions.shouldBeEqualTo
import com.wire.android.assertions.shouldBeInstanceOf
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.authentication.create.common.CreateAccountNavArgs
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.ValidatePasswordResult
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class, NavigationTestExtension::class)
class CreateAccountDetailsViewModelTest {

    @Test
    fun `given invalid password, when executing, then show error`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withValidatePasswordResult(ValidatePasswordResult.Invalid())
            .arrange()
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("password")

        viewModel.onDetailsContinue()
        advanceUntilIdle()

        viewModel.detailsState.error shouldBeInstanceOf
                CreateAccountDetailsViewState.DetailsError.TextFieldError.InvalidPasswordError::class
        viewModel.detailsState.success shouldBeEqualTo false
    }

    @Test
    fun `given passwords do not match, when executing, then show error`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withValidatePasswordResult(ValidatePasswordResult.Valid)
            .arrange()
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("password")
        viewModel.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("different-password")

        viewModel.onDetailsContinue()
        advanceUntilIdle()

        viewModel.detailsState.error shouldBeInstanceOf
                CreateAccountDetailsViewState.DetailsError.TextFieldError.PasswordsNotMatchingError::class
        viewModel.detailsState.success shouldBeEqualTo false
    }

    @Test
    fun `given valid passwords, when executing, then show success`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withValidatePasswordResult(ValidatePasswordResult.Valid)
            .arrange()
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("password")
        viewModel.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("password")

        viewModel.onDetailsContinue()
        advanceUntilIdle()

        viewModel.detailsState.error shouldBeInstanceOf CreateAccountDetailsViewState.DetailsError.None::class
        viewModel.detailsState.success shouldBeEqualTo true
    }

    private class Arrangement {
        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var validatePasswordUseCase: ValidatePasswordUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { savedStateHandle.navArgs<CreateAccountNavArgs>() } returns
                    CreateAccountNavArgs(CreateAccountFlowType.CreatePersonalAccount)
        }

        fun withValidatePasswordResult(result: ValidatePasswordResult) = apply {
            coEvery { validatePasswordUseCase(any()) } returns result
        }

        fun arrange() = this to CreateAccountDetailsViewModel(savedStateHandle, validatePasswordUseCase, ServerConfig.STAGING)
    }
}
