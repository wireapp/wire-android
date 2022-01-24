package com.wire.android.feature.auth.login

import com.wire.kalium.logic.CoreLogic
import javax.inject.Inject
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import dagger.hilt.android.scopes.ViewModelScoped

@ViewModelScoped
class LoginUseCase @Inject constructor(private val coreLogic: CoreLogic) {
    suspend operator fun invoke(userIdentifier: String, password: String): AuthenticationResult =
        coreLogic.authenticationScope {
            login(userIdentifier, password, true)
        }
}
