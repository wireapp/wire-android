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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.StatusBox
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversations.SelfDeletionTimerHelper
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.model.DeliveryStatusContent
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
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
    @Composable
    fun messageContent() {

        if (isBubbleUiEnabled) {
            // TODO MessageBubbleItem will be introduced in next PR
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
            showAuthor = showAuthor,
            useSmallBottomPadding = useSmallBottomPadding,
            fullAvatarOuterPadding = dimensions().avatarClickablePadding,
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
                    showAuthor = showAuthor,
                    assetStatus = assetStatus,
                    shouldDisplayMessageStatus = shouldDisplayMessageStatus,
                    shouldDisplayFooter = shouldDisplayFooter,
                    failureInteractionAvailable = failureInteractionAvailable,
//                    useSmallBottomPadding = useSmallBottomPadding,
                    selfDeletionTimerState = selfDeletionTimerState,
//                    isBubble = false
                )
            }
        )
    }

    when (swipeableMessageConfiguration) {
        is SwipeableMessageConfiguration.Swipeable -> {
            SwipeableMessageBox(swipeableMessageConfiguration) {
                messageContent()
            }
        }

        SwipeableMessageConfiguration.NotSwipeable -> messageContent()
    }
}

@Composable
fun EphemeralMessageExpiredLabel(
    isSelfMessage: Boolean,
    conversationDetailsData: ConversationDetailsData,
    modifier: Modifier = Modifier,
) {

    val stringResource = if (!isSelfMessage) {
        stringResource(id = R.string.label_information_waiting_for_deleation_when_self_not_sender)
    } else if (conversationDetailsData is ConversationDetailsData.OneOne) {
        conversationDetailsData.otherUserName?.let {
            stringResource(
                R.string.label_information_waiting_for_recipient_timer_to_expire_one_to_one,
                conversationDetailsData.otherUserName
            )
        } ?: stringResource(id = R.string.unknown_user_name)
    } else {
        stringResource(R.string.label_information_waiting_for_recipient_timer_to_expire_group)
    }

    Text(
        modifier = modifier,
        text = stringResource,
        style = typography().body05
    )
}

@Composable
fun MessageExpireLabel(messageContent: UIMessageContent?, timeLeft: String) {
    when (messageContent) {
        is UIMessageContent.Location,
        is UIMessageContent.AssetMessage,
        is UIMessageContent.AudioAssetMessage,
        is UIMessageContent.ImageMessage,
        is UIMessageContent.TextMessage -> {
            StatusBox(
                statusText = stringResource(
                    R.string.self_deleting_message_time_left,
                    timeLeft
                )
            )
        }

        is UIMessageContent.Deleted -> {
            val context = LocalContext.current

            StatusBox(
                statusText = stringResource(
                    R.string.self_deleting_message_time_left,
                    context.resources.getQuantityString(
                        R.plurals.seconds_left,
                        0,
                        0
                    )
                )
            )
        }

        else -> {}
    }
}

internal val DeliveryStatusContent.expandable
    get() = this is DeliveryStatusContent.PartialDelivery && !this.isSingleUserFailure
