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

package com.wire.android.ui.home.conversations.call

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wire.android.ui.common.ActionsManager
import com.wire.android.ui.common.ActionsManagerImpl
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo
import com.wire.kalium.logic.data.user.type.isInternal
import com.wire.kalium.logic.data.user.type.isTeamAdmin
import com.wire.kalium.logic.feature.call.usecase.AnswerCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ConferenceCallingResult
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.IsEligibleToStartCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveDegradedConversationNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.SetUserInformedAboutVerificationUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import io.github.esentsov.PackagePrivate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Suppress("LongParameterList", "TooManyFunctions")
class JoinOrStartCallManager(
    private val scope: CoroutineScope,
    val currentAccount: UserId,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val observeParticipantsForConversation: ObserveParticipantsForConversationUseCase,
    private val answerCall: AnswerCallUseCase,
    private val endCall: EndCallUseCase,
    private val observeSyncState: ObserveSyncStateUseCase,
    private val isEligibleToStartCall: IsEligibleToStartCallUseCase,
    private val setUserInformedAboutVerification: SetUserInformedAboutVerificationUseCase,
    private val observeDegradedConversationNotified: ObserveDegradedConversationNotifiedUseCase,
    private val observeSelf: ObserveSelfUserUseCase,
    private val initialState: JoinOrStartCallViewState = JoinOrStartCallViewState() // for testing
) : ActionsManager<JoinOrStartCallViewActions> by ActionsManagerImpl(scope) {

    var joinOrStartCallViewState by mutableStateOf(initialState)
        @VisibleForTesting internal set

    @VisibleForTesting
    internal val selfTeamRole: MutableState<UserTypeInfo> = mutableStateOf(UserTypeInfo.Regular(UserType.GUEST))
    private var establishedCallConversationId: QualifiedID? = null

    init {
        observeEstablishedCall()
        observeSelfTeamRole()
    }

    private fun observeSelfTeamRole() {
        scope.launch {
            observeSelf().collectLatest { self ->
                selfTeamRole.value = self.userType
            }
        }
    }

    private fun observeEstablishedCall() = scope.launch {
        observeEstablishedCalls()
            .distinctUntilChanged()
            .collect {
                val hasEstablishedCall = it.isNotEmpty()
                establishedCallConversationId = if (it.isNotEmpty()) {
                    it.first().conversationId
                } else {
                    null
                }
                joinOrStartCallViewState = joinOrStartCallViewState.copy(hasEstablishedCall = hasEstablishedCall)
            }
    }

    @PackagePrivate
    internal fun joinAnyway(conversationId: ConversationId) {
        scope.launch {
            establishedCallConversationId?.let {
                endCall(it)
                delay(DELAY_END_CALL)
            }
            answerAndOpenOngoingCall(conversationId)
        }
    }

    fun joinOngoingCall(conversationId: ConversationId) {
        scope.launch {
            if (joinOrStartCallViewState.hasEstablishedCall) {
                updateDialogType(JoinOrStartCallScreenDialogType.JoinAnyway(conversationId))
            } else {
                answerAndOpenOngoingCall(conversationId)
            }
        }
    }

    private suspend fun answerAndOpenOngoingCall(conversationId: ConversationId) {
        updateDialogType(JoinOrStartCallScreenDialogType.None)
        answerCall(conversationId = conversationId)
        sendAction(JoinOrStartCallViewActions.JoinedCall(conversationId, currentAccount))
    }

    @PackagePrivate
    internal fun initiateCall(conversationId: ConversationId) {
        scope.launch {
            initiateCallInternal(conversationId)
        }
    }

    private suspend fun initiateCallInternal(conversationId: ConversationId) {
        establishedCallConversationId?.let {
            endCall(it)
        }
        sendAction(JoinOrStartCallViewActions.InitiatedCall(conversationId, currentAccount))
    }

    fun startCallIfPossible(
        conversationId: ConversationId,
        conversationType: Conversation.Type,
        shouldCheckParticipantCount: Boolean = true,
    ) = scope.launch {
        startCallOrShowDialog(
            conversationId = conversationId,
            conversationType = conversationType,
            shouldCheckParticipantCount = shouldCheckParticipantCount
        )
    }

    @PackagePrivate
    internal fun startCallAfterDegradedVerification(conversationId: ConversationId, conversationType: Conversation.Type) {
        scope.launch {
            onApplyConversationDegradation(conversationId)
            startCallOrShowDialog(conversationId = conversationId, conversationType = conversationType, shouldCheckParticipantCount = true)
        }
    }

    @PackagePrivate
    internal fun startCallAfterConfirmingParticipantsCount(conversationId: ConversationId, conversationType: Conversation.Type) {
        scope.launch {
            startCallIfPossible(conversationId = conversationId, conversationType = conversationType, shouldCheckParticipantCount = false)
        }
    }

    private suspend fun startCallOrShowDialog(
        conversationId: ConversationId,
        conversationType: Conversation.Type,
        shouldCheckParticipantCount: Boolean = true,
    ) {
        val participantsCount = if (shouldCheckParticipantCount) participantsCountForConversation(conversationId) else null
        when {
            !hasStableConnectivity() -> JoinOrStartCallScreenDialogType.NoConnectivity
            participantsCount != null && participantsCount > MAX_GROUP_SIZE_FOR_CALL_WITHOUT_ALERT ->
                JoinOrStartCallScreenDialogType.CallConfirmation(conversationId, participantsCount, conversationType)
            shouldInformAboutVerification(conversationId) ->
                JoinOrStartCallScreenDialogType.VerificationDegraded(conversationId, conversationType)
            else -> dialogTypeForCallAvailability(conversationId, conversationType)
        }.also {
            updateDialogType(it)
            if (it == JoinOrStartCallScreenDialogType.None) {
                initiateCallInternal(conversationId)
            }
        }
    }

    private suspend fun dialogTypeForCallAvailability(
        conversationId: ConversationId,
        conversationType: Conversation.Type,
    ) = when (isConferenceCallingEnabled(conversationId, conversationType)) {
        ConferenceCallingResult.Enabled -> JoinOrStartCallScreenDialogType.None

        ConferenceCallingResult.Disabled.Established -> {
            sendAction(JoinOrStartCallViewActions.JoinedCall(conversationId, currentAccount))
            JoinOrStartCallScreenDialogType.None
        }

        ConferenceCallingResult.Disabled.OngoingCall -> JoinOrStartCallScreenDialogType.OngoingActiveCall(conversationId)
        ConferenceCallingResult.Disabled.Unavailable -> when {
            selfTeamRole.value.isInternal() -> JoinOrStartCallScreenDialogType.CallingFeatureUnavailable.TeamMember
            selfTeamRole.value.isTeamAdmin() -> JoinOrStartCallScreenDialogType.CallingFeatureUnavailable.TeamAdmin
            else -> JoinOrStartCallScreenDialogType.CallingFeatureUnavailable.Other
        }
    }

    private suspend fun hasStableConnectivity() = observeSyncState().firstOrNull()?.let {
        when (it) {
            is SyncState.Failed, SyncState.Waiting -> false
            SyncState.GatheringPendingEvents, SyncState.SlowSync, SyncState.Live -> true
        }
    } ?: false

    private suspend fun participantsCountForConversation(conversationId: ConversationId): Int =
        observeParticipantsForConversation(conversationId).firstOrNull()?.allCount ?: 0

    private suspend fun isConferenceCallingEnabled(conversationId: ConversationId, conversationType: Conversation.Type) =
        isEligibleToStartCall.invoke(conversationId, conversationType)

    private suspend fun onApplyConversationDegradation(conversationId: ConversationId) {
        setUserInformedAboutVerification.invoke(conversationId)
    }

    private suspend fun shouldInformAboutVerification(conversationId: ConversationId) =
        observeDegradedConversationNotified(conversationId).first().not()

    private fun updateDialogType(dialogType: JoinOrStartCallScreenDialogType) {
        joinOrStartCallViewState = joinOrStartCallViewState.copy(dialogType = dialogType)
    }

    @PackagePrivate
    internal fun dismissDialog() = updateDialogType(JoinOrStartCallScreenDialogType.None)

    companion object {
        const val DELAY_END_CALL = 200L
        const val MAX_GROUP_SIZE_FOR_CALL_WITHOUT_ALERT = 5
    }
}
