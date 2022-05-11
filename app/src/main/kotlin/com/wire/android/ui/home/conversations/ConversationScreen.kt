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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialog
import com.wire.android.ui.home.conversations.edit.EditMessageMenuItems
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
        onImageFullScreenMode = conversationViewModel::navigateToGallery,
        onBackButtonClick = conversationViewModel::navigateBack,
        onDeleteMessage = conversationViewModel::showDeleteMessageDialog,
        onCallStart = audioPermissionCheck::launch
    )
    DeleteMessageDialog(conversationViewModel = conversationViewModel)
    DownloadedAssetDialog(conversationViewModel = conversationViewModel)
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
    onCallStart: () -> Unit
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
                            onError = { errorMessage ->
                                scope.launch {
                                    conversationScreenState.snackBarHostState.showSnackbar(errorMessage)
                                }
                            }
                        )
                    }
                )
            }
        )
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
    onError: (String) -> Unit,
    conversationState: ConversationViewState
) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(conversationState.messages) {
        lazyListState.animateScrollToItem(0)
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
        onError = onError,
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
        {}, {}, {}, {}, {}, {}, { _: String, _: Boolean -> }
    ) {}
}
