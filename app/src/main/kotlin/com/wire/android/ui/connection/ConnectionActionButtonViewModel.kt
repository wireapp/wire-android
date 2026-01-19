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

package com.wire.android.ui.connection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.di.ViewModelScopedPreview
import com.wire.android.di.scopedArgs
import com.wire.android.ui.common.ActionsManager
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.id.ConversationId
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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScopedPreview
interface ConnectionActionButtonViewModel : ActionsManager<ConnectionButtonAction> {
    val infoMessage: SharedFlow<UIText> get() = MutableSharedFlow()
    fun actionableState(): ConnectionActionState = ConnectionActionState()
    fun onSendConnectionRequest() {}
    fun onCancelConnectionRequest() {}
    fun onAcceptConnectionRequest() {}
    fun onIgnoreConnectionRequest() {}
    fun onUnblockUser() {}
    fun onMissingLegalHoldConsentDismissed() {}
    fun onOpenConversation() {}
}

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
internal class ConnectionActionButtonViewModelImpl @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val sendConnectionRequest: SendConnectionRequestUseCase,
    private val cancelConnectionRequest: CancelConnectionRequestUseCase,
    private val acceptConnectionRequest: AcceptConnectionRequestUseCase,
    private val ignoreConnectionRequest: IgnoreConnectionRequestUseCase,
    private val unblockUser: UnblockUserUseCase,
    private val getOrCreateOneToOneConversation: GetOrCreateOneToOneConversationUseCase,
    savedStateHandle: SavedStateHandle
) : ConnectionActionButtonViewModel, ActionsViewModel<ConnectionButtonAction>() {

    private val args: ConnectionActionButtonArgs = savedStateHandle.scopedArgs()
    private val userId: QualifiedID = args.userId
    val userName: String = args.userName

    var state: ConnectionActionState by mutableStateOf(ConnectionActionState())

    private val _infoMessage = MutableSharedFlow<UIText>()
    override val infoMessage = _infoMessage.asSharedFlow()

    override fun actionableState(): ConnectionActionState = state

    override fun onSendConnectionRequest() {
        if (state.isPerformingAction) return
        state = state.performAction()
        viewModelScope.launch {
            when (sendConnectionRequest(userId)) {
                is SendConnectionRequestResult.Success -> {
                    _infoMessage.emit(UIText.StringResource(R.string.connection_request_sent))
                }

                is SendConnectionRequestResult.Failure.MissingLegalHoldConsent -> {
                    appLogger.d(("Couldn't send a connect request to user ${userId.toLogString()} - missing legal hold consent"))
                    state = state.copy(missingLegalHoldConsentDialogState = MissingLegalHoldConsentDialogState.Visible(userId))
                }

                is SendConnectionRequestResult.Failure.FederationDenied -> {
                    appLogger.d(("Couldn't send a connect request to user ${userId.toLogString()} - federation denied"))
                    _infoMessage.emit(UIText.StringResource(R.string.connection_request_sent_federation_denied_error, userName))
                }

                is SendConnectionRequestResult.Failure -> {
                    appLogger.d(("Couldn't send a connect request to user ${userId.toLogString()}"))
                    _infoMessage.emit(UIText.StringResource(R.string.connection_request_sent_error))
                }
            }
            state = state.finishAction()
        }
    }

    override fun onCancelConnectionRequest() {
        if (state.isPerformingAction) return
        state = state.performAction()
        viewModelScope.launch {
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
        if (state.isPerformingAction) return
        state = state.performAction()
        viewModelScope.launch {
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
        if (state.isPerformingAction) return
        state = state.performAction()
        viewModelScope.launch {
            when (ignoreConnectionRequest(userId)) {
                is IgnoreConnectionRequestUseCaseResult.Failure -> {
                    appLogger.e(("Couldn't ignore a connection request to user ${userId.toLogString()}"))
                    state = state.finishAction()
                    _infoMessage.emit(UIText.StringResource(R.string.connection_request_ignore_error))
                }

                is IgnoreConnectionRequestUseCaseResult.Success -> {
                    sendAction(ConnectionRequestIgnored(userName))
                    state = state.finishAction()
                }
            }
        }
    }

    override fun onUnblockUser() {
        if (state.isPerformingAction) return
        state = state.performAction()
        viewModelScope.launch {
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
        if (state.isPerformingAction) return
        state = state.performAction()
        viewModelScope.launch {
            val result = withContext(dispatchers.io()) {
                getOrCreateOneToOneConversation(userId)
            }
            when (result) {
                is CreateConversationResult.Failure -> {
                    appLogger.d(("Couldn't retrieve or create the conversation"))
                    state = state.finishAction()
                    if (result.coreFailure is CoreFailure.MissingKeyPackages) {
                        sendAction(MissingKeyPackages)
                    }
                }

                is CreateConversationResult.Success -> {
                    sendAction(OpenConversation(result.conversation.id))
                    state = state.finishAction()
                }
            }
        }
    }

    override fun onMissingLegalHoldConsentDismissed() {
        state = state.copy(missingLegalHoldConsentDialogState = MissingLegalHoldConsentDialogState.Hidden)
    }
}

sealed interface ConnectionButtonAction
internal data class OpenConversation(val conversationId: ConversationId) : ConnectionButtonAction
internal data object MissingKeyPackages : ConnectionButtonAction
internal data class ConnectionRequestIgnored(val userName: String) : ConnectionButtonAction
