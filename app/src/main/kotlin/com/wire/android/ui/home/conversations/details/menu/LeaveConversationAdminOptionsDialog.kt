/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

package com.wire.android.ui.home.conversations.details.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.home.conversationslist.model.LeaveGroupOptionsDialogState

@Composable
internal fun LeaveConversationAdminOptionsDialog(
    dialogState: VisibilityState<LeaveGroupOptionsDialogState>,
    onPromoteAdmin: (LeaveGroupOptionsDialogState) -> Unit,
    onDeleteGroup: (LeaveGroupOptionsDialogState) -> Unit,
) {
    VisibilityState(dialogState) { state ->
        val isInformationalOnly = !state.showPromoteOption && !state.canDeleteGroup
        WireDialog(
            title = stringResource(id = titleRes(state), state.conversationName),
            text = stringResource(id = descriptionRes(state)),
            buttonsHorizontalAlignment = false,
            onDismiss = dialogState::dismiss,
            optionButton1Properties = when {
                isInformationalOnly -> WireDialogButtonProperties(
                    onClick = dialogState::dismiss,
                    text = stringResource(id = R.string.label_ok),
                    type = WireDialogButtonType.Primary,
                )
                state.showPromoteOption -> WireDialogButtonProperties(
                    onClick = { onPromoteAdmin(state) },
                    text = stringResource(id = R.string.leave_conversation_admin_options_dialog_promote_button),
                    type = WireDialogButtonType.Primary,
                )
                else -> null
            },
            optionButton2Properties = when {
                !isInformationalOnly && state.canDeleteGroup -> WireDialogButtonProperties(
                    onClick = { onDeleteGroup(state) },
                    text = stringResource(id = R.string.leave_conversation_admin_options_dialog_delete_button),
                    type = WireDialogButtonType.Primary,
                )
                else -> null
            },
            dismissButtonProperties = if (isInformationalOnly) {
                null
            } else {
                WireDialogButtonProperties(
                    onClick = dialogState::dismiss,
                    text = stringResource(id = R.string.label_cancel),
                )
            },
        )
    }
}

@Composable
private fun descriptionRes(state: LeaveGroupOptionsDialogState): Int = when {
    state.showPromoteOption && state.canDeleteGroup ->
        R.string.leave_conversation_admin_options_dialog_description_with_promote

    state.showPromoteOption && !state.canDeleteGroup ->
        R.string.leave_conversation_admin_options_dialog_description_with_promote_no_delete

    !state.showPromoteOption && state.canDeleteGroup ->
        R.string.leave_conversation_admin_options_dialog_description_no_promote

    else ->
        R.string.leave_conversation_admin_options_dialog_description_no_promote_no_delete
}

@Composable
private fun titleRes(state: LeaveGroupOptionsDialogState): Int = when {
    state.canDeleteGroup || state.showPromoteOption -> R.string.leave_conversation_dialog_title
    else -> R.string.cannot_leave_conversation_dialog_title
}
