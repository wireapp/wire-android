package com.wire.android.ui.registration.details

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.android.ui.authentication.create.common.UserRegistrationInfo
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.ValidatePasswordResult
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.register.RequestActivationCodeResult
import com.wire.kalium.logic.feature.register.RequestActivationCodeUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class, NavigationTestExtension::class)
class CreateAccountDataDetailViewModelTest {

    @Test
    fun `given invalid password, when executing, then show error`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withValidatePasswordResult(ValidatePasswordResult.Invalid())
            .arrange()
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("password")

        viewModel.onDetailsContinue()
        advanceUntilIdle()

        assertEquals(false, viewModel.detailsState.success)
        coVerify(exactly = 0) { arrangement.validateEmailUseCase(any()) }
    }

    fun `given passwords do not match, when executing, then show error`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withValidatePasswordResult(ValidatePasswordResult.Valid)
            .arrange()
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("password")
        viewModel.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("different-password")

        viewModel.onDetailsContinue()
        advanceUntilIdle()

        assertInstanceOf<CreateAccountDataDetailViewState.DetailsError.PasswordError.PasswordsNotMatchingError>(
            viewModel.detailsState.error
        )
        assertEquals(false, viewModel.detailsState.success)
        coVerify(exactly = 0) { arrangement.validateEmailUseCase(any()) }
        verify(exactly = 1) {
            arrangement.anonymousAnalyticsManager.sendEvent(eq(AnalyticsEvent.RegistrationPersonalAccount.AccountSetup(true)))
        }
    }

    @Test
    fun `given passwords do not match, when executing and fixed, then track error`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withValidatePasswordResult(ValidatePasswordResult.Valid)
            .withValidateEmailResult(true)
            .withActivationCodeResult(RequestActivationCodeResult.Success)
            .arrange()
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("password")
        viewModel.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("different-password")

        viewModel.onDetailsContinue()
        advanceUntilIdle()

        // fix the password then continue
        viewModel.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("password")
        viewModel.onDetailsContinue()
        advanceUntilIdle()

        assertInstanceOf<CreateAccountDataDetailViewState.DetailsError.None>(viewModel.detailsState.error)
        assertEquals(false, viewModel.detailsState.success)
        coVerify(exactly = 1) { arrangement.validateEmailUseCase(any()) }
        verify(exactly = 1) {
            arrangement.anonymousAnalyticsManager.sendEvent(eq(AnalyticsEvent.RegistrationPersonalAccount.TermsOfUseDialog))
        }
        verify(exactly = 1) {
            arrangement.anonymousAnalyticsManager.sendEvent(eq(AnalyticsEvent.RegistrationPersonalAccount.AccountSetup(true)))
        }
        assertInstanceOf<CreateAccountDataDetailViewState.DetailsError.None>(viewModel.detailsState.error)
        assertEquals(true, viewModel.detailsState.termsDialogVisible)
    }

    @Test
    fun `given valid passwords, when executing, then validate email and request terms of service acceptance`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withValidatePasswordResult(ValidatePasswordResult.Valid)
            .withValidateEmailResult(true)
            .withActivationCodeResult(RequestActivationCodeResult.Success)
            .arrange()
        viewModel.passwordTextState.setTextAndPlaceCursorAtEnd("password")
        viewModel.confirmPasswordTextState.setTextAndPlaceCursorAtEnd("password")

        viewModel.onDetailsContinue()
        advanceUntilIdle()

        assertInstanceOf<CreateAccountDataDetailViewState.DetailsError.None>(viewModel.detailsState.error)
        assertEquals(false, viewModel.detailsState.success)
        coVerify(exactly = 1) { arrangement.validateEmailUseCase(any()) }
        verify(exactly = 1) {
            arrangement.anonymousAnalyticsManager.sendEvent(eq(AnalyticsEvent.RegistrationPersonalAccount.TermsOfUseDialog))
        }
        verify(exactly = 1) {
            arrangement.anonymousAnalyticsManager.sendEvent(eq(AnalyticsEvent.RegistrationPersonalAccount.AccountSetup(false)))
        }
        assertInstanceOf<CreateAccountDataDetailViewState.DetailsError.None>(viewModel.detailsState.error)
        assertEquals(true, viewModel.detailsState.termsDialogVisible)
    }

    @Test
    fun `given request code error, when terms accepted, then show error`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withValidateEmailResult(true)
            .withValidatePasswordResult(ValidatePasswordResult.Valid)
            .withActivationCodeResult(RequestActivationCodeResult.Failure.InvalidEmail)
            .arrange()

        viewModel.onDetailsContinue()
        viewModel.onTermsAccept()
        advanceUntilIdle()

        assertInstanceOf<CreateAccountDataDetailViewState.DetailsError.EmailFieldError.InvalidEmailError>(viewModel.detailsState.error)
        assertEquals(false, viewModel.detailsState.success)
    }

    @Test
    fun `given request code success, when terms accepted, then show success`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withValidateEmailResult(true)
            .withValidatePasswordResult(ValidatePasswordResult.Valid)
            .withActivationCodeResult(RequestActivationCodeResult.Success)
            .arrange()

        viewModel.onDetailsContinue()
        viewModel.onTermsAccept()
        advanceUntilIdle()

        assertInstanceOf<CreateAccountDataDetailViewState.DetailsError.None>(viewModel.detailsState.error)
        assertEquals(true, viewModel.detailsState.success)
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
        lateinit var authenticationScope: AuthenticationScope

        @MockK
        lateinit var requestActivationCodeUseCase: RequestActivationCodeUseCase

        @MockK
        lateinit var validatePasswordUseCase: ValidatePasswordUseCase

        @MockK
        lateinit var anonymousAnalyticsManager: AnonymousAnalyticsManager

        @MockK
        lateinit var globalDataStore: GlobalDataStore

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { savedStateHandle.navArgs<CreateAccountDataNavArgs>() } returns
                    CreateAccountDataNavArgs(userRegistrationInfo = UserRegistrationInfo())

            coEvery { coreLogic.versionedAuthenticationScope(any()) } returns autoVersionAuthScopeUseCase
            coEvery {
                autoVersionAuthScopeUseCase(null)
            } returns AutoVersionAuthScopeUseCase.Result.Success(
                authenticationScope
            )
            coEvery { autoVersionAuthScopeUseCase(any()) } returns
                    AutoVersionAuthScopeUseCase.Result.Success(authenticationScope)
            coEvery { authenticationScope.registerScope.requestActivationCode } returns requestActivationCodeUseCase
            coEvery { anonymousAnalyticsManager.sendEvent(any()) } returns Unit
        }

        fun withActivationCodeResult(result: RequestActivationCodeResult) = apply {
            coEvery { requestActivationCodeUseCase(any()) } returns result
        }

        fun withValidateEmailResult(result: Boolean) = apply {
            coEvery { validateEmailUseCase(any()) } returns result
        }

        fun withValidatePasswordResult(result: ValidatePasswordResult) = apply {
            coEvery { validatePasswordUseCase(any()) } returns result
        }

        fun arrange() = this to CreateAccountDataDetailViewModel(
            savedStateHandle = savedStateHandle,
            validateEmail = validateEmailUseCase,
            validatePassword = validatePasswordUseCase,
            coreLogic = coreLogic,
            anonymousAnalyticsManager = anonymousAnalyticsManager,
            globalDataStore = globalDataStore
        )
    }
}
