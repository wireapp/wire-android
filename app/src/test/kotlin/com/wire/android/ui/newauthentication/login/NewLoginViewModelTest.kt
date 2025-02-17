package com.wire.android.ui.newauthentication.login

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.ui.newauthentication.login.ValidateEmailOrSSOCodeUseCase.Result.ValidEmail
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.auth.LoginRedirectPath
import io.mockk.MockKAnnotations
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

    @Test
    fun `given onLoginStarted is called, when valid input, then proceed`() = runTest {
        val (arrangement, sut) = Arrangement()
            .withEmailOrSSOCodeValidatorReturning(ValidateEmailOrSSOCodeUseCase.Result.ValidSSOCode)
            .arrange()

        sut.onLoginStarted(onSuccess = arrangement.onSuccess)
        advanceUntilIdle()

        verify { arrangement.onSuccess(any()) }
        assertEquals(DomainCheckupState.Default, sut.loginEmailSSOState.flowState)
    }

    @Test
    fun `given onLoginStarted is called, when invalid input, then update error state`() = runTest {
        val (arrangement, sut) = Arrangement()
            .withEmailOrSSOCodeValidatorReturning(ValidateEmailOrSSOCodeUseCase.Result.InvalidInput)
            .arrange()

        sut.onLoginStarted(onSuccess = arrangement.onSuccess)

        verify(exactly = 0) { arrangement.onSuccess(any()) }
        assertEquals(DomainCheckupState.Error.TextFieldError.InvalidValue, sut.loginEmailSSOState.flowState)
    }

    private class Arrangement {
        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        private lateinit var savedStateHandle: SavedStateHandle
        val validateEmailOrSSOCodeUseCase: ValidateEmailOrSSOCodeUseCase = mockk()

        val onSuccess: (LoginRedirectPath) -> Unit = mockk()

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
        )
    }
}
