package com.wire.android.ui.home.settings.account.email

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.mockUri
import com.wire.android.framework.TestUser
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
import org.junit.jupiter.api.Assertions.assertTrue
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

            assertTrue {
                viewModel.state.flowState is ChangeEmailState.FlowState.Success
                        && (viewModel.state.flowState as ChangeEmailState.FlowState.Success).newEmail == newEmail
            }
        }

    @Test
    fun `given updateEmail returns success with NoChange, when updateEmail is called, then navigate back`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNewEmail("newEmail")
            .withUpdateEmailResult(UpdateEmailUseCase.Result.Success.NoChange)
            .arrange()

        viewModel.onSaveClicked()

        assertTrue { viewModel.state.flowState is ChangeEmailState.FlowState.NoChange }
    }

    @Test
    fun `given update EmailAlreadyInUse error is returned, when onSaveClicked is called, then show error state is updated`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNewEmail("newEmail")
            .withUpdateEmailResult(UpdateEmailUseCase.Result.Failure.EmailAlreadyInUse)
            .arrange()

        viewModel.onSaveClicked()

        assertTrue { viewModel.state.flowState is ChangeEmailState.FlowState.Error.TextFieldError.AlreadyInUse }
        coVerify(exactly = 1) { arrangement.updateEmail(any()) }
    }

    @Test
    fun `given update error is returned, when onSaveClicked is called, then show error state is updated`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNewEmail("newEmail")
            .withUpdateEmailResult(UpdateEmailUseCase.Result.Failure.GenericFailure(NetworkFailure.NoNetworkConnection(IOException())))
            .arrange()

        viewModel.onSaveClicked()

        assertTrue { viewModel.state.flowState is ChangeEmailState.FlowState.Error.TextFieldError.Generic }
        coVerify(exactly = 1) { arrangement.updateEmail(any()) }
    }

    @Test
    fun `given update EmailInvalid error is returned, when onSaveClicked is called, then show error state is updated`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNewEmail("newEmail")
            .withUpdateEmailResult(UpdateEmailUseCase.Result.Failure.InvalidEmail)
            .arrange()

        viewModel.onSaveClicked()

        assertTrue { viewModel.state.flowState is ChangeEmailState.FlowState.Error.TextFieldError.InvalidEmail }
        coVerify(exactly = 1) { arrangement.updateEmail(any()) }
    }

    private class Arrangement {

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
