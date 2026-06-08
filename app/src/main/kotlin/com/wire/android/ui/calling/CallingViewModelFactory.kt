/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.calling

import androidx.lifecycle.SavedStateHandle
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.ObserveScreenshotCensoringConfigUseCaseProvider
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.mapper.UICallParticipantMapper
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.media.CallRinger
import com.wire.android.notification.CallNotificationManager
import com.wire.android.ui.CallFeedbackViewModel
import com.wire.android.ui.analytics.IsAnalyticsAvailableUseCase
import com.wire.android.ui.calling.common.SharedCallingViewModel
import com.wire.android.ui.calling.incoming.IncomingCallViewModel
import com.wire.android.ui.calling.ongoing.OngoingCallViewModel
import com.wire.android.ui.calling.outgoing.OutgoingCallViewModel
import com.wire.android.ui.calling.usecase.HangUpCallUseCase
import com.wire.android.ui.home.appLock.LockCodeTimeManager
import com.wire.android.ui.home.conversations.call.ConversationCallViewModel
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.home.conversationslist.ConversationListCallViewModelImpl
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.FlipToBackCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.FlipToFrontCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.GetIncomingCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.IsEligibleToStartCallUseCase
import com.wire.kalium.logic.feature.call.usecase.IsLastCallClosedUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveCallModerationActionsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveCallQualityDataUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveConferenceCallingEnabledUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveInCallReactionsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveLastActiveCallWithSortedParticipantsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOngoingCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOutgoingCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveSpeakerUseCase
import com.wire.kalium.logic.feature.call.usecase.RejectCallUseCase
import com.wire.kalium.logic.feature.call.usecase.RequestVideoStreamsUseCase
import com.wire.kalium.logic.feature.call.usecase.SetCallQualityIntervalUseCase
import com.wire.kalium.logic.feature.call.usecase.SetUIRotationUseCase
import com.wire.kalium.logic.feature.call.usecase.SetVideoPreviewUseCase
import com.wire.kalium.logic.feature.call.usecase.StartCallUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOffUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOnUseCase
import com.wire.kalium.logic.feature.call.usecase.UnMuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.video.SetVideoSendStateUseCase
import com.wire.kalium.logic.feature.call.usecase.video.UpdateVideoStateUseCase
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveDegradedConversationNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.SetUserInformedAboutVerificationUseCase
import com.wire.kalium.logic.feature.incallreaction.SendInCallReactionUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import com.wire.kalium.network.NetworkStateObserver
import dev.zacsweers.metro.Inject

