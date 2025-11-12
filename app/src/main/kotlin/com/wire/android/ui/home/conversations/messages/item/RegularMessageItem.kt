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

package com.wire.android.ui.home.conversations.messages.item

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.SelfDeletionTimerHelper
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.model.DeliveryStatusContent
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent.PartialDeliverable
import com.wire.kalium.logic.data.asset.AssetTransferStatus

// TODO: a definite candidate for a refactor and cleanup WPB-14390
@Suppress("ComplexMethod")
@Composable
fun RegularMessageItem(
    clickActions: MessageClickActions,
    message: UIMessage.Regular,
    conversationDetailsData: ConversationDetailsData,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    showAuthor: Boolean = true,
    assetStatus: AssetTransferStatus? = null,
    swipeableMessageConfiguration: SwipeableMessageConfiguration = SwipeableMessageConfiguration.NotSwipeable,
    shouldDisplayMessageStatus: Boolean = true,
    shouldDisplayFooter: Boolean = true,
    failureInteractionAvailable: Boolean = true,
    useSmallBottomPadding: Boolean = false,
    selfDeletionTimerState: SelfDeletionTimerHelper.SelfDeletionTimerState = SelfDeletionTimerHelper.SelfDeletionTimerState.NotExpirable,
    isBubbleUiEnabled: Boolean = false
): Unit = with(message) {
    val messageStyle = when {
        !isBubbleUiEnabled -> MessageStyle.NORMAL
        message.isMyMessage -> MessageStyle.BUBBLE_SELF
        else -> MessageStyle.BUBBLE_OTHER
    }

    @Composable
    fun messageContent() {
        if (isBubbleUiEnabled) {
            val footerSlot: (@Composable (inner: PaddingValues) -> Unit)? =
                if (shouldDisplayFooter && !message.header.messageStatus.isDeleted) {
                    { innerPadding ->
                        MessageReactionsItem(
                            messageFooter = message.messageFooter,
                            messageStyle = messageStyle,
                            onReactionClicked = clickActions.onReactionClicked,
                            modifier = Modifier.padding(innerPadding),
                            itemsAlignment = if (message.isMyMessage) {
                                Alignment.End
                            } else {
                                Alignment.Start
                            },
                            onLongClick = when {
                                message.header.messageStatus.isDeleted -> null // do not allow long press on deleted messages
                                else -> clickActions.onFullMessageLongClicked?.let {
                                    {
                                        it(message)
                                    }
                                }
                            },
                        )
                    }
                } else {
                    null
                }

            val leadingSlot: (@Composable () -> Unit)? =
                if (source == MessageSource.OtherUser && conversationDetailsData is ConversationDetailsData.Group) {
                    {
                        RegularMessageItemLeading(
                            header = header,
                            showAuthor = !useSmallBottomPadding,
                            userAvatarData = message.userAvatarData,
                            onOpenProfile = clickActions.onProfileClicked
                        )
                    }
                } else {
                    null
                }

            val headerSlot: (@Composable (inner: PaddingValues) -> Unit)? =
                if (showAuthor && (source == MessageSource.OtherUser)) {
                    { innerPadding ->
                        MessageAuthorRow(
                            messageHeader = message.header,
                            messageStyle = messageStyle,
                            modifier = Modifier
                                .padding(innerPadding)
                                .padding(
                                    bottom = dimensions().spacing4x,
                                )
                        )
                    }
                } else {
                    null
                }

            val errorSlot: (@Composable () -> Unit)? = when {
                sendingFailed -> {
                    {
                        MessageSendFailureWarning(
                            messageStatus = header.messageStatus.flowStatus as MessageFlowStatus.Failure.Send,
                            isInteractionAvailable = failureInteractionAvailable,
                            messageStyle = messageStyle,
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

                messageContent is PartialDeliverable && messageContent.deliveryStatus.hasAnyFailures -> {
                    {
                        PartialDeliveryInformation(messageContent.deliveryStatus, messageStyle)
                    }
                }

                else -> null
            }

            MessageBubbleItem(
                message = message,
                source = source,
                messageStatus = header.messageStatus,
                showAuthor = showAuthor,
                accent = header.accent,
                useSmallBottomPadding = useSmallBottomPadding,
                leading = leadingSlot,
                error = errorSlot,
                onClick = clickActions.onFullMessageClicked?.let { onFullMessageClicked ->
                    {
                        onFullMessageClicked(message.header.messageId)
                    }
                },
                onLongClick = when {
                    message.header.messageStatus.isDeleted -> null // do not allow long press on deleted messages
                    else -> clickActions.onFullMessageLongClicked?.let {
                        {
                            it(message)
                        }
                    }
                },
                footer = footerSlot,
                header = headerSlot,
                content = { innerPadding ->
                    MessageContentItem(
                        clickActions = clickActions,
                        message = message,
                        conversationDetailsData = conversationDetailsData,
                        modifier = modifier,
                        accent = header.accent,
                        searchQuery = searchQuery,
                        assetStatus = assetStatus,
                        shouldDisplayMessageStatus = shouldDisplayMessageStatus,
                        shouldDisplayFooter = shouldDisplayFooter,
                        failureInteractionAvailable = failureInteractionAvailable,
                        useSmallBottomPadding = useSmallBottomPadding,
                        selfDeletionTimerState = selfDeletionTimerState,
                        innerPadding = innerPadding,
                        messageStyle = messageStyle
                    )
                }
            )
        } else {
            val headerSlot: (@Composable () -> Unit)? =
                if (showAuthor) {
                    {
                        MessageAuthorRow(
                            messageHeader = message.header,
                            messageStyle = messageStyle,
                            modifier = Modifier
                                .padding(top = dimensions().avatarClickablePadding, bottom = dimensions().spacing4x)
                        )
                    }
                } else {
                    null
                }

            MessageItemTemplate(
                modifier = modifier
                    .interceptCombinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        onClick = clickActions.onFullMessageClicked?.let { onFullMessageClicked ->
                            {
                                onFullMessageClicked(message.header.messageId)
                            }
                        },
                        onLongPress = when {
                            message.header.messageStatus.isDeleted -> null // do not allow long press on deleted messages
                            else -> clickActions.onFullMessageLongClicked?.let {
                                {
                                    it(message)
                                }
                            }
                        },
                    ),
                useSmallBottomPadding = useSmallBottomPadding,
                fullAvatarOuterPadding = dimensions().avatarClickablePadding,
                header = headerSlot,
                leading = {
                    RegularMessageItemLeading(
                        header = header,
                        showAuthor = showAuthor,
                        userAvatarData = message.userAvatarData,
                        onOpenProfile = clickActions.onProfileClicked
                    )
                },
                content = {
                    MessageContentItem(
                        clickActions = clickActions,
                        message = message,
                        conversationDetailsData = conversationDetailsData,
                        modifier = modifier,
                        searchQuery = searchQuery,
                        assetStatus = assetStatus,
                        shouldDisplayMessageStatus = shouldDisplayMessageStatus,
                        shouldDisplayFooter = shouldDisplayFooter,
                        failureInteractionAvailable = failureInteractionAvailable,
                        useSmallBottomPadding = useSmallBottomPadding,
                        selfDeletionTimerState = selfDeletionTimerState,
                        messageStyle = messageStyle
                    )
                }
            )
        }
    }

    SwipeableMessageBox(
        configuration = swipeableMessageConfiguration,
        messageStyle = messageStyle,
    ) {
        messageContent()
    }
}

internal val DeliveryStatusContent.expandable
    get() = this is DeliveryStatusContent.PartialDelivery && !this.isSingleUserFailure
