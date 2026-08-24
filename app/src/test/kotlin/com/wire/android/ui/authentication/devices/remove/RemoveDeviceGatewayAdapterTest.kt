/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.authentication.devices.remove

import com.wire.android.framework.TestClient
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.authentication.devices.register.AuthenticationFailure
import com.wire.android.ui.authentication.devices.register.PasswordRequirement
import com.wire.android.ui.authentication.devices.register.RegisterDeviceGateway
import com.wire.android.ui.authentication.devices.register.RegisterDeviceRequest
import com.wire.android.ui.authentication.devices.register.RegisterDeviceResult
import com.wire.android.ui.authentication.devices.register.RequestVerificationCodeResult
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.data.client.ClientType
import com.wire.kalium.logic.data.client.DeleteClientParam
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.feature.client.DeleteClientResult
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.FetchSelfClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.SelfClientsResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RemoveDeviceGatewayAdapterTest {

    @Test
    fun `fetch maps only permanent clients into host devices`() = runTest {
        val arrangement = Arrangement()
        val temporary = TestClient.CLIENT.copy(id = ClientId("temporary"), type = ClientType.Temporary)
        val legalHold = TestClient.CLIENT.copy(id = ClientId("legal-hold"), type = ClientType.LegalHold)
        coEvery { arrangement.fetchClients() } returns SelfClientsResult.Success(
            clients = listOf(temporary, TestClient.CLIENT, legalHold),
            currentClientId = temporary.id,
        )

        assertEquals(
            FetchPermanentDevicesResult.Success(listOf(Device(TestClient.CLIENT))),
            arrangement.gateway.fetchPermanentDevices(),
        )
    }

    @Test
    fun `fetch generic failures preserve every authentication classification`() = runTest {
        val arrangement = Arrangement()

        failureMappings().forEach { (coreFailure, authenticationFailure) ->
            coEvery { arrangement.fetchClients() } returns SelfClientsResult.Failure.Generic(coreFailure)
            assertEquals(
                FetchPermanentDevicesResult.Failure(authenticationFailure),
                arrangement.gateway.fetchPermanentDevices(),
            )
        }
    }

    @Test
    fun `delete passes exact password and client id and maps every structured result`() = runTest {
        val arrangement = Arrangement()
        val device = Device(TestClient.CLIENT)
        val parameter = slot<DeleteClientParam>()
        val mappings = listOf(
            DeleteClientResult.Success to DeleteDeviceResult.Success,
            DeleteClientResult.Failure.InvalidCredentials to DeleteDeviceResult.InvalidCredentials,
            DeleteClientResult.Failure.PasswordAuthRequired to DeleteDeviceResult.PasswordRequired,
        )

        mappings.forEach { (kaliumResult, featureResult) ->
            coEvery { arrangement.deleteClient(capture(parameter)) } returns kaliumResult
            assertEquals(featureResult, arrangement.gateway.deleteDevice("password", device))
            assertEquals(DeleteClientParam("password", TestClient.CLIENT.id), parameter.captured)
        }
    }

    @Test
    fun `delete generic failures preserve every authentication classification`() = runTest {
        val arrangement = Arrangement()
        val device = Device(TestClient.CLIENT)

        failureMappings().forEach { (coreFailure, authenticationFailure) ->
            coEvery { arrangement.deleteClient(any()) } returns DeleteClientResult.Failure.Generic(coreFailure)
            assertEquals(
                DeleteDeviceResult.Failure(authenticationFailure),
                arrangement.gateway.deleteDevice(null, device),
            )
        }
    }

    @Test
    fun `register password and verification operations delegate without remapping`() = runTest {
        val arrangement = Arrangement()
        val request = RegisterDeviceRequest("password", "123456")
        coEvery { arrangement.registerGateway.passwordRequirement() } returns PasswordRequirement.NotRequired
        coEvery { arrangement.registerGateway.registerClient(request) } returns RegisterDeviceResult.Success(
            initialSyncCompleted = true,
            isE2EIRequired = true,
        )
        coEvery { arrangement.registerGateway.requestVerificationCode() } returns
            RequestVerificationCodeResult.TooManyRequests("member@example.com")

        assertEquals(PasswordRequirement.NotRequired, arrangement.gateway.passwordRequirement())
        assertEquals(
            RegisterDeviceResult.Success(initialSyncCompleted = true, isE2EIRequired = true),
            arrangement.gateway.registerClient(request),
        )
        assertEquals(
            RequestVerificationCodeResult.TooManyRequests("member@example.com"),
            arrangement.gateway.requestVerificationCode(),
        )
        coVerify(exactly = 1) {
            arrangement.registerGateway.passwordRequirement()
            arrangement.registerGateway.registerClient(request)
            arrangement.registerGateway.requestVerificationCode()
        }
    }

    private fun failureMappings(): List<Pair<CoreFailure, AuthenticationFailure>> = listOf(
        NetworkFailure.NoNetworkConnection(null) to AuthenticationFailure.NoNetwork,
        NetworkFailure.ServerMiscommunication(IllegalStateException("server")) to
            AuthenticationFailure.ServerMiscommunication,
        CoreFailure.Unknown(IllegalStateException("unknown")) to AuthenticationFailure.Unknown,
    )

    private class Arrangement {
        val fetchClients = mockk<FetchSelfClientsFromRemoteUseCase>()
        val deleteClient = mockk<DeleteClientUseCase>()
        val registerGateway = mockk<RegisterDeviceGateway>()

        val gateway = KaliumRemoveDeviceGateway(
            fetchSelfClientsFromRemote = fetchClients,
            deleteClient = deleteClient,
            registerDeviceGateway = registerGateway,
        )

        init {
            coEvery { fetchClients() } returns SelfClientsResult.Success(
                clients = listOf(TestClient.CLIENT),
                currentClientId = TestClient.CLIENT.id,
            )
            coEvery { deleteClient(any()) } returns DeleteClientResult.Success
        }
    }
}
