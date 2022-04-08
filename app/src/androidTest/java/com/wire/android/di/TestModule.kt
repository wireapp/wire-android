package com.wire.android.di

import com.wire.android.utils.EMAIL
import com.wire.android.utils.PASSWORD
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.ServerConfig
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.session.CurrentSessionResult
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
            val result = restoreSession(coreLogic)
            if (result != null) {
                result.userId
            } else {
                // execute manual login
                val authResult = coreLogic.getAuthenticationScope()
                    .login(EMAIL, PASSWORD, false, ServerConfig.DEFAULT)

                if (authResult is AuthenticationResult.Success) {
                    // persist locally the session if successful
                    coreLogic.getAuthenticationScope().addAuthenticatedAccount.invoke(authResult.userSession)
                    authResult.userSession.userId
                } else {
                    throw RuntimeException("Failed to setup testing custom injection")
                }
            }
        }
    }
}

private suspend fun restoreSession(coreLogic: CoreLogic): AuthSession? {
    return coreLogic.authenticationScope {
        when (val currentSessionResult = session.currentSession()) {
            is CurrentSessionResult.Success -> currentSessionResult.authSession
            else -> null
        }
    }
}
