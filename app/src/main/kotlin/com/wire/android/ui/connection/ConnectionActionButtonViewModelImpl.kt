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
import com.wire.android.model.LoadableState
import com.wire.android.model.finishLoading
import com.wire.android.model.startLoading
import com.wire.android.model.updateState
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.EXTRA_CONNECTION_IGNORED_USER_NAME
import com.wire.android.navigation.EXTRA_CONNECTION_STATE
import com.wire.android.navigation.EXTRA_USER_ID
import com.wire.android.navigation.EXTRA_USER_NAME
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface ConnectionActionButtonViewModel {

    fun loadableState(): LoadableState<ConnectionState>
    fun onSendConnectionRequest()
    fun onCancelConnectionRequest()
    fun onAcceptConnectionRequest()
    fun onIgnoreConnectionRequest()
    fun onUnblockUser()
    fun onOpenConversation()
}

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class ConnectionActionButtonViewModelImpl @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider,
    private val sendConnectionRequest: SendConnectionRequestUseCase,
    private val cancelConnectionRequest: CancelConnectionRequestUseCase,
    private val acceptConnectionRequest: AcceptConnectionRequestUseCase,
    private val ignoreConnectionRequest: IgnoreConnectionRequestUseCase,
    private val unblockUser: UnblockUserUseCase,
    private val getOrCreateOneToOneConversation: GetOrCreateOneToOneConversationUseCase,
    savedStateHandle: SavedStateHandle,
    qualifiedIdMapper: QualifiedIdMapper
) : ConnectionActionButtonViewModel, ViewModel() {

    private val userId: QualifiedID = savedStateHandle.get<String>(EXTRA_USER_ID)!!.toQualifiedID(qualifiedIdMapper)
    private val userName: String = savedStateHandle.get<String>(EXTRA_USER_NAME)!!
    private val extraConnectionState: ConnectionState = ConnectionState.valueOf(savedStateHandle.get<String>(EXTRA_CONNECTION_STATE)!!)

    private var state: LoadableState<ConnectionState> by mutableStateOf(LoadableState(extraConnectionState))

    private val _infoMessage = MutableSharedFlow<UIText>()
    val infoMessage = _infoMessage.asSharedFlow()

    override fun loadableState(): LoadableState<ConnectionState> = state

    override fun onSendConnectionRequest() {
        viewModelScope.launch {
            state = state.startLoading()
            when (sendConnectionRequest(userId)) {
                is SendConnectionRequestResult.Failure -> {
                    appLogger.d(("Couldn't send a connect request to user $userId"))
                    state = state.finishLoading()
                    _infoMessage.emit(UIText.StringResource(R.string.connection_request_sent_error))
                }

                is SendConnectionRequestResult.Success -> {
                    state = state.updateState(ConnectionState.SENT)
                    _infoMessage.emit(UIText.StringResource(R.string.connection_request_sent))
                }
            }
        }
    }

    override fun onCancelConnectionRequest() {
        viewModelScope.launch {
            state = state.startLoading()
            when (cancelConnectionRequest(userId)) {
                is CancelConnectionRequestUseCaseResult.Failure -> {
                    appLogger.d(("Couldn't cancel a connect request to user $userId"))
                    state = state.finishLoading()
                    _infoMessage.emit(UIText.StringResource(R.string.connection_request_cancel_error))
                }

                is CancelConnectionRequestUseCaseResult.Success -> {
                    state = state.updateState(ConnectionState.NOT_CONNECTED)
                    _infoMessage.emit(UIText.StringResource(R.string.connection_request_canceled))
                }
            }
        }
    }

    override fun onAcceptConnectionRequest() {
        viewModelScope.launch {
            state = state.startLoading()
            when (acceptConnectionRequest(userId)) {
                is AcceptConnectionRequestUseCaseResult.Failure -> {
                    appLogger.d(("Couldn't accept a connect request to user $userId"))
                    state = state.finishLoading()
                    _infoMessage.emit(UIText.StringResource(R.string.connection_request_accept_error))
                }

                is AcceptConnectionRequestUseCaseResult.Success -> {
                    state = state.updateState(ConnectionState.ACCEPTED)
                    _infoMessage.emit(UIText.StringResource(R.string.connection_request_accepted))
                }
            }
        }
    }

    override fun onIgnoreConnectionRequest() {
        viewModelScope.launch {
            state = state.startLoading()
            when (ignoreConnectionRequest(userId)) {
                is IgnoreConnectionRequestUseCaseResult.Failure -> {
                    appLogger.d(("Couldn't ignore a connect request to user $userId"))
                    state = state.finishLoading()
                    _infoMessage.emit(UIText.StringResource(R.string.connection_request_ignore_error))
                }

                is IgnoreConnectionRequestUseCaseResult.Success -> {
                    state = state.updateState(ConnectionState.IGNORED)
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
            state = state.startLoading()
            when (val result = withContext(dispatchers.io()) { unblockUser(userId) }) {
                is UnblockUserResult.Failure -> {
                    appLogger.e("Error while unblocking user $userId ; Error ${result.coreFailure}")
                    state = state.finishLoading()
                    _infoMessage.emit(UIText.StringResource(R.string.error_unblocking_user))
                }

                UnblockUserResult.Success -> {
                    appLogger.i("User $userId was unblocked")
                    state = state.updateState(ConnectionState.ACCEPTED)
                }
            }
        }
    }

    override fun onOpenConversation() {
        viewModelScope.launch {
            state = state.startLoading()
            when (val result = withContext(dispatchers.io()) { getOrCreateOneToOneConversation(userId) }) {
                is CreateConversationResult.Failure -> {
                    appLogger.d(("Couldn't retrieve or create the conversation"))
                    state = state.finishLoading()
                }

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
        const val ARGS_KEY = "ConnectionActionButtonViewModelKey"
    }
}

@Suppress("EmptyFunctionBlock")
class ConnectionActionButtonPreviewModel(private val state: LoadableState<ConnectionState>) : ConnectionActionButtonViewModel {
    override fun loadableState(): LoadableState<ConnectionState> = state
    override fun onSendConnectionRequest() {}
    override fun onCancelConnectionRequest() {}
    override fun onAcceptConnectionRequest() {}
    override fun onIgnoreConnectionRequest() {}
    override fun onUnblockUser() {}
    override fun onOpenConversation() {}
}
