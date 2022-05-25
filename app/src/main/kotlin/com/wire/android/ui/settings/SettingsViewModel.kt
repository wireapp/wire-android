package com.wire.android.ui.settings

import androidx.lifecycle.ViewModel
import com.wire.kalium.logic.feature.user.EnableLoggingUseCase
import com.wire.kalium.logic.feature.user.IsLoggingEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class SettingsViewModel
@Inject constructor(
    private val enableLoggingUseCase: EnableLoggingUseCase,
    private val isLoggingEnabled: IsLoggingEnabledUseCase

) : ViewModel() {

    fun isLoggingEnabled(): Boolean {
        return isLoggingEnabled.invoke()
    }


    fun enableLogging(isEnabled: Boolean) {
        enableLoggingUseCase.invoke(isEnabled)
    }

}
