package com.wire.android.ui.home.sync

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ShareCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.home.FeatureFlagState
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AccountInfo
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeatureFlagNotificationViewModel @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val getSessions: GetSessionsUseCase,
    private val currentSessionUseCase: CurrentSessionUseCase
) : ViewModel() {

    var featureFlagState by mutableStateOf(FeatureFlagState())
        private set

    fun loadSync() {
        viewModelScope.launch {
            currentSessionUseCase().let {
                when (it) {
                    is CurrentSessionResult.Failure -> {
                        appLogger.e("Failure while getting current session from FeatureFlagNotificationViewModel")
                    }
                    is CurrentSessionResult.Success -> {
                        coreLogic.getSessionScope(it.accountInfo.userId).observeSyncState().collect { newState ->
                            if (newState == SyncState.Live) {
                                setFileSharingState(it.accountInfo.userId)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setFileSharingState(userId: UserId) {
        viewModelScope.launch {
            coreLogic.getSessionScope(userId).observeFileSharingStatus().collect {
                if (it.isFileSharingEnabled != null) {
                    featureFlagState = featureFlagState.copy(isFileSharingEnabledState = it.isFileSharingEnabled!!)
                }
                if (it.isStatusChanged != null && it.isStatusChanged!!) {
                    featureFlagState = featureFlagState.copy(showFileSharingDialog = it.isStatusChanged!!)
                }
            }
        }
    }

    fun hideDialogStatus() {
        featureFlagState = featureFlagState.copy(showFileSharingDialog = false)
    }

    private suspend fun checkNumberOfSessions(): Int {
        getSessions().let {
            return when (it) {
                is GetAllSessionsResult.Success -> {
                    it.sessions.filterIsInstance<AccountInfo.Valid>().size
                }

                is GetAllSessionsResult.Failure.Generic -> 0
                GetAllSessionsResult.Failure.NoSessionFound -> 0
            }
        }
    }

    fun updateSharingStateIfNeeded(activity: AppCompatActivity) {
        viewModelScope.launch {
            val incomingIntent = ShareCompat.IntentReader(activity)
            if (incomingIntent.isShareIntent) {
                if (checkNumberOfSessions() > 0) {
                    featureFlagState = if (!featureFlagState.isFileSharingEnabledState) {
                        featureFlagState.copy(showFileSharingRestrictedDialog = true)
                    } else {
                        featureFlagState.copy(openImportMediaScreen = true, showFileSharingRestrictedDialog = false)
                    }
                }
            }
        }
    }
}
