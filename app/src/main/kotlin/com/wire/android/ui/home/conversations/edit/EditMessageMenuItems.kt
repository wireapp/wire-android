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
 *
 *
 */

package com.wire.android.ui.home.conversations.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumTouchTargetEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wire.android.R
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.debug.LocalFeatureVisibilityFlags

@Composable
fun EditMessageMenuItems(
    message: UIMessage,
    hideEditMessageMenu: () -> Unit,
    onCopyClick: (text: String) -> Unit,
    onDeleteClick: (messageId: String, isMyMessage: Boolean) -> Unit,
    onReactionClick: (messageId: String, emoji: String) -> Unit,
    onReplyClick: (message: UIMessage) -> Unit,
    onDetailsClick: (messageId: String, isMyMessage: Boolean) -> Unit,
    onEditClick: (messageId: String, originalText: String) -> Unit,
): List<@Composable () -> Unit> {
    val localFeatureVisibilityFlags = LocalFeatureVisibilityFlags.current
    val localContext = LocalContext.current
    val isCopyable = message.isTextMessage
    val isEditable = message.isMyMessage && localFeatureVisibilityFlags.MessageEditIcon
    val isAvailable = message.isAvailable

    val onCopyItemClick = remember(message) {
        {
            hideEditMessageMenu()
            onCopyClick((message.messageContent as UIMessageContent.TextMessage).messageBody.message.asString(localContext.resources))
        }
    }
    val onDeleteItemClick = remember(message) {
        {
            hideEditMessageMenu()
            onDeleteClick(message.messageHeader.messageId, message.isMyMessage)
        }
    }
    val onReactionItemClick = remember(message) {
        { emoji: String ->
            hideEditMessageMenu()
            onReactionClick(message.messageHeader.messageId, emoji)
        }
    }
    val onReplyItemClick = remember(message) {
        {
            hideEditMessageMenu()
            onReplyClick(message)
        }
    }
    val onDetailsItemClick = remember(message) {
        {
            hideEditMessageMenu()
            onDetailsClick(message.messageHeader.messageId, message.isMyMessage)
        }
    }
    val onEditItemClick = remember(message) {
        {
            hideEditMessageMenu()
            onEditClick(
                message.messageHeader.messageId,
                (message.messageContent as UIMessageContent.TextMessage).messageBody.message.asString(localContext.resources)
            )
        }
    }

    return buildList {
        if (isAvailable) {
            add { ReactionOptions(onReactionItemClick) }
            add { MessageDetails(onDetailsItemClick) }
            add {
                if (isCopyable) {
                    MenuBottomSheetItem(
                        icon = {
                            MenuItemIcon(
                                id = R.drawable.ic_copy,
                                contentDescription = stringResource(R.string.content_description_copy_the_message),
                            )
                        },
                        title = stringResource(R.string.label_copy),
                        onItemClick = onCopyItemClick
                    )
                }
            }
            add {
                MenuBottomSheetItem(
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_reply,
                            contentDescription = stringResource(R.string.content_description_reply_to_messge),
                        )
                    },
                    title = stringResource(R.string.notification_action_reply),
                    onItemClick = onReplyItemClick
                )
            }
            if (isEditable) {
                add {
                    MenuBottomSheetItem(
                        icon = {
                            MenuItemIcon(
                                id = R.drawable.ic_edit,
                                contentDescription = stringResource(R.string.content_description_edit_the_message)
                            )
                        },
                        title = stringResource(R.string.label_edit),
                        onItemClick = onEditItemClick
                    )
                }
            }
        }
        add {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                MenuBottomSheetItem(
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_delete,
                            contentDescription = stringResource(R.string.content_description_delete_the_message),
                        )
                    },
                    title = stringResource(R.string.label_delete),
                    onItemClick = onDeleteItemClick
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReactionOptions(
    onReactionClick: (emoji: String) -> Unit,
    emojiFontSize: TextUnit = 28.sp
) {
    Column {
        Row {
            Spacer(modifier = Modifier.width(dimensions().spacing8x))
            Text(
                ("${stringResource(R.string.label_reactions)} ${stringResource(id = R.string.label_more_comming_soon)}").uppercase(),
                style = MaterialTheme.wireTypography.label01
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("❤️", "👍", "😁", "🙂", "☹️", "👎").forEach { emoji ->
                CompositionLocalProvider(
                    LocalMinimumTouchTargetEnforcement provides false
                ) {
                    Button(
                        onClick = {
                            // TODO remove when all emojis will be available
                            if (emoji == "❤️") {
                                // So we display the pretty emoji,
                                // but we match the ugly one sent from other platforms
                                val correctedEmoji = "❤"
                                onReactionClick(correctedEmoji)
                            }
                        },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                            // TODO remove when all emojis will be available
                            .alpha(if (emoji == "❤️") 1F else 0.3F),
                        contentPadding = PaddingValues(dimensions().spacing8x),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.wireColorScheme.surface,
                            contentColor = MaterialTheme.wireColorScheme.secondaryButtonSelectedOutline
                        )
                    ) {
                        Text(emoji, style = TextStyle(fontSize = emojiFontSize))
                    }
                }
            }
            IconButton(
                onClick = {
                    // TODO show more emojis
                },
                modifier = Modifier
                    // TODO remove when all emojis will be available
                    .alpha(0.1F),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_more_emojis),
                    contentDescription = stringResource(R.string.content_description_more_emojis)
                )
            }
        }
    }
}

@Composable
private fun MessageDetails(
    onMessageDetailsClick: () -> Unit
) {
    MenuBottomSheetItem(
        icon = {
            MenuItemIcon(
                id = R.drawable.ic_info,
                contentDescription = stringResource(R.string.content_description_open_message_details),
            )
        },
        title = stringResource(R.string.label_message_details),
        onItemClick = onMessageDetailsClick
    )
}
