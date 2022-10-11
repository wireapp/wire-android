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
import com.wire.kalium.logic.functional.Either
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
        activeAccounts.forEach { activeAccount ->
            val userId = handleMigratingUserId(activeAccount, serverConfig)
            val ssoId = activeAccount.ssoId?.let { mapper.fromScalaSsoID(it) }
            val authTokens = handleAuthTokens(userId, activeAccount)
            coreLogic.globalScope {
                addAuthenticatedAccount(
                    serverConfigId = serverConfig.id,
                    ssoId = ssoId,
                    authTokens = authTokens,
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

    private fun handleUserDomain(activeAccount: ScalaActiveAccountsEntity, serverConfig: ServerConfig): String =
        activeAccount.domain ?: serverConfig.metaData.domain ?: TODO("domain need to be fetched form remote")

    private fun handleMigratingUserId(activeAccountsEntity: ScalaActiveAccountsEntity, serverConfig: ServerConfig): UserId {
        val userIdValue = activeAccountsEntity.id
        val userDomain = handleUserDomain(activeAccountsEntity, serverConfig)
        return UserId(userIdValue, userDomain)
    }

    private fun handleAuthTokens(userId: UserId, activeAccount: ScalaActiveAccountsEntity): AuthTokens {

        val refreshToken = activeAccount.refreshToken
        val accessToken = activeAccount.accessToken?.token ?: TODO("need to fetch new access token from remote")
        val tokenType = activeAccount.accessToken?.tokenType ?: TODO("need to fetch new access token from remote")
        return AuthTokens(
            userId = userId,
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = tokenType
        )
    }
}
