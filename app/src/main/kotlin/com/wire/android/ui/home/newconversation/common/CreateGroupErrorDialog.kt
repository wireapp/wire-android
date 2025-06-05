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
package com.wire.android.ui.home.newconversation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.DialogTextSuffixLink
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.DialogErrorStrings
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.stringWithStyledArgs

@Composable
fun CreateGroupErrorDialog(
    error: CreateGroupState.Error,
    onDismiss: () -> Unit,
    onEditParticipantsList: () -> Unit,
    onCancel: () -> Unit
) {
    val (dialogStrings, dialogSuffixLink) = when (error) {
        is CreateGroupState.Error.LackingConnection -> DialogErrorStrings(
            title = stringResource(R.string.error_no_network_title),
            message = stringResource(R.string.error_no_network_message),
        ) to null

        is CreateGroupState.Error.Unknown -> DialogErrorStrings(
            title = stringResource(R.string.error_unknown_title),
            message = stringResource(R.string.error_unknown_message),
        ) to null

        is CreateGroupState.Error.ConflictedBackends -> DialogErrorStrings(
            title = stringResource(id = R.string.conversation_can_not_be_created_title),
            annotatedMessage = LocalContext.current.resources.stringWithStyledArgs(
                stringResId = R.string.conversation_can_not_be_created_federation_conflict_description,
                normalStyle = typography().body01,
                argsStyle = typography().body02,
                normalColor = colorsScheme().secondaryText,
                argsColor = colorsScheme().onBackground,
                error.domains.dropLast(1).joinToString(", "),
                error.domains.last()
            ),
        ) to DialogTextSuffixLink(
            linkText = stringResource(id = R.string.label_learn_more),
            linkUrl = stringResource(id = R.string.url_message_details_offline_backends_learn_more)
        )
        is CreateGroupState.Error.Forbidden -> DialogErrorStrings(
            title = stringResource(R.string.conversation_can_not_be_created_title),
            message = stringResource(R.string.create_channel_error_forbidden_message),
        ) to null
    }

    WireDialog(
        title = dialogStrings.title,
        text = dialogStrings.annotatedMessage,
        textSuffixLink = dialogSuffixLink,
        onDismiss = onDismiss,
        buttonsHorizontalAlignment = false,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = if (error.isConflictedBackends) onEditParticipantsList else onDismiss,
            text = stringResource(
                id = if (error.isConflictedBackends) {
                    R.string.conversation_can_not_be_created_edit_participant_list
                } else {
                    R.string.label_ok
                }
            ),
            type = WireDialogButtonType.Primary,
        ),
        optionButton2Properties = if (error.isConflictedBackends) {
            WireDialogButtonProperties(
                onClick = onCancel,
                text = stringResource(R.string.conversation_can_not_be_created_discard_creation),
                type = WireDialogButtonType.Secondary,
            )
        } else {
            null
        },
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewCreateGroupErrorDialogLackingConnection() {
    WireTheme {
        CreateGroupErrorDialog(CreateGroupState.Error.LackingConnection, {}, {}, {})
    }
}
@PreviewMultipleThemes
@Composable
private fun PreviewCreateGroupErrorDialogUnknown() {
    WireTheme {
        CreateGroupErrorDialog(CreateGroupState.Error.Unknown, {}, {}, {})
    }
}
@PreviewMultipleThemes
@Composable
private fun PreviewCreateGroupErrorDialogConflictedBackends() {
    WireTheme {
        CreateGroupErrorDialog(CreateGroupState.Error.ConflictedBackends(listOf("some.com", "other.com")), {}, {}, {})
    }
}
