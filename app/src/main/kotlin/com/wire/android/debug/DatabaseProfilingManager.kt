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
package com.wire.android.debug

import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.functional.mapToRightOr
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseProfilingManager @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val globalDataStore: GlobalDataStore,
) {

    suspend fun observeAndUpdateProfiling() {
        globalDataStore.isLoggingEnabled()
            .flatMapLatest { isLoggingEnabled ->
                coreLogic.getGlobalScope().sessionRepository.allValidSessionsFlow()
                    .mapToRightOr(emptyList())
                    .map { it.map { it.userId } }
                    .scan(emptyList<UserId>()) { previousList, currentList -> currentList - previousList.toSet() }
                    .map { userIds -> isLoggingEnabled to userIds }
            }
            .filter { (_, userIds) -> userIds.isNotEmpty() }
            .distinctUntilChanged()
            .collect { (isLoggingEnabled, userIds) ->
                userIds.forEach { userId ->
                    coreLogic.getSessionScope(userId).debug.changeProfiling(isLoggingEnabled)
                }
            }
    }
}
