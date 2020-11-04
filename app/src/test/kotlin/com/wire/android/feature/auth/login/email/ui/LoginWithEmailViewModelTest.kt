package com.wire.android.feature.auth.login.email.ui

import com.wire.android.R
import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.core.ui.dialog.ErrorMessage
import com.wire.android.core.ui.dialog.GeneralErrorMessage
import com.wire.android.core.ui.dialog.NetworkErrorMessage
import com.wire.android.feature.auth.login.email.usecase.LoginAuthenticationFailure
import com.wire.android.feature.auth.login.email.usecase.LoginTooFrequentFailure
import com.wire.android.feature.auth.login.email.usecase.LoginWithEmailUseCase
import com.wire.android.feature.auth.login.email.usecase.LoginWithEmailUseCaseParams
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.livedata.shouldBeUpdated
import com.wire.android.shared.user.email.EmailInvalid
import com.wire.android.shared.user.email.ValidateEmailParams
import com.wire.android.shared.user.email.ValidateEmailUseCase
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class LoginWithEmailViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @MockK
    private lateinit var validateEmailUseCase: ValidateEmailUseCase

    @MockK
    private lateinit var loginWithEmailUseCase: LoginWithEmailUseCase

    private lateinit var loginWithEmailViewModel: LoginWithEmailViewModel

    @Before
    fun setUp() {
        loginWithEmailViewModel = LoginWithEmailViewModel(
            coroutinesTestRule.dispatcherProvider, validateEmailUseCase, loginWithEmailUseCase
        )
    }

    @Test
    fun `given a valid email with no password, then sets continueEnabledLiveData to false`() {
        mockEmailValidation(true)

        loginWithEmailViewModel.validateEmail(TEST_EMAIL)

        loginWithEmailViewModel.continueEnabledLiveData shouldBeUpdated { it shouldBe false }
        coVerify(exactly = 1) { validateEmailUseCase.run(ValidateEmailParams(TEST_EMAIL)) }
    }

    @Test
    fun `given a valid email but an empty password, then sets continueEnabledLiveData to false`() {
        mockEmailValidation(true)

        loginWithEmailViewModel.validatePassword(TEST_EMPTY_PASSWORD)

        loginWithEmailViewModel.continueEnabledLiveData shouldBeUpdated { it shouldBe false }

        loginWithEmailViewModel.validateEmail(TEST_EMAIL)

        loginWithEmailViewModel.continueEnabledLiveData shouldBeUpdated { it shouldBe false }
        coVerify(exactly = 1) { validateEmailUseCase.run(ValidateEmailParams(TEST_EMAIL)) }
    }

    @Test
    fun `given a valid password with no email, then sets continueEnabledLiveData to false`() {
        loginWithEmailViewModel.validatePassword(TEST_VALID_PASSWORD)

        loginWithEmailViewModel.continueEnabledLiveData shouldBeUpdated { it shouldBe false }
        verify(exactly = 1) { validateEmailUseCase wasNot Called }
    }

    @Test
    fun `given a valid password but an invalid email, then sets continueEnabledLiveData to false`() {
        mockEmailValidation(false)

        loginWithEmailViewModel.validateEmail(TEST_EMAIL)
        loginWithEmailViewModel.continueEnabledLiveData shouldBeUpdated { it shouldBe false }

        loginWithEmailViewModel.validatePassword(TEST_VALID_PASSWORD)

        loginWithEmailViewModel.continueEnabledLiveData shouldBeUpdated { it shouldBe false }
        coVerify(exactly = 1) { validateEmailUseCase.run(ValidateEmailParams(TEST_EMAIL)) }
    }

    @Test
    fun `given an invalid email and an empty password, then sets continueEnabledLiveData to false`() {
        mockEmailValidation(false)

        loginWithEmailViewModel.validateEmail(TEST_EMAIL)
        loginWithEmailViewModel.continueEnabledLiveData shouldBeUpdated { it shouldBe false }

        loginWithEmailViewModel.validatePassword(TEST_EMPTY_PASSWORD)

        loginWithEmailViewModel.continueEnabledLiveData shouldBeUpdated { it shouldBe false }
        coVerify(exactly = 1) { validateEmailUseCase.run(ValidateEmailParams(TEST_EMAIL)) }
    }

    @Test
    fun `given a valid email and a non-empty password, then sets continueEnabledLiveData to true`() {
        mockEmailValidation(true)

        loginWithEmailViewModel.validateEmail(TEST_EMAIL)
        loginWithEmailViewModel.continueEnabledLiveData shouldBeUpdated { it shouldBe false }

        loginWithEmailViewModel.validatePassword(TEST_VALID_PASSWORD)

        loginWithEmailViewModel.continueEnabledLiveData shouldBeUpdated { it shouldBe true }
        coVerify(exactly = 1) { validateEmailUseCase.run(ValidateEmailParams(TEST_EMAIL)) }
    }

    @Test
    fun `given login is called, when loginWithEmailUseCase returns success, then sets success to loginResultLiveData`() {
        val params = LoginWithEmailUseCaseParams(email = TEST_EMAIL, password = TEST_VALID_PASSWORD)

        coEvery { loginWithEmailUseCase.run(params) } returns Either.Right(Unit)

        loginWithEmailViewModel.login(TEST_EMAIL, TEST_VALID_PASSWORD)

        loginWithEmailViewModel.loginResultLiveData shouldBeUpdated { it shouldSucceed {} }
        coVerify(exactly = 1) { loginWithEmailUseCase.run(params) }
    }

    @Test
    fun `given login is called, when use case returns NetworkConnection failure, then sets NetworkErrorMessage to loginResultLiveData`() =
        verifyLoginResultErrorMessage(NetworkConnection) { it shouldBe NetworkErrorMessage }

    @Test
    fun `given login is called, when use case returns LoginAuthenticationFailure, then sets proper error message to loginResultLiveData`() =
        verifyLoginResultErrorMessage(LoginAuthenticationFailure) {
            with(it) {
                title shouldBeEqualTo R.string.login_authentication_failure_title
                message shouldBeEqualTo R.string.login_authentication_failure_message
            }
        }

    @Test
    fun `given login is called, when use case returns LoginTooFrequentFailure, then sets proper error message to loginResultLiveData`() =
        verifyLoginResultErrorMessage(LoginTooFrequentFailure) {
            with(it) {
                title shouldBeEqualTo R.string.login_too_frequent_failure_title
                message shouldBeEqualTo R.string.login_too_frequent_failure_message
            }
        }

    @Test
    fun `given login is called, when loginWithEmailUseCase returns other failure, then sets GeneralErrorMessage to loginResultLiveData`() =
        verifyLoginResultErrorMessage(ServerError) { it shouldBe GeneralErrorMessage }

    private fun verifyLoginResultErrorMessage(failure: Failure, errorAssertion: (ErrorMessage) -> Unit) {
        val params = LoginWithEmailUseCaseParams(email = TEST_EMAIL, password = TEST_VALID_PASSWORD)
        coEvery { loginWithEmailUseCase.run(params) } returns Either.Left(failure)

        loginWithEmailViewModel.login(TEST_EMAIL, TEST_VALID_PASSWORD)

        loginWithEmailViewModel.loginResultLiveData shouldBeUpdated { it shouldFail { errorAssertion(it) } }
        coVerify(exactly = 1) { loginWithEmailUseCase.run(params) }
    }

    private fun mockEmailValidation(success: Boolean) =
        coEvery { validateEmailUseCase.run(ValidateEmailParams(TEST_EMAIL)) } returns
            if (success) Either.Right(Unit) else Either.Left(EmailInvalid)

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_EMPTY_PASSWORD = ""
        private const val TEST_VALID_PASSWORD = "password"
    }
}
