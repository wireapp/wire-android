package com.wire.android.ui.debugscreen

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.platformLogWriter
import com.wire.android.ui.debugscreen.PersistentWebSocketService.Companion.ACTION_STOP_FOREGROUND
import com.wire.android.util.DataDogLogger
import com.wire.android.util.LogFileWriter
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logic.CoreLogger
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountResult
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountUseCase
import com.wire.kalium.logic.feature.user.loggingStatus.EnableLoggingUseCase
import com.wire.kalium.logic.feature.user.loggingStatus.IsLoggingEnabledUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.EnableWebSocketUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.IsWebSocketEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugScreenViewModel
@Inject constructor(
    private val mlsKeyPackageCountUseCase: MLSKeyPackageCountUseCase,
    private val enableLoggingUseCase: EnableLoggingUseCase,
    private val enableWebSocketUseCase: EnableWebSocketUseCase,
    private val logFileWriter: LogFileWriter,
    isLoggingEnabledUseCase: IsLoggingEnabledUseCase,
    isWebSocketEnabledUseCase: IsWebSocketEnabledUseCase
) : ViewModel() {
    var isLoggingEnabled by mutableStateOf(isLoggingEnabledUseCase())
    var isWebSocketEnabled by mutableStateOf(isWebSocketEnabledUseCase())

    var mlsData by mutableStateOf(listOf<String>())

    fun logFilePath(): String = logFileWriter.activeLoggingFile.absolutePath

    init {
        viewModelScope.launch {
            mlsKeyPackageCountUseCase().let {
                when (it) {
                    is MLSKeyPackageCountResult.Success -> {
                        mlsData = listOf("KeyPackages Count: ${it.count}", "ClientId: ${it.clientId.value}")
                    }
                    is MLSKeyPackageCountResult.Failure.NetworkCallFailure -> {
                        mlsData = listOf("Network Error!")
                    }
                    is MLSKeyPackageCountResult.Failure.FetchClientIdFailure -> {
                        mlsData = listOf("ClientId Fetch Error!")
                    }
                    else -> {}
                }
            }
        }
    }

    fun deleteAllLogs() {
        logFileWriter.deleteAllLogFiles()
    }

    fun setWebSocketState(isEnabled: Boolean, context: Context) {
        enableWebSocketUseCase(isEnabled)
        isWebSocketEnabled = isEnabled
        if (isEnabled) {
            context.startService(Intent(context, PersistentWebSocketService::class.java))
        } else {
            val intentStop = Intent(context, PersistentWebSocketService::class.java)
            intentStop.action = ACTION_STOP_FOREGROUND
            context.startService(intentStop)
        }
    }

    fun setLoggingEnabledState(isEnabled: Boolean) {
        enableLoggingUseCase(isEnabled)
        isLoggingEnabled = isEnabled
        if (isEnabled) {
            logFileWriter.start()
            CoreLogger.setLoggingLevel(level = KaliumLogLevel.VERBOSE, logWriters = arrayOf(DataDogLogger, platformLogWriter()))
        } else {
            logFileWriter.stop()
            CoreLogger.setLoggingLevel(level = KaliumLogLevel.DISABLED, logWriters = arrayOf(DataDogLogger, platformLogWriter()))
        }
    }
}

