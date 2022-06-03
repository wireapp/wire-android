package com.wire.android.ui.home.conversations

import android.app.DownloadManager
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorDownloadingAsset
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorMaxAssetSize
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorMaxImageSize
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorOpeningAssetFile
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorSendingAsset
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorSendingImage
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.OnFileDownloaded
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialog
import com.wire.android.ui.home.conversations.edit.EditMessageMenuItems
import com.wire.android.ui.home.conversations.mock.getMockedMessages
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.MessageContent
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

    LaunchedEffect(conversationViewModel.savedStateHandle) {
        conversationViewModel.checkPendingActions()
    }

    ConversationScreen(
        conversationViewState = uiState,
        onMessageChanged = conversationViewModel::onMessageChanged,
        onSendButtonClicked = conversationViewModel::sendMessage,
        onSendAttachment = conversationViewModel::sendAttachmentMessage,
        onDownloadAsset = conversationViewModel::downloadOrFetchAssetToInternalStorage,
        onImageFullScreenMode = conversationViewModel::navigateToGallery,
        onBackButtonClick = conversationViewModel::navigateBack,
        onDeleteMessage = conversationViewModel::showDeleteMessageDialog,
        onCallStart = audioPermissionCheck::launch,
        onSnackbarMessage = conversationViewModel::onSnackbarMessage
    )
    DeleteMessageDialog(conversationViewModel = conversationViewModel)
    DownloadedAssetDialog(
        downloadedAssetDialogState = conversationViewModel.conversationViewState.downloadedAssetDialogState,
        onSaveFileToExternalStorage = conversationViewModel::onSaveFile,
        onOpenFileWithExternalApp = conversationViewModel::onOpenFileWithExternalApp,
        hideOnAssetDownloadedDialog = conversationViewModel::hideOnAssetDownloadedDialog
    )
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
    onImageFullScreenMode: (String, Boolean) -> Unit,
    onBackButtonClick: () -> Unit,
    onDeleteMessage: (String, Boolean) -> Unit,
    onCallStart: () -> Unit,
    onSnackbarMessage: (ConversationSnackbarMessages) -> Unit
) {
    val conversationScreenState = rememberConversationScreenState()
    val scope = rememberCoroutineScope()

    with(conversationViewState) {
        MenuModalSheetLayout(
            sheetState = conversationScreenState.modalBottomSheetState,
            coroutineScope = scope,
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
                    content = { internalPadding ->
                        Box(modifier = Modifier.padding(internalPadding)) {
                            ConversationScreenContent(
                                messages = messages,
                                onMessageChanged = onMessageChanged,
                                messageText = conversationViewState.messageText,
                                onSendButtonClicked = onSendButtonClicked,
                                onShowContextMenu = conversationScreenState::showEditContextMenu,
                                onSendAttachment = onSendAttachment,
                                onDownloadAsset = onDownloadAsset,
                                onImageFullScreenMode = onImageFullScreenMode,
                                conversationState = conversationViewState,
                                onMessageComposerError = onSnackbarMessage,
                                conversationScreenState = conversationScreenState
                            )
                        }
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
    onImageFullScreenMode: (String, Boolean) -> Unit,
    onMessageComposerError: (ConversationSnackbarMessages) -> Unit,
    conversationState: ConversationViewState,
    conversationScreenState: ConversationScreenState
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    conversationState.onSnackbarMessage?.let { messageCode ->
        val (message, actionLabel) = getSnackbarMessage(messageCode)
        LaunchedEffect(conversationState.onSnackbarMessage) {
            val snackbarResult = conversationScreenState.snackBarHostState.showSnackbar(message = message, actionLabel = actionLabel)
            when {
                // Show downloads folder when clicking on Snackbar cta button
                messageCode is OnFileDownloaded && snackbarResult == SnackbarResult.ActionPerformed -> {
                    context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                }
            }
        }
    }

    val lazyListState = rememberLazyListState()

    LaunchedEffect(messages) {
        lazyListState.animateScrollToItem(0)
    }

    MessageComposer(
        content = {
            MessageList(
                messages = messages,
                lazyListState = lazyListState,
                onShowContextMenu = onShowContextMenu,
                onDownloadAsset = onDownloadAsset,
                onImageFullScreenMode = onImageFullScreenMode
            )
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

@Composable
private fun getSnackbarMessage(messageCode: ConversationSnackbarMessages): Pair<String, String?> {
    val msg = when (messageCode) {
        is OnFileDownloaded -> stringResource(R.string.conversation_on_file_downloaded, messageCode.assetName ?: "")
        is ErrorMaxAssetSize -> stringResource(R.string.error_conversation_max_asset_size_limit, messageCode.maxLimitInMB)
        ErrorMaxImageSize -> stringResource(R.string.error_conversation_max_image_size_limit)
        ErrorSendingImage -> stringResource(R.string.error_conversation_sending_image)
        ErrorSendingAsset -> stringResource(R.string.error_conversation_sending_asset)
        ErrorDownloadingAsset -> stringResource(R.string.error_conversation_downloading_asset)
        ErrorOpeningAssetFile -> stringResource(R.string.error_conversation_opening_asset_file)
        ConversationSnackbarMessages.ErrorPickingAttachment -> stringResource(R.string.error_conversation_generic)
    }
    val actionLabel = when (messageCode) {
        is OnFileDownloaded -> stringResource(R.string.label_show)
        else -> null
    }
    return msg to actionLabel
}

@Composable
fun MessageList(
    messages: List<MessageViewWrapper>,
    lazyListState: LazyListState,
    onShowContextMenu: (MessageViewWrapper) -> Unit,
    onDownloadAsset: (String) -> Unit,
    onImageFullScreenMode: (String, Boolean) -> Unit
) {
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
            if (message.messageContent is MessageContent.MemberChangeMessage)
                SystemMessageItem(message = message.messageContent)
            else
                MessageItem(
                    message = message,
                    onLongClicked = onShowContextMenu,
                    onAssetMessageClicked = onDownloadAsset,
                    onImageMessageClicked = onImageFullScreenMode
                )
        }
    }
}

@Preview
@Composable
fun ConversationScreenPreview() {
    ConversationScreen(
        ConversationViewState(
            conversationName = "Some test conversation",
            messages = getMockedMessages(),
        ),
        {}, {}, {}, {}, { _, _ -> }, {}, { _, _ -> }, {}, {}
    )
}