@Suppress("LongParameterList")
class CallingViewModelFactory @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val currentSession: CurrentSessionUseCase,
    private val observeScreenshotCensoringConfigUseCaseProviderFactory:
    ObserveScreenshotCensoringConfigUseCaseProvider.Factory,
    private val accountSwitch: AccountSwitchUseCase,
    @KaliumCoreLogic private val coreLogic: Lazy<CoreLogic>,
    private val currentSessionFlow: Lazy<CurrentSessionFlowUseCase>,
    private val isAnalyticsAvailable: Lazy<IsAnalyticsAvailableUseCase>,
    private val analyticsManager: Lazy<AnonymousAnalyticsManager>,
    @CurrentAccount private val currentAccount: UserId,
    private val callNotificationManager: CallNotificationManager,
    private val incomingCalls: GetIncomingCallsUseCase,
    private val rejectCall: RejectCallUseCase,
    private val answerCall: AnswerCallUseCase,
    private val muteCall: MuteCallUseCase,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val endCall: EndCallUseCase,
    private val lockCodeTimeManager: LockCodeTimeManager,
    private val observeOutgoingCall: ObserveOutgoingCallUseCase,
    private val startCall: StartCallUseCase,
    private val isLastCallClosed: IsLastCallClosedUseCase,
    private val callRinger: CallRinger,
    private val globalDataStore: GlobalDataStore,
    private val networkStateObserver: NetworkStateObserver,
    private val observeLastActiveCallWithSortedParticipants: ObserveLastActiveCallWithSortedParticipantsUseCase,
    private val requestVideoStreams: RequestVideoStreamsUseCase,
    private val setVideoSendState: SetVideoSendStateUseCase,
    private val observeCallQualityData: ObserveCallQualityDataUseCase,
    private val setCallQualityInterval: SetCallQualityIntervalUseCase,
    private val getCurrentClientId: ObserveCurrentClientIdUseCase,
    private val observeInCallReactionsUseCase: ObserveInCallReactionsUseCase,
    private val sendInCallReactionUseCase: SendInCallReactionUseCase,
    private val observeCallModerationActions: ObserveCallModerationActionsUseCase,
    private val uiCallParticipantMapper: UICallParticipantMapper,
    private val conversationDetails: ObserveConversationDetailsUseCase,
    private val hangUpCall: HangUpCallUseCase,
    private val unMuteCall: UnMuteCallUseCase,
    private val updateVideoState: UpdateVideoStateUseCase,
    private val setVideoPreview: SetVideoPreviewUseCase,
    private val setUIRotationUseCase: SetUIRotationUseCase,
    private val turnLoudSpeakerOff: TurnLoudSpeakerOffUseCase,
    private val turnLoudSpeakerOn: TurnLoudSpeakerOnUseCase,
    private val flipToFrontCamera: FlipToFrontCameraUseCase,
    private val flipToBackCamera: FlipToBackCameraUseCase,
    private val observeSpeaker: ObserveSpeakerUseCase,
    private val userTypeMapper: UserTypeMapper,
    private val observeOngoingCalls: ObserveOngoingCallsUseCase,
    private val observeParticipantsForConversation: ObserveParticipantsForConversationUseCase,
    private val observeSyncState: ObserveSyncStateUseCase,
    private val isConferenceCallingEnabled: IsEligibleToStartCallUseCase,
    private val setUserInformedAboutVerification: SetUserInformedAboutVerificationUseCase,
    private val observeDegradedConversationNotified: ObserveDegradedConversationNotifiedUseCase,
    private val observeConferenceCallingEnabled: ObserveConferenceCallingEnabledUseCase,
    private val observeSelf: ObserveSelfUserUseCase,
) {
    fun callActivityViewModel() = CallActivityViewModel(
        dispatchers = dispatchers,
        currentSession = currentSession,
        observeScreenshotCensoringConfigUseCaseProviderFactory =
        observeScreenshotCensoringConfigUseCaseProviderFactory,
        accountSwitch = accountSwitch,
    )

    fun callFeedbackViewModel() = CallFeedbackViewModel(
        coreLogic = coreLogic,
        currentSessionFlow = currentSessionFlow,
        isAnalyticsAvailable = isAnalyticsAvailable,
        analyticsManager = analyticsManager,
    )

    fun incomingCallViewModel(conversationId: ConversationId) = IncomingCallViewModel(
        conversationId = conversationId,
        currentAccount = currentAccount,
        callNotificationManager = callNotificationManager,
        incomingCalls = incomingCalls,
        rejectCall = rejectCall,
        acceptCall = answerCall,
        muteCall = muteCall,
        observeEstablishedCalls = observeEstablishedCalls,
        endCall = endCall,
        lockCodeTimeManager = lockCodeTimeManager,
    )

    fun outgoingCallViewModel(conversationId: ConversationId) = OutgoingCallViewModel(
        conversationId = conversationId,
        observeEstablishedCalls = observeEstablishedCalls,
        observeOutgoingCall = observeOutgoingCall,
        startCall = startCall,
        endCall = endCall,
        isLastCallClosed = isLastCallClosed,
        callRinger = callRinger,
    )

    fun ongoingCallViewModel(conversationId: ConversationId) = OngoingCallViewModel(
        conversationId = conversationId,
        currentUserId = currentAccount,
        globalDataStore = globalDataStore,
        networkStateObserver = networkStateObserver,
        observeLastActiveCall = observeLastActiveCallWithSortedParticipants,
        requestVideoStreams = requestVideoStreams,
        setVideoSendState = setVideoSendState,
        observeCallQualityData = observeCallQualityData,
        setCallQualityInterval = setCallQualityInterval,
        getCurrentClientId = getCurrentClientId,
        observeInCallReactionsUseCase = observeInCallReactionsUseCase,
        sendInCallReactionUseCase = sendInCallReactionUseCase,
        observeCallModerationActions = observeCallModerationActions,
        uiCallParticipantMapper = uiCallParticipantMapper,
        dispatchers = dispatchers,
    )

    fun sharedCallingViewModel(conversationId: ConversationId) = SharedCallingViewModel(
        conversationId = conversationId,
        conversationDetails = conversationDetails,
        observeLastActiveCallWithSortedParticipants = observeLastActiveCallWithSortedParticipants,
        hangUpCall = hangUpCall,
        muteCall = muteCall,
        unMuteCall = unMuteCall,
        updateVideoState = updateVideoState,
        setVideoPreview = setVideoPreview,
        setUIRotationUseCase = setUIRotationUseCase,
        turnLoudSpeakerOff = turnLoudSpeakerOff,
        turnLoudSpeakerOn = turnLoudSpeakerOn,
        flipToFrontCamera = flipToFrontCamera,
        flipToBackCamera = flipToBackCamera,
        observeSpeaker = observeSpeaker,
        userTypeMapper = userTypeMapper,
        dispatchers = dispatchers,
    )

    fun conversationCallViewModel(savedStateHandle: SavedStateHandle) = ConversationCallViewModel(
        savedStateHandle = savedStateHandle,
        currentAccount = currentAccount,
        observeOngoingCalls = observeOngoingCalls,
        observeEstablishedCalls = observeEstablishedCalls,
        observeParticipantsForConversation = observeParticipantsForConversation,
        answerCall = answerCall,
        endCall = endCall,
        observeSyncState = observeSyncState,
        isConferenceCallingEnabled = isConferenceCallingEnabled,
        observeConversationDetails = conversationDetails,
        setUserInformedAboutVerification = setUserInformedAboutVerification,
        observeDegradedConversationNotified = observeDegradedConversationNotified,
        observeConferenceCallingEnabled = observeConferenceCallingEnabled,
        observeSelf = observeSelf,
    )

    fun conversationListCallViewModel() = ConversationListCallViewModelImpl(
        currentAccount = currentAccount,
        answerCall = answerCall,
        observeEstablishedCalls = observeEstablishedCalls,
        endCall = endCall,
    )
}
