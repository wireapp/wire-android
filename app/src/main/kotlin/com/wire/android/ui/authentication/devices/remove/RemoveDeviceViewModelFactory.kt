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

import com.wire.android.datastore.UserDataStore
import com.wire.kalium.logic.feature.auth.verification.RequestSecondFactorVerificationCodeUseCase
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.FetchSelfClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import dev.zacsweers.metro.Inject

@Inject
class RemoveDeviceViewModelFactory(
    private val fetchSelfClientsFromRemote: FetchSelfClientsFromRemoteUseCase,
    private val deleteClientUseCase: DeleteClientUseCase,
    private val registerClientUseCase: GetOrRegisterClientUseCase,
    private val isPasswordRequired: IsPasswordRequiredUseCase,
    private val userDataStore: UserDataStore,
    private val getSelfUser: GetSelfUserUseCase,
    private val requestSecondFactorVerificationCodeUseCase: RequestSecondFactorVerificationCodeUseCase,
) {
    fun create(): RemoveDeviceViewModel = RemoveDeviceViewModel(
        fetchSelfClientsFromRemote = fetchSelfClientsFromRemote,
        deleteClientUseCase = deleteClientUseCase,
        registerClientUseCase = registerClientUseCase,
        isPasswordRequired = isPasswordRequired,
        userDataStore = userDataStore,
        getSelfUser = getSelfUser,
        requestSecondFactorVerificationCodeUseCase = requestSecondFactorVerificationCodeUseCase,
    )
}
