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
import io.mockk.verify
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

            viewModel.onSaveClicked(arrangement.onSuccess, arrangement.onNoChange)

            verify(exactly = 1) { arrangement.onSuccess(eq(newEmail)) }
            verify(exactly = 0) { arrangement.onNoChange() }
        }

    @Test
    fun `given updateEmail returns success with NoChange, when updateEmail is called, then navigate back`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNewEmail("newEmail")
            .withUpdateEmailResult(UpdateEmailUseCase.Result.Success.NoChange)
            .arrange()

        viewModel.onSaveClicked(arrangement.onSuccess, arrangement.onNoChange)

        verify(exactly = 0) { arrangement.onSuccess(any()) }
        verify(exactly = 1) { arrangement.onNoChange() }
    }

    @Test
    fun `given update EmailAlreadyInUse error is returned, when onSaveClicked is called, then show error state is updated`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNewEmail("newEmail")
            .withUpdateEmailResult(UpdateEmailUseCase.Result.Failure.EmailAlreadyInUse)
            .arrange()

        viewModel.onSaveClicked(arrangement.onSuccess, arrangement.onNoChange)

        assertEquals(ChangeEmailState.EmailError.TextFieldError.AlreadyInUse, viewModel.state.error)

        coVerify(exactly = 0) {
            arrangement.onSuccess(any())
            arrangement.onNoChange()
        }
        coVerify(exactly = 1) { arrangement.updateEmail(any()) }
    }

    @Test
    fun `given update error is returned, when onSaveClicked is called, then show error state is updated`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNewEmail("newEmail")
            .withUpdateEmailResult(UpdateEmailUseCase.Result.Failure.GenericFailure(NetworkFailure.NoNetworkConnection(IOException())))
            .arrange()

        viewModel.onSaveClicked(arrangement.onSuccess, arrangement.onNoChange)

        assertEquals(ChangeEmailState.EmailError.TextFieldError.Generic, viewModel.state.error)

        coVerify(exactly = 0) {
            arrangement.onSuccess(any())
            arrangement.onNoChange()
        }
        coVerify(exactly = 1) { arrangement.updateEmail(any()) }
    }

    @Test
    fun `given update EmailInvalid error is returned, when onSaveClicked is called, then show error state is updated`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNewEmail("newEmail")
            .withUpdateEmailResult(UpdateEmailUseCase.Result.Failure.InvalidEmail)
            .arrange()

        viewModel.onSaveClicked(arrangement.onSuccess, arrangement.onNoChange)

        assertEquals(ChangeEmailState.EmailError.TextFieldError.InvalidEmail, viewModel.state.error)

        coVerify(exactly = 0) {
            arrangement.onSuccess(any())
            arrangement.onNoChange()
        }
        coVerify(exactly = 1) { arrangement.updateEmail(any()) }
    }

    private class Arrangement {

        @MockK
        lateinit var updateEmail: UpdateEmailUseCase

        @MockK
        lateinit var self: GetSelfUserUseCase

        @MockK(relaxed = true)
        lateinit var onSuccess: (email: String) -> Unit
        @MockK(relaxed = true)
        lateinit var onNoChange: () -> Unit

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
