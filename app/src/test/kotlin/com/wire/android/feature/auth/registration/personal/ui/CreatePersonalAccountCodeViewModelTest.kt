package com.wire.android.feature.auth.registration.personal.ui

import com.wire.android.R
import com.wire.android.UnitTest
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.core.ui.dialog.GeneralErrorMessage
import com.wire.android.core.ui.dialog.NetworkErrorMessage
import com.wire.android.feature.auth.registration.personal.usecase.ActivateEmailUseCase
import com.wire.android.feature.auth.registration.personal.usecase.InvalidEmailCode
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.livedata.shouldBeUpdated
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class CreatePersonalAccountCodeViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @MockK
    private lateinit var activateEmailUseCase: ActivateEmailUseCase

    private lateinit var codeViewModel: CreatePersonalAccountCodeViewModel

    @Before
    fun setUp() {
        codeViewModel = CreatePersonalAccountCodeViewModel(coroutinesTestRule.dispatcherProvider, activateEmailUseCase)
    }

    @Test
    fun `given activateEmail is called, when activateEmailUseCase returns success, then notifies success to activateEmailLiveData`() {
        coEvery { activateEmailUseCase.run(any()) } returns Either.Right(Unit)

        codeViewModel.activateEmail(TEST_EMAIL, TEST_CODE)

        codeViewModel.activateEmailLiveData shouldBeUpdated { it shouldSucceed { it shouldBeEqualTo TEST_CODE } }
    }

    @Test
    fun `given activateEmail is called, when activateEmailUseCase returns InvalidEmailCode, sets ErrorMessage to activateEmailLiveData`() {
        //TODO: separate feature failures
        coEvery { activateEmailUseCase.run(any()) } returns Either.Left(InvalidEmailCode)

        codeViewModel.activateEmail(TEST_EMAIL, TEST_CODE)

        codeViewModel.activateEmailLiveData shouldBeUpdated { result ->
            result shouldFail { it.message shouldBeEqualTo R.string.create_personal_account_code_invalid_code_error }
        }
    }

    @Test
    fun `given activateEmail is called, when activateEmailUseCase returns NetworkConnection, sets NetworkError to activateEmailLiveData`() {
        coEvery { activateEmailUseCase.run(any()) } returns Either.Left(NetworkConnection)

        codeViewModel.activateEmail(TEST_EMAIL, TEST_CODE)

        codeViewModel.activateEmailLiveData shouldBeUpdated { result ->
            result shouldFail { it shouldBe NetworkErrorMessage }
        }
    }

    @Test
    fun `given activateEmail is called, when activateEmailUseCase returns other error, sets GeneralErrorMsg to activateEmailLiveData`() {
        coEvery { activateEmailUseCase.run(any()) } returns Either.Left(ServerError)

        codeViewModel.activateEmail(TEST_EMAIL, TEST_CODE)

        codeViewModel.activateEmailLiveData shouldBeUpdated { result ->
            result shouldFail { it shouldBe GeneralErrorMessage }
        }
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_CODE = "123456"
    }
}
