package com.wire.android.ui.debugscreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.platformLogWriter
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.DataDogLogger
import com.wire.android.util.EMPTY
import com.wire.android.util.LogFileWriter
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logic.CoreLogger
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountResult
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountUseCase
import com.wire.kalium.logic.feature.user.loggingStatus.EnableLoggingUseCase
import com.wire.kalium.logic.feature.user.loggingStatus.IsLoggingEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DebugScreenState(
    val isLoggingEnabled: Boolean = false,
    val currentClientId: String = String.EMPTY,
    val keyPackagesCount: Int = 0,
    val mslClientId: String = String.EMPTY
)

@HiltViewModel
class DebugScreenViewModel
@Inject constructor(
    private val navigationManager: NavigationManager,
    private val mlsKeyPackageCountUseCase: MLSKeyPackageCountUseCase,
    private val enableLogging: EnableLoggingUseCase,
    private val logFileWriter: LogFileWriter,
    private val currentClientIdUseCase: ObserveCurrentClientIdUseCase,
    isLoggingEnabledUseCase: IsLoggingEnabledUseCase
) : ViewModel() {
    val logPath: String = logFileWriter.activeLoggingFile.absolutePath

    var state by mutableStateOf(
        DebugScreenState(
            isLoggingEnabled = isLoggingEnabledUseCase()
        )
    )

    init {
        observeMlsMetadata()
        observeCurrentClientId()
    }

    private fun observeCurrentClientId() {
        viewModelScope.launch {
            currentClientIdUseCase().collect {
                val clientId = it?.let { clientId -> clientId.value } ?: "Client not fount"
                state = state.copy(currentClientId = clientId)
            }
        }
    }

    private fun observeMlsMetadata() {
        viewModelScope.launch {
            mlsKeyPackageCountUseCase().let {
                when (it) {
                    is MLSKeyPackageCountResult.Success -> {
                        state = state.copy(
                            keyPackagesCount = it.count,
                            mslClientId = it.clientId.value
                        )
                    }
                    is MLSKeyPackageCountResult.Failure.NetworkCallFailure -> {
//                        state = state.copy(mlsData = listOf("Network Error!"))
                    }
                    is MLSKeyPackageCountResult.Failure.FetchClientIdFailure -> {
//                        state = state.copy(mlsData = listOf("ClientId Fetch Error!"))
                    }
                    is MLSKeyPackageCountResult.Failure.Generic -> {}
                }
            }
        }
    }

    fun deleteLogs() {
        logFileWriter.deleteAllLogFiles()
    }

    fun setLoggingEnabledState(isEnabled: Boolean) {
        enableLogging(isEnabled)
        state = state.copy(isLoggingEnabled = isEnabled)
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
