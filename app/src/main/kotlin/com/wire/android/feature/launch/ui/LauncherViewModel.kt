package com.wire.android.feature.launch.ui

import androidx.lifecycle.ViewModel
import com.wire.android.shared.auth.activeuser.GetActiveUserUseCase

class LauncherViewModel(private val getActiveUserUseCase: GetActiveUserUseCase) : ViewModel() {

    fun hasActiveUser() = getActiveUserUseCase.hasActiveUser()
}
