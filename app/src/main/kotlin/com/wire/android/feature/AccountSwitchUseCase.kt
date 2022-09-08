package com.wire.android.feature

import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.session.UpdateCurrentSessionUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountSwitchUseCase @Inject constructor(
    private val updateCurrentSessionUseCase: UpdateCurrentSessionUseCase,
    private val navigationManager: NavigationManager,
    private val getSessionsUseCase: GetSessionsUseCase
) {
    suspend operator fun invoke(userId: UserId?) {
        val navigationDistention = if (userId == null) {
            NavigationItem.Welcome
        } else {
            NavigationItem.Home
        }.getRouteWithArgs()

        when (updateCurrentSessionUseCase(userId)) {
            is UpdateCurrentSessionUseCase.Result.Success -> {
                navigationManager.navigate(
                    NavigationCommand(
                        navigationDistention,
                        BackStackMode.CLEAR_WHOLE
                    )
                )
            }
            is UpdateCurrentSessionUseCase.Result.Failure -> {
                return
            }
        }
    }

    suspend fun switchToNextAccountOrWelcome() {
        val nextSessionId: UserId? = getSessionsUseCase().let {
            when(it) {
                is GetAllSessionsResult.Failure.Generic -> null
                GetAllSessionsResult.Failure.NoSessionFound -> null
                is GetAllSessionsResult.Success ->
                    it.sessions.firstOrNull { sessionList -> sessionList.token is AuthSession.Token.Valid }?.token?.userId
            }
        }
        invoke(nextSessionId)
    }
}
