/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.di

import com.wire.android.utils.EMAIL
import com.wire.android.utils.EMAIL_2
import com.wire.android.utils.PASSWORD
import com.wire.android.utils.PASSWORD_2
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.session.SessionRepository
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AccountInfo
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.functional.Either
import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.runBlocking

@Module
@TestInstallIn(
    components = [ViewModelComponent::class],
    replaces = [SessionModule::class]
)
class TestModule {

    /**
     * This provides a custom session [UserId] for testing, having to manually first; Login and then store the session locally
     * This orchestration is necessary so we can test authenticated flows isolated
     *
     * The rest of the modules are the ones from production code, DI provided by Hilt
     */
    @CurrentAccount
    @ViewModelScoped
    @Provides
    fun currentSessionProvider(@KaliumCoreLogic coreLogic: CoreLogic): UserId {
        return runBlocking {
            val sessionRepository = coreLogic.getGlobalScope().sessionRepository
            val result = restoreSession(sessionRepository)
            if (result != null) {
                result.userId
            } else {
                // execute manual login
                val authResult = coreLogic.getAuthenticationScope(ServerConfig.STAGING)
                    .login(EMAIL, PASSWORD, false)

                if (authResult is AuthenticationResult.Success) {
                    val (authTokens, ssoId, serverConfigId) = authResult
                    // persist locally the session if successful
                    sessionRepository.storeSession(serverConfigId, ssoId, authTokens)
                    authResult.authData.userId
                } else {
                    val (authTokens, ssoId, serverConfigId) = coreLogic.getAuthenticationScope(ServerConfig.STAGING)
                        .login(EMAIL_2, PASSWORD_2, false)
                        .let {
                            when (it) {
                                is AuthenticationResult.Success -> it
                                is AuthenticationResult.Failure ->
                                    throw RuntimeException("Failed to setup testing custom injection")
                            }
                        }
                    sessionRepository.storeSession(serverConfigId, ssoId, authTokens)
                    authTokens.userId
                }
            }
        }
    }
}

private fun restoreSession(sessionRepository: SessionRepository): AccountInfo? {
    return when (val currentSessionResult = sessionRepository.currentSession()) {
        is Either.Right -> currentSessionResult.value
        else -> null
    }
}
