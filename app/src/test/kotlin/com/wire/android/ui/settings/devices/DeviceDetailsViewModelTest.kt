package com.wire.android.ui.settings.devices

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestClient
import com.wire.android.framework.TestUser
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceDialogState
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceError
import com.wire.android.ui.settings.devices.DeviceDetailsViewModelTest.Arrangement.Companion.CLIENT_ID
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.client.ClientFingerprintUseCase
import com.wire.kalium.logic.feature.client.DeleteClientResult
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.GetClientDetailsResult
import com.wire.kalium.logic.feature.client.ObserveClientDetailsUseCase
import com.wire.kalium.logic.feature.client.UpdateClientVerificationStatusUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoResult
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.ObserveUserInfoUseCase
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.IOException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class DeviceDetailsViewModelTest {

    @Test
    fun `given a self client id, when fetching details, then returns device information`() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withRequiredMockSetup()
            .withClientDetailsResult(GetClientDetailsResult.Success(TestClient.CLIENT, false))
            .arrange()

        // then
        assertEquals(CLIENT_ID, viewModel.state?.device?.clientId)
    }

    @Test
    fun `given a self client id, when fetching details fails, then returns to device lists`() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withRequiredMockSetup()
            .withClientDetailsResult(
                GetClientDetailsResult.Failure.Generic(CoreFailure.Unknown(RuntimeException("error")))
            )
            .arrange()

        // then
        assertEquals(ClientId(""), viewModel.state.device?.clientId)
    }

    @Test
    fun `given a self client id, when removing a device and password not required, then should delete the device`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withRequiredMockSetup()
            .withClientDetailsResult(GetClientDetailsResult.Success(TestClient.CLIENT, false))
            .withUserRequiresPasswordResult(IsPasswordRequiredUseCase.Result.Success(false))
            .withDeleteDeviceResult(DeleteClientResult.Success)
            .arrange()

        viewModel.removeDevice()

        // then
        coVerify {
            arrangement.deleteClientUseCase(any())
        }
    }

    @Test
    fun `given a self client id, when removing a device and password required, then should emit to show password dialog`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withRequiredMockSetup()
            .withClientDetailsResult(GetClientDetailsResult.Success(TestClient.CLIENT, false))
            .withUserRequiresPasswordResult(IsPasswordRequiredUseCase.Result.Success(true))
            .arrange()

        viewModel.removeDevice()

        coVerify {
            arrangement.deleteClientUseCase(any()) wasNot Called
        }
        assertTrue(viewModel.state?.removeDeviceDialogState is RemoveDeviceDialogState.Visible)
        assertTrue(viewModel.state?.error is RemoveDeviceError.None)
    }

    @Test
    fun `given a dismiss dialog clicked, when password required dialog is visible, then should emit to hide password dialog`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .withRequiredMockSetup()
                .withClientDetailsResult(GetClientDetailsResult.Success(TestClient.CLIENT, false))
                .withUserRequiresPasswordResult(IsPasswordRequiredUseCase.Result.Success(true))
                .arrange()

            viewModel.onDialogDismissed()

            coVerify {
                arrangement.deleteClientUseCase(any()) wasNot Called
            }
            assertTrue(viewModel.state?.removeDeviceDialogState is RemoveDeviceDialogState.Hidden)
            assertTrue(viewModel.state?.error is RemoveDeviceError.None)
        }

    @Test
    fun `given an error on password dialog, when error should be cleared, then should emit to clear the error`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .withRequiredMockSetup()
                .withClientDetailsResult(GetClientDetailsResult.Success(TestClient.CLIENT, false))
                .withUserRequiresPasswordResult(IsPasswordRequiredUseCase.Result.Success(true))
                .withDeleteDeviceResult(DeleteClientResult.Failure.Generic(CoreFailure.Unknown(RuntimeException("error"))))
                .arrange()

            viewModel.removeDevice()
            viewModel.clearDeleteClientError()

            coVerify {
                arrangement.deleteClientUseCase.invoke(any()) wasNot Called
            }
            assertTrue(viewModel.state?.removeDeviceDialogState is RemoveDeviceDialogState.Visible)
            assertTrue(viewModel.state?.error is RemoveDeviceError.None)
        }

    @Test
    fun `given a password dialog, when confirmation clicked, then should call to delete device`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .withRequiredMockSetup()
                .withClientDetailsResult(GetClientDetailsResult.Success(TestClient.CLIENT, false))
                .withUserRequiresPasswordResult(IsPasswordRequiredUseCase.Result.Success(true))
                .withDeleteDeviceResult(DeleteClientResult.Success)
                .arrange()

            viewModel.removeDevice()
            viewModel.onRemoveConfirmed()
            advanceUntilIdle()

            coVerify {
                arrangement.deleteClientUseCase.invoke(any())
            }
            assertTrue(viewModel.state.removeDeviceDialogState is RemoveDeviceDialogState.Visible)
            assertTrue((viewModel.state.removeDeviceDialogState as? RemoveDeviceDialogState.Visible)?.removeEnabled == false)
            assertTrue(viewModel.state.error is RemoveDeviceError.None)
        }

    @Test
    fun `given remove a device succeeds, then should call deletion of device`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .withRequiredMockSetup()
                .withClientDetailsResult(GetClientDetailsResult.Success(TestClient.CLIENT, false))
                .withUserRequiresPasswordResult(IsPasswordRequiredUseCase.Result.Success(false))
                .withDeleteDeviceResult(DeleteClientResult.Success)
                .arrange()

            viewModel.removeDevice()

            coVerify {
                arrangement.deleteClientUseCase(any())
            }
            assertTrue(viewModel.state?.removeDeviceDialogState is RemoveDeviceDialogState.Hidden)
            assertTrue(viewModel.state?.error is RemoveDeviceError.None)
        }

    private class Arrangement {
        @MockK
        lateinit var navigationManager: NavigationManager

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var deleteClientUseCase: DeleteClientUseCase

        @MockK
        lateinit var observeClientDetails: ObserveClientDetailsUseCase

        @MockK
        lateinit var deviceFingerprint: ClientFingerprintUseCase

        @MockK
        lateinit var updateClientVerificationStatus: UpdateClientVerificationStatusUseCase

        @MockK
        lateinit var isPasswordRequiredUseCase: IsPasswordRequiredUseCase

        @MockK
        lateinit var observeUserInfo: ObserveUserInfoUseCase

        val currentUserId = UserId("currentUserId", "currentUserDomain")

        val viewModel by lazy {
            DeviceDetailsViewModel(
                savedStateHandle = savedStateHandle,
                navigationManager = navigationManager,
                deleteClient = deleteClientUseCase,
                observeClientDetails = observeClientDetails,
                isPasswordRequired = isPasswordRequiredUseCase,
                fingerprintUseCase = deviceFingerprint,
                updateClientVerificationStatus = updateClientVerificationStatus,
                currentUserId = currentUserId,
                observeUserInfo = observeUserInfo
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            withFingerprintSuccess()
            coEvery { observeUserInfo(any()) } returns flowOf(GetUserInfoResult.Success(TestUser.OTHER_USER, null))
        }

        fun withUserRequiresPasswordResult(result: IsPasswordRequiredUseCase.Result = IsPasswordRequiredUseCase.Result.Success(true)) =
            apply {
                coEvery { isPasswordRequiredUseCase() } returns result
            }

        fun withFingerprintSuccess() = apply {
            coEvery { deviceFingerprint(any(), any()) } returns ClientFingerprintUseCase.Result.Success("fingerprint".encodeToByteArray())
        }

        fun withFingerprintFailure() = apply {
            coEvery { deviceFingerprint(any(), any()) } returns ClientFingerprintUseCase.Result.Failure(
                NetworkFailure.NoNetworkConnection(
                    IOException()
                )
            )
        }

        fun withClientDetailsResult(result: GetClientDetailsResult) = apply {
            coEvery { observeClientDetails(any(), any()) } returns flowOf(result)
        }

        fun withDeleteDeviceResult(result: DeleteClientResult) = apply {
            coEvery { deleteClientUseCase(any()) } returns result
        }

        fun withRequiredMockSetup() = apply {
            every { savedStateHandle.get<String>(any()) } returns "SOMETHING"
            coEvery { navigationManager.navigate(any()) } returns Unit
        }

        fun arrange() = this to viewModel

        companion object {
            val CLIENT_ID = TestClient.CLIENT.id
        }
    }
}
