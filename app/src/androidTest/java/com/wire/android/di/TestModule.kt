package com.wire.android.di

import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.ServerConfig
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
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
            when (val result = coreLogic.getAuthenticationScope().getSessions.invoke()) {
                is GetAllSessionsResult.Success -> result.sessions.first().userId
                else -> {
                    val userId = QualifiedID(value = "f48ea18d-4dfd-4adf-9252-9e48443c15c8", domain = "staging.zinfra.io")
                    val authResult = getAuthSessionSnapshot(userId = userId)
                    coreLogic.getAuthenticationScope().addAuthenticatedAccount.invoke(authSession = authResult)
                    userId
                }
            }
        }
    }
}

@Suppress("MaxLineLength")
private fun getAuthSessionSnapshot(userId: UserId) = AuthSession(
    userId = userId,
    accessToken = "SncygyfgwC628HSn1akeAP1KZIRr6UX994dl9Ap2FsSI18fHlYhVvjqEulPRMTjKgP9hYOSPPoD5QrjYqGERAg==.v=1.k=1.d=1649375417.t=a.l=.u=f48ea18d-4dfd-4adf-9252-9e48443c15c8.c=17777963853806308667", // ktlint-disable max-line-length
    refreshToken = "MGLVjdzdLBay0wtMAG56k3Nne9E08bRs6Q-XSO0C8tFqdDMJazYedyxixr0n9lfUScF7tOhmcLBN9g9NQ9eUBg==.v=1.k=1.d=1649460917.t=u.l=s.u=f48ea18d-4dfd-4adf-9252-9e48443c15c8.r=3becd00d", // ktlint-disable max-line-length
    tokenType = "Bearer",
    serverConfig = ServerConfig(
        apiBaseUrl = "staging-nginz-https.zinfra.io",
        accountsBaseUrl = "wire-account-staging.zinfra.io",
        webSocketBaseUrl = "staging-nginz-ssl.zinfra.io",
        blackListUrl = "clientblacklist.wire.com/staging",
        teamsUrl = "wire-teams-staging.zinfra.io",
        websiteUrl = "wire.com",
        title = "Staging"
    )
)
