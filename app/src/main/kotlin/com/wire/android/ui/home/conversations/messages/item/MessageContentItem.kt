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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversations.SelfDeletionTimerHelper
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.model.MessageEditStatus
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.wireColorScheme
import com.wire.kalium.logic.data.asset.AssetTransferStatus

@Suppress("CyclomaticComplexMethod")
@Composable
fun MessageContentItem(
    clickActions: MessageClickActions,
    message: UIMessage.Regular,
    conversationDetailsData: ConversationDetailsData,
    messageStyle: MessageStyle,
    modifier: Modifier = Modifier,
    accent: Accent = Accent.Unknown,
    searchQuery: String = "",
    assetStatus: AssetTransferStatus? = null,
    shouldDisplayMessageStatus: Boolean = true,
    shouldDisplayFooter: Boolean = true,
    failureInteractionAvailable: Boolean = true,
    useSmallBottomPadding: Boolean = false,
    selfDeletionTimerState: SelfDeletionTimerHelper.SelfDeletionTimerState = SelfDeletionTimerHelper.SelfDeletionTimerState.NotExpirable,
    innerPadding: PaddingValues = PaddingValues(),
) {

    with(message) {
        Column(
            modifier = modifier,
            horizontalAlignment = if (messageStyle == MessageStyle.BUBBLE_SELF) Alignment.End else Alignment.Start
        ) {
            if (!messageStyle.isBubble()) {
                MessageStatusAndExpireTimer(
                    message = message,
                    conversationDetailsData = conversationDetailsData,
                    selfDeletionTimerState = selfDeletionTimerState,
                )
            }

            if (isDeleted) {
                if (messageStyle.isBubble()) {
                    MessageBubbleEphemeralItem(
                        message = message,
                        conversationDetailsData = conversationDetailsData,
                        selfDeletionTimerState = selfDeletionTimerState
                    )
                }

                return@Column
            }

            if (!decryptionFailed) {
                MessageContentAndStatus(
                    message = message,
                    assetStatus = assetStatus,
                    messageStyle = messageStyle,
                    onAssetClicked = clickActions.onAssetClicked,
                    onImageClicked = clickActions.onImageClicked,
                    searchQuery = searchQuery,
                    accent = accent,
                    onProfileClicked = clickActions.onProfileClicked,
                    onLinkClicked = clickActions.onLinkClicked,
                    shouldDisplayMessageStatus = shouldDisplayMessageStatus,
                    conversationDetailsData = conversationDetailsData,
                    onReplyClicked = clickActions.onReplyClicked,
                )
                if (shouldDisplayFooter && !messageStyle.isBubble()) {
                    VerticalSpace.x4()
                    MessageReactionsItem(
                        messageFooter = messageFooter,
                        messageStyle = messageStyle,
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
            if (shouldShowBottomLabels(messageStyle, header.messageStatus, useSmallBottomPadding, selfDeletionTimerState)) {
                VerticalSpace.x4()
                Row(
                    Modifier.padding(innerPadding),
                    horizontalArrangement = if (source == MessageSource.Self) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MessageSmallLabel(
                        text = header.messageTime.formattedDate,
                        messageStyle = messageStyle
                    )

                    if (header.messageStatus.editStatus is MessageEditStatus.Edited) {
                        HorizontalSpace.x8()
                        MessageSmallLabel(
                            text = "• " + stringResource(R.string.label_message_status_edited),
                            messageStyle = messageStyle
                        )
                        HorizontalSpace.x8()
                    }

                    MessageBubbleExpireFooter(
                        messageStyle = messageStyle,
                        selfDeletionTimerState = selfDeletionTimerState,
                        accentColor = MaterialTheme.wireColorScheme.wireAccentColors.getOrDefault(
                            header.accent,
                            MaterialTheme.wireColorScheme.primary
                        )
                    )

                    if (isMyMessage && shouldDisplayMessageStatus) {
                        HorizontalSpace.x8()
                        MessageStatusIndicator(
                            status = message.header.messageStatus.flowStatus,
                            isGroupConversation = conversationDetailsData is ConversationDetailsData.Group,
                            messageStyle = messageStyle,
                            modifier = Modifier
                                .size(dimensions().spacing14x)
                        )
                    } else {
                        HorizontalSpace.x12()
                    }
                }
            }
        }
    }
}

@Composable
private fun shouldShowBottomLabels(
    messageStyle: MessageStyle,
    messageStatus: MessageStatus,
    useSmallBottomPadding: Boolean,
    selfDeletionTimerState: SelfDeletionTimerHelper.SelfDeletionTimerState
): Boolean = messageStyle.isBubble() && (
        !useSmallBottomPadding
                || messageStatus.editStatus is MessageEditStatus.Edited
                || selfDeletionTimerState is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable
        )
