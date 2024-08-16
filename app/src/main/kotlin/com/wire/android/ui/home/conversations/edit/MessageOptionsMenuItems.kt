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
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.util.Copyable
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.message.mention.MessageMention

@Composable
fun messageOptionsMenuItems(
    message: UIMessage.Regular,
    hideEditMessageMenu: (OnComplete) -> Unit,
    onCopyClick: (text: String) -> Unit,
    onDeleteClick: (messageId: String, isMyMessage: Boolean) -> Unit,
    onReactionClick: (messageId: String, reactionEmoji: String) -> Unit,
    onDetailsClick: (messageId: String, isSelfMessage: Boolean) -> Unit,
    onReplyClick: (UIMessage.Regular) -> Unit,
    onEditClick: (messageId: String, messageBody: String, mentions: List<MessageMention>) -> Unit,
    onShareAssetClick: (messageId: String) -> Unit,
    onDownloadAssetClick: (messageId: String) -> Unit,
    onOpenAssetClick: (messageId: String) -> Unit
): List<@Composable () -> Unit> {
    val localContext = LocalContext.current
    val isUploading = message.isPending
    val isDeleted = message.isDeleted
    val isMyMessage = message.isMyMessage
    val isComposite = message.messageContent is UIMessageContent.Composite
    val isEphemeral = message.header.messageStatus.expirationStatus is ExpirationStatus.Expirable
    val isEditable = !isUploading && !isDeleted && message.messageContent is UIMessageContent.TextMessage && isMyMessage
    val isCopyable = !isUploading && !isDeleted && message.messageContent is Copyable

    val onCopyItemClick = remember(message.messageContent) {
        (message.messageContent as? Copyable)?.textToCopy(localContext.resources)?.let {
            {
                hideEditMessageMenu { onCopyClick(it) }
            }
        } ?: {}
    }

    val onDeleteItemClick = remember(message.header.messageId) {
        {
            hideEditMessageMenu {
                onDeleteClick(message.header.messageId, message.isMyMessage)
            }
        }
    }
    val onReactionItemClick: (emoji: String) -> Unit = remember(message.header.messageId) {
        {
            hideEditMessageMenu {
                onReactionClick(message.header.messageId, it)
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
    val onShareAssetItemClick = remember(message) {
        {
            hideEditMessageMenu {
                onShareAssetClick(message.header.messageId)
            }
        }
    }

    return if (message.isAssetMessage) {
        assetMessageOptionsMenuItems(
            isEphemeral = isEphemeral,
            isUploading = isUploading,
            isOpenable = true,
            onDeleteClick = onDeleteItemClick,
            onDetailsClick = onDetailsItemClick,
            onShareAsset = onShareAssetItemClick,
            onDownloadAsset = onDownloadAssetItemClick,
            onReplyClick = onReplyItemClick,
            onReactionClick = onReactionItemClick,
            onOpenAsset = onOpenAssetItemClick,
        )
    } else {
        textMessageEditMenuItems(
            isEphemeral = isEphemeral,
            isUploading = isUploading,
            isComposite = isComposite,
            isEditable = isEditable,
            isCopyable = isCopyable,
            onDeleteClick = onDeleteItemClick,
            onDetailsClick = onDetailsItemClick,
            onReactionClick = onReactionItemClick,
            onEditClick = onEditItemClick,
            onCopyClick = onCopyItemClick,
            onReplyClick = onReplyItemClick
        )
    }
}

typealias OnComplete = () -> Unit
