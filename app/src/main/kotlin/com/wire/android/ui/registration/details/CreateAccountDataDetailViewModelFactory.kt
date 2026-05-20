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
package com.wire.android.ui.registration.details

import com.wire.android.analytics.RegistrationAnalyticsManagerUseCase
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import dev.zacsweers.metro.Inject

@Inject
class CreateAccountDataDetailViewModelFactory(
    private val validatePassword: ValidatePasswordUseCase,
    private val validateEmail: ValidateEmailUseCase,
    private val globalDataStore: GlobalDataStore,
    private val registrationAnalyticsManager: RegistrationAnalyticsManagerUseCase,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val defaultServerConfig: ServerConfig.Links,
) {
    fun create(args: CreateAccountDataNavArgs): CreateAccountDataDetailViewModel = CreateAccountDataDetailViewModel(
        createAccountNavArgs = args,
        validatePassword = validatePassword,
        validateEmail = validateEmail,
        globalDataStore = globalDataStore,
        registrationAnalyticsManager = registrationAnalyticsManager,
        coreLogic = coreLogic,
        defaultServerConfig = defaultServerConfig,
    )
}
