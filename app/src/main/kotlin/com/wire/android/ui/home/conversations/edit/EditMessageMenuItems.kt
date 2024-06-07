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

package com.wire.android.ui.home.conversations.edit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.util.Copyable
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.message.mention.MessageMention

@Composable
fun editMessageMenuItems(
    message: UIMessage.Regular,
    messageOptionsEnabled: Boolean,
    hideEditMessageMenu: (OnComplete) -> Unit,
    onCopyClick: (String) -> Unit,
    onDeleteClick: (messageId: String, Boolean) -> Unit,
    onReactionClick: (messageId: String, reactionEmoji: String) -> Unit,
    onDetailsClick: (messageId: String, isSelfMessage: Boolean) -> Unit,
    onReplyClick: (UIMessage.Regular) -> Unit,
    onEditClick: (String, String, List<MessageMention>) -> Unit,
    onShareAssetClick: () -> Unit,
    onDownloadAssetClick: (String) -> Unit,
    onOpenAssetClick: (String) -> Unit
): List<@Composable () -> Unit> {
    val localContext = LocalContext.current

    val isComposite = remember(message.header.messageId) {
        message.messageContent is UIMessageContent.Composite
    }

    val onCopyItemClick: (() -> Unit)? = remember(message.header.messageId) {
        (message.messageContent as? Copyable)?.textToCopy(localContext.resources)?.let {
            {
                hideEditMessageMenu { onCopyClick(it) }
            }
        }
    }

    val onDeleteItemClick = remember(message.header.messageId) {
        {
            hideEditMessageMenu {
                onDeleteClick(message.header.messageId, message.isMyMessage)
            }
        }
    }
    val onReactionItemClick = remember(message.header.messageId) {
        { emoji: String ->
            hideEditMessageMenu {
                onReactionClick(message.header.messageId, emoji)
            }
        }
    }
    val onReplyItemClick = remember(message.header.messageId) {
        {
            hideEditMessageMenu {
                onReplyClick(message)
            }
        }
    }
    val onDetailsItemClick = remember(message.header.messageId) {
        {
            hideEditMessageMenu {
                onDetailsClick(message.header.messageId, message.isMyMessage)
            }
        }
    }
    val onEditItemClick = remember(message) {
        {
            hideEditMessageMenu {
                with(message.messageContent as UIMessageContent.TextMessage) {
                    onEditClick(
                        message.header.messageId,
                        messageBody.message.asString(localContext.resources),
                        if (messageBody.message is UIText.DynamicString) messageBody.message.mentions else listOf()
                    )
                }
            }
        }
    }
    val onDownloadAssetItemClick = remember(message) {
        {
            hideEditMessageMenu {
                onDownloadAssetClick(message.header.messageId)
            }
        }
    }
    val onOpenAssetItemClick = remember(message) {
        {
            hideEditMessageMenu {
                onOpenAssetClick(message.header.messageId)
            }
        }
    }

    return if (message.isAssetMessage) {
        assetEditMenuItems(
            messageOptionsEnabled = messageOptionsEnabled,
            isEphemeral = message.header.messageStatus.expirationStatus is ExpirationStatus.Expirable,
            isUploading = message.isPending,
            onDeleteClick = onDeleteItemClick,
            onDetailsClick = onDetailsItemClick,
            onShareAsset = onShareAssetClick,
            onDownloadAsset = onDownloadAssetItemClick,
            onReplyClick = onReplyItemClick,
            onReactionClick = onReactionItemClick,
            onOpenAsset = onOpenAssetItemClick
        )
    } else {
        TextMessageEditMenuItems(
            isEphemeral = message.header.messageStatus.expirationStatus is ExpirationStatus.Expirable,
            isUploading = message.isPending,
            isComposite = isComposite,
            isLocation = message.isLocation,
            onDeleteClick = onDeleteItemClick,
            onDetailsClick = onDetailsItemClick,
            onReactionClick = onReactionItemClick,
            onEditClick = if (message.isMyMessage && !message.isDeleted) onEditItemClick else null,
            onCopyClick = onCopyItemClick,
            onReplyClick = onReplyItemClick
        )
    }
}

@Composable
fun CopyItemMenuOption(onCopyItemClick: () -> Unit) {
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

@Composable
fun ShareAssetMenuOption(onShareAsset: () -> Unit) {
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

@Composable
fun EditMessageMenuOption(onEditItemClick: () -> Unit) {
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

typealias OnComplete = () -> Unit
