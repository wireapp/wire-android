/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combineTransform
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Singleton
class ObserveAppLockConfigUseCase @Inject constructor(
    private val globalDataStore: GlobalDataStore,
    @KaliumCoreLogic private val coreLogic: CoreLogic
) {
    operator fun invoke(): Flow<AppLockConfig> = channelFlow {
        coreLogic.getGlobalScope().session.currentSessionFlow().collectLatest { sessionResult ->
            when (sessionResult) {
                is CurrentSessionResult.Failure -> {
                    send(AppLockConfig.Disabled(DEFAULT_APP_LOCK_TIMEOUT))
                }

                is CurrentSessionResult.Success -> {
                    val userId = sessionResult.accountInfo.userId
                    val appLockTeamFeatureConfigFlow =
                        coreLogic.getSessionScope(userId).appLockTeamFeatureConfigObserver

                    appLockTeamFeatureConfigFlow().combineTransform(
                        globalDataStore.isAppLockPasscodeSetFlow()
                    ) { teamAppLockConfig, isAppLockConfigured ->
                        when {
                            isAppLockConfigured -> {
                                emit(AppLockConfig.Enabled(teamAppLockConfig?.timeout ?: DEFAULT_APP_LOCK_TIMEOUT))
                            }

                            teamAppLockConfig != null && teamAppLockConfig.isEnforced -> {
                                emit(AppLockConfig.EnforcedByTeam(teamAppLockConfig.timeout))
                            }

                            else -> {
                                emit(AppLockConfig.Disabled(teamAppLockConfig?.timeout ?: DEFAULT_APP_LOCK_TIMEOUT))
                            }
                        }
                    }.collectLatest {
                        send(it)
                    }
                }
            }
        }
    }

    companion object {
        val DEFAULT_APP_LOCK_TIMEOUT = 60.seconds
    }
}

sealed class AppLockConfig(open val timeout: Duration) {
    data class Disabled(override val timeout: Duration) : AppLockConfig(timeout)
    data class Enabled(override val timeout: Duration) : AppLockConfig(timeout)
    data class EnforcedByTeam(override val timeout: Duration) : AppLockConfig(timeout)
}
