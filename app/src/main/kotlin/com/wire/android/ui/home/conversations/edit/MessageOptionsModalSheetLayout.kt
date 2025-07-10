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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.wire.android.R
import com.wire.android.di.hiltViewModelScoped
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.WireMenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.WireSheetValue
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.collectAsStateLifecycleAware
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.isEditable
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.Copyable
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.mention.MessageMention

@Suppress("CyclomaticComplexMethod")
@Composable
fun MessageOptionsModalSheetLayout(
    conversationId: ConversationId,
    sheetState: WireModalSheetState<String>,
    onCopyClick: (text: String) -> Unit,
    onDeleteClick: (messageId: String, isMyMessage: Boolean) -> Unit,
    onReactionClick: (messageId: String, reactionEmoji: String) -> Unit,
    onDetailsClick: (messageId: String, isSelfMessage: Boolean) -> Unit,
    onReplyClick: (UIMessage.Regular) -> Unit,
    onEditClick: (messageId: String, messageBody: String, mentions: List<MessageMention>) -> Unit,
    onShareAssetClick: (messageId: String) -> Unit,
    onDownloadAssetClick: (messageId: String) -> Unit,
    onOpenAssetClick: (messageId: String) -> Unit,
    viewModel: MessageOptionsMenuViewModel =
        hiltViewModelScoped<MessageOptionsMenuViewModelImpl, MessageOptionsMenuViewModel, MessageOptionsMenuArgs>(
            MessageOptionsMenuArgs(conversationId)
        )
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    WireModalSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            when (val state = viewModel.observeMessageStateFlow(it).collectAsStateLifecycleAware().value) {
                is MessageOptionsMenuState.Message -> MessageOptionsModalContent( // message state - show the sheet with proper content
                    message = state.message,
                    sheetState = sheetState,
                    onCopyClick = onCopyClick,
                    onDeleteClick = onDeleteClick,
                    onReactionClick = onReactionClick,
                    onDetailsClick = onDetailsClick,
                    onReplyClick = onReplyClick,
                    onEditClick = onEditClick,
                    onShareAssetClick = onShareAssetClick,
                    onDownloadAssetClick = onDownloadAssetClick,
                    onOpenAssetClick = onOpenAssetClick
                )

                MessageOptionsMenuState.Loading -> WireCircularProgressIndicator( // loading state - show a progress indicator
                    progressColor = colorsScheme().onSurface,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                MessageOptionsMenuState.NotAvailable -> sheetState.hide { // message not found - hide the sheet and show a message
                    snackbarHostState.showSnackbar(context.getString(R.string.deleted_message_options_closed))
                }
            }
        }
    )
}

@Composable
private fun MessageOptionsModalContent(
    message: UIMessage.Regular,
    sheetState: WireModalSheetState<String>,
    onCopyClick: (text: String) -> Unit,
    onDeleteClick: (messageId: String, isMyMessage: Boolean) -> Unit,
    onReactionClick: (messageId: String, reactionEmoji: String) -> Unit,
    onDetailsClick: (messageId: String, isSelfMessage: Boolean) -> Unit,
    onReplyClick: (UIMessage.Regular) -> Unit,
    onEditClick: (messageId: String, messageBody: String, mentions: List<MessageMention>) -> Unit,
    onShareAssetClick: (messageId: String) -> Unit,
    onDownloadAssetClick: (messageId: String) -> Unit,
    onOpenAssetClick: (messageId: String) -> Unit,
) {
    val context = LocalContext.current
    val isUploading = message.isPending
    val isDeleted = message.isDeleted
    val isMyMessage = message.isMyMessage
    val isEphemeral = message.header.messageStatus.expirationStatus is ExpirationStatus.Expirable
    WireMenuModalSheetContent(
        header = MenuModalSheetHeader.Gone,
        menuItems = messageOptionsMenuItems(
            isAssetMessage = message.isAssetMessage,
            isUploading = message.isPending,
            isComposite = message.messageContent is UIMessageContent.Composite,
            isEphemeral = isEphemeral,
            isEditable = !isUploading && !isDeleted && (message.messageContent?.isEditable() ?: false) && isMyMessage,
            isCopyable = !isUploading && !isDeleted && !isEphemeral && message.messageContent is Copyable,
            isOpenable = true,
            onCopyClick = remember(message.messageContent) {
                (message.messageContent as? Copyable)?.textToCopy(context.resources)?.let {
                    {
                        sheetState.hide {
                            onCopyClick(it)
                        }
                    }
                } ?: {}
            },
            onDeleteClick = remember(message.header.messageId) {
                {
                    sheetState.hide {
                        onDeleteClick(
                            message.header.messageId,
                            message.isMyMessage,
                        )
                    }
                }
            },
            onReactionClick = remember(message.header.messageId) {
                {
                    sheetState.hide {
                        onReactionClick(message.header.messageId, it)
                    }
                }
            },
            onDetailsClick = remember(message.header.messageId) {
                {
                    sheetState.hide {
                        onDetailsClick(message.header.messageId, message.isMyMessage)
                    }
                }
            },
            onReplyClick = remember(message.header.messageId) {
                {
                    sheetState.hide {
                        onReplyClick(message)
                    }
                }
            },
            onEditClick = remember(message.header.messageId, message.messageContent) {
                {
                    when (message.messageContent) {
                        is UIMessageContent.TextMessage ->
                            sheetState.hide {
                                onEditClick(
                                    message.header.messageId,
                                    message.messageContent.messageBody.message.asString(context.resources),
                                    (message.messageContent.messageBody.message as? UIText.DynamicString)?.mentions ?: listOf()
                                )
                            }

                        is UIMessageContent.Multipart ->
                            sheetState.hide {
                                with(message.messageContent.messageBody) {
                                    onEditClick(
                                        message.header.messageId,
                                        this?.message?.asString(context.resources) ?: "",
                                        (this?.message as? UIText.DynamicString)?.mentions ?: listOf()
                                    )
                                }
                            }

                        else -> error("Unsupported message type")
                    }
                }
            },
            onShareAssetClick = remember(message.header.messageId) {
                {
                    sheetState.hide {
                        onShareAssetClick(message.header.messageId)
                    }
                }
            },
            onDownloadAssetClick = remember(message.header.messageId) {
                {
                    sheetState.hide {
                        onDownloadAssetClick(message.header.messageId)
                    }
                }
            },
            onOpenAssetClick = remember(message.header.messageId) {
                {
                    sheetState.hide {
                        onOpenAssetClick(message.header.messageId)
                    }
                }
            },
        )
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageOptionsModalSheetLayout() = WireTheme {
    MessageOptionsModalSheetLayout(
        conversationId = ConversationId("cid", "domain"),
        sheetState = rememberWireModalSheetState(initialValue = WireSheetValue.Expanded("id")),
        onCopyClick = {},
        onDeleteClick = { _, _ -> },
        onReactionClick = { _, _ -> },
        onDetailsClick = { _, _ -> },
        onReplyClick = { },
        onEditClick = { _, _, _ -> },
        onShareAssetClick = { },
        onDownloadAssetClick = { },
        onOpenAssetClick = { }
    )
}
