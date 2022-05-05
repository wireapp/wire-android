package com.wire.android.di

import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.GetIncomingCallsUseCase
import com.wire.kalium.logic.feature.message.GetNotificationsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class GetIncomingCallsUseCaseProvider @AssistedInject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    @Assisted private val userId: UserId
) {
    val getCalls: GetIncomingCallsUseCase
        get() = coreLogic.getSessionScope(userId).calls.getIncomingCalls

    @AssistedFactory
    interface Factory {
        fun create(userId: UserId): GetIncomingCallsUseCaseProvider
    }
}
