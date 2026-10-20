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
import com.wire.kalium.logic.di.KaliumCallsApi
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
import com.wire.kalium.logic.feature.call.usecase.ObserveJoinableCallsUseCase
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
import com.wire.kalium.logic.feature.call.usecase.ObserveActiveCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.video.SetVideoSendStateUseCase
import com.wire.kalium.logic.feature.call.usecase.video.UpdateVideoStateUseCase
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides

@BindingContainer
@Suppress("TooManyFunctions")
class CallsModule {

    @Provides
    fun provideCallsApi(
        @KaliumCoreLogic coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId
    ): KaliumCallsApi = coreLogic.getSessionScope(currentAccount).callsApiFactory.create()

    @Provides
    fun provideGetIncomingCallsUseCase(api: KaliumCallsApi): GetIncomingCallsUseCase =
        api.callsGetIncomingCalls

    @Provides
    fun provideRequestVideoStreamsUseCase(api: KaliumCallsApi): RequestVideoStreamsUseCase =
        api.callsRequestVideoStreams

    @Provides
    fun provideIsLastCallClosedUseCase(api: KaliumCallsApi): IsLastCallClosedUseCase =
        api.callsIsLastCallClosed

    @Provides
    fun provideObserveOngoingCallsUseCase(api: KaliumCallsApi): ObserveOngoingCallsUseCase =
        api.callsObserveOngoingCalls

    @Provides
    fun provideObserveJoinableCallsUseCase(api: KaliumCallsApi): ObserveJoinableCallsUseCase =
        api.callsObserveJoinableCalls

    @Provides
    fun provideObserveEstablishedCallWithSortedParticipantsUseCase(
        api: KaliumCallsApi
    ): ObserveEstablishedCallWithSortedParticipantsUseCase =
        api.callsObserveEstablishedCallWithSortedParticipants

    @Provides
    fun provideObserveLastActiveCallWithSortedParticipantsUseCase(
        api: KaliumCallsApi
    ): ObserveLastActiveCallWithSortedParticipantsUseCase =
        api.callsObserveLastActiveCallWithSortedParticipants

    @Provides
    fun provideRejectCallUseCase(api: KaliumCallsApi): RejectCallUseCase =
        api.callsRejectCall

    @Provides
    fun provideAcceptCallUseCase(api: KaliumCallsApi): AnswerCallUseCase =
        api.callsAnswerCall

    @Provides
    fun provideOnGoingCallUseCase(
        api: KaliumCallsApi
    ): ObserveEstablishedCallsUseCase =
        api.callsEstablishedCall

    @Provides
    fun provideObserveOutgoingCallUseCase(
        api: KaliumCallsApi
    ): ObserveOutgoingCallUseCase =
        api.callsObserveOutgoingCall

    @Provides
    fun provideActiveCallsUseCase(api: KaliumCallsApi): ObserveActiveCallsUseCase =
        api.callsObserveActiveCalls

    @Provides
    fun provideStartCallUseCase(api: KaliumCallsApi): StartCallUseCase =
        api.callsStartCall

    @Provides
    fun provideEndCallUseCase(api: KaliumCallsApi): EndCallUseCase =
        api.callsEndCall

    @Provides
    fun provideEndCallOnConversationChangeUseCase(
        api: KaliumCallsApi
    ): EndCallOnConversationChangeUseCase =
        api.callsEndCallOnConversationChange

    @Provides
    fun provideMuteCallUseCase(api: KaliumCallsApi): MuteCallUseCase =
        api.callsMuteCall

    @Provides
    fun provideUnMuteCallUseCase(api: KaliumCallsApi): UnMuteCallUseCase =
        api.callsUnMuteCall

    @Provides
    fun provideSetVideoPreviewUseCase(
        api: KaliumCallsApi
    ): SetVideoPreviewUseCase = api.callsSetVideoPreview

    @Provides
    fun provideSetUIRotationUseCase(
        api: KaliumCallsApi
    ): SetUIRotationUseCase = api.callsSetUIRotation

    @Provides
    fun provideFlipToBackCameraUseCase(
        api: KaliumCallsApi
    ): FlipToBackCameraUseCase = api.callsFlipToBackCamera

    @Provides
    fun provideFlipToFrontCameraUseCase(
        api: KaliumCallsApi
    ): FlipToFrontCameraUseCase = api.callsFlipToFrontCamera

    @Provides
    fun turnLoudSpeakerOffUseCaseProvider(
        api: KaliumCallsApi
    ): TurnLoudSpeakerOffUseCase = api.callsTurnLoudSpeakerOff

    @Provides
    fun provideTurnLoudSpeakerOnUseCase(
        api: KaliumCallsApi
    ): TurnLoudSpeakerOnUseCase = api.callsTurnLoudSpeakerOn

    @Provides
    fun provideObserveSpeakerUseCase(
        api: KaliumCallsApi
    ): ObserveSpeakerUseCase = api.callsObserveSpeaker

    @Provides
    fun provideUpdateVideoStateUseCase(
        api: KaliumCallsApi
    ): UpdateVideoStateUseCase =
        api.callsUpdateVideoState

    @Provides
    fun provideSetVideoSendStateUseCase(
        api: KaliumCallsApi
    ): SetVideoSendStateUseCase =
        api.callsSetVideoSendState

    @Provides
    fun provideIsCallRunningUseCase(api: KaliumCallsApi): IsCallRunningUseCase =
        api.callsIsCallRunning

    @Provides
    fun provideIsEligibleToStartCall(api: KaliumCallsApi): IsEligibleToStartCallUseCase =
        api.callsIsEligibleToStartCall

    @Provides
    fun provideObserveConferenceCallingEnabledUseCase(api: KaliumCallsApi): ObserveConferenceCallingEnabledUseCase =
        api.callsObserveConferenceCallingEnabled

    @Provides
    fun provideObserveInCallReactionsUseCase(api: KaliumCallsApi): ObserveInCallReactionsUseCase =
        api.callsObserveInCallReactions

    @Provides
    fun provideObserveCallQualityDataUseCase(api: KaliumCallsApi): ObserveCallQualityDataUseCase =
        api.callsObserveCallQualityData

    @Provides
    fun provideSetCallQualityIntervalUseCase(api: KaliumCallsApi): SetCallQualityIntervalUseCase =
        api.callsSetCallQualityInterval

    @Provides
    fun provideObserveCallModerationActionsUseCase(api: KaliumCallsApi): ObserveCallModerationActionsUseCase =
        api.callsObserveCallModerationActions
}
