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
package com.wire.android.di.accountScoped

import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.CallsScope
import com.wire.kalium.logic.feature.call.usecase.EndCallOnConversationChangeUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.FlipToBackCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.FlipToFrontCameraUseCase
import com.wire.kalium.logic.feature.call.usecase.GetIncomingCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.IsCallRunningUseCase
import com.wire.kalium.logic.feature.call.usecase.IsEligibleToStartCallUseCase
import com.wire.kalium.logic.feature.call.usecase.IsLastCallClosedUseCase
import com.wire.kalium.logic.feature.call.usecase.MuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveCallModerationActionsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveCallQualityDataUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveConferenceCallingEnabledUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallWithSortedParticipantsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveInCallReactionsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveLastActiveCallWithSortedParticipantsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOngoingCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOutgoingCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveSpeakerUseCase
import com.wire.kalium.logic.feature.call.usecase.RejectCallUseCase
import com.wire.kalium.logic.feature.call.usecase.RequestVideoStreamsUseCase
import com.wire.kalium.logic.feature.call.usecase.SetUIRotationUseCase
import com.wire.kalium.logic.feature.call.usecase.SetCallQualityIntervalUseCase
import com.wire.kalium.logic.feature.call.usecase.SetVideoPreviewUseCase
import com.wire.kalium.logic.feature.call.usecase.StartCallUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOffUseCase
import com.wire.kalium.logic.feature.call.usecase.TurnLoudSpeakerOnUseCase
import com.wire.kalium.logic.feature.call.usecase.UnMuteCallUseCase
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.video.SetVideoSendStateUseCase
import com.wire.kalium.logic.feature.call.usecase.video.UpdateVideoStateUseCase
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides

@BindingContainer
@Suppress("TooManyFunctions")
class CallsModule {

    @Provides
    fun providesCallsScope(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): CallsScope = coreLogic.getSessionScope(currentAccount).calls

    @Provides
    fun provideGetIncomingCallsUseCase(callsScope: CallsScope): GetIncomingCallsUseCase =
        callsScope.getIncomingCalls

    @Provides
    fun provideRequestVideoStreamsUseCase(callsScope: CallsScope): RequestVideoStreamsUseCase =
        callsScope.requestVideoStreams

    @Provides
    fun provideIsLastCallClosedUseCase(callsScope: CallsScope): IsLastCallClosedUseCase =
        callsScope.isLastCallClosed

    @Provides
    fun provideObserveOngoingCallsUseCase(callsScope: CallsScope): ObserveOngoingCallsUseCase =
        callsScope.observeOngoingCalls

    @Provides
    fun provideObserveEstablishedCallWithSortedParticipantsUseCase(
        callsScope: CallsScope
    ): ObserveEstablishedCallWithSortedParticipantsUseCase =
        callsScope.observeEstablishedCallWithSortedParticipants

    @Provides
    fun provideObserveLastActiveCallWithSortedParticipantsUseCase(
        callsScope: CallsScope
    ): ObserveLastActiveCallWithSortedParticipantsUseCase =
        callsScope.observeLastActiveCallWithSortedParticipants

    @Provides
    fun provideRejectCallUseCase(callsScope: CallsScope): RejectCallUseCase =
        callsScope.rejectCall

    @Provides
    fun provideAcceptCallUseCase(callsScope: CallsScope): AnswerCallUseCase =
        callsScope.answerCall

    @Provides
    fun provideOnGoingCallUseCase(
        callsScope: CallsScope
    ): ObserveEstablishedCallsUseCase =
        callsScope.establishedCall

    @Provides
    fun provideObserveOutgoingCallUseCase(
        callsScope: CallsScope
    ): ObserveOutgoingCallUseCase =
        callsScope.observeOutgoingCall

    @Provides
    fun provideStartCallUseCase(callsScope: CallsScope): StartCallUseCase =
        callsScope.startCall

    @Provides
    fun provideEndCallUseCase(callsScope: CallsScope): EndCallUseCase =
        callsScope.endCall

    @Provides
    fun provideEndCallOnConversationChangeUseCase(
        callsScope: CallsScope
    ): EndCallOnConversationChangeUseCase =
        callsScope.endCallOnConversationChange

    @Provides
    fun provideMuteCallUseCase(callsScope: CallsScope): MuteCallUseCase =
        callsScope.muteCall

    @Provides
    fun provideUnMuteCallUseCase(callsScope: CallsScope): UnMuteCallUseCase =
        callsScope.unMuteCall

    @Provides
    fun provideSetVideoPreviewUseCase(
        callsScope: CallsScope
    ): SetVideoPreviewUseCase = callsScope.setVideoPreview

    @Provides
    fun provideSetUIRotationUseCase(
        callsScope: CallsScope
    ): SetUIRotationUseCase = callsScope.setUIRotation

    @Provides
    fun provideFlipToBackCameraUseCase(
        callsScope: CallsScope
    ): FlipToBackCameraUseCase = callsScope.flipToBackCamera

    @Provides
    fun provideFlipToFrontCameraUseCase(
        callsScope: CallsScope
    ): FlipToFrontCameraUseCase = callsScope.flipToFrontCamera

    @Provides
    fun turnLoudSpeakerOffUseCaseProvider(
        callsScope: CallsScope
    ): TurnLoudSpeakerOffUseCase = callsScope.turnLoudSpeakerOff

    @Provides
    fun provideTurnLoudSpeakerOnUseCase(
        callsScope: CallsScope
    ): TurnLoudSpeakerOnUseCase = callsScope.turnLoudSpeakerOn

    @Provides
    fun provideObserveSpeakerUseCase(
        callsScope: CallsScope
    ): ObserveSpeakerUseCase = callsScope.observeSpeaker

    @Provides
    fun provideUpdateVideoStateUseCase(
        callsScope: CallsScope
    ): UpdateVideoStateUseCase =
        callsScope.updateVideoState

    @Provides
    fun provideSetVideoSendStateUseCase(
        callsScope: CallsScope
    ): SetVideoSendStateUseCase =
        callsScope.setVideoSendState

    @Provides
    fun provideIsCallRunningUseCase(callsScope: CallsScope): IsCallRunningUseCase =
        callsScope.isCallRunning

    @Provides
    fun provideIsEligibleToStartCall(callsScope: CallsScope): IsEligibleToStartCallUseCase =
        callsScope.isEligibleToStartCall

    @Provides
    fun provideObserveConferenceCallingEnabledUseCase(callsScope: CallsScope): ObserveConferenceCallingEnabledUseCase =
        callsScope.observeConferenceCallingEnabled

    @Provides
    fun provideObserveInCallReactionsUseCase(callsScope: CallsScope): ObserveInCallReactionsUseCase =
        callsScope.observeInCallReactions

    @Provides
    fun provideObserveCallQualityDataUseCase(callsScope: CallsScope): ObserveCallQualityDataUseCase =
        callsScope.observeCallQualityData

    @Provides
    fun provideSetCallQualityIntervalUseCase(callsScope: CallsScope): SetCallQualityIntervalUseCase =
        callsScope.setCallQualityInterval

    @Provides
    fun provideObserveCallModerationActionsUseCase(callsScope: CallsScope): ObserveCallModerationActionsUseCase =
        callsScope.observeCallModerationActions
}
