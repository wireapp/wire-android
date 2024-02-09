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
import com.wire.android.migration.userDatabase.ScalaUserDatabaseProvider
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.auth.AccountTokens
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOLoginSessionResult
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.flatMap
import com.wire.kalium.logic.functional.getOrNull
import com.wire.kalium.logic.functional.map
import com.wire.kalium.logic.functional.mapLeft
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrateActiveAccountsUseCase @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val scalaGlobalDB: ScalaAppDataBaseProvider,
    private val scalaUserDB: ScalaUserDatabaseProvider,
    private val mapper: MigrationMapper
) {
    suspend operator fun invoke(serverConfig: ServerConfig): Result {
        val resultAcc: MutableMap<String, Either<AccountMigrationFailure, UserId>> = mutableMapOf()
        val scalaAccountList = scalaGlobalDB.scalaAccountsDAO.activeAccounts()

        repeat(scalaAccountList.size) { index ->
            val activeAccount =
                scalaAccountList[index].copy(
                    refreshToken = scalaAccountList[index].refreshToken.removePrefix(REFRESH_TOKEN_PREFIX)
                )
            val isDataComplete = isDataComplete(serverConfig, activeAccount)
            val ssoId = activeAccount.ssoId?.let { ssoId -> mapper.fromScalaSsoID(ssoId) }
            val accountTokensEither: Either<CoreFailure, AccountTokens> = if (isDataComplete) {
                // when the data is complete it means the user has a domain and an access token
                // which make the following double bang operator safe
                val domain = if (!activeAccount.domain.isNullOrBlank()) {
                    activeAccount.domain
                } else {
                    serverConfig.metaData.domain!!
                }

                val userId = UserId(activeAccount.id, domain)
                Either.Right(
                    AccountTokens(
                        userId = userId,
                        accessToken = activeAccount.accessToken?.token!!,
                        tokenType = activeAccount.accessToken.tokenType,
                        refreshToken = activeAccount.refreshToken,
                        cookieLabel = null
                    )
                )
            } else {
                handleMissingData(
                    serverConfig,
                    activeAccount.refreshToken
                )
            }

            val accountResult = accountTokensEither.flatMap { authTokens ->
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
            }.mapLeft {
                val userData = scalaUserDB.userDAO(activeAccount.id).map { it.users(listOf(activeAccount.id)) }.getOrNull()?.firstOrNull()
                AccountMigrationFailure(userData?.name, userData?.handle, it)
            }
            resultAcc[activeAccount.id] = accountResult
        }
        return Result(resultAcc.toMap(), serverConfig.metaData.federation)
    }

    private fun isDataComplete(serverConfig: ServerConfig, activeAccount: ScalaActiveAccountsEntity): Boolean {
        val isDomainPresent = (!activeAccount.domain.isNullOrBlank()) or (serverConfig.metaData.domain != null)
        val isAccessTokenPresent = activeAccount.accessToken != null
        return isDomainPresent and isAccessTokenPresent
    }

    private suspend fun handleMissingData(
        serverConfig: ServerConfig,
        refreshToken: String,
    ): Either<CoreFailure, AccountTokens> = coreLogic.authenticationScope(
        serverConfig,
        // scala did not support proxy mode so we can pass null
        proxyCredentials = null
    ) {
        ssoLoginScope.getLoginSession(refreshToken)
    }.let {
        when (it) {
            is SSOLoginSessionResult.Failure.Generic -> Either.Left(it.genericFailure)
            SSOLoginSessionResult.Failure.InvalidCookie -> Either.Left(MigrationFailure.InvalidRefreshToken)
            is SSOLoginSessionResult.Success -> Either.Right(it.accountTokens)
        }
    }

    data class Result(val userIds: Map<String, Either<AccountMigrationFailure, UserId>>, val isFederationEnabled: Boolean)

    data class AccountMigrationFailure(val userName: String?, val userHandle: String?, val cause: CoreFailure)

    private companion object {
        const val REFRESH_TOKEN_PREFIX = "zuid="
    }
}
