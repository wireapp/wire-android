package com.wire.android.ui.debugscreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.kaliumFileWriter
import com.wire.android.util.LOG_FILE_NAME
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logic.CoreLogger
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountResult
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountUseCase
import com.wire.kalium.logic.feature.user.EnableLoggingUseCase
import com.wire.kalium.logic.feature.user.IsLoggingEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DebugScreenViewModel
@Inject constructor(
    private val mlsKeyPackageCountUseCase: MLSKeyPackageCountUseCase,
    private val enableLoggingUseCase: EnableLoggingUseCase,
    private val isLoggingEnabled: IsLoggingEnabledUseCase
) : ViewModel() {
    var mlsData by mutableStateOf(listOf<String>())
    val checkedState = remember { mutableStateOf(isLoggingEnabled.invoke()) }

    fun logFilePath(absolutePath: String) = "$absolutePath/logs/$LOG_FILE_NAME"

    init {
        viewModelScope.launch {
            mlsKeyPackageCountUseCase().let {
                when (it) {
                    is MLSKeyPackageCountResult.Success -> {
                        mlsData = listOf("KeyPackages Count: ${it.count}", "ClientId: ${it.clientId.value}")
                    }
                    is MLSKeyPackageCountResult.Failure.Generic -> {
                        mlsData = listOf("KeyPackages Count: ${it.genericFailure}")
                    }
                    else -> {}
                }
            }
        }
    }

    fun setLoggingEnabledState(isEnabled: Boolean, absolutePath: String) {
        enableLoggingUseCase.invoke(isEnabled)
        checkedState.value = isEnabled
        if (isEnabled) {
            kaliumFileWriter.init(absolutePath)
            CoreLogger.setLoggingLevel(
                level = KaliumLogLevel.DEBUG, kaliumFileWriter
            )
        } else {
            kaliumFileWriter.clearFileContent(
                File(logFilePath(absolutePath))
            )
            CoreLogger.setLoggingLevel(
                level = KaliumLogLevel.DISABLED, kaliumFileWriter
            )
        }
    }
}

