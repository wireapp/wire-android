package com.wire.android.ui.home.conversations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.home.conversations.ConversationErrors.ErrorMaxAssetSize
import com.wire.android.ui.home.conversations.ConversationErrors.ErrorMaxImageSize
import com.wire.android.ui.home.conversations.ConversationErrors.ErrorSendingAsset
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialog
import com.wire.android.ui.home.conversations.mock.getMockedMessages
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.MessageViewWrapper
import com.wire.android.ui.home.conversationslist.common.GroupConversationAvatar
import com.wire.android.ui.home.messagecomposer.MessageComposeInputState
import com.wire.android.ui.home.messagecomposer.MessageComposer
import com.wire.android.util.permission.rememberCallingRecordAudioBluetoothRequestFlow
import kotlinx.coroutines.launch

@Composable
fun ConversationScreen(conversationViewModel: ConversationViewModel) {
    val audioPermissionCheck = AudioBluetoothPermissionCheckFlow(conversationViewModel)
    val uiState = conversationViewModel.conversationViewState

    ConversationScreen(
        conversationViewState = uiState,
        onMessageChanged = conversationViewModel::onMessageChanged,
        onSendButtonClicked = conversationViewModel::sendMessage,
        onSendAttachment = conversationViewModel::sendAttachmentMessage,
        onDownloadAsset = conversationViewModel::downloadAsset,
        onImageFullScreenMode = { conversationViewModel.navigateToGallery(it) },
        onBackButtonClick = conversationViewModel::navigateBack,
        onDeleteMessage = conversationViewModel::showDeleteMessageDialog,
        onCallStart = audioPermissionCheck::launch,
        onError = conversationViewModel::onError
    )
    DeleteMessageDialog(conversationViewModel = conversationViewModel)
}

@Composable
private fun AudioBluetoothPermissionCheckFlow(conversationViewModel: ConversationViewModel) =
    rememberCallingRecordAudioBluetoothRequestFlow(onAudioBluetoothPermissionGranted = {
        conversationViewModel.navigateToInitiatingCallScreen()
    }) {
        //TODO display an error dialog
    }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
private fun ConversationScreen(
    conversationViewState: ConversationViewState,
    onMessageChanged: (String) -> Unit,
    onSendButtonClicked: () -> Unit,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onDownloadAsset: (String) -> Unit,
    onImageFullScreenMode: (String) -> Unit,
    onBackButtonClick: () -> Unit,
    onDeleteMessage: (String, Boolean) -> Unit,
    onCallStart: () -> Unit,
    onError: (ConversationErrors) -> Unit
) {
    val conversationScreenState = rememberConversationScreenState()
    val scope = rememberCoroutineScope()

    with(conversationViewState) {
        MenuModalSheetLayout(
            sheetState = conversationScreenState.modalBottomSheetState,
            menuItems = EditMessageMenuItems(
                isMyMessage = conversationScreenState.isSelectedMessageMyMessage(),
                onCopyMessage = conversationScreenState::copyMessage,
                onDeleteMessage = {
                    conversationScreenState.hideEditContextMenu()
                    onDeleteMessage(
                        conversationScreenState.selectedMessage?.messageHeader!!.messageId,
                        conversationScreenState.isSelectedMessageMyMessage()
                    )
                }
            ),
            content = {
                Scaffold(
                    topBar = {
                        ConversationScreenTopAppBar(
                            title = conversationName,
                            avatar = {
                                when (conversationAvatar) {
                                    is ConversationAvatar.Group -> GroupConversationAvatar(colorValue = conversationAvatar.groupColorValue)
                                    is ConversationAvatar.OneOne -> UserProfileAvatar(userAvatarAsset = conversationAvatar.avatarAsset)
                                    ConversationAvatar.None -> Box(modifier = Modifier.size(dimensions().userAvatarDefaultSize))
                                }
                            },
                            onBackButtonClick = onBackButtonClick,
                            onDropDownClick = { },
                            onSearchButtonClick = { },
                            onPhoneButtonClick = { onCallStart() }
                        )
                    },
                    snackbarHost = {
                        SwipeDismissSnackbarHost(
                            hostState = conversationScreenState.snackBarHostState,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    content = {
                        ConversationScreenContent(
                            messages = messages,
                            onMessageChanged = onMessageChanged,
                            messageText = conversationViewState.messageText,
                            onSendButtonClicked = onSendButtonClicked,
                            onShowContextMenu = { message -> conversationScreenState.showEditContextMenu(message) },
                            onSendAttachment = onSendAttachment,
                            onDownloadAsset = onDownloadAsset,
                            onImageFullScreenMode = onImageFullScreenMode,
                            conversationState = this,
                            onMessageComposerError = onError,
                            conversationScreenState = conversationScreenState
                        )
                    }
                )
            }
        )
    }
}

@Composable
fun getErrorMessage(errorCode: ConversationErrors) =
    when (errorCode) {
        ErrorMaxAssetSize -> stringResource(R.string.error_conversation_max_asset_size_limit)
        ErrorMaxImageSize -> stringResource(R.string.error_conversation_max_image_size_limit)
        ErrorSendingAsset -> stringResource(R.string.error_conversation_sending_image)
        else -> stringResource(R.string.error_conversation_generic)
    }

@Composable
private fun EditMessageMenuItems(
    isMyMessage: Boolean,
    onCopyMessage: () -> Unit,
    onDeleteMessage: () -> Unit
): List<@Composable () -> Unit> {
    return buildList {
        add {
            MenuBottomSheetItem(
                icon = {
                    MenuItemIcon(
                        id = R.drawable.ic_copy,
                        contentDescription = stringResource(R.string.content_description_block_the_user),
                    )
                },
                title = stringResource(R.string.label_copy),
                onItemClick = onCopyMessage
            )
        }
        if (isMyMessage)
            add {
                MenuBottomSheetItem(
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_edit,
                            contentDescription = stringResource(R.string.content_description_edit_the_message)
                        )
                    },
                    title = stringResource(R.string.label_edit),
                )
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
                    onItemClick = onDeleteMessage
                )
            }
        }
    }
}

