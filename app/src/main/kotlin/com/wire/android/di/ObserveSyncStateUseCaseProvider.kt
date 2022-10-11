package com.wire.android.di

import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ObserveSyncStateUseCaseProvider @AssistedInject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    @Assisted private val userId: UserId
) {
    val observeSyncState: ObserveSyncStateUseCase
        get() = coreLogic.getSessionScope(userId).observeSyncState

    @AssistedFactory
    interface Factory {
        fun create(userId: UserId): ObserveSyncStateUseCaseProvider
    }
}
