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

package com.wire.android.migration.feature

import com.wire.android.di.KaliumCoreLogic
import com.wire.android.migration.MigrationMapper
import com.wire.android.migration.failure.MigrationFailure
import com.wire.android.migration.globalDatabase.ScalaActiveAccountsEntity
import com.wire.android.migration.globalDatabase.ScalaAppDataBaseProvider
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthTokens
import com.wire.kalium.logic.feature.auth.sso.SSOLoginSessionResult
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.flatMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrateActiveAccountsUseCase @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val scalaGlobalDB: ScalaAppDataBaseProvider,
    private val mapper: MigrationMapper
) {
    suspend operator fun invoke(serverConfig: ServerConfig): Result {
        val resultAcc: MutableMap<String, Either<CoreFailure, UserId>> = mutableMapOf()
        val scalaAccountList = scalaGlobalDB.scalaAccountsDAO.activeAccounts()

        repeat(scalaAccountList.size) { index ->
            val activeAccount =
                scalaAccountList[index].copy(
                    refreshToken = scalaAccountList[index].refreshToken.removePrefix(REFRESH_TOKEN_PREFIX)
                )

            val isDataComplete = isDataComplete(serverConfig, activeAccount)
            val ssoId = activeAccount.ssoId?.let { ssoId -> mapper.fromScalaSsoID(ssoId) }
            val authTokensEither: Either<CoreFailure, AuthTokens> = if (isDataComplete) {
                // when the data is complete it means the user has a domain and an access token
                // which make the following double bang operator safe
                val domain = activeAccount.domain ?: serverConfig.metaData.domain!!
                val userId = UserId(activeAccount.id, domain)
                Either.Right(
                    AuthTokens(
                        userId = userId,
                        accessToken = activeAccount.accessToken?.token!!,
                        tokenType = activeAccount.accessToken.tokenType,
                        refreshToken = activeAccount.refreshToken
                    )
                )
            } else {
                handleMissingData(
                    serverConfig,
                    activeAccount.refreshToken
                )
            }

            val accountResult = authTokensEither.flatMap { authTokens ->
                val addAccountResult = coreLogic.globalScope {
                    addAuthenticatedAccount(
                        serverConfigId = serverConfig.id,
                        ssoId = ssoId,
                        authTokens = authTokens,
                        null, // users migrated form the scala app will not have proxy
                        replace = false
                    )
                }
                when (addAccountResult) {
                    is AddAuthenticatedUserUseCase.Result.Failure.Generic -> Either.Left(addAccountResult.genericFailure)
                    else -> Either.Right(authTokens.userId)
                }
            }
            resultAcc[activeAccount.id] = accountResult
        }
        return Result(resultAcc.toMap(), serverConfig.metaData.federation)
    }

    private fun isDataComplete(serverConfig: ServerConfig, activeAccount: ScalaActiveAccountsEntity): Boolean {
        val isDomainExist = (activeAccount.domain != null) or (serverConfig.metaData.domain != null)
        val isAccessTokenExist = activeAccount.accessToken != null
        return isDomainExist and isAccessTokenExist
    }

    private suspend fun handleMissingData(
        serverConfig: ServerConfig,
        refreshToken: String,
    ): Either<CoreFailure, AuthTokens> = coreLogic.authenticationScope(serverConfig) {
        ssoLoginScope.getLoginSession(refreshToken)
    }.let {
        when (it) {
            is SSOLoginSessionResult.Failure.Generic -> Either.Left(it.genericFailure)
            SSOLoginSessionResult.Failure.InvalidCookie -> Either.Left(MigrationFailure.InvalidRefreshToken)
            is SSOLoginSessionResult.Success -> Either.Right(it.authTokens)
        }
    }

    data class Result(val userIds: Map<String, Either<CoreFailure, UserId>>, val isFederationEnabled: Boolean)

    private companion object {
        const val REFRESH_TOKEN_PREFIX = "zuid="
    }
}
