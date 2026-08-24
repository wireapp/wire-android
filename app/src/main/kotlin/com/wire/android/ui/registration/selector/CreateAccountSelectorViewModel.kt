/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.registration.selector

import com.wire.android.datastore.GlobalDataStore
import com.wire.android.ui.authentication.legacyregistration.selector.LegacyRegistrationSelectorGateway
import com.wire.android.ui.authentication.legacyregistration.selector.LegacyRegistrationSelectorInput
import com.wire.android.ui.authentication.legacyregistration.selector.LegacyRegistrationSelectorViewModel
import com.wire.kalium.logic.configuration.server.ServerConfig
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

class CreateAccountSelectorViewModel @AssistedInject constructor(
    private val globalDataStore: GlobalDataStore,
    @Assisted val navArgs: CreateAccountSelectorNavArgs,
    defaultServerConfig: ServerConfig.Links
) : LegacyRegistrationSelectorViewModel<ServerConfig.Links>(
    input = LegacyRegistrationSelectorInput(navArgs.customServerConfig, navArgs.email),
    defaultServerConfig = defaultServerConfig,
    gateway = LegacyRegistrationSelectorGateway { globalDataStore.setAnonymousRegistrationEnabled(it) },
) {
    @AssistedFactory
    interface Factory {
        fun create(navArgs: CreateAccountSelectorNavArgs): CreateAccountSelectorViewModel
    }
    val teamAccountCreationUrl = serverConfig.teams
}
