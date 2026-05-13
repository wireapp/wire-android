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
package com.wire.android.ui.home.settings.appsettings.networkSettings

import com.wire.android.emm.ManagedConfigurationsManager
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.PersistPersistentWebSocketConnectionStatusUseCase
import dev.zacsweers.metro.Inject

@Inject
class NetworkSettingsViewModelFactory(
    private val persistPersistentWebSocketConnectionStatus: PersistPersistentWebSocketConnectionStatusUseCase,
    private val observePersistentWebSocketConnectionStatus: ObservePersistentWebSocketConnectionStatusUseCase,
    private val currentSession: CurrentSessionUseCase,
    private val managedConfigurationsManager: ManagedConfigurationsManager,
    private val networkSettingsDefaultsProvider: NetworkSettingsDefaultsProvider,
) {
    fun create(): NetworkSettingsViewModel = NetworkSettingsViewModel(
        persistPersistentWebSocketConnectionStatus = persistPersistentWebSocketConnectionStatus,
        observePersistentWebSocketConnectionStatus = observePersistentWebSocketConnectionStatus,
        currentSession = currentSession,
        managedConfigurationsManager = managedConfigurationsManager,
        networkSettingsDefaultsProvider = networkSettingsDefaultsProvider,
    )
}
