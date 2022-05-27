package com.wire.android.di

import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class UserSessionScopeProvider @AssistedInject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    @Assisted private val userId: UserId
) {

    val userSessionScope: UserSessionScope
        get() = coreLogic.getSessionScope(userId)

    @AssistedFactory
    interface Factory {
        fun create(userId: UserId): UserSessionScopeProvider
    }
}
