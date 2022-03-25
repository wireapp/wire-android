package com.wire.android.di

import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.feature.client.ClientScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ClientScopeProvider @AssistedInject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    @Assisted private val authSession: AuthSession
) {
    val clientScope: ClientScope
        get() = coreLogic.getSessionScope(authSession.userId).client

    @AssistedFactory
    interface Factory {
        fun create(authSession: AuthSession): ClientScopeProvider
    }
}
