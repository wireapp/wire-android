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
package com.wire.android.ui.authentication.welcome

import com.wire.android.BuildConfig
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.feature.session.DoesValidNomadAccountExistUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import dev.zacsweers.metro.Inject

internal class KaliumWelcomeSessionGateway(
    private val getSessions: GetSessionsUseCase,
    private val doesValidNomadAccountExist: DoesValidNomadAccountExistUseCase,
    private val maxAccounts: Int,
) : WelcomeSessionGateway {

    override suspend fun loadSessions(): WelcomeSessionResult {
        if (doesValidNomadAccountExist()) {
            return WelcomeSessionResult.NomadAccountBlocksLogin
        }
        return when (val result = getSessions()) {
            is GetAllSessionsResult.Success -> WelcomeSessionResult.Sessions(
                validSessionCount = result.sessions.count { it is AccountInfo.Valid },
                maxAccounts = maxAccounts,
            )

            GetAllSessionsResult.Failure.NoSessionFound -> WelcomeSessionResult.NoSessionFound
            is GetAllSessionsResult.Failure.Generic -> WelcomeSessionResult.Unavailable
        }
    }
}

class WelcomeViewModelHostFactory private constructor(
    getSessions: GetSessionsUseCase,
    doesValidNomadAccountExist: DoesValidNomadAccountExistUseCase,
    private val defaultServerConfig: ServerConfig.Links,
    maxAccounts: Int,
) {
    @Inject
    constructor(
        getSessions: GetSessionsUseCase,
        doesValidNomadAccountExist: DoesValidNomadAccountExistUseCase,
        defaultServerConfig: ServerConfig.Links,
    ) : this(
        getSessions = getSessions,
        doesValidNomadAccountExist = doesValidNomadAccountExist,
        defaultServerConfig = defaultServerConfig,
        maxAccounts = BuildConfig.MAX_ACCOUNTS,
    )

    private val sessionGateway = KaliumWelcomeSessionGateway(
        getSessions = getSessions,
        doesValidNomadAccountExist = doesValidNomadAccountExist,
        maxAccounts = maxAccounts,
    )

    fun create(navArgs: WelcomeNavArgs): WelcomeViewModel<ServerConfig.Links> = WelcomeViewModel(
        links = navArgs.customServerConfig ?: defaultServerConfig,
        sessionGateway = sessionGateway,
    )
}
