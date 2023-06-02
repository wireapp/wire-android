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
 *
 *
 */

package com.wire.android.ui.debug

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.platformLogWriter
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.CurrentAccount
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.DataDogLogger
import com.wire.android.util.EMPTY
import com.wire.android.util.LogFileWriter
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logic.CoreLogger
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserDebugState(
    val isLoggingEnabled: Boolean = false,
    val clientId: String = String.EMPTY
)

@Suppress("LongParameterList")
@HiltViewModel
class UserDebugViewModel
@Inject constructor(
    @ApplicationContext private val context: Context,
    @CurrentAccount val currentAccount: UserId,
    private val navigationManager: NavigationManager,
    private val logFileWriter: LogFileWriter,
    private val currentClientIdUseCase: ObserveCurrentClientIdUseCase,
    private val globalDataStore: GlobalDataStore
) : ViewModel() {

    val logPath: String = logFileWriter.activeLoggingFile.absolutePath

    var state by mutableStateOf(
        UserDebugState()
    )

    init {
        observeLoggingState()
        observeCurrentClientId()
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
                val clientId = it?.let { clientId -> clientId.value } ?: "Client not fount"
                state = state.copy(clientId = clientId)
            }
        }
    }

    fun deleteLogs() {
        logFileWriter.deleteAllLogFiles()
    }

    fun setLoggingEnabledState(isEnabled: Boolean) {
        viewModelScope.launch {
            globalDataStore.setLoggingEnabled(isEnabled)
        }
        if (isEnabled) {
            logFileWriter.start()
            CoreLogger.setLoggingLevel(level = KaliumLogLevel.VERBOSE, logWriters = arrayOf(DataDogLogger, platformLogWriter()))
        } else {
            logFileWriter.stop()
            CoreLogger.setLoggingLevel(level = KaliumLogLevel.DISABLED, logWriters = arrayOf(DataDogLogger, platformLogWriter()))
        }
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }
}
