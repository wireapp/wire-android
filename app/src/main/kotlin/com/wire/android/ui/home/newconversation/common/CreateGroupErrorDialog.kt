/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.markdown.MarkdownConstants
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.DialogAnnotatedErrorStrings
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun CreateGroupErrorDialog(
    error: CreateGroupState.Error,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onCancel: () -> Unit
) {
    val dialogStrings = when (error) {
        is CreateGroupState.Error.LackingConnection -> DialogAnnotatedErrorStrings(
            stringResource(R.string.error_no_network_title),
            buildAnnotatedString { append(stringResource(R.string.error_no_network_message)) }
        )

        is CreateGroupState.Error.Unknown -> DialogAnnotatedErrorStrings(
            stringResource(R.string.error_unknown_title),
            buildAnnotatedString { append(stringResource(R.string.error_unknown_message)) }
        )

        is CreateGroupState.Error.ConflictedBackends -> DialogAnnotatedErrorStrings(
            title = stringResource(id = R.string.group_can_not_be_created_title),
            annotatedMessage = buildAnnotatedString {
                val description = stringResource(
                    id = R.string.group_can_not_be_created_federation_conflict_description,
                    error.domains.dropLast(1).joinToString(", "),
                    error.domains.last()
                )
                val learnMore = stringResource(id = R.string.label_learn_more)

                append(description)
                append(' ')

                withStyle(
                    style = SpanStyle(
                        color = colorsScheme().primary,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(learnMore)
                }
                addStringAnnotation(
                    tag = MarkdownConstants.TAG_URL,
                    annotation = stringResource(id = R.string.url_message_details_offline_backends_learn_more),
                    start = description.length + 1,
                    end = description.length + 1 + learnMore.length
                )
            }
        )
    }

    WireDialog(
        dialogStrings.title,
        dialogStrings.annotatedMessage,
        onDismiss = onDismiss,
        buttonsHorizontalAlignment = false,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = if (error.isConflictedBackends) onAccept else onDismiss,
            text = stringResource(
                id = if (error.isConflictedBackends) {
                    R.string.group_can_not_be_created_edit_participiant_list
                } else {
                    R.string.label_ok
                }
            ),
            type = WireDialogButtonType.Primary,
        ),
        optionButton2Properties = if (error.isConflictedBackends) {
            WireDialogButtonProperties(
                onClick = onCancel,
                text = stringResource(R.string.group_can_not_be_created_discard_group_creation),
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
