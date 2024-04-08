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

package com.wire.android.ui.authentication.welcome

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.LogFileDirectory
import com.wire.android.util.LogFileWriter
import com.wire.android.util.saveFileToDownloadsFolder
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logic.CoreLogger
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.AccountInfo
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import java.io.File
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val authServerConfigProvider: AuthServerConfigProvider,
    private val getSessions: GetSessionsUseCase,
    @ApplicationContext private val context: Context,
    @LogFileDirectory private val logFileDirectory: File,
    private val globalDataStore: GlobalDataStore,
    private val logFileWriter: LogFileWriter
) : ViewModel() {

    var state by mutableStateOf(WelcomeScreenState(ServerConfig.DEFAULT))
        private set

    init {
        observerAuthServer()
        checkNumberOfSessions()
        observeLoggingState()
    }
// TODO: extract this logic into a shared viewModel between here and debug screen
    private fun observeLoggingState() {
        viewModelScope.launch {
            globalDataStore.isLoggingEnabled().collect {
                state = state.copy(isLoggingEnabled = it)
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
            CoreLogger.setLoggingLevel(level = KaliumLogLevel.VERBOSE)
        } else {
            logFileWriter.stop()
            CoreLogger.setLoggingLevel(level = KaliumLogLevel.DISABLED)
        }
    }

    private fun observerAuthServer() {
        viewModelScope.launch {
            authServerConfigProvider.authServer.collect {
                state = state.copy(links = it)
            }
        }
    }

    fun downloadLogsLocally() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                logFileDirectory.listFiles()?.forEach { logFile ->
                    saveFileToDownloadsFolder(logFile.name, logFile.absolutePath.toPath(), logFile.length(), context)
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Logs downloaded  ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkNumberOfSessions() {
        viewModelScope.launch {
            getSessions().let {
                when (it) {
                    is GetAllSessionsResult.Success -> {
                        state = state.copy(
                            isThereActiveSession = it.sessions.filterIsInstance<AccountInfo.Valid>().isEmpty().not(),
                            maxAccountsReached = it.sessions.filterIsInstance<AccountInfo.Valid>().size >= BuildConfig.MAX_ACCOUNTS
                        )
                    }

                    is GetAllSessionsResult.Failure.Generic -> {}
                    GetAllSessionsResult.Failure.NoSessionFound -> {
                        state = state.copy(isThereActiveSession = false)
                    }
                }
            }
        }
    }
}

fun ServerConfig.Links.isProxyEnabled() = this.apiProxy != null
