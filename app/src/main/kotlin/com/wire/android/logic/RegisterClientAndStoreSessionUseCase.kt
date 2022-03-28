package com.wire.android.logic

import com.wire.android.di.ClientScopeProvider
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.session.SaveSessionUseCase
import com.wire.kalium.logic.feature.session.UpdateCurrentSessionUseCase

class RegisterClientAndStoreSessionUseCase(
    val clientScopeProviderFactory: ClientScopeProvider.Factory,
    val saveSessionUseCase: SaveSessionUseCase,
    val updateCurrentSessionUseCase: UpdateCurrentSessionUseCase
) {
    suspend operator fun invoke(authSession: AuthSession, password: String): RegisterClientResult =
        clientScopeProviderFactory.create(authSession.userId).clientScope.register(password, null)
            .let { registerClientResult ->
                if (registerClientResult is RegisterClientResult.Success) {
                    saveSessionUseCase(authSession)
                    updateCurrentSessionUseCase(authSession.userId)
                }
                registerClientResult
            }
}
