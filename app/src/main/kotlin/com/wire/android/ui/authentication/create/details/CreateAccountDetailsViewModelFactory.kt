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
package com.wire.android.ui.authentication.create.details

import com.wire.android.ui.authentication.create.common.CreateAccountNavArgs
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import dev.zacsweers.metro.Inject

@Inject
class CreateAccountDetailsViewModelFactory(
    private val validatePasswordUseCase: ValidatePasswordUseCase,
    private val defaultServerConfig: ServerConfig.Links,
) {
    fun create(args: CreateAccountNavArgs): CreateAccountDetailsViewModel = CreateAccountDetailsViewModel(
        createAccountNavArgs = args,
        validatePasswordUseCase = validatePasswordUseCase,
        defaultServerConfig = defaultServerConfig,
    )
}
