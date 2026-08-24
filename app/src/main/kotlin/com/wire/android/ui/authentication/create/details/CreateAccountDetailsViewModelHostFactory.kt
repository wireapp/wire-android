/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.create.details

import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.authentication.create.common.CreateAccountNavArgs
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import dev.zacsweers.metro.Inject

internal class KaliumCreateAccountDetailsGateway(
    private val validatePassword: ValidatePasswordUseCase,
) : CreateAccountDetailsGateway {
    override suspend fun isPasswordValid(password: String): Boolean = validatePassword(password).isValid
}

class CreateAccountDetailsViewModelHostFactory @Inject constructor(
    validatePassword: ValidatePasswordUseCase,
    private val defaultServerConfig: ServerConfig.Links,
) {
    private val gateway = KaliumCreateAccountDetailsGateway(validatePassword)

    fun create(
        navArgs: CreateAccountNavArgs,
    ): CreateAccountDetailsViewModel<ServerConfig.Links, NetworkFailure> = CreateAccountDetailsViewModel(
        customServerConfig = navArgs.customServerConfig,
        defaultServerConfig = defaultServerConfig,
        requiresTeamName = navArgs.flowType == CreateAccountFlowType.CreateTeam,
        gateway = gateway,
    )
}
