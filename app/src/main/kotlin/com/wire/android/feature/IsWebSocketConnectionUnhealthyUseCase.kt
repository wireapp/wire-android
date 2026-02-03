/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours

/**
 * Use case to check if the WebSocket connection is unhealthy.
 * A connection is considered unhealthy if any user with persistent WebSocket enabled
 * has not received any WebSocket events in the last [UNHEALTHY_THRESHOLD].
 *
 * @return [Result.success] with `true` if connection is unhealthy, `false` if healthy,
 *         or [Result.failure] if the check could not be performed.
 */
@Singleton
class IsWebSocketConnectionUnhealthyUseCase @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic
) {
    @Suppress("ReturnCount")
    suspend operator fun invoke(): Result<Boolean> {
        return coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus().let { result ->
            when (result) {
                is ObservePersistentWebSocketConnectionStatusUseCase.Result.Failure ->
                    Result.failure(IllegalStateException("Failed to observe persistent WebSocket status"))

                is ObservePersistentWebSocketConnectionStatusUseCase.Result.Success -> {
                    val statusList = withTimeoutOrNull(TIMEOUT) {
                        result.persistentWebSocketStatusListFlow.firstOrNull()
                    }
                    if (statusList == null) {
                        return Result.failure(IllegalStateException("Failed to fetch WebSocket status"))
                    }

                    val usersWithPersistentWebSocket = statusList.filter { it.isPersistentWebSocketEnabled }
                    if (usersWithPersistentWebSocket.isEmpty()) {
                        return Result.success(false)
                    }

                    val now = Clock.System.now()
                    val isUnhealthy = usersWithPersistentWebSocket.any { status ->
                        val lastEventInstant = coreLogic.getSessionScope(status.userId)
                            .getLastWebSocketEventInstant()
                        val timeSinceLastEvent = lastEventInstant?.let { now - it }

                        // Connection is unhealthy only if we received events before but threshold exceeded
                        // null means service just started and hasn't received events yet - not unhealthy
                        lastEventInstant != null && timeSinceLastEvent!! > UNHEALTHY_THRESHOLD
                    }

                    Result.success(isUnhealthy)
                }
            }
        }
    }

    companion object {
        const val TIMEOUT = 10_000L
        val UNHEALTHY_THRESHOLD = 12.hours
    }
}
