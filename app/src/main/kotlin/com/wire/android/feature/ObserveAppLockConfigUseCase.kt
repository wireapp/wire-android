/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.feature

import com.wire.android.applock.passcode.isAppLockedByUserFlow
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.applock.AppLockTeamFeatureConfigObserverImpl.Companion.DEFAULT_TIMEOUT
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combineTransform
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration

@Singleton
class ObserveAppLockConfigUseCase @Inject constructor(
    private val globalDataStore: GlobalDataStore,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val currentSession: CurrentSessionUseCase
) {

    operator fun invoke(): Flow<AppLockConfig> = channelFlow {
        when (val currentSession = currentSession()) {
            is CurrentSessionResult.Failure -> {
                send(AppLockConfig.Disabled(DEFAULT_TIMEOUT))
            }

            is CurrentSessionResult.Success -> {
                val userId = currentSession.accountInfo.userId
                val appLockTeamFeatureConfigFlow =
                    coreLogic.getSessionScope(userId).appLockTeamFeatureConfigObserver

                appLockTeamFeatureConfigFlow().combineTransform(
                    globalDataStore.isAppLockedByUserFlow()
                ) { isAppLockedByTeam, isAppLocked ->
                    if (isAppLockedByTeam.isEnabled) {
                        emit(AppLockConfig.EnforcedByTeam(isAppLockedByTeam.timeout))
                    } else {
                        if (isAppLocked) {
                            emit(AppLockConfig.Enabled(isAppLockedByTeam.timeout))
                        } else {
                            emit(AppLockConfig.Disabled(isAppLockedByTeam.timeout))
                        }
                    }
                }.collectLatest {
                    send(it)
                }
            }
        }
    }
}

sealed class AppLockConfig(open val timeout: Duration = DEFAULT_TIMEOUT) {
    data class Disabled(override val timeout: Duration) : AppLockConfig(timeout)
    data class Enabled(override val timeout: Duration) : AppLockConfig(timeout)
    data class EnforcedByTeam(override val timeout: Duration) : AppLockConfig(timeout)
}
