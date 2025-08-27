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

package com.wire.android.ui.debug

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.CurrentAccount
import com.wire.android.util.EMPTY
import com.wire.android.util.logging.LogFileWriter
import com.wire.kalium.common.logger.CoreLogger
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.debug.ChangeProfilingUseCase
import com.wire.kalium.logic.feature.debug.ObserveDatabaseLoggerStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserDebugState(
    val isLoggingEnabled: Boolean = false,
    val isDBLoggingEnabled: Boolean = false,
    val clientId: String = String.EMPTY,
    val commitish: String = String.EMPTY,
    val debugId: String = String.EMPTY,
    val logPath: String,
    val isWireCellFeatureEnabled: Boolean = false,
)

@Suppress("LongParameterList")
@HiltViewModel
class UserDebugViewModel
@Inject constructor(
    @CurrentAccount val currentAccount: UserId,
    private val logFileWriter: LogFileWriter,
    private val currentClientIdUseCase: ObserveCurrentClientIdUseCase,
    private val globalDataStore: GlobalDataStore,
    private val changeProfilingUseCase: ChangeProfilingUseCase,
    private val observeDatabaseLoggerState: ObserveDatabaseLoggerStateUseCase
) : ViewModel() {

    var state by mutableStateOf(
        UserDebugState(logPath = logFileWriter.activeLoggingFile.absolutePath)
    )

    init {
        observeLoggingState()
        observeCurrentClientId()
        observeDBLoggingState()
    }

    fun setDatabaseLoggerEnabledState(isEnabled: Boolean) {
        viewModelScope.launch {
            changeProfilingUseCase(isEnabled)
        }
    }

    fun observeDBLoggingState() {
        viewModelScope.launch {
            observeDatabaseLoggerState().collect {
                state = state.copy(isDBLoggingEnabled = it)
            }
        }
    }

    fun deleteLogs() {
        logFileWriter.deleteAllLogFiles()
    }

    fun flushLogs(): Deferred<Unit> {
        return viewModelScope.async {
            logFileWriter.forceFlush()
        }
    }

    fun setLoggingEnabledState(isEnabled: Boolean) {
        viewModelScope.launch {
            globalDataStore.setLoggingEnabled(isEnabled)
            if (isEnabled) {
                logFileWriter.start()
                CoreLogger.setLoggingLevel(level = KaliumLogLevel.VERBOSE)
            } else {
                logFileWriter.stop()
                CoreLogger.setLoggingLevel(level = KaliumLogLevel.DISABLED)
            }
        }
    }

    private fun observeLoggingState() {
        viewModelScope.launch {
            globalDataStore.isLoggingEnabled().collect {
                state = state.copy(isLoggingEnabled = it)
            }
        }
    }

    private fun observeCurrentClientId() {
        viewModelScope.launch {
            currentClientIdUseCase().collect {
                val clientId = it?.let { clientId -> clientId.value } ?: "null"
                state = state.copy(clientId = clientId)
            }
        }
    }

    //endregion
}
