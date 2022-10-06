package com.wire.android.scalaMigration.feature

import com.wire.android.scalaMigration.MigrationMapper
import com.wire.android.scalaMigration.globalDatabase.ScalaAccountsDAO
import com.wire.android.scalaMigration.globalDatabase.ScalaActiveAccountsEntity
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthTokens

class MigrateActiveAccountsUseCase internal constructor(
    private val addAuthenticatedAccount: AddAuthenticatedUserUseCase,
    private val scalaAccountsDAO: ScalaAccountsDAO,
    private val serverConfig: ServerConfig,
    private val mapper: MigrationMapper = MigrationMapper
) {
    suspend operator fun invoke() {
        val activeAccounts = scalaAccountsDAO.activeAccounts()
        activeAccounts.forEach { activeAccount ->
            val userId = handleMigratingUserId(activeAccount, serverConfig)
            val ssoId = activeAccount.ssoId?.let { mapper.fromScalaSsoID(it) }
            val authTokens = handleAuthTokens(userId, activeAccount)
            addAuthenticatedAccount(
                serverConfigId = serverConfig.id,
                ssoId = ssoId,
                authTokens = authTokens,
                replace = false
            )
        }
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
        val tokenType = activeAccount.accessToken?.tokenType ?:  TODO("need to fetch new access token from remote")
        return AuthTokens(
            userId = userId,
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = tokenType
        )
    }
}
