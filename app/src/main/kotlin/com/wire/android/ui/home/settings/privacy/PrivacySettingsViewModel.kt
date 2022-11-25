package com.wire.android.ui.home.settings.privacy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.user.readReceipts.IsReadReceiptsEnabledUseCase
import com.wire.kalium.logic.feature.user.readReceipts.PersistReadReceiptsStatusConfigUseCase
import com.wire.kalium.logic.feature.user.readReceipts.ReadReceiptStatusConfigResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider,
    private val persistReadReceiptsStatusConfig: PersistReadReceiptsStatusConfigUseCase,
    private val isReadReceiptsEnabled: IsReadReceiptsEnabledUseCase,
) : ViewModel() {

    var state by mutableStateOf(PrivacySettingsState())
        private set

    init {
        viewModelScope.launch {
            val isReadReceiptsEnabled = isReadReceiptsEnabled()
            state = state.copy(isReadReceiptsEnabled = isReadReceiptsEnabled)
        }
    }

    fun navigateBack() {
        viewModelScope.launch { navigationManager.navigateBack() }
    }

    fun setReadReceiptsState(isEnabled: Boolean) {
        viewModelScope.launch {
            when (withContext(dispatchers.io()) { persistReadReceiptsStatusConfig(isEnabled) }) {
                is ReadReceiptStatusConfigResult.Failure -> {
                    appLogger.e("Something went wrong while updating read receipts config")
                    state = state.copy(isReadReceiptsEnabled = !isEnabled)
                }
                is ReadReceiptStatusConfigResult.Success -> {
                    appLogger.d("Read receipts config changed")
                    state = state.copy(isReadReceiptsEnabled = isEnabled)
                }
            }
        }
    }
}
