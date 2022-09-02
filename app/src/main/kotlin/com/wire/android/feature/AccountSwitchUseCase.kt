package com.wire.android.feature

import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.UpdateCurrentSessionUseCase
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class AccountSwitchUseCase @Inject constructor(
    private val updateCurrentSessionUseCase: UpdateCurrentSessionUseCase,
    private val navigationManager: NavigationManager
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
}
