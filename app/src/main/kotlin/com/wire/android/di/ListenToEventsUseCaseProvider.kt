package com.wire.android.di

import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.sync.ListenToEventsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ListenToEventsUseCaseProvider @AssistedInject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    @Assisted private val userId: UserId
) {
    val listenToEvents: ListenToEventsUseCase
        get() = coreLogic.getSessionScope(userId).listenToEvents

    @AssistedFactory
    interface Factory {
        fun create(userId: UserId): ListenToEventsUseCaseProvider
    }
}
