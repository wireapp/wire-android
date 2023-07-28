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
import com.wire.android.di.scopedArgs
import com.wire.android.model.ActionableState
import com.wire.android.model.finishAction
import com.wire.android.model.performAction
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.EXTRA_CONNECTION_IGNORED_USER_NAME
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.id.QualifiedID
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

    fun actionableState(): ActionableState
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
    savedStateHandle: SavedStateHandle
) : ConnectionActionButtonViewModel, ViewModel() {

    private val args: ConnectionActionButtonArgs = savedStateHandle.scopedArgs()
    private val userId: QualifiedID = args.userId
    private val userName: String = args.userName

    private var state: ActionableState by mutableStateOf(ActionableState())

    private val _infoMessage = MutableSharedFlow<UIText>()
    val infoMessage = _infoMessage.asSharedFlow()

    override fun actionableState(): ActionableState = state

    override fun onSendConnectionRequest() {
        viewModelScope.launch {
            state = state.performAction()
            when (sendConnectionRequest(userId)) {
                is SendConnectionRequestResult.Failure -> {
                    appLogger.e(("Couldn't send a connection request to user ${userId.toLogString()}"))
                    state = state.finishAction()
                    _infoMessage.emit(UIText.StringResource(R.string.connection_request_sent_error))
                }

                is SendConnectionRequestResult.Success -> {
                    state = state.finishAction()
                    _infoMessage.emit(UIText.StringResource(R.string.connection_request_sent))
                }
            }
        }
    }

    override fun onCancelConnectionRequest() {
        viewModelScope.launch {
            state = state.performAction()
            when (cancelConnectionRequest(userId)) {
                is CancelConnectionRequestUseCaseResult.Failure -> {
                    appLogger.e(("Couldn't cancel a connection request to user ${userId.toLogString()}"))
                    state = state.finishAction()
                    _infoMessage.emit(UIText.StringResource(R.string.connection_request_cancel_error))
                }

                is CancelConnectionRequestUseCaseResult.Success -> {
                    state = state.finishAction()
                    _infoMessage.emit(UIText.StringResource(R.string.connection_request_canceled))
                }
            }
        }
    }

    override fun onAcceptConnectionRequest() {
        viewModelScope.launch {
            state = state.performAction()
            when (acceptConnectionRequest(userId)) {
                is AcceptConnectionRequestUseCaseResult.Failure -> {
                    appLogger.e(("Couldn't accept a connection request to user ${userId.toLogString()}"))
                    state = state.finishAction()
                    _infoMessage.emit(UIText.StringResource(R.string.connection_request_accept_error))
                }

                is AcceptConnectionRequestUseCaseResult.Success -> {
                    state = state.finishAction()
                    _infoMessage.emit(UIText.StringResource(R.string.connection_request_accepted))
                }
            }
        }
    }

    override fun onIgnoreConnectionRequest() {
        viewModelScope.launch {
            state = state.performAction()
            when (ignoreConnectionRequest(userId)) {
                is IgnoreConnectionRequestUseCaseResult.Failure -> {
                    appLogger.e(("Couldn't ignore a connection request to user ${userId.toLogString()}"))
                    state = state.finishAction()
                    _infoMessage.emit(UIText.StringResource(R.string.connection_request_ignore_error))
                }

                is IgnoreConnectionRequestUseCaseResult.Success -> {
                    state = state.finishAction()
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
            state = state.performAction()
            when (val result = withContext(dispatchers.io()) { unblockUser(userId) }) {
                is UnblockUserResult.Failure -> {
                    appLogger.e("Error while unblocking user ${userId.toLogString()} ; Error ${result.coreFailure}")
                    state = state.finishAction()
                    _infoMessage.emit(UIText.StringResource(R.string.error_unblocking_user))
                }

                UnblockUserResult.Success -> {
                    appLogger.i("User ${userId.toLogString()} was unblocked")
                    state = state.finishAction()
                }
            }
        }
    }

    override fun onOpenConversation() {
        viewModelScope.launch {
            state = state.performAction()
            when (val result = withContext(dispatchers.io()) { getOrCreateOneToOneConversation(userId) }) {
                is CreateConversationResult.Failure -> {
                    appLogger.d(("Couldn't retrieve or create the conversation"))
                    state = state.finishAction()
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
}

@Suppress("EmptyFunctionBlock")
class ConnectionActionButtonPreviewModel(private val state: ActionableState) : ConnectionActionButtonViewModel {
    override fun actionableState(): ActionableState = state
    override fun onSendConnectionRequest() {}
    override fun onCancelConnectionRequest() {}
    override fun onAcceptConnectionRequest() {}
    override fun onIgnoreConnectionRequest() {}
    override fun onUnblockUser() {}
    override fun onOpenConversation() {}
}
