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

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.edit.DownloadAssetExternallyOption
import com.wire.android.ui.edit.MessageDetailsMenuOption
import com.wire.android.ui.edit.OpenAssetExternallyOption
import com.wire.android.ui.edit.ReactionOption
import com.wire.android.ui.edit.ReplyMessageOption
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.util.debug.LocalFeatureVisibilityFlags

@Composable
fun EditMessageMenuItems(
    message: UIMessage.Regular,
    hideEditMessageMenu: (OnComplete) -> Unit,
    onCopyClick: (text: String) -> Unit,
    onDeleteClick: (messageId: String, isMyMessage: Boolean) -> Unit,
    onReactionClick: (messageId: String, emoji: String) -> Unit,
    onReplyClick: (message: UIMessage.Regular) -> Unit,
    onDetailsClick: (messageId: String, isMyMessage: Boolean) -> Unit,
    onEditClick: (messageId: String, originalText: String) -> Unit,
    onShareAsset: () -> Unit,
    onDownloadAsset: (messageId: String) -> Unit,
    onOpenAsset: (messageId: String) -> Unit,
): List<@Composable () -> Unit> {
    val localFeatureVisibilityFlags = LocalFeatureVisibilityFlags.current
    val localContext = LocalContext.current
    val isCopyable = message.isTextMessage
    val isAvailable = message.isAvailable
    val isAssetMessage = message.messageContent is UIMessageContent.AssetMessage
            || message.messageContent is UIMessageContent.ImageMessage
            || message.messageContent is UIMessageContent.AudioAssetMessage
    val isEditable = message.isTextMessage && message.isMyMessage && localFeatureVisibilityFlags.MessageEditIcon
    val isGenericAsset = message.messageContent is UIMessageContent.AssetMessage

    val onCopyItemClick = remember(message) {
        {
            hideEditMessageMenu {
                onCopyClick((message.messageContent as UIMessageContent.TextMessage).messageBody.message.asString(localContext.resources))
            }
        }
    }
    val onDeleteItemClick = remember(message) {
        {
            hideEditMessageMenu {
                onDeleteClick(message.header.messageId, message.isMyMessage)
            }
        }
    }
    val onReactionItemClick = remember(message) {
        { emoji: String ->
            hideEditMessageMenu {
                onReactionClick(message.header.messageId, emoji)
            }
        }
    }
    val onReplyItemClick = remember(message) {
        {
            hideEditMessageMenu {
                onReplyClick(message)
            }
        }
    }
    val onDetailsItemClick = remember(message) {
        {
            hideEditMessageMenu {
                onDetailsClick(message.header.messageId, message.isMyMessage)
            }
        }
    }
    val onEditItemClick = remember(message) {
        {
            hideEditMessageMenu {
                onEditClick(
                    message.header.messageId,
                    (message.messageContent as UIMessageContent.TextMessage).messageBody.message.asString(localContext.resources)
                )
            }
        }
    }
    val onDownloadAssetClick = remember(message) {
        {
            hideEditMessageMenu {
                onDownloadAsset(message.header.messageId)
            }
        }
    }
    val onOpenAssetClick = remember(message) {
        {
            hideEditMessageMenu {
                onOpenAsset(message.header.messageId)
            }
        }
    }

    return buildList {
        if (isAvailable) {
            add { ReactionOption(onReactionItemClick) }
            add { MessageDetailsMenuOption(onDetailsItemClick) }
            if (isCopyable) {
                add {
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
            add { ReplyMessageOption(onReplyItemClick) }
            if (isAssetMessage) add { DownloadAssetExternallyOption(onDownloadAssetClick) }
            if (isGenericAsset) add { OpenAssetExternallyOption(onOpenAssetClick) }
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
            if (isAssetMessage) {
                add {
                    MenuBottomSheetItem(
                        icon = {
                            MenuItemIcon(
                                id = R.drawable.ic_share_file,
                                contentDescription = stringResource(R.string.content_description_share_the_file),
                            )
                        },
                        title = stringResource(R.string.label_share),
                        onItemClick = onShareAsset
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

typealias OnComplete = () -> Unit
