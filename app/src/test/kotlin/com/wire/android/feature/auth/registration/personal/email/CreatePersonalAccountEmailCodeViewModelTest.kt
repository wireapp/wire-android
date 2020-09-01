package com.wire.android.feature.auth.registration.personal.email

import com.wire.android.R
import com.wire.android.UnitTest
import com.wire.android.any
import com.wire.android.core.exception.NetworkConnection
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.core.ui.dialog.GeneralErrorMessage
import com.wire.android.core.ui.dialog.NetworkErrorMessage
import com.wire.android.feature.auth.registration.personal.email.usecase.ActivateEmailUseCase
import com.wire.android.feature.auth.registration.personal.email.usecase.InvalidEmailCode
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import com.wire.android.framework.livedata.awaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

@ExperimentalCoroutinesApi
class CreatePersonalAccountEmailCodeViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @Mock
    private lateinit var activateEmailUseCase: ActivateEmailUseCase

    private lateinit var emailCodeViewModel: CreatePersonalAccountEmailCodeViewModel

    @Before
    fun setUp() {
        emailCodeViewModel = CreatePersonalAccountEmailCodeViewModel(coroutinesTestRule.dispatcherProvider, activateEmailUseCase)
    }

    @Test
    fun `given activateEmail is called, when activateEmailUseCase returns success, then notifies success to activateEmailLiveData`() {
        coroutinesTestRule.runTest {
            `when`(activateEmailUseCase.run(any())).thenReturn(Either.Right(Unit))

            emailCodeViewModel.activateEmail(TEST_EMAIL, TEST_CODE)

            emailCodeViewModel.activateEmailLiveData.awaitValue().assertRight {
                assertThat(it).isEqualTo(TEST_CODE)
            }
        }
    }

    @Test
    fun `given activateEmail is called, when activateEmailUseCase returns InvalidEmailCode, sets ErrorMessage to activateEmailLiveData`() {
        //TODO: separate feature failures
        coroutinesTestRule.runTest {
            `when`(activateEmailUseCase.run(any())).thenReturn(Either.Left(InvalidEmailCode))

            emailCodeViewModel.activateEmail(TEST_EMAIL, TEST_CODE)

            emailCodeViewModel.activateEmailLiveData.awaitValue().assertLeft {
                assertThat(it.message).isEqualTo(R.string.create_personal_account_email_code_invalid_code_error)
            }
        }
    }

    @Test
    fun `given activateEmail is called, when activateEmailUseCase returns NetworkConnection, sets NetworkError to activateEmailLiveData`() {
        coroutinesTestRule.runTest {
            `when`(activateEmailUseCase.run(any())).thenReturn(Either.Left(NetworkConnection))

            emailCodeViewModel.activateEmail(TEST_EMAIL, TEST_CODE)

            emailCodeViewModel.activateEmailLiveData.awaitValue().assertLeft {
                assertThat(it).isEqualTo(NetworkErrorMessage)
            }
        }
    }

    @Test
    fun `given activateEmail is called, when activateEmailUseCase returns other error, sets GeneralErrorMsg to activateEmailLiveData`() {
        coroutinesTestRule.runTest {
            `when`(activateEmailUseCase.run(any())).thenReturn(Either.Left(ServerError))

            emailCodeViewModel.activateEmail(TEST_EMAIL, TEST_CODE)

            emailCodeViewModel.activateEmailLiveData.awaitValue().assertLeft {
                assertThat(it).isEqualTo(GeneralErrorMessage)
            }
        }
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_CODE = "123456"
    }
}
