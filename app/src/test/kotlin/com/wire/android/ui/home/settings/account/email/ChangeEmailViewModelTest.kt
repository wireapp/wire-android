package com.wire.android.ui.home.settings.account.email

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.mockUri
import com.wire.android.framework.TestUser
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.settings.account.email.updateEmail.ChangeEmailState
import com.wire.android.ui.home.settings.account.email.updateEmail.ChangeEmailViewModel
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.UpdateEmailUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okio.IOException
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ChangeEmailViewModelTest {

    @Test
    fun `given updateEmail returns success with VerificationEmailSent, when updateEmail is called, then navigate to VerifyEmail`() =
        runTest {
            val newEmail = "newEmail"
            val (arrangement, viewModel) = Arrangement()
                .withNewEmail(newEmail)
                .withUpdateEmailResult(UpdateEmailUseCase.Result.Success.VerificationEmailSent)
                .arrange()

            viewModel.onSaveClicked()

            coVerify(exactly = 1) {
                arrangement.navigationManager.navigate(
                    NavigationCommand(
                        NavigationItem.VerifyEmailAddress.getRouteWithArgs(listOf(newEmail)),
                        BackStackMode.REMOVE_CURRENT
                    )
                )
            }
        }

    @Test
    fun `given updateEmail returns success with NoChange, when updateEmail is called, then navigate back`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNewEmail("newEmail")
            .withUpdateEmailResult(UpdateEmailUseCase.Result.Success.NoChange)
            .arrange()

        viewModel.onSaveClicked()

        coVerify(exactly = 1) {
            arrangement.navigationManager.navigateBack()
        }
    }

    @Test
    fun `given update EmailAlreadyInUse error is returned, when onSaveClicked is called, then show error state is updated`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNewEmail("newEmail")
            .withUpdateEmailResult(UpdateEmailUseCase.Result.Failure.EmailAlreadyInUse)
            .arrange()

        viewModel.onSaveClicked()

        assertEquals(ChangeEmailState.EmailError.TextFieldError.AlreadyInUse, viewModel.state.error)

        coVerify(exactly = 0) { arrangement.navigationManager.navigate(any()) }
        coVerify(exactly = 1) { arrangement.updateEmail(any()) }
    }

    @Test
    fun `given update error is returned, when onSaveClicked is called, then show error state is updated`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNewEmail("newEmail")
            .withUpdateEmailResult(UpdateEmailUseCase.Result.Failure.GenericFailure(NetworkFailure.NoNetworkConnection(IOException())))
            .arrange()

        viewModel.onSaveClicked()

        assertEquals(ChangeEmailState.EmailError.TextFieldError.Generic, viewModel.state.error)

        coVerify(exactly = 0) { arrangement.navigationManager.navigate(any()) }
        coVerify(exactly = 1) { arrangement.updateEmail(any()) }
    }

    @Test
    fun `given update EmailInvalid error is returned, when onSaveClicked is called, then show error state is updated`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNewEmail("newEmail")
            .withUpdateEmailResult(UpdateEmailUseCase.Result.Failure.InvalidEmail)
            .arrange()

        viewModel.onSaveClicked()

        assertEquals(ChangeEmailState.EmailError.TextFieldError.InvalidEmail, viewModel.state.error)

        coVerify(exactly = 0) { arrangement.navigationManager.navigate(any()) }
        coVerify(exactly = 1) { arrangement.updateEmail(any()) }
    }

    private class Arrangement {

        @MockK
        lateinit var navigationManager: NavigationManager

        @MockK
        lateinit var updateEmail: UpdateEmailUseCase

        @MockK
        lateinit var self: GetSelfUserUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockUri()

            coEvery { self() } returns flowOf(TestUser.SELF_USER)
        }

        private val viewModel = ChangeEmailViewModel(
            navigationManager,
            updateEmail,
            self
        )

        fun withNewEmail(newEmail: String) = apply {
            viewModel.state = viewModel.state.copy(email = TextFieldValue(newEmail))
        }

        fun withUpdateEmailResult(result: UpdateEmailUseCase.Result) = apply {
            coEvery { updateEmail(any()) } returns result
        }

        fun arrange() = this to viewModel
    }
}
