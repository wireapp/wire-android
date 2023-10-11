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

import com.wire.android.datastore.GlobalDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObserveAppLockConfigUseCase @Inject constructor(
    private val globalDataStore: GlobalDataStore,
) {

    operator fun invoke(): Flow<AppLockConfig> =
        globalDataStore.getAppLockPasscodeFlow().map { // TODO: include checking if any logged account does not enforce app-lock
            when {
                it.isNullOrEmpty() -> AppLockConfig.Disabled
                else -> AppLockConfig.Enabled
            }
        }
}

sealed class AppLockConfig(open val timeoutInSeconds: Int = DEFAULT_TIMEOUT) {
    data object Disabled : AppLockConfig()
    data object Enabled : AppLockConfig()
    data class EnforcedByTeam(override val timeoutInSeconds: Int) : AppLockConfig(timeoutInSeconds)

    companion object {
        const val DEFAULT_TIMEOUT = 60
    }
}
