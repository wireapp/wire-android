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
package com.wire.android.ui.settings.devices

import com.wire.android.di.CurrentAccount
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.client.ClientFingerprintUseCase
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.ObserveClientDetailsUseCase
import com.wire.kalium.logic.feature.client.UpdateClientVerificationStatusUseCase
import com.wire.kalium.logic.feature.debug.BreakSessionUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetMLSClientIdentityUseCase
import com.wire.kalium.logic.feature.user.IsE2EIEnabledUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.ObserveUserInfoUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class DeviceDetailsViewModelFactory(
    @CurrentAccount private val currentUserId: UserId,
    private val deleteClient: DeleteClientUseCase,
    private val observeClientDetails: ObserveClientDetailsUseCase,
    private val isPasswordRequired: IsPasswordRequiredUseCase,
    private val fingerprintUseCase: ClientFingerprintUseCase,
    private val updateClientVerificationStatus: UpdateClientVerificationStatusUseCase,
    private val observeUserInfo: ObserveUserInfoUseCase,
    private val mlsClientIdentity: GetMLSClientIdentityUseCase,
    private val breakSession: BreakSessionUseCase,
    private val isE2EIEnabledUseCase: IsE2EIEnabledUseCase,
) {
    fun create(args: DeviceDetailsNavArgs): DeviceDetailsViewModel = DeviceDetailsViewModel(
        deviceDetailsNavArgs = args,
        currentUserId = currentUserId,
        deleteClient = deleteClient,
        observeClientDetails = observeClientDetails,
        isPasswordRequired = isPasswordRequired,
        fingerprintUseCase = fingerprintUseCase,
        updateClientVerificationStatus = updateClientVerificationStatus,
        observeUserInfo = observeUserInfo,
        mlsClientIdentity = mlsClientIdentity,
        breakSession = breakSession,
        isE2EIEnabledUseCase = isE2EIEnabledUseCase,
    )
}
