package com.wire.android.migration.feature

import com.wire.android.di.KaliumCoreLogic
import com.wire.android.migration.MigrationMapper
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
import com.wire.kalium.logic.functional.fold
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrateActiveAccountsUseCase @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val scalaGlobalDB: ScalaAppDataBaseProvider,
    private val mapper: MigrationMapper
) {
    suspend operator fun invoke(serverConfig: ServerConfig): Either<CoreFailure, Unit> {
        val activeAccounts = scalaGlobalDB.scalaAccountsDAO.activeAccounts()
        activeAccounts.forEach {
            val activeAccount = it.copy(refreshToken = (REFRESH_TOKEN_SUFFIX + it.refreshToken))

            val isDataComplete = isDataComplete(serverConfig, activeAccount)
            val ssoId = activeAccount.ssoId?.let { ssoId -> mapper.fromScalaSsoID(ssoId) }

            val authToken = if (isDataComplete) {
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
            }.fold({ return Either.Left(it) }, { it })

            coreLogic.globalScope {
                addAuthenticatedAccount(
                    serverConfigId = serverConfig.id,
                    ssoId = ssoId,
                    authTokens = authToken,
                    replace = false
                )
            }.also {
                if (it is AddAuthenticatedUserUseCase.Result.Failure.Generic) {
                    return Either.Left(it.genericFailure)
                }
            }
        }
        return Either.Right(Unit)
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
        const val REFRESH_TOKEN_SUFFIX = "zuid="
    }
}


sealed class MigrationFailure: CoreFailure.FeatureFailure() {
    object InvalidRefreshToken: MigrationFailure()
}


