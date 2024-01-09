/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */
package com.wire.android.ui.home.appLock.forgot

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountParam
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.WireNotificationManager
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.client.DeleteClientParam
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.client.DeleteClientResult
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class ForgotLockScreenViewModel @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val globalDataStore: GlobalDataStore,
    private val userDataStoreProvider: UserDataStoreProvider,
    private val notificationChannelsManager: NotificationChannelsManager,
    private val notificationManager: WireNotificationManager,
    private val getSelf: GetSelfUserUseCase,
    private val isPasswordRequired: IsPasswordRequiredUseCase,
    private val validatePassword: ValidatePasswordUseCase,
    private val observeCurrentClientId: ObserveCurrentClientIdUseCase,
    private val deleteClient: DeleteClientUseCase,
    private val getSessions: GetSessionsUseCase,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val endCall: EndCallUseCase,
    private val accountSwitch: AccountSwitchUseCase,
) : ViewModel() {

    var state: ForgotLockCodeViewState by mutableStateOf(ForgotLockCodeViewState())
        private set

    private fun updateIfDialogStateVisible(update: (ForgotLockCodeDialogState.Visible) -> ForgotLockCodeDialogState) {
        (state.dialogState as? ForgotLockCodeDialogState.Visible)?.let { dialogStateVisible ->
            state = state.copy(dialogState = update(dialogStateVisible))
        }
    }

    fun onPasswordChanged(password: TextFieldValue) {
        updateIfDialogStateVisible { it.copy(password = password, resetDeviceEnabled = password.text.isNotBlank()) }
    }

    fun onResetDevice() {
        viewModelScope.launch {
            state = when (val isPasswordRequiredResult = isPasswordRequired()) {
                is IsPasswordRequiredUseCase.Result.Success -> {
                    state.copy(
                        dialogState = ForgotLockCodeDialogState.Visible(
                            username = getSelf().firstOrNull()?.name ?: "",
                            passwordRequired = isPasswordRequiredResult.value,
                            resetDeviceEnabled = !isPasswordRequiredResult.value,
                        )
                    )
                }
                is IsPasswordRequiredUseCase.Result.Failure -> {
                    appLogger.e("$TAG Failed to check if password is required when opening reset passcode dialog")
                    state.copy(error = isPasswordRequiredResult.cause)
                }
            }
        }
    }

    fun onDialogDismissed() {
        state = state.copy(dialogState = ForgotLockCodeDialogState.Hidden)
    }

    fun onErrorDismissed() {
        state = state.copy(error = null)
    }

    fun onResetDeviceConfirmed() {
        (state.dialogState as? ForgotLockCodeDialogState.Visible)?.let { dialogStateVisible ->
            updateIfDialogStateVisible { it.copy(resetDeviceEnabled = false) }
            viewModelScope.launch {
                validatePasswordIfNeeded(dialogStateVisible.password.text)
                    .flatMapIfSuccess { validatedPassword ->
                        updateIfDialogStateVisible { it.copy(loading = true) }
                        deleteCurrentClient(validatedPassword)
                    }
                    .flatMapIfSuccess { hardLogoutAllAccounts() }
                    .let { result ->
                        when (result) {
                            is Result.Failure.Generic -> {
                                state = state.copy(error = result.cause)
                                updateIfDialogStateVisible { it.copy(loading = false, resetDeviceEnabled = true) }
                            }
                            Result.Failure.PasswordRequired ->
                                updateIfDialogStateVisible { it.copy(passwordRequired = true, passwordValid = false, loading = false) }
                            Result.Failure.InvalidPassword ->
                                updateIfDialogStateVisible { it.copy(passwordValid = false, loading = false) }
                            Result.Success ->
                                state = state.copy(completed = true, dialogState = ForgotLockCodeDialogState.Hidden)
                        }
                    }
            }
        }
    }

    @VisibleForTesting
    internal suspend fun validatePasswordIfNeeded(password: String): Pair<Result, String> =
        when (val isPasswordRequiredResult = isPasswordRequired()) {
            is IsPasswordRequiredUseCase.Result.Failure -> {
                appLogger.e("$TAG Failed to check if password is required when resetting passcode")
                Result.Failure.Generic(isPasswordRequiredResult.cause) to password
            }
            is IsPasswordRequiredUseCase.Result.Success -> when {
                isPasswordRequiredResult.value && password.isBlank() -> Result.Failure.PasswordRequired to password
                isPasswordRequiredResult.value && !validatePassword(password).isValid -> Result.Failure.InvalidPassword to password
                else -> Result.Success to if (isPasswordRequiredResult.value) password else ""
            }
        }

    @VisibleForTesting
    internal suspend fun deleteCurrentClient(password: String): Result =
        observeCurrentClientId()
            .filterNotNull()
            .first()
            .let { clientId ->
                when (val deleteClientResult = deleteClient(DeleteClientParam(password, clientId))) {
                    is DeleteClientResult.Failure.Generic -> {
                        appLogger.e("$TAG Failed to delete current client when resetting passcode")
                        Result.Failure.Generic(deleteClientResult.genericFailure)
                    }
                    DeleteClientResult.Success -> Result.Success
                    else -> Result.Failure.InvalidPassword
                }
            }

    @VisibleForTesting
    internal suspend fun hardLogoutAllAccounts(): Result =
        when (val getAllSessionsResult = getSessions()) {
            is GetAllSessionsResult.Failure.Generic -> {
                appLogger.e("$TAG Failed to get all sessions when resetting passcode")
                Result.Failure.Generic(getAllSessionsResult.genericFailure)
            }
            is GetAllSessionsResult.Failure.NoSessionFound,
            is GetAllSessionsResult.Success -> {
                observeEstablishedCalls().firstOrNull()?.let { establishedCalls ->
                    establishedCalls.forEach { endCall(it.conversationId) }
                }
                val sessions = if (getAllSessionsResult is GetAllSessionsResult.Success) getAllSessionsResult.sessions else emptyList()
                sessions.filterIsInstance<AccountInfo.Valid>().map { session ->
                    viewModelScope.launch {
                        hardLogoutAccount(session.userId)
                    }
                }.joinAll() // wait until all accounts are logged out
                globalDataStore.clearAppLockPasscode()
                accountSwitch(SwitchAccountParam.Clear)
                Result.Success
            }
        }

    // TODO: we should have a dedicated manager to perform these required actions in AR after every LogoutUseCase call
    private suspend fun hardLogoutAccount(userId: UserId) {
        notificationManager.stopObservingOnLogout(userId)
        notificationChannelsManager.deleteChannelGroup(userId)
        coreLogic.getSessionScope(userId).logout(reason = LogoutReason.SELF_HARD_LOGOUT, waitUntilCompletes = true)
        userDataStoreProvider.getOrCreate(userId).clear()
    }

    internal sealed class Result {
        sealed class Failure : Result() {
            data object InvalidPassword : Failure()
            data object PasswordRequired : Failure()
            data class Generic(val cause: CoreFailure) : Failure()
        }
        data object Success : Result()
    }

    private inline fun Result.flatMapIfSuccess(block: () -> Result): Result =
        if (this is Result.Success) block() else this

    private inline fun <T> Pair<Result, T>.flatMapIfSuccess(block: (T) -> Result): Result =
        if (this.first is Result.Success) block(this.second) else this.first

    companion object {
        const val TAG = "ForgotLockResetPasscode"
    }
}
