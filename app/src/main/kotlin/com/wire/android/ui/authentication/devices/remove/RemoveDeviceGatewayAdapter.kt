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

import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.authentication.devices.register.RegisterDeviceGateway
import com.wire.android.ui.authentication.devices.register.RegisterDeviceRequest
import com.wire.android.ui.authentication.devices.register.RegisterDeviceResult
import com.wire.android.ui.authentication.devices.register.RequestVerificationCodeResult
import com.wire.android.ui.authentication.devices.register.PasswordRequirement
import com.wire.android.ui.authentication.devices.register.toAuthenticationFailure
import com.wire.kalium.logic.data.client.ClientType
import com.wire.kalium.logic.data.client.DeleteClientParam
import com.wire.kalium.logic.feature.client.DeleteClientResult
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.FetchSelfClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.SelfClientsResult
import com.wire.navigation.WireSessionId

internal class KaliumRemoveDeviceGateway(
    private val fetchSelfClientsFromRemote: FetchSelfClientsFromRemoteUseCase,
    private val deleteClient: DeleteClientUseCase,
    private val registerDeviceGateway: RegisterDeviceGateway<WireSessionId>,
) : RemoveDeviceGateway<Device> {

    override suspend fun passwordRequirement(): PasswordRequirement = registerDeviceGateway.passwordRequirement()

    override suspend fun requestVerificationCode(): RequestVerificationCodeResult =
        registerDeviceGateway.requestVerificationCode()

    override suspend fun registerClient(request: RegisterDeviceRequest): RegisterDeviceResult<Nothing> =
        when (val result = registerDeviceGateway.registerClient(request)) {
            is RegisterDeviceResult.Success -> RegisterDeviceResult.Success<Nothing>(
                initialSyncCompleted = result.initialSyncCompleted,
                isE2EIRequired = result.isE2EIRequired,
            )
            RegisterDeviceResult.TooManyDevices -> RegisterDeviceResult.TooManyDevices
            RegisterDeviceResult.MissingSecondFactor -> RegisterDeviceResult.MissingSecondFactor
            RegisterDeviceResult.InvalidSecondFactor -> RegisterDeviceResult.InvalidSecondFactor
            RegisterDeviceResult.InvalidCredentials -> RegisterDeviceResult.InvalidCredentials
            RegisterDeviceResult.PasswordRequired -> RegisterDeviceResult.PasswordRequired
            is RegisterDeviceResult.Failure -> RegisterDeviceResult.Failure(result.failure)
        }

    override suspend fun fetchPermanentDevices(): FetchPermanentDevicesResult<Device> =
        when (val result = fetchSelfClientsFromRemote()) {
            is SelfClientsResult.Success -> FetchPermanentDevicesResult.Success(
                result.clients
                    .filter { it.type == ClientType.Permanent }
                    .map(::Device)
            )

            is SelfClientsResult.Failure.Generic -> FetchPermanentDevicesResult.Failure(
                result.genericFailure.toAuthenticationFailure()
            )
        }

    override suspend fun deleteDevice(password: String?, device: Device): DeleteDeviceResult =
        when (val result = deleteClient(DeleteClientParam(password, device.clientId))) {
            DeleteClientResult.Success -> DeleteDeviceResult.Success
            DeleteClientResult.Failure.InvalidCredentials -> DeleteDeviceResult.InvalidCredentials
            DeleteClientResult.Failure.PasswordAuthRequired -> DeleteDeviceResult.PasswordRequired
            is DeleteClientResult.Failure.Generic -> DeleteDeviceResult.Failure(
                result.genericFailure.toAuthenticationFailure()
            )
        }
}
