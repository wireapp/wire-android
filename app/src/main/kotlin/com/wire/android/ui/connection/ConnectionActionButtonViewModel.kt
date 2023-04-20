/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.connection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.EXTRA_CONNECTION_IGNORED_USER_NAME
import com.wire.android.navigation.EXTRA_CONNECTION_STATE
import com.wire.android.navigation.EXTRA_USER_ID
import com.wire.android.navigation.EXTRA_USER_NAME
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.common.snackbar.ShowSnackBarUseCase
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.toQualifiedID
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.feature.connection.AcceptConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.AcceptConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.CancelConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.CancelConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.IgnoreConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.IgnoreConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.UnblockUserResult
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.conversation.CreateConversationResult
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

abstract class ConnectionActionButtonBaseViewModel : ViewModel() {

    abstract fun state() : ConnectionState
    abstract fun onSendConnectionRequest()
    abstract fun onCancelConnectionRequest()
    abstract fun onAcceptConnectionRequest()
    abstract fun onIgnoreConnectionRequest()
    abstract fun onUnblockUser()
    abstract fun onOpenConversation()
}

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class ConnectionActionButtonViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider,
    private val sendConnectionRequest: SendConnectionRequestUseCase,
    private val cancelConnectionRequest: CancelConnectionRequestUseCase,
    private val acceptConnectionRequest: AcceptConnectionRequestUseCase,
    private val ignoreConnectionRequest: IgnoreConnectionRequestUseCase,
    private val unblockUser: UnblockUserUseCase,
    private val getOrCreateOneToOneConversation: GetOrCreateOneToOneConversationUseCase,
    private val showSnackBarUseCase: ShowSnackBarUseCase,
    private val savedStateHandle: SavedStateHandle,
    qualifiedIdMapper: QualifiedIdMapper
) : ConnectionActionButtonBaseViewModel() {

    private val userId: QualifiedID = savedStateHandle.get<String>(EXTRA_USER_ID)!!.toQualifiedID(qualifiedIdMapper)
    private val userName: String = savedStateHandle.get<String>(EXTRA_USER_NAME)!!
    private val extraConnectionState: ConnectionState = ConnectionState.valueOf(savedStateHandle.get<String>(EXTRA_CONNECTION_STATE)!!)

    var state: ConnectionState by mutableStateOf(extraConnectionState)
    var requestInProgress: Boolean by mutableStateOf(false)
    override fun state(): ConnectionState = state

    override fun onSendConnectionRequest() {
        viewModelScope.launch {
            when (sendConnectionRequest(userId)) {
                is SendConnectionRequestResult.Failure -> {
                    appLogger.d(("Couldn't send a connect request to user $userId"))
                    showSnackBarUseCase(UIText.StringResource(R.string.connection_request_sent_error))
                }

                is SendConnectionRequestResult.Success -> {
                    state = ConnectionState.SENT
                    showSnackBarUseCase(UIText.StringResource(R.string.connection_request_sent))
                }
            }
        }
    }

    override fun onCancelConnectionRequest() {
        viewModelScope.launch {
            when (cancelConnectionRequest(userId)) {
                is CancelConnectionRequestUseCaseResult.Failure -> {
                    appLogger.d(("Couldn't cancel a connect request to user $userId"))
                    showSnackBarUseCase(UIText.StringResource(R.string.connection_request_cancel_error))
                }

                is CancelConnectionRequestUseCaseResult.Success -> {
                    state = ConnectionState.NOT_CONNECTED
                    showSnackBarUseCase(UIText.StringResource(R.string.connection_request_canceled))
                }
            }
        }
    }

    override fun onAcceptConnectionRequest() {
        viewModelScope.launch {
            when (acceptConnectionRequest(userId)) {
                is AcceptConnectionRequestUseCaseResult.Failure -> {
                    appLogger.d(("Couldn't accept a connect request to user $userId"))
                    showSnackBarUseCase(UIText.StringResource(R.string.connection_request_accept_error))
                }

                is AcceptConnectionRequestUseCaseResult.Success -> {
                    state = ConnectionState.ACCEPTED
                    showSnackBarUseCase(UIText.StringResource(R.string.connection_request_accepted))
                }
            }
        }
    }

    override fun onIgnoreConnectionRequest() {
        viewModelScope.launch {
            when (ignoreConnectionRequest(userId)) {
                is IgnoreConnectionRequestUseCaseResult.Failure -> {
                    appLogger.d(("Couldn't ignore a connect request to user $userId"))
                    showSnackBarUseCase(UIText.StringResource(R.string.connection_request_ignore_error))
                }

                is IgnoreConnectionRequestUseCaseResult.Success -> {
                    state = ConnectionState.IGNORED
                    navigationManager.navigateBack(
                        mapOf(
                            EXTRA_CONNECTION_IGNORED_USER_NAME to userName
                        )
                    )
                }
            }
        }
    }

    override fun onUnblockUser() {
        viewModelScope.launch {
            requestInProgress = true
            when (val result = withContext(dispatchers.io()) { unblockUser(userId) }) {
                UnblockUserResult.Success -> {
                    appLogger.i("User $userId was unblocked")
                }

                is UnblockUserResult.Failure -> {
                    appLogger.e("Error while unblocking user $userId ; Error ${result.coreFailure}")
                    showSnackBarUseCase(UIText.StringResource(R.string.error_unblocking_user))
                }
            }
            requestInProgress = false
        }
    }

    override fun onOpenConversation() {
        viewModelScope.launch {
            when (val result = withContext(dispatchers.io()) { getOrCreateOneToOneConversation(userId) }) {
                is CreateConversationResult.Failure -> appLogger.d(("Couldn't retrieve or create the conversation"))
                is CreateConversationResult.Success ->
                    navigationManager.navigate(
                        command = NavigationCommand(
                            destination = NavigationItem.Conversation.getRouteWithArgs(listOf(result.conversation.id)),
                            backStackMode = BackStackMode.UPDATE_EXISTED
                        )
                    )
            }
        }
    }

    companion object {
        const val MY_ARGS_KEY = "ConnectionActionButtonViewModelKey"
    }
}

@Suppress("EmptyFunctionBlock")
class ConnectionActionButtonPreviewModel(private val state: ConnectionState) : ConnectionActionButtonBaseViewModel() {
    override fun state(): ConnectionState = state
    override fun onSendConnectionRequest() {}
    override fun onCancelConnectionRequest() {}
    override fun onAcceptConnectionRequest() {}
    override fun onIgnoreConnectionRequest() {}
    override fun onUnblockUser() {}
    override fun onOpenConversation() {}
}
