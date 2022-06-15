package com.wire.android.ui.debugscreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountResult
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountUseCase
import com.wire.kalium.logic.feature.user.EnableLoggingUseCase
import com.wire.kalium.logic.feature.user.IsLoggingEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugScreenViewModel
@Inject constructor(
    private val mlsKeyPackageCountUseCase: MLSKeyPackageCountUseCase,
    private val enableLoggingUseCase: EnableLoggingUseCase,
    private val isLoggingEnabled: IsLoggingEnabledUseCase
) : ViewModel() {
    var mlsData by mutableStateOf(listOf<String>())

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

    fun isLoggingEnabled(): Boolean {
        return isLoggingEnabled.invoke()
    }


    fun enableLogging(isEnabled: Boolean) {
        enableLoggingUseCase.invoke(isEnabled)
    }

}

