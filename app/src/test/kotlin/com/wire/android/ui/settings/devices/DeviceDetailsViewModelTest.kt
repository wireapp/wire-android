/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.settings.devices

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.feature.e2ei.GetE2EICertificateUseCase
import com.wire.android.framework.TestClient
import com.wire.android.framework.TestUser
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceDialogState
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceError
import com.wire.android.ui.navArgs
import com.wire.android.ui.settings.devices.DeviceDetailsViewModelTest.Arrangement.Companion.CLIENT_ID
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.data.client.ClientType
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.client.ClientFingerprintUseCase
import com.wire.kalium.logic.feature.client.DeleteClientResult
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.GetClientDetailsResult
import com.wire.kalium.logic.feature.client.ObserveClientDetailsUseCase
import com.wire.kalium.logic.feature.client.Result
import com.wire.kalium.logic.feature.client.UpdateClientVerificationStatusUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetE2EICertificateUseCaseResult
import com.wire.kalium.logic.feature.e2ei.usecase.GetE2eiCertificateUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoResult
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.ObserveUserInfoUseCase
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.IOException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class DeviceDetailsViewModelTest {

    @Test
    fun `given a self client id, when fetching details, then returns device information`() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withRequiredMockSetup()
            .withClientDetailsResult(GetClientDetailsResult.Success(TestClient.CLIENT, false))
            .arrange()

        // then
        assertEquals(CLIENT_ID, viewModel.state.device.clientId)
    }

    @Test
    fun `given a self client id, when fetching details fails, then returns InitError`() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withRequiredMockSetup()
            .withClientDetailsResult(
                GetClientDetailsResult.Failure.Generic(CoreFailure.Unknown(RuntimeException("error")))
            )
            .arrange()

        // then
        assert(viewModel.state.error is RemoveDeviceError.InitError)
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

        viewModel.removeDevice(arrangement.onSuccess)

        // then
        coVerify {
            arrangement.deleteClientUseCase(any())
        }
        verify { arrangement.onSuccess() }
    }

    @Test
    fun `given a self client id, when removing a device and password required, then should emit to show password dialog`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withRequiredMockSetup()
            .withClientDetailsResult(GetClientDetailsResult.Success(TestClient.CLIENT, false))
            .withUserRequiresPasswordResult(IsPasswordRequiredUseCase.Result.Success(true))
            .arrange()

        viewModel.removeDevice(arrangement.onSuccess)

        coVerify {
            arrangement.deleteClientUseCase(any()) wasNot Called
        }
        verify { arrangement.onSuccess wasNot Called }
        assertTrue(viewModel.state.removeDeviceDialogState is RemoveDeviceDialogState.Visible)
        assertTrue(viewModel.state.error is RemoveDeviceError.None)
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
            verify { arrangement.onSuccess wasNot Called }
            assertTrue(viewModel.state.removeDeviceDialogState is RemoveDeviceDialogState.Hidden)
            assertTrue(viewModel.state.error is RemoveDeviceError.None)
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

            viewModel.removeDevice(arrangement.onSuccess)
            viewModel.clearDeleteClientError()

            coVerify {
                arrangement.deleteClientUseCase.invoke(any()) wasNot Called
            }
            verify { arrangement.onSuccess wasNot Called }
            assertTrue(viewModel.state.removeDeviceDialogState is RemoveDeviceDialogState.Visible)
            assertTrue(viewModel.state.error is RemoveDeviceError.None)
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

            viewModel.removeDevice(arrangement.onSuccess)
            viewModel.onRemoveConfirmed(arrangement.onSuccess)
            advanceUntilIdle()

            coVerify {
                arrangement.deleteClientUseCase.invoke(any())
            }
            verify { arrangement.onSuccess() }
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

            viewModel.removeDevice(arrangement.onSuccess)

            coVerify {
                arrangement.deleteClientUseCase(any())
            }
            verify { arrangement.onSuccess() }
            assertTrue(viewModel.state.removeDeviceDialogState is RemoveDeviceDialogState.Hidden)
            assertTrue(viewModel.state.error is RemoveDeviceError.None)
        }

    @Test
    fun `given self legal hold client, when fetching state, then canBeRemoved is false`() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withRequiredMockSetup()
            .withClientDetailsResult(GetClientDetailsResult.Success(TestClient.CLIENT.copy(type = ClientType.LegalHold), false))
            .arrange()
        // then
        assertFalse(viewModel.state.canBeRemoved)
    }

    @Test
    fun `given self temporary client, when fetching state, then canBeRemoved is true`() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withRequiredMockSetup()
            .withClientDetailsResult(GetClientDetailsResult.Success(TestClient.CLIENT.copy(type = ClientType.Temporary), false))
            .arrange()
        // then
        assertFalse(viewModel.state.canBeRemoved)
    }

    @Test
    fun `given self permanent client, when fetching state, then canBeRemoved is true`() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withRequiredMockSetup()
            .withClientDetailsResult(GetClientDetailsResult.Success(TestClient.CLIENT.copy(type = ClientType.Permanent), false))
            .arrange()
        // then
        assertTrue(viewModel.state.canBeRemoved)
    }

    @Test
    fun `given self permanent current client, when fetching state, then canBeRemoved is false`() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withRequiredMockSetup()
            .withClientDetailsResult(GetClientDetailsResult.Success(TestClient.CLIENT.copy(type = ClientType.Permanent), true))
            .arrange()
        // then
        assertFalse(viewModel.state.canBeRemoved)
    }

    @Test
    fun `given other user permanent client, when fetching state, then canBeRemoved is false`() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withRequiredMockSetup(userId = UserId("otherUserId", "otherUserDomain"))
            .withClientDetailsResult(GetClientDetailsResult.Success(TestClient.CLIENT.copy(type = ClientType.Permanent), false))
            .arrange()
        // then
        assertFalse(viewModel.state.canBeRemoved)
    }

    @Test
    fun `given get certificate clicked, then should call GetE2EICertificate`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .withRequiredMockSetup()
                .withClientDetailsResult(GetClientDetailsResult.Success(TestClient.CLIENT, true))
                .arrange()

            viewModel.enrollE2eiCertificate(arrangement.context)

            coVerify {
                arrangement.enrolE2EICertificateUseCase(any(), any())
            }
            assertTrue(viewModel.state.isLoadingCertificate)
        }

    private class Arrangement {

        @MockK
        lateinit var context: Context

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

        @MockK
        lateinit var getE2eiCertificate: GetE2eiCertificateUseCase

        @MockK
        lateinit var enrolE2EICertificateUseCase: GetE2EICertificateUseCase

        @MockK(relaxed = true)
        lateinit var onSuccess: () -> Unit

        val currentUserId = UserId("currentUserId", "currentUserDomain")

        val viewModel by lazy {
            DeviceDetailsViewModel(
                savedStateHandle = savedStateHandle,
                deleteClient = deleteClientUseCase,
                observeClientDetails = observeClientDetails,
                isPasswordRequired = isPasswordRequiredUseCase,
                fingerprintUseCase = deviceFingerprint,
                updateClientVerificationStatus = updateClientVerificationStatus,
                currentUserId = currentUserId,
                observeUserInfo = observeUserInfo,
                e2eiCertificate = getE2eiCertificate,
                enrolE2EICertificateUseCase = enrolE2EICertificateUseCase
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            withFingerprintSuccess()
            coEvery { observeUserInfo(any()) } returns flowOf(GetUserInfoResult.Success(TestUser.OTHER_USER, null))
            coEvery { getE2eiCertificate(any()) } returns GetE2EICertificateUseCaseResult.Failure.NotActivated
        }

        fun withUserRequiresPasswordResult(result: IsPasswordRequiredUseCase.Result = IsPasswordRequiredUseCase.Result.Success(true)) =
            apply {
                coEvery { isPasswordRequiredUseCase() } returns result
            }

        fun withFingerprintSuccess() = apply {
            coEvery { deviceFingerprint(any(), any()) } returns Result.Success("fingerprint".encodeToByteArray())
        }

        fun withFingerprintFailure() = apply {
            coEvery { deviceFingerprint(any(), any()) } returns Result.Failure(
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

        fun withRequiredMockSetup(userId: UserId = currentUserId) = apply {
            every { savedStateHandle.navArgs<DeviceDetailsNavArgs>() } returns DeviceDetailsNavArgs(
                userId = userId,
                clientId = CLIENT_ID
            )
        }

        fun arrange() = this to viewModel

        companion object {
            val CLIENT_ID = TestClient.CLIENT.id
        }
    }
}
