/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
import com.wire.android.util.logging.LogFileWriter
import com.wire.kalium.common.logger.CoreLogger
import com.wire.kalium.logger.KaliumLogLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogManagementState(
    val isLoggingEnabled: Boolean = false,
    val logPath: String
)

@HiltViewModel
class LogManagementViewModel @Inject constructor(
    private val logFileWriter: LogFileWriter,
    private val globalDataStore: GlobalDataStore
) : ViewModel() {

    var state by mutableStateOf(
        LogManagementState(logPath = logFileWriter.activeLoggingFile.absolutePath)
    )

    init {
        observeLoggingState()
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

    fun deleteLogs() {
        logFileWriter.deleteAllLogFiles()
    }

    fun flushLogs(): Deferred<Unit> {
        return viewModelScope.async {
            logFileWriter.forceFlush()
        }
    }

    private fun observeLoggingState() {
        viewModelScope.launch {
            globalDataStore.isLoggingEnabled().collect {
                state = state.copy(isLoggingEnabled = it)
            }
        }
    }
}
