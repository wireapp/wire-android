package com.wire.android.feature.auth.registration.personal.ui

import com.wire.android.R
import com.wire.android.UnitTest
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.core.ui.dialog.GeneralErrorMessage
import com.wire.android.core.ui.dialog.NetworkErrorMessage
import com.wire.android.feature.auth.activation.usecase.EmailBlacklisted
import com.wire.android.feature.auth.activation.usecase.EmailInUse
import com.wire.android.feature.auth.activation.usecase.SendEmailActivationCodeParams
import com.wire.android.feature.auth.activation.usecase.SendEmailActivationCodeUseCase
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.livedata.awaitValue
import com.wire.android.shared.user.email.EmailInvalid
import com.wire.android.shared.user.email.EmailTooShort
import com.wire.android.shared.user.email.ValidateEmailUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class CreatePersonalAccountEmailViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    private lateinit var emailViewModel: CreatePersonalAccountEmailViewModel

    @MockK
    private lateinit var sendActivationCodeUseCase: SendEmailActivationCodeUseCase

    @MockK
    private lateinit var validateEmailUseCase: ValidateEmailUseCase

    @Before
    fun setUp() {
        emailViewModel = CreatePersonalAccountEmailViewModel(
            coroutinesTestRule.dispatcherProvider, validateEmailUseCase, sendActivationCodeUseCase
        )
    }

    @Test
    fun `given validateEmail is called, when the validation succeeds then isValidEmail should be true`() {
        coEvery { validateEmailUseCase.run(any()) } returns Either.Right(Unit)

        coroutinesTestRule.runTest {
            emailViewModel.validateEmail(TEST_EMAIL)

            emailViewModel.isValidEmailLiveData.awaitValue() shouldBe true
        }
    }

    @Test
    fun `given validateEmail is called, when the validation fails with EmailTooShort error then isValidEmail should be false`() {
        coEvery { validateEmailUseCase.run(any()) } returns Either.Left(EmailTooShort)

        coroutinesTestRule.runTest {
            emailViewModel.validateEmail(TEST_EMAIL)

            emailViewModel.isValidEmailLiveData.awaitValue() shouldBe false
        }
    }

    @Test
    fun `given validateEmail is called, when the validation fails with EmailInvalid error then isValidEmail should be false`() {
        coEvery { validateEmailUseCase.run(any()) } returns Either.Left(EmailInvalid)

        coroutinesTestRule.runTest {
            emailViewModel.validateEmail(TEST_EMAIL)

            emailViewModel.isValidEmailLiveData.awaitValue() shouldBe false
        }
    }

    @Test
    fun `given sendActivation is called, then calls SendEmailActivationCodeUseCase`() {
        val params = SendEmailActivationCodeParams(TEST_EMAIL)
        coEvery { sendActivationCodeUseCase.run(params) } returns Either.Right(Unit)

        coroutinesTestRule.runTest {
            emailViewModel.sendActivationCode(TEST_EMAIL)

            emailViewModel.sendActivationCodeLiveData.awaitValue()
        }
        coVerify(exactly = 1) { sendActivationCodeUseCase.run(params) }
    }

    @Test
    fun `given sendActivation is called, when use case is successful, then sets email to sendActivationCodeLiveData`() {
        coEvery { sendActivationCodeUseCase.run(any()) } returns Either.Right(Unit)

        coroutinesTestRule.runTest {
            emailViewModel.sendActivationCode(TEST_EMAIL)

            emailViewModel.sendActivationCodeLiveData.awaitValue() shouldSucceed { it shouldBeEqualTo TEST_EMAIL }
        }
    }

    @Test
    fun `given sendActivation is called, when use case returns NetworkError, then sets NetworkErrorMsg to sendActivationCodeLiveData`() {
        coEvery { sendActivationCodeUseCase.run(any()) } returns Either.Left(NetworkConnection)

        coroutinesTestRule.runTest {
            emailViewModel.sendActivationCode(TEST_EMAIL)

            emailViewModel.sendActivationCodeLiveData.awaitValue() shouldFail { it shouldBe NetworkErrorMessage }
        }
    }

    @Test
    fun `given sendActivation is called, when use case returns EmailBlacklisted, then sets error message to sendActivationCodeLiveData`() {
        coEvery { sendActivationCodeUseCase.run(any()) } returns Either.Left(EmailBlacklisted)

        coroutinesTestRule.runTest {
            emailViewModel.sendActivationCode(TEST_EMAIL)

            emailViewModel.sendActivationCodeLiveData.awaitValue() shouldFail {
                it.message shouldBeEqualTo R.string.create_personal_account_with_email_email_blacklisted_error
            }
        }
    }

    @Test
    fun `given sendActivation is called, when use case returns EmailInUse, then sets error message to sendActivationCodeLiveData`() {
        coEvery { sendActivationCodeUseCase.run(any()) } returns Either.Left(EmailInUse)

        coroutinesTestRule.runTest {
            emailViewModel.sendActivationCode(TEST_EMAIL)

            emailViewModel.sendActivationCodeLiveData.awaitValue() shouldFail {
                it.message shouldBeEqualTo R.string.create_personal_account_with_email_email_in_use_error
            }
        }
    }

    @Test
    fun `given sendActivation is called, when use case returns other error, then sets GeneralErrorMessage to sendActivationCodeLiveData`() {
        coEvery { sendActivationCodeUseCase.run(any()) } returns Either.Left(ServerError)

        coroutinesTestRule.runTest {
            emailViewModel.sendActivationCode(TEST_EMAIL)

            emailViewModel.sendActivationCodeLiveData.awaitValue() shouldFail { it shouldBe GeneralErrorMessage }
        }
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
    }
}
