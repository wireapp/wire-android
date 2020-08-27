package com.wire.android.feature.auth.login.email.ui

import com.wire.android.UnitTest
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.login.email.usecase.LoginWithEmailUseCase
import com.wire.android.feature.auth.login.email.usecase.LoginWithEmailUseCaseParams
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import com.wire.android.framework.livedata.awaitValue
import com.wire.android.shared.user.email.EmailInvalid
import com.wire.android.shared.user.email.ValidateEmailParams
import com.wire.android.shared.user.email.ValidateEmailUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*

@ExperimentalCoroutinesApi
class LoginWithEmailViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @Mock
    private lateinit var validateEmailUseCase: ValidateEmailUseCase

    @Mock
    private lateinit var loginWithEmailUseCase: LoginWithEmailUseCase

    private lateinit var loginWithEmailViewModel: LoginWithEmailViewModel

    @Before
    fun setUp() {
        loginWithEmailViewModel = LoginWithEmailViewModel(validateEmailUseCase, loginWithEmailUseCase)
    }

    @Test
    fun `given a valid email with no password, then sets continueEnabledLiveData to false`() {
        runBlocking {
            mockEmailValidation(true)

            loginWithEmailViewModel.validateEmail(TEST_EMAIL)

            assertThat(loginWithEmailViewModel.continueEnabledLiveData.awaitValue()).isFalse()
            verify(validateEmailUseCase).run(ValidateEmailParams(TEST_EMAIL))
        }
    }

    @Test
    fun `given a valid email but an empty password, then sets continueEnabledLiveData to false`() {
        runBlocking {
            mockEmailValidation(true)

            loginWithEmailViewModel.validatePassword(TEST_EMPTY_PASSWORD)
            assertThat(loginWithEmailViewModel.continueEnabledLiveData.awaitValue()).isFalse()

            loginWithEmailViewModel.validateEmail(TEST_EMAIL)

            assertThat(loginWithEmailViewModel.continueEnabledLiveData.awaitValue()).isFalse()
            verify(validateEmailUseCase).run(ValidateEmailParams(TEST_EMAIL))
        }
    }

    @Test
    fun `given a valid password with no email, then sets continueEnabledLiveData to false`() {
        runBlocking {
            loginWithEmailViewModel.validatePassword(TEST_VALID_PASSWORD)

            assertThat(loginWithEmailViewModel.continueEnabledLiveData.awaitValue()).isFalse()
            verifyNoInteractions(validateEmailUseCase)
        }
    }

    @Test
    fun `given a valid password but an invalid email, then sets continueEnabledLiveData to false`() {
        runBlocking {
            mockEmailValidation(false)

            loginWithEmailViewModel.validateEmail(TEST_EMAIL)
            assertThat(loginWithEmailViewModel.continueEnabledLiveData.awaitValue()).isFalse()

            loginWithEmailViewModel.validatePassword(TEST_VALID_PASSWORD)

            assertThat(loginWithEmailViewModel.continueEnabledLiveData.awaitValue()).isFalse()
            verify(validateEmailUseCase).run(ValidateEmailParams(TEST_EMAIL))
        }
    }

    @Test
    fun `given an invalid email and an empty password, then sets continueEnabledLiveData to false`() {
        runBlocking {
            mockEmailValidation(false)

            loginWithEmailViewModel.validateEmail(TEST_EMAIL)
            assertThat(loginWithEmailViewModel.continueEnabledLiveData.awaitValue()).isFalse()

            loginWithEmailViewModel.validatePassword(TEST_EMPTY_PASSWORD)

            assertThat(loginWithEmailViewModel.continueEnabledLiveData.awaitValue()).isFalse()
            verify(validateEmailUseCase).run(ValidateEmailParams(TEST_EMAIL))
        }
    }

    @Test
    fun `given a valid email and a non-empty password, then sets continueEnabledLiveData to true`() {
        runBlocking {
            mockEmailValidation(true)

            loginWithEmailViewModel.validateEmail(TEST_EMAIL)
            assertThat(loginWithEmailViewModel.continueEnabledLiveData.awaitValue()).isFalse()

            loginWithEmailViewModel.validatePassword(TEST_VALID_PASSWORD)

            assertThat(loginWithEmailViewModel.continueEnabledLiveData.awaitValue()).isTrue()
            verify(validateEmailUseCase).run(ValidateEmailParams(TEST_EMAIL))
        }
    }

    @Test
    fun `given login is called, when loginWithEmailUseCase returns success, then sets success to loginResultLiveData`() {
        runBlocking {
            val params = LoginWithEmailUseCaseParams(email = TEST_EMAIL, password = TEST_VALID_PASSWORD)
            `when`(loginWithEmailUseCase.run(params)).thenReturn(Either.Right(Unit))

            loginWithEmailViewModel.login(TEST_EMAIL, TEST_VALID_PASSWORD)

            loginWithEmailViewModel.loginResultLiveData.awaitValue().assertRight()
            verify(loginWithEmailUseCase).run(params)
        }
    }

    @Test
    fun `given login is called, when loginWithEmailUseCase returns error, then sets that error to loginResultLiveData`() {
        runBlocking {
            val params = LoginWithEmailUseCaseParams(email = TEST_EMAIL, password = TEST_VALID_PASSWORD)
            `when`(loginWithEmailUseCase.run(params)).thenReturn(Either.Left(ServerError))

            loginWithEmailViewModel.login(TEST_EMAIL, TEST_VALID_PASSWORD)

            loginWithEmailViewModel.loginResultLiveData.awaitValue().assertLeft {
                assertThat(it).isEqualTo(ServerError)
            }
            verify(loginWithEmailUseCase).run(params)
        }
    }

    private suspend fun mockEmailValidation(success: Boolean) {
        `when`(validateEmailUseCase.run(ValidateEmailParams(TEST_EMAIL))).thenReturn(
            if (success) Either.Right(Unit) else Either.Left(EmailInvalid)
        )
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_EMPTY_PASSWORD = ""
        private const val TEST_VALID_PASSWORD = "password"
    }
}
