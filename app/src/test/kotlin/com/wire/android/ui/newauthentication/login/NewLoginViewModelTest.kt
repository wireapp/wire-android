package com.wire.android.ui.newauthentication.login

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.ui.authentication.login.LoginViewModelExtension
import com.wire.android.ui.authentication.login.sso.LoginSSOViewModelExtension
import com.wire.android.ui.newauthentication.login.ValidateEmailOrSSOCodeUseCase.Result.ValidEmail
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class NewLoginViewModelTest {
    private val dispatchers = TestDispatcherProvider()

    @Test
    fun `given onLoginStarted is called, when valid input is SSO, then proceed to SSO flow`() = runTest(dispatchers.main()) {
        val (arrangement, sut) = Arrangement()
            .withEmailOrSSOCodeValidatorReturning(ValidateEmailOrSSOCodeUseCase.Result.ValidSSOCode)
            .arrange()

        sut.onLoginStarted(action = arrangement.onSuccess)
        advanceUntilIdle()

        coVerify { arrangement.loginSSOViewModelExtension.initiateSSO(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `given onLoginStarted is called, when invalid input, then update error state`() = runTest(dispatchers.main()) {
        val (arrangement, sut) = Arrangement()
            .withEmailOrSSOCodeValidatorReturning(ValidateEmailOrSSOCodeUseCase.Result.InvalidInput)
            .arrange()

        sut.onLoginStarted(action = arrangement.onSuccess)

        verify(exactly = 0) { arrangement.onSuccess(any()) }
        assertEquals(DomainCheckupState.Error.TextFieldError.InvalidValue, sut.state.flowState)
    }

    inner class Arrangement {
        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var loginViewModelExtension: LoginViewModelExtension

        @MockK
        lateinit var addAuthenticatedUserUseCase: AddAuthenticatedUserUseCase

        @MockK
        lateinit var loginSSOViewModelExtension: LoginSSOViewModelExtension

        @MockK
        private lateinit var savedStateHandle: SavedStateHandle

        @MockK
        private lateinit var clientScopeProviderFactory: ClientScopeProvider.Factory

        @MockK
        private lateinit var userDataStoreProvider: UserDataStoreProvider

        val validateEmailOrSSOCodeUseCase: ValidateEmailOrSSOCodeUseCase = mockk()

        val onSuccess: (NewLoginAction) -> Unit = mockk()

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { savedStateHandle.get<String>(any()) } returns null
            every { savedStateHandle[any()] = any<String>() } returns Unit
            every { onSuccess(any()) } returns Unit
        }

        fun withEmailOrSSOCodeValidatorReturning(result: ValidateEmailOrSSOCodeUseCase.Result = ValidEmail) =
            apply {
                every { validateEmailOrSSOCodeUseCase(any()) } returns result
            }

        fun arrange() = this to NewLoginViewModel(
            validateEmailOrSSOCodeUseCase,
            coreLogic,
            savedStateHandle,
            addAuthenticatedUserUseCase,
            clientScopeProviderFactory,
            userDataStoreProvider,
            loginViewModelExtension,
            loginSSOViewModelExtension,
            dispatchers
        )
    }
}
