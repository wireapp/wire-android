package com.wire.android.feature

import com.wire.android.di.ApplicationScope
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AccountInfo
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.session.UpdateCurrentSessionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("LongParameterList")
@Singleton
class AccountSwitchUseCase @Inject constructor(
    private val updateCurrentSession: UpdateCurrentSessionUseCase,
    private val navigationManager: NavigationManager,
    private val getSessions: GetSessionsUseCase,
    private val getCurrentSession: CurrentSessionUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    @ApplicationScope private val coroutineScope: CoroutineScope,
) {

    val currentAccount
        get() = when (val result = getCurrentSession()) {
            is CurrentSessionResult.Failure.Generic -> null
            CurrentSessionResult.Failure.SessionNotFound -> null
            is CurrentSessionResult.Success -> result.accountInfo
        }

    suspend operator fun invoke(params: SwitchAccountParam) {
        val current = currentAccount
        when (params) {
            is SwitchAccountParam.SwitchToAccount -> switch(params.userId, current)
            SwitchAccountParam.SwitchToNextAccountOrWelcome -> switchToNextAccountOrWelcome(current)
        }
    }

    private suspend fun switchToNextAccountOrWelcome(current: AccountInfo?) {

        val nextSessionId: UserId? = getSessions().let {
            when (it) {
                is GetAllSessionsResult.Failure.Generic -> null
                GetAllSessionsResult.Failure.NoSessionFound -> null
                is GetAllSessionsResult.Success ->
                    it.sessions.firstOrNull { accountInfo ->
                        (accountInfo is AccountInfo.Valid) &&
                                accountInfo.userId != current?.userId
                    }?.userId
            }
        }
        switch(nextSessionId, current)
    }

    private suspend fun switch(userId: UserId?, current: AccountInfo?) {
        val navigationDistention = if (userId == null) {
            NavigationItem.Welcome
        } else {
            NavigationItem.Home
        }.getRouteWithArgs()

        when (updateCurrentSession(userId)) {
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
        current?.also {
            handleOldSession(it)
        }
    }

    private fun handleOldSession(oldSession: AccountInfo) {
        when (oldSession) {
            is AccountInfo.Valid -> {
                // do nothing
            }
            is AccountInfo.Invalid -> coroutineScope.launch {
                handleInvalidSession(oldSession)
            }
        }
    }

    private suspend fun handleInvalidSession(invalidAccount: AccountInfo.Invalid) {
        when (invalidAccount.logoutReason) {
            LogoutReason.SELF_SOFT_LOGOUT, LogoutReason.SELF_HARD_LOGOUT -> {
                // do nothing the logout use case will handle this
            }
            LogoutReason.DELETED_ACCOUNT,
            LogoutReason.REMOVED_CLIENT,
            LogoutReason.SESSION_EXPIRED -> deleteSessionUseCase(invalidAccount.userId)
        }
    }
}

sealed class SwitchAccountParam {
    object SwitchToNextAccountOrWelcome : SwitchAccountParam()
    data class SwitchToAccount(val userId: UserId) : SwitchAccountParam()
}
