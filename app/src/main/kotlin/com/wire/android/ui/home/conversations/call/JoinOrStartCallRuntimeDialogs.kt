/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

@Composable
fun JoinOrStartCallManager.HandleJoinOrStartCallScreenDialogs() = when (val dialogType = joinOrStartCallViewState.dialogType) {
    is JoinOrStartCallScreenDialogType.JoinAnyway -> JoinAnywayDialog(
        onDismiss = ::dismissDialog,
        onConfirm = { joinAnyway(dialogType.conversationId) },
    )

    is JoinOrStartCallScreenDialogType.OngoingActiveCall -> OngoingActiveCallDialog(
        onInitiateCallAnyway = {
            initiateCall(dialogType.conversationId)
        },
        onDialogDismiss = ::dismissDialog
    )

    JoinOrStartCallScreenDialogType.NoConnectivity -> CoreFailureErrorDialog(
        coreFailure = NetworkFailure.NoNetworkConnection(null),
        onDialogDismiss = ::dismissDialog
    )

    is JoinOrStartCallScreenDialogType.CallConfirmation -> ConfirmStartCallDialog(
        participantsCount = dialogType.participantsCount,
        onConfirm = { startCallAfterConfirmingParticipantsCount(dialogType.conversationId, dialogType.conversationType) },
        onDialogDismiss = ::dismissDialog
    )

    JoinOrStartCallScreenDialogType.CallingFeatureUnavailable.Other ->
        CallingFeatureUnavailableDialog(onDialogDismiss = ::dismissDialog)

    JoinOrStartCallScreenDialogType.CallingFeatureUnavailable.TeamMember ->
        CallingFeatureUnavailableTeamMemberDialog(onDialogDismiss = ::dismissDialog)

    JoinOrStartCallScreenDialogType.CallingFeatureUnavailable.TeamAdmin -> CallingFeatureUnavailableTeamAdminDialog(
        onUpgradeAction = LocalUriHandler.current::openUri,
        onDialogDismiss = ::dismissDialog
    )

    is JoinOrStartCallScreenDialogType.VerificationDegraded -> SureAboutCallingInDegradedConversationDialog(
        callAnyway = { startCallAfterDegradedVerification(dialogType.conversationId, dialogType.conversationType) },
        onDialogDismiss = ::dismissDialog
    )

    JoinOrStartCallScreenDialogType.None -> {}
}