@Composable
private fun ConversationScreenContent(
    messages: List<MessageViewWrapper>,
    onMessageChanged: (String) -> Unit,
    messageText: String,
    onSendButtonClicked: () -> Unit,
    onShowContextMenu: (MessageViewWrapper) -> Unit,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onDownloadAsset: (String) -> Unit,
    onImageFullScreenMode: (String) -> Unit,
    onMessageComposerError: (ConversationErrors) -> Unit,
    conversationState: ConversationViewState,
    conversationScreenState: ConversationScreenState
) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(conversationState.messages) {
        lazyListState.animateScrollToItem(0)
    }

    conversationState.onError?.let { errorCode ->
        val errorMessage = getErrorMessage(errorCode)
        LaunchedEffect(conversationState.onError) {
            conversationScreenState.snackBarHostState.showSnackbar(errorMessage)
        }
    }

    MessageComposer(
        content = {
            LazyColumn(
                state = lazyListState,
                reverseLayout = true,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
            ) {
                items(messages, key = {
                    it.messageHeader.messageId
                }) { message ->
                    MessageItem(
                        message = message,
                        onLongClicked = { onShowContextMenu(message) },
                        onAssetMessageClicked = onDownloadAsset,
                        onImageMessageClicked = onImageFullScreenMode
                    )
                }
            }
        },
        messageText = messageText,
        onMessageChanged = onMessageChanged,
        onSendButtonClicked = onSendButtonClicked,
        onSendAttachment = onSendAttachment,
        onMessageComposerError = onMessageComposerError,
        onMessageComposerInputStateChange = { messageComposerState ->
            if (messageComposerState.to == MessageComposeInputState.Active
                && messageComposerState.from == MessageComposeInputState.Enabled
            ) {
                coroutineScope.launch { lazyListState.animateScrollToItem(messages.size) }
            }
        }
    )
}

@Preview
@Composable
fun ConversationScreenPreview() {
    ConversationScreen(
        ConversationViewState(
            conversationName = "Some test conversation",
            messages = getMockedMessages(),
        ),
        {}, {}, {}, {}, {}, {}, { _: String, _: Boolean -> }, {}, {}
    )
}
