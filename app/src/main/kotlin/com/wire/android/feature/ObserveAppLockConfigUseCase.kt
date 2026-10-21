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
import com.wire.android.session.observeCurrentValidSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.SingleIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SingleIn(AppScope::class)
class ObserveAppLockConfigUseCase @Inject constructor(
    private val globalDataStore: GlobalDataStore,
    @KaliumCoreLogic private val coreLogic: CoreLogic
) {
    operator fun invoke(): Flow<AppLockConfig> = coreLogic.getGlobalScope().session.currentSessionFlow()
        .observeCurrentValidSession(coreLogic, waitForAdmission = true) {
            appLockTeamFeatureConfigObserver().combine(globalDataStore.isAppLockPasscodeSetFlow()) { teamConfig, passcodeSet ->
                when {
                    passcodeSet -> AppLockConfig.Enabled(teamConfig?.timeout ?: DEFAULT_APP_LOCK_TIMEOUT)
                    teamConfig != null && teamConfig.isEnforced -> AppLockConfig.EnforcedByTeam(teamConfig.timeout)
                    else -> AppLockConfig.Disabled(teamConfig?.timeout ?: DEFAULT_APP_LOCK_TIMEOUT)
                }
            }
        }
        // Pending admission of raw Valid keeps the last security config; Invalid/missing still disables it.
        .map { it ?: AppLockConfig.Disabled(DEFAULT_APP_LOCK_TIMEOUT) }

    companion object {
        val DEFAULT_APP_LOCK_TIMEOUT = 60.seconds
    }
}

sealed class AppLockConfig(open val timeout: Duration) {
    data class Disabled(override val timeout: Duration) : AppLockConfig(timeout)
    data class Enabled(override val timeout: Duration) : AppLockConfig(timeout)
    data class EnforcedByTeam(override val timeout: Duration) : AppLockConfig(timeout)
}
