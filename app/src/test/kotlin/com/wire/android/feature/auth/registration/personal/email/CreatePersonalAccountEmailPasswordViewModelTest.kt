package com.wire.android.feature.auth.registration.personal.email

import com.wire.android.UnitTest
import com.wire.android.any
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.livedata.awaitValue
import com.wire.android.shared.user.password.InvalidPasswordFailure
import com.wire.android.shared.user.password.ValidatePasswordParams
import com.wire.android.shared.user.password.ValidatePasswordUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*

@ExperimentalCoroutinesApi
class CreatePersonalAccountEmailPasswordViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @Mock
    private lateinit var validatePasswordUseCase: ValidatePasswordUseCase

    private lateinit var viewModel: CreatePersonalAccountEmailPasswordViewModel

    @Before
    fun setUp() {
        viewModel = CreatePersonalAccountEmailPasswordViewModel(validatePasswordUseCase)
    }

    @Test
    fun `given a validatePasswordUseCase, when minPasswordLength() is called, then returns minLength constraint of the use case`() {
        val minLength = 4
        `when`(validatePasswordUseCase.minLength()).thenReturn(minLength)

        assertThat(viewModel.minPasswordLength()).isEqualTo(minLength)
        verify(validatePasswordUseCase).minLength()
    }

    @Test
    fun `given a password, when validatePassword is called, then calls validatePasswordUseCase with correct params`() {
        runBlocking {
            `when`(validatePasswordUseCase.run(any())).thenReturn(Either.Right(Unit))

            viewModel.validatePassword(TEST_PASSWORD)

            viewModel.continueEnabledLiveData.awaitValue()
            verify(validatePasswordUseCase).run(ValidatePasswordParams(TEST_PASSWORD))
        }
    }

    @Test
    fun `given validatePassword is called, when useCase returns success, then sets continueEnabledLiveData to true`() {
        runBlocking {
            `when`(validatePasswordUseCase.run(any())).thenReturn(Either.Right(Unit))

            viewModel.validatePassword(TEST_PASSWORD)

            assertThat(viewModel.continueEnabledLiveData.awaitValue()).isTrue()
        }
    }

    @Test
    fun `given validatePassword is called, when useCase returns InvalidPasswordFailure, then sets continueEnabledLiveData to false`() {
        runBlocking {
            `when`(validatePasswordUseCase.run(any())).thenReturn(Either.Left(InvalidPasswordFailure))

            viewModel.validatePassword(TEST_PASSWORD)

            assertThat(viewModel.continueEnabledLiveData.awaitValue()).isFalse()
        }
    }

    @Test
    fun `given validatePassword is called, when useCase returns general Failure, then sets continueEnabledLiveData to false`() {
        runBlocking {
            val failure = mock(Failure::class.java)
            `when`(validatePasswordUseCase.run(any())).thenReturn(Either.Left(failure))

            viewModel.validatePassword(TEST_PASSWORD)

            assertThat(viewModel.continueEnabledLiveData.awaitValue()).isFalse()
        }
    }

    companion object {
        private const val TEST_PASSWORD = "123ABCdef!*"
    }
}
