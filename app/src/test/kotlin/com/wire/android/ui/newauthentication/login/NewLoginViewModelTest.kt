package com.wire.android.ui.newauthentication.login

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.util.newServerConfig
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class NewLoginViewModelTest {

    @Test
    fun `given onLoginStarted is called, when valid input, then proceed`() = runTest {
        val (arrangement, sut) = Arrangement()
            .withEmailOrSSOCodeValidatorReturning(true)
            .arrange()

        sut.onLoginStarted(onSuccess = arrangement.onSuccess)

        assertEquals(LoginState.Loading, sut.loginState.flowState)
        advanceUntilIdle()

        verify { arrangement.onSuccess() }
        assertEquals(LoginState.Default, sut.loginState.flowState)
    }

    @Test
    fun `given onLoginStarted is called, when invalid input, then update error state`() = runTest {
        val (arrangement, sut) = Arrangement()
            .withEmailOrSSOCodeValidatorReturning(false)
            .arrange()

        sut.onLoginStarted(onSuccess = arrangement.onSuccess)

        assertEquals(LoginState.Error.TextFieldError.InvalidValue, sut.loginState.flowState)
        verify(exactly = 0) { arrangement.onSuccess() }
    }

    private class Arrangement {
        @MockK
        private lateinit var savedStateHandle: SavedStateHandle
        val authServerConfigProvider: AuthServerConfigProvider = mockk()
        val validateEmailOrSSOCodeUseCase: ValidateEmailOrSSOCodeUseCase = mockk()

        val onSuccess: () -> Unit = mockk()

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { authServerConfigProvider.authServer } returns MutableStateFlow((newServerConfig(1).links))
            every { savedStateHandle.get<String>(any()) } returns null
            every { savedStateHandle.set(any(), any<String>()) } returns Unit
            every { onSuccess() } returns Unit
        }

        fun withEmailOrSSOCodeValidatorReturning(result: Boolean = true) = apply {
            every { validateEmailOrSSOCodeUseCase(any()) } returns result
        }

        fun arrange() = this to NewLoginViewModel(
            authServerConfigProvider,
            validateEmailOrSSOCodeUseCase,
            savedStateHandle
        )
    }
}
