package com.wire.android.ui.debugscreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.platformLogWriter
import com.wire.android.util.DataDogLogger
import com.wire.android.util.LogFileWriter
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logic.CoreLogger
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountResult
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountUseCase
import com.wire.kalium.logic.feature.user.loggingStatus.EnableLoggingUseCase
import com.wire.kalium.logic.feature.user.loggingStatus.IsLoggingEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugScreenViewModel
@Inject constructor(
    private val mlsKeyPackageCountUseCase: MLSKeyPackageCountUseCase,
    private val enableLoggingUseCase: EnableLoggingUseCase,
    private val logFileWriter: LogFileWriter,
    isLoggingEnabledUseCase: IsLoggingEnabledUseCase
) : ViewModel() {
    var isLoggingEnabled by mutableStateOf(isLoggingEnabledUseCase())

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
