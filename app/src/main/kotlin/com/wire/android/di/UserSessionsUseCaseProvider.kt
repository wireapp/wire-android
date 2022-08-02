package com.wire.android.di

import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class UserSessionsUseCaseProvider @AssistedInject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
) {
    val sessionsUseCase: GetSessionsUseCase
        get() = coreLogic.getGlobalScope().getSessions

    @AssistedFactory
    interface Factory {
        fun create(): UserSessionsUseCaseProvider
    }
}
