package com.wire.android.di

import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.client.ClientScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ClientScopeProvider @AssistedInject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    @Assisted private val userId: String
) {
    val clientScope: ClientScope
        get() = coreLogic.getSessionScope(userId).client

    @AssistedFactory
    interface Factory {
        fun create(userId: String): ClientScopeProvider
    }
}
