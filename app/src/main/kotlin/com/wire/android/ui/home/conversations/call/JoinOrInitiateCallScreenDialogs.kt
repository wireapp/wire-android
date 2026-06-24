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
@file:Suppress("MatchingDeclarationName")

package com.wire.android.ui.home.conversations.call

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import com.wire.android.ui.common.dialogs.calling.CallingFeatureUnavailableDialog
import com.wire.android.ui.common.dialogs.calling.CallingFeatureUnavailableTeamAdminDialog
import com.wire.android.ui.common.dialogs.calling.CallingFeatureUnavailableTeamMemberDialog
import com.wire.android.ui.common.dialogs.calling.ConfirmStartCallDialog
import com.wire.android.ui.common.dialogs.calling.JoinAnywayDialog
import com.wire.android.ui.common.dialogs.calling.OngoingActiveCallDialog
import com.wire.android.ui.common.dialogs.calling.SureAboutCallingInDegradedConversationDialog
import com.wire.android.ui.common.error.CoreFailureErrorDialog
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId

sealed interface JoinOrInitiateCallScreenDialogType {
    data class JoinAnyway(val conversationId: ConversationId) : JoinOrInitiateCallScreenDialogType
    data class VerificationDegraded(
        val conversationId: ConversationId,
        val conversationType: Conversation.Type
    ) : JoinOrInitiateCallScreenDialogType

    data class OngoingActiveCall(val conversationId: ConversationId) : JoinOrInitiateCallScreenDialogType
    data object NoConnectivity : JoinOrInitiateCallScreenDialogType
    data class CallConfirmation(
        val conversationId: ConversationId,
        val participantsCount: Int,
        val conversationType: Conversation.Type
    ) : JoinOrInitiateCallScreenDialogType

    sealed interface CallingFeatureUnavailable : JoinOrInitiateCallScreenDialogType {
        data object TeamMember : CallingFeatureUnavailable
        data object TeamAdmin : CallingFeatureUnavailable
        data object Other : CallingFeatureUnavailable
    }

    data object None : JoinOrInitiateCallScreenDialogType
}

@Composable
fun JoinOrInitiateCallViewModel.HandleJoinOrInitiateCallScreenDialogs() = when (val dialogType = joinOrInitiateCallViewState.dialogType) {
    is JoinOrInitiateCallScreenDialogType.JoinAnyway -> JoinAnywayDialog(
        onDismiss = ::dismissDialog,
        onConfirm = { joinAnyway(dialogType.conversationId) },
    )

    is JoinOrInitiateCallScreenDialogType.OngoingActiveCall -> OngoingActiveCallDialog(
        onInitiateCallAnyway = {
            initiateCall(dialogType.conversationId)
            dismissDialog()
        },
        onDialogDismiss = ::dismissDialog
    )

    JoinOrInitiateCallScreenDialogType.NoConnectivity -> CoreFailureErrorDialog(
        coreFailure = NetworkFailure.NoNetworkConnection(null),
        onDialogDismiss = ::dismissDialog
    )

    is JoinOrInitiateCallScreenDialogType.CallConfirmation -> ConfirmStartCallDialog(
        participantsCount = dialogType.participantsCount,
        onConfirm = { startCallAfterConfirming(dialogType.conversationId, dialogType.conversationType) },
        onDialogDismiss = ::dismissDialog
    )

    JoinOrInitiateCallScreenDialogType.CallingFeatureUnavailable.Other ->
        CallingFeatureUnavailableDialog(onDialogDismiss = ::dismissDialog)

    JoinOrInitiateCallScreenDialogType.CallingFeatureUnavailable.TeamMember ->
        CallingFeatureUnavailableTeamMemberDialog(onDialogDismiss = ::dismissDialog)

    JoinOrInitiateCallScreenDialogType.CallingFeatureUnavailable.TeamAdmin -> CallingFeatureUnavailableTeamAdminDialog(
        onUpgradeAction = LocalUriHandler.current::openUri,
        onDialogDismiss = ::dismissDialog
    )

    is JoinOrInitiateCallScreenDialogType.VerificationDegraded -> SureAboutCallingInDegradedConversationDialog(
        callAnyway = { startCallAfterDegradedVerification(dialogType.conversationId, dialogType.conversationType) },
        onDialogDismiss = ::dismissDialog
    )

    JoinOrInitiateCallScreenDialogType.None -> {}
}
