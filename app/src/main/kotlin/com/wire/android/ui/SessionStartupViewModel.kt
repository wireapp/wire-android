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

package com.wire.android.ui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.startup.KaliumStartup
import com.wire.kalium.logic.startup.StartupFailure
import com.wire.kalium.logic.startup.StartupResult
import com.wire.kalium.logic.startup.StartupState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal data class BlockingWorkPresentationPolicy(
    val revealDelay: Duration = 750.milliseconds,
    val minimumVisibleDuration: Duration = 500.milliseconds,
)

internal sealed interface SessionStartupUiState {
    data object ResolvingCurrentSession : SessionStartupUiState

    data class Working(
        val technicalState: StartupState,
        val showBlockingMigration: Boolean,
    ) : SessionStartupUiState

    data class Ready(val userId: UserId?) : SessionStartupUiState

    data class Failed(
        val userId: UserId,
        val failure: StartupFailure,
    ) : SessionStartupUiState
}

internal class SessionStartupViewModel(
    private val startup: KaliumStartup,
    private val resolveCurrentSession: suspend () -> CurrentSessionResult,
    private val dispatchers: DispatcherProvider,
    private val presentationPolicy: BlockingWorkPresentationPolicy = BlockingWorkPresentationPolicy(),
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
) : ViewModel() {
    private val stateMutable = MutableStateFlow<SessionStartupUiState>(
        SessionStartupUiState.ResolvingCurrentSession
    )
    val state: StateFlow<SessionStartupUiState> = stateMutable.asStateFlow()

    private var preparationJob: Job? = null

    init {
        prepareSession()
    }

    fun retry() {
        val failure = stateMutable.value as? SessionStartupUiState.Failed ?: return
        if (!failure.failure.isRetryable) return
        prepareSession(retryUserId = failure.userId)
    }

    private fun prepareSession(retryUserId: UserId? = null) {
        if (preparationJob?.isActive == true) return

        preparationJob = viewModelScope.launch(dispatchers.main()) {
            val userId = retryUserId ?: resolveValidCurrentUserId()
            if (userId == null) {
                stateMutable.value = SessionStartupUiState.Ready(userId = null)
                return@launch
            }

            awaitSessionStartup(userId = userId, retry = retryUserId != null)
        }
    }

    private suspend fun resolveValidCurrentUserId(): UserId? =
        withContext(dispatchers.io()) {
            (resolveCurrentSession() as? CurrentSessionResult.Success)
                ?.accountInfo
                ?.takeIf { it.isValid() }
                ?.userId
        }

    private suspend fun awaitSessionStartup(userId: UserId, retry: Boolean) {
        val handle = startup.session(userId)
        stateMutable.value = SessionStartupUiState.Working(
            technicalState = handle.state.value,
            showBlockingMigration = false,
        )

        var revealedAtMillis: Long? = null
        val stateCollection = viewModelScope.launch(dispatchers.main()) {
            handle.state.collect { technicalState ->
                stateMutable.update { current ->
                    (current as? SessionStartupUiState.Working)?.copy(
                        technicalState = technicalState
                    ) ?: current
                }
            }
        }
        val reveal = viewModelScope.launch(dispatchers.main()) {
            delay(presentationPolicy.revealDelay)
            stateMutable.update { current ->
                if (current is SessionStartupUiState.Working) {
                    revealedAtMillis = elapsedRealtime()
                    current.copy(showBlockingMigration = true)
                } else {
                    current
                }
            }
        }

        val result = if (retry) handle.retry() else handle.open()
        reveal.cancel()
        stateCollection.cancel()

        when (result) {
            is StartupResult.Success -> {
                revealedAtMillis?.let { revealedAt ->
                    val visibleFor = (elapsedRealtime() - revealedAt).milliseconds
                    val remaining = presentationPolicy.minimumVisibleDuration - visibleFor
                    if (remaining.isPositive()) delay(remaining)
                }
                stateMutable.value = SessionStartupUiState.Ready(userId)
            }

            is StartupResult.Failure -> {
                stateMutable.value = SessionStartupUiState.Failed(userId, result.failure)
            }
        }
    }
}
