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

import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShouldStartPersistentWebSocketServiceUseCase @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic
) {
    suspend operator fun invoke(): Result {
        return coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus().let { result ->
            when (result) {
                is ObservePersistentWebSocketConnectionStatusUseCase.Result.Failure -> Result.Failure

                is ObservePersistentWebSocketConnectionStatusUseCase.Result.Success -> {
                    val statusList = withTimeoutOrNull(TIMEOUT) {
                        val res = result.persistentWebSocketStatusListFlow.firstOrNull()
                        res
                    }
                    if (statusList != null && statusList.map { it.isPersistentWebSocketEnabled }.contains(true)) {
                        Result.Success(true)
                    } else {
                        Result.Success(false)
                    }
                }
            }
        }
    }

    sealed class Result {
        data object Failure : Result()
        data class Success(val shouldStartPersistentWebSocketService: Boolean) : Result()
    }

    companion object {
        const val TIMEOUT = 10_000L
    }
}
