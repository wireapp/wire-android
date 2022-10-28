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
import com.wire.kalium.logic.functional.foldToEitherWhileRight
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrateActiveAccountsUseCase @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val scalaGlobalDB: ScalaAppDataBaseProvider,
    private val mapper: MigrationMapper
) {
    suspend operator fun invoke(serverConfig: ServerConfig): Either<CoreFailure, List<UserId>> =
        scalaGlobalDB.scalaAccountsDAO.activeAccounts().foldToEitherWhileRight(emptyList()) { item, acc ->

            val activeAccount = item.copy(refreshToken = item.refreshToken.removePrefix(REFRESH_TOKEN_PREFIX))
            val isDataComplete = isDataComplete(serverConfig, activeAccount)
            val ssoId = activeAccount.ssoId?.let { ssoId -> mapper.fromScalaSsoID(ssoId) }
            val authTokensEither: Either<CoreFailure, AuthTokens> = if (isDataComplete) {
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

            authTokensEither.flatMap { authTokens ->
                val addAccountResult = coreLogic.globalScope {
                    addAuthenticatedAccount(
                        serverConfigId = serverConfig.id,
                        ssoId = ssoId,
                        authTokens = authTokens,
                        replace = false
                    )
                }
                when (addAccountResult) {
                    is AddAuthenticatedUserUseCase.Result.Failure.Generic -> Either.Left(addAccountResult.genericFailure)
                    else -> Either.Right(acc + authTokens.userId)
                }
            }
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

    private companion object {
        const val REFRESH_TOKEN_PREFIX = "zuid="
    }
}
