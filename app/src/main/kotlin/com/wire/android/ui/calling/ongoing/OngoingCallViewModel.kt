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

package com.wire.android.ui.calling.ongoing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig
import com.wire.android.appLogger
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.CurrentAccount
import com.wire.android.mapper.UICallParticipantMapper
import com.wire.android.ui.calling.model.InCallReaction
import com.wire.android.ui.calling.model.ReactionSender
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.fullscreen.SelectedParticipant
import com.wire.android.ui.calling.ongoing.incallreactions.InCallReactions
import com.wire.android.ui.calling.ongoing.toast.InCallToast
import com.wire.android.util.ExpiringMap
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.extension.withDelayAfterFirst
import com.wire.kalium.common.functional.onSuccess
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallClient
import com.wire.kalium.logic.data.call.CallModerationAction
import com.wire.kalium.logic.data.call.CallResolutionQuality
import com.wire.kalium.logic.data.call.CallingParticipantsOrderType
import com.wire.kalium.logic.data.call.VideoState
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.ObserveCallModerationActionsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveCallQualityDataUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveInCallReactionsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveLastActiveCallWithSortedParticipantsUseCase
import com.wire.kalium.logic.feature.call.usecase.RequestVideoStreamsUseCase
import com.wire.kalium.logic.feature.call.usecase.SetCallQualityIntervalUseCase
import com.wire.kalium.logic.feature.call.usecase.video.SetVideoSendStateUseCase
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.incallreaction.SendInCallReactionUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel(assistedFactory = OngoingCallViewModel.Factory::class)
class OngoingCallViewModel @AssistedInject constructor(
    @Assisted val conversationId: ConversationId,
    @CurrentAccount val currentUserId: UserId,
    private val globalDataStore: GlobalDataStore,
    private val observeLastActiveCall: ObserveLastActiveCallWithSortedParticipantsUseCase,
    private val requestVideoStreams: RequestVideoStreamsUseCase,
    private val setVideoSendState: SetVideoSendStateUseCase,
    private val observeCallQualityData: ObserveCallQualityDataUseCase,
    private val setCallQualityInterval: SetCallQualityIntervalUseCase,
    private val getCurrentClientId: ObserveCurrentClientIdUseCase,
    private val observeInCallReactionsUseCase: ObserveInCallReactionsUseCase,
    private val sendInCallReactionUseCase: SendInCallReactionUseCase,
    private val observeCallModerationActions: ObserveCallModerationActionsUseCase,
    private val uiCallParticipantMapper: UICallParticipantMapper,
    private val dispatchers: DispatcherProvider,
    private val currentTime: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    var state by mutableStateOf(OngoingCallState())
        private set

    private val _inCallReactions = Channel<InCallReaction>(
        capacity = 300, // Max reactions to keep in queue
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val inCallReactions = _inCallReactions.receiveAsFlow().withDelayAfterFirst(InCallReactions.reactionsThrottleDelayMs)
    val recentReactions: MutableMap<UserId, String> = ExpiringMap(
        scope = viewModelScope,
        currentTime = currentTime,
        expirationMs = InCallReactions.recentReactionShowDurationMs,
        delegate = mutableStateMapOf()
    )

    val toasts: ExpiringMap<String, InCallToast> = ExpiringMap(
        scope = viewModelScope,
        currentTime = currentTime,
        expirationMs = MODERATION_ACTION_TOAST_DISPLAY_TIME,
        delegate = mutableStateMapOf(),
        onEntryExpired = { toastId, _ ->
            handleDismissedToast(toastId)
        }
    )

    private val orderTypeStateFlow = MutableStateFlow(state.currentOrderType())

    init {
        viewModelScope.launch {
            val callSharedFlow = orderTypeStateFlow
                .flatMapLatest { orderType ->
                    observeLastActiveCall(conversationId = conversationId, orderType = orderType)
                }
                .flowOn(dispatchers.default()).shareIn(scope = this, started = SharingStarted.Lazily, replay = 1)

            observeCurrentCallFlowState(callSharedFlow)
            observeParticipants(callSharedFlow)
            observeModerationActions(callSharedFlow)
            if (BuildConfig.CALL_REACTIONS_ENABLED) observeInCallReactions()
            observeCallQuality()
            initialShowDoubleTapToFullscreenToast()
        }
    }

    fun startSendingVideoFeed() {
        viewModelScope.launch {
            setVideoSendState(conversationId, VideoState.STARTED)
        }
    }

    fun pauseSendingVideoFeed() {
        viewModelScope.launch {
            setVideoSendState(conversationId, VideoState.PAUSED)
        }
    }

    fun stopSendingVideoFeed() {
        viewModelScope.launch {
            setVideoSendState(conversationId, VideoState.STOPPED)
        }
    }

    private fun observeCurrentCallFlowState(sharedFlow: SharedFlow<Call?>) {
        viewModelScope.launch {
            sharedFlow
                .map { it != null }
                .distinctUntilChanged()
                .map { isActive ->
                    when (isActive) {
                        false -> OngoingCallState.FlowState.CallClosed
                        true -> OngoingCallState.FlowState.Default
                    }
                }
                .collectLatest { flowState ->
                    state = state.copy(flowState = flowState)
                }
        }
    }

    fun observeCallQuality() {
        viewModelScope.launch {
            observeCallQualityData(conversationId).collectLatest { callQualityData ->
                state = state.copy(callQualityData = callQualityData)
            }
        }
    }

    private fun observeParticipants(sharedFlow: SharedFlow<Call?>) {
        viewModelScope.launch {
            combine(
                getCurrentClientId().filterNotNull(),
                sharedFlow.filterNotNull(),
            ) { clientId, call ->
                call.participants.map {
                    uiCallParticipantMapper.toUICallParticipant(it, clientId)
                }.toPersistentList()
            }.collectLatest {
                state = state.copy(participants = it)
            }
        }
    }

    private fun observeModerationActions(callFlow: SharedFlow<Call?>) {
        viewModelScope.launch {
            observeCallModerationActions(conversationId)
                .map { action ->
                    callFlow.senderName(action.senderUserId)?.let { senderName ->
                        InCallToast.ModerationAction(
                            time = currentTime(),
                            actionId = action.id,
                            moderatorName = senderName,
                            type = when (action.type) {
                                CallModerationAction.Type.MUTED -> InCallToast.ModerationAction.Type.Muted
                            }
                        )
                    }
                }
                .filterNotNull()
                .collect { inCallToast ->
                    toasts.putWithExpireIn(inCallToast.id, inCallToast, MODERATION_ACTION_TOAST_DISPLAY_TIME)
                }
        }
    }

    private fun observeInCallReactions() {
        viewModelScope.launch {
            observeInCallReactionsUseCase(conversationId).collect { message ->

                val sender = state.participants.senderName(message.senderUserId)?.let { name ->
                    ReactionSender.Other(name)
                } ?: ReactionSender.Unknown

                message.emojis.forEach { emoji ->
                    _inCallReactions.send(InCallReaction(emoji, sender))
                }

                if (message.emojis.isNotEmpty()) {
                    recentReactions[message.senderUserId] = message.emojis.last()
                }
            }
        }
    }

    fun onReactionClick(emoji: String) {
        viewModelScope.launch {
            sendInCallReactionUseCase(conversationId, emoji).toEither()
                .onSuccess {
                    _inCallReactions.send(InCallReaction(emoji, ReactionSender.You))
                    recentReactions[currentUserId] = emoji
                }
        }
    }

    fun requestVideoStreams(participants: List<UICallParticipant>) {
        viewModelScope.launch {
            participants
                .filter {
                    (it.isCameraOn || it.isSharingScreen) && !state.othersVideosDisabled
                }
                .also {
                    val clients: List<CallClient> = it.map { uiParticipant ->
                        CallClient(
                            userId = uiParticipant.id.toString(),
                            clientId = uiParticipant.clientId,
                            quality = mapQualityStream(uiParticipant)
                        )
                    }
                    requestVideoStreams(conversationId, clients)
                }
        }
    }

    private fun mapQualityStream(uiParticipant: UICallParticipant): CallResolutionQuality {
        return if (uiParticipant.clientId == state.selectedParticipant?.clientId) {
            CallResolutionQuality.HIGH
        } else {
            CallResolutionQuality.LOW
        }
    }

    private fun initialShowDoubleTapToFullscreenToast() {
        viewModelScope.launch {
            delay(DELAY_TO_SHOW_DOUBLE_TAP_TOAST)
            if (globalDataStore.getShouldShowDoubleTapToast(currentUserId.toString())) {
                val doubleTapToOpenFullscreenToast = InCallToast.Fullscreen(currentTime(), InCallToast.Fullscreen.Type.DoubleTapToOpen)
                toasts.putWithExpireIn(doubleTapToOpenFullscreenToast.id, doubleTapToOpenFullscreenToast, DOUBLE_TAP_TOAST_DISPLAY_TIME)
            }
        }
    }

    fun dismissToast(toastId: String) {
        toasts.remove(toastId)
        handleDismissedToast(toastId)
    }

    private fun handleDismissedToast(toastId: String) {
        // if the "open fullscreen" toast is closed, update the data store so that it won't be shown again automatically
        if (toastId == InCallToast.Fullscreen.Type.DoubleTapToOpen.id) {
            viewModelScope.launch {
                globalDataStore.setShouldShowDoubleTapToastStatus(currentUserId.toString(), false)
            }
        }
    }

    fun onSelectedParticipant(selectedParticipant: SelectedParticipant?) {
        appLogger.d("$TAG - Selected participant: ${selectedParticipant?.toLogString()}")
        state = state.copy(selectedParticipant = selectedParticipant)
        if (selectedParticipant != null) { // fullscreen opened
            // remove "open fullscreen" toast when a participant is selected as it's no longer relevant, user already used that
            toasts.remove(InCallToast.Fullscreen.Type.DoubleTapToOpen.id)
            handleDismissedToast(InCallToast.Fullscreen.Type.DoubleTapToOpen.id)
            // instead, show "close fullscreen" toast to let user know how to exit the fullscreen, it shouldn't expire automatically
            val doubleTapToCloseFullscreenToast = InCallToast.Fullscreen(currentTime(), InCallToast.Fullscreen.Type.DoubleTapToClose)
            toasts.putNonExpiring(doubleTapToCloseFullscreenToast.id, doubleTapToCloseFullscreenToast)
        } else { // fullscreen closed
            // when exiting fullscreen, remove "close fullscreen" toast as it's no longer relevant
            toasts.remove(InCallToast.Fullscreen.Type.DoubleTapToClose.id)
        }
    }

    fun setOthersVideosDisabled(othersVideosDisabled: Boolean) {
        state = state.copy(othersVideosDisabled = othersVideosDisabled)
        orderTypeStateFlow.value = state.currentOrderType() // update order type based on current state to trigger participants reordering
    }

    fun setQualityInterval(interval: QualityInterval) {
        viewModelScope.launch {
            setCallQualityInterval(interval.intervalInSeconds)
        }
    }

    @Suppress("MagicNumber")
    enum class QualityInterval(val intervalInSeconds: Int) {
        NORMAL(5),
        SHORT(1)
    }

    companion object {
        const val MODERATION_ACTION_TOAST_DISPLAY_TIME = 3000L // aligned with other platforms
        const val DOUBLE_TAP_TOAST_DISPLAY_TIME = 7000L // according to the designs
        const val DELAY_TO_SHOW_DOUBLE_TAP_TOAST = 500L
        const val TAG = "OngoingCallViewModel"
    }

    @AssistedFactory
    interface Factory {
        fun create(conversationId: ConversationId): OngoingCallViewModel
    }
}

private fun List<UICallParticipant>.senderName(userId: QualifiedID) = firstOrNull { it.id.value == userId.value }?.name
private fun Call.senderName(userId: QualifiedID) = participants.firstOrNull { it.id.value == userId.value }?.name
private suspend fun SharedFlow<Call?>.senderName(userId: QualifiedID) =
    filterNotNull().filter { it.participants.isNotEmpty() }.first().senderName(userId)

private fun OngoingCallState.currentOrderType(): CallingParticipantsOrderType = when (othersVideosDisabled) {
    true -> CallingParticipantsOrderType.ALPHABETICALLY
    false -> CallingParticipantsOrderType.VIDEOS_FIRST
}
