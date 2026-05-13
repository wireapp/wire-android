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
import com.wire.kalium.logic.feature.client.FetchSelfClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.ObserveClientsByUserIdUseCase
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetUserMlsClientIdentitiesUseCase
import com.wire.kalium.logic.feature.user.IsE2EIEnabledUseCase
import dev.zacsweers.metro.Inject

@Inject
class SelfDevicesViewModelFactory(
    @CurrentAccount private val currentAccountId: UserId,
    private val fetchSelfClientsFromRemote: FetchSelfClientsFromRemoteUseCase,
    private val observeClientList: ObserveClientsByUserIdUseCase,
    private val currentClientIdUseCase: ObserveCurrentClientIdUseCase,
    private val getUserMlsClientIdentities: GetUserMlsClientIdentitiesUseCase,
    private val isE2EIEnabledUseCase: IsE2EIEnabledUseCase,
) {
    fun create(): SelfDevicesViewModel = SelfDevicesViewModel(
        currentAccountId = currentAccountId,
        fetchSelfClientsFromRemote = fetchSelfClientsFromRemote,
        observeClientList = observeClientList,
        currentClientIdUseCase = currentClientIdUseCase,
        getUserMlsClientIdentities = getUserMlsClientIdentities,
        isE2EIEnabledUseCase = isE2EIEnabledUseCase,
    )
}
