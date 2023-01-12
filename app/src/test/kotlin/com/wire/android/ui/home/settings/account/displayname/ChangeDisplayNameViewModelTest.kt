package com.wire.android.ui.home.settings.account.displayname

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestUser
import com.wire.android.navigation.EXTRA_SETTINGS_DISPLAY_NAME_CHANGED
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.feature.user.DisplayNameUpdateResult
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.UpdateDisplayNameUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ChangeDisplayNameViewModelTest {

    @Test
    fun `when saving the new display name, and ok then should navigate back indicating EXTRA_SETTINGS_DISPLAY_NAME_CHANGED success`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .withUserSaveNameResult(DisplayNameUpdateResult.Success)
                .arrange()

            viewModel.saveDisplayName()

            coVerify {
                arrangement.navigationManager.navigateBack(eq(mapOf(EXTRA_SETTINGS_DISPLAY_NAME_CHANGED to true)))
            }
        }

    @Test
    fun `when saving the new display name, and fails then should navigate back indicating EXTRA_SETTINGS_DISPLAY_NAME_CHANGED failure`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .withUserSaveNameResult(DisplayNameUpdateResult.Failure(CoreFailure.Unknown(Error())))
                .arrange()

            viewModel.saveDisplayName()

            coVerify {
                arrangement.navigationManager.navigateBack(eq(mapOf(EXTRA_SETTINGS_DISPLAY_NAME_CHANGED to false)))
            }
        }

    @Test
    fun `when validating new name, and we have an empty value, then should propagate NameEmptyError`() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        val newValue = TextFieldValue(" ")
        viewModel.onNameChange(newValue)

        assertEquals(DisplayNameState.NameError.TextFieldError.NameEmptyError, viewModel.displayNameState.error)
        assertTrue(viewModel.displayNameState.animatedNameError)
        assertFalse(viewModel.displayNameState.continueEnabled)
    }

    @Test
    fun `when validating new name, and the value exceeds 64 chars, then should propagate NameExceedLimitError`() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        val over64CharString = TextFieldValue("a9p8fIRG12wvOJ8AKH77UqwHt8lzTTOBlSdIlq1N6xxYBsEIUomLKoRY2IZ1hClOM")
        viewModel.onNameChange(over64CharString)

        assertEquals(DisplayNameState.NameError.TextFieldError.NameExceedLimitError, viewModel.displayNameState.error)
        assertTrue(viewModel.displayNameState.animatedNameError)
        assertFalse(viewModel.displayNameState.continueEnabled)
    }

    @Test
    fun `when validating new name, and the value is the same, then should propagate None`() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        viewModel.onNameChange(TextFieldValue("username "))

        assertEquals(DisplayNameState.NameError.None, viewModel.displayNameState.error)
        assertFalse(viewModel.displayNameState.animatedNameError)
        assertFalse(viewModel.displayNameState.continueEnabled)
    }

    @Test
    fun `when validating new name, and the value is valid, then should propagate None and enable 'continue'`() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        viewModel.onNameChange(TextFieldValue("valid new name"))

        assertEquals(DisplayNameState.NameError.None, viewModel.displayNameState.error)
        assertFalse(viewModel.displayNameState.animatedNameError)
        assertTrue(viewModel.displayNameState.continueEnabled)
    }

    @Test
    fun `when calling onAnimatedError, should emit animatedNameError false to clean state`() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        viewModel.onNameErrorAnimated()

        assertFalse(viewModel.displayNameState.animatedNameError)
    }

    @Test
    fun `when navigating back requested, then should delegate call to manager navigateBack`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()
        viewModel.navigateBack()

        coVerify(exactly = 1) { arrangement.navigationManager.navigateBack() }
    }

    private class Arrangement {

        @MockK
        lateinit var navigationManager: NavigationManager

        @MockK
        lateinit var getSelfUserUseCase: GetSelfUserUseCase

        @MockK
        lateinit var updateDisplayNameUseCase: UpdateDisplayNameUseCase

        @MockK
        private lateinit var savedStateHandle: SavedStateHandle

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { savedStateHandle.get<String>(any()) } returns "SOMETHING"
            coEvery { getSelfUserUseCase() } returns flowOf(TestUser.SELF_USER)
        }

        fun withUserSaveNameResult(result: DisplayNameUpdateResult) = apply {
            coEvery { updateDisplayNameUseCase(any()) } returns result
        }

        fun arrange() =
            this to ChangeDisplayNameViewModel(
                getSelfUserUseCase,
                updateDisplayNameUseCase,
                navigationManager,
                TestDispatcherProvider()
            )
    }
}
