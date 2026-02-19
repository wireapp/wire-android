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

package com.wire.android.ui.authentication.create.email

import androidx.lifecycle.SavedStateHandle
import com.wire.android.assertions.shouldBeEqualTo
import com.wire.android.assertions.shouldBeInstanceOf
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.authentication.create.common.CreateAccountNavArgs
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.register.RequestActivationCodeResult
import com.wire.kalium.logic.feature.register.RequestActivationCodeUseCase
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
class CreateAccountEmailViewModelTest {

    @Test
    fun `given request code error, when terms accepted, then show error`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withRequestActivationCodeResult(RequestActivationCodeResult.Failure.InvalidEmail)
            .arrange()

        viewModel.onTermsAccept()
        advanceUntilIdle()

        viewModel.emailState.error shouldBeInstanceOf CreateAccountEmailViewState.EmailError.TextFieldError.InvalidEmailError::class
        viewModel.emailState.success shouldBeEqualTo false
    }

    @Test
    fun `given request code success, when terms accepted, then show success`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withRequestActivationCodeResult(RequestActivationCodeResult.Success)
            .arrange()

        viewModel.onTermsAccept()
        advanceUntilIdle()

        viewModel.emailState.error shouldBeInstanceOf CreateAccountEmailViewState.EmailError.None::class
        viewModel.emailState.success shouldBeEqualTo true
    }

    private class Arrangement {
        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var validateEmailUseCase: ValidateEmailUseCase

        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var autoVersionAuthScopeUseCase: AutoVersionAuthScopeUseCase

        @MockK
        lateinit var versionedAuthenticationScope: AuthenticationScope

        @MockK
        lateinit var requestActivationCodeUseCase: RequestActivationCodeUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { savedStateHandle.navArgs<CreateAccountNavArgs>() } returns
                    CreateAccountNavArgs(CreateAccountFlowType.CreatePersonalAccount)
            coEvery { coreLogic.versionedAuthenticationScope(any()) } returns autoVersionAuthScopeUseCase
            coEvery { autoVersionAuthScopeUseCase(any()) } returns
                    AutoVersionAuthScopeUseCase.Result.Success(versionedAuthenticationScope)
            coEvery { versionedAuthenticationScope.registerScope.requestActivationCode } returns requestActivationCodeUseCase
        }

        fun withRequestActivationCodeResult(result: RequestActivationCodeResult) = apply {
            coEvery { requestActivationCodeUseCase(any()) } returns result
        }

        fun arrange() = this to CreateAccountEmailViewModel(savedStateHandle, validateEmailUseCase, coreLogic, ServerConfig.STAGING)
    }
}
