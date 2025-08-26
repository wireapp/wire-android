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
package com.wire.android.ui.home.conversations.messages.item

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.StatusBox
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversations.SelfDeletionTimerHelper
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.kalium.logic.data.asset.AssetTransferStatus

@Composable
fun MessageContentItem(
    clickActions: MessageClickActions,
    message: UIMessage.Regular,
    conversationDetailsData: ConversationDetailsData,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    showAuthor: Boolean = true,
    assetStatus: AssetTransferStatus? = null,
    shouldDisplayMessageStatus: Boolean = true,
    shouldDisplayFooter: Boolean = true,
    failureInteractionAvailable: Boolean = true,
//    useSmallBottomPadding: Boolean = false, // TODO will be handled in next PR
    selfDeletionTimerState: SelfDeletionTimerHelper.SelfDeletionTimerState = SelfDeletionTimerHelper.SelfDeletionTimerState.NotExpirable,
//    innerPadding: PaddingValues = PaddingValues(),
//    isBubble: Boolean = false,
) {
    with(message) {
        Column(
            modifier = modifier,
        ) {
            if (showAuthor) {
                Spacer(modifier = Modifier.height(dimensions().avatarClickablePadding))
                MessageAuthorRow(messageHeader = message.header)
                Spacer(modifier = Modifier.height(dimensions().spacing4x))
            }
            if (selfDeletionTimerState is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable) {
                MessageExpireLabel(messageContent, selfDeletionTimerState.timeLeftFormatted)

                // if the message is marked as deleted and is [SelfDeletionTimer.SelfDeletionTimerState.Expirable]
                // the deletion responsibility belongs to the receiver, therefore we need to wait for the receiver
                // timer to expire to permanently delete the message, in the meantime we show the EphemeralMessageExpiredLabel
                if (isDeleted) {
                    EphemeralMessageExpiredLabel(
                        message.isMyMessage,
                        conversationDetailsData
                    )
                }
            } else {
                MessageStatusLabel(messageStatus = message.header.messageStatus)
            }

            if (isDeleted) return@Column

            if (!decryptionFailed) {
                MessageContentAndStatus(
                    message = message,
                    assetStatus = assetStatus,
                    onAssetClicked = clickActions.onAssetClicked,
                    onImageClicked = clickActions.onImageClicked,
                    searchQuery = searchQuery,
                    onProfileClicked = clickActions.onProfileClicked,
                    onLinkClicked = clickActions.onLinkClicked,
                    shouldDisplayMessageStatus = shouldDisplayMessageStatus,
                    conversationDetailsData = conversationDetailsData,
                    onReplyClicked = clickActions.onReplyClicked,
                )
                if (shouldDisplayFooter) {
                    VerticalSpace.x4()
                    MessageReactionsItem(
                        messageFooter = messageFooter,
                        onReactionClicked = clickActions.onReactionClicked
                    )
                }
            } else {
                MessageDecryptionFailure(
                    messageHeader = header,
                    decryptionStatus = header.messageStatus.flowStatus as MessageFlowStatus.Failure.Decryption,
                    onResetSessionClicked = clickActions.onResetSessionClicked,
                    conversationProtocol = conversationDetailsData.conversationProtocol
                )
            }
            if (message.sendingFailed) {
                MessageSendFailureWarning(
                    messageStatus = header.messageStatus.flowStatus as MessageFlowStatus.Failure.Send,
                    isInteractionAvailable = failureInteractionAvailable,
                    onRetryClick = remember(message) {
                        {
                            clickActions.onFailedMessageRetryClicked(
                                header.messageId,
                                message.conversationId
                            )
                        }
                    },
                    onCancelClick = remember(message) {
                        {
                            clickActions.onFailedMessageCancelClicked(header.messageId)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MessageStatusLabel(messageStatus: MessageStatus) {
    messageStatus.badgeText?.let {
        StatusBox(it.asString())
    }
}
