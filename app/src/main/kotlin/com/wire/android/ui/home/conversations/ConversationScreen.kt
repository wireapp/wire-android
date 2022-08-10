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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.conversationColor
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorDeletingMessage
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorDownloadingAsset
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorMaxAssetSize
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorMaxImageSize
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorOpeningAssetFile
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorPickingAttachment
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorSendingAsset
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorSendingImage
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.OnFileDownloaded
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialog
import com.wire.android.ui.home.conversations.edit.EditMessageMenuItems
import com.wire.android.ui.home.conversations.mock.getMockedMessages
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.MessageContent
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversationslist.common.GroupConversationAvatar
import com.wire.android.ui.home.messagecomposer.MessageComposeInputState
import com.wire.android.ui.home.messagecomposer.MessageComposer
import com.wire.android.util.permission.rememberCallingRecordAudioBluetoothRequestFlow
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.launch
import okio.Path
import okio.Path.Companion.toPath

@Composable
fun ConversationScreen(conversationViewModel: ConversationViewModel) {
    val showDialog = remember { mutableStateOf(false) }

    val startCallAudioPermissionCheck = StartCallAudioBluetoothPermissionCheckFlow {
        conversationViewModel.navigateToInitiatingCallScreen()
    }
    val joinCallAudioPermissionCheck = JoinCallAudioBluetoothPermissionCheckFlow(conversationViewModel)
    val uiState = conversationViewModel.conversationViewState

    LaunchedEffect(conversationViewModel.savedStateHandle) {
        conversationViewModel.checkPendingActions()
    }

    if (showDialog.value) {
        WireDialog(
            title = stringResource(id = R.string.calling_ongoing_call_title_alert),
            text = stringResource(id = R.string.calling_ongoing_call_start_message_alert),
            onDismiss = { showDialog.value = false },
            optionButton1Properties = WireDialogButtonProperties(
                onClick = { showDialog.value = false },
                text = stringResource(id = R.string.label_cancel),
                type = WireDialogButtonType.Secondary
            ),
            optionButton2Properties = WireDialogButtonProperties(
                onClick = {
                    conversationViewModel.navigateToInitiatingCallScreen()
                    showDialog.value = false
                },
                text = stringResource(id = R.string.calling_ongoing_call_start_anyway),
                type = WireDialogButtonType.Primary
            )
        )
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
        onStartCall = {
            conversationViewModel.establishedCallConversationId?.let {
                showDialog.value = true
            } ?: run {
                startCallAudioPermissionCheck.launch()
            }
        },
        onJoinCall = joinCallAudioPermissionCheck::launch,
        onSnackbarMessage = conversationViewModel::onSnackbarMessage,
        onSnackbarMessageShown = conversationViewModel::clearSnackbarMessage,
        onDropDownClick = conversationViewModel::navigateToDetails,
        tempCachePath = conversationViewModel.provideTempCachePath(),
        onOpenProfile = conversationViewModel::navigateToProfile
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
private fun StartCallAudioBluetoothPermissionCheckFlow(
    onStartCall: () -> Unit
) = rememberCallingRecordAudioBluetoothRequestFlow(onAudioBluetoothPermissionGranted = {
    onStartCall()
}) {
    //TODO display an error dialog
}

@Composable
private fun JoinCallAudioBluetoothPermissionCheckFlow(conversationViewModel: ConversationViewModel) =
    rememberCallingRecordAudioBluetoothRequestFlow(onAudioBluetoothPermissionGranted = {
        conversationViewModel.joinOngoingCall()
    }) {
        //TODO display an error dialog
    }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Suppress("LongParameterList")
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
    onStartCall: () -> Unit,
    onJoinCall: () -> Unit,
    onSnackbarMessage: (ConversationSnackbarMessages) -> Unit,
    onSnackbarMessageShown: () -> Unit,
    onDropDownClick: () -> Unit,
    tempCachePath: Path,
    onOpenProfile: (MessageSource, UserId) -> Unit
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
                            title = conversationName.asString(),
                            avatar = {
                                when (conversationAvatar) {
                                    is ConversationAvatar.Group ->
                                        GroupConversationAvatar(
                                            color = colorsScheme().conversationColor(id = conversationAvatar.conversationId)
                                        )
                                    is ConversationAvatar.OneOne -> UserProfileAvatar(
                                        UserAvatarData(conversationAvatar.avatarAsset, conversationAvatar.status)
                                    )
                                    ConversationAvatar.None -> Box(modifier = Modifier.size(dimensions().userAvatarDefaultSize))
                                }
                            },
                            onBackButtonClick = onBackButtonClick,
                            onDropDownClick = onDropDownClick,
                            onSearchButtonClick = { },
                            onPhoneButtonClick = onStartCall,
                            hasOngoingCall = hasOngoingCall,
                            onJoinCallButtonClick = onJoinCall
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
                                onSnackbarMessageShown = onSnackbarMessageShown,
                                conversationScreenState = conversationScreenState,
                                isFileSharingEnabled = isFileSharingEnabled,
                                tempCachePath = tempCachePath,
                                onOpenProfile = onOpenProfile
                            )
                        }
                    }
                )
            }
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun ConversationScreenContent(
    messages: List<UIMessage>,
    onMessageChanged: (String) -> Unit,
    messageText: String,
    onSendButtonClicked: () -> Unit,
    onShowContextMenu: (UIMessage) -> Unit,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onDownloadAsset: (String) -> Unit,
    onImageFullScreenMode: (String, Boolean) -> Unit,
    onOpenProfile: (MessageSource, UserId) -> Unit,
    onMessageComposerError: (ConversationSnackbarMessages) -> Unit,
    conversationState: ConversationViewState,
    onSnackbarMessageShown: () -> Unit,
    conversationScreenState: ConversationScreenState,
    isFileSharingEnabled: Boolean,
    tempCachePath: Path
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
                    onSnackbarMessageShown()
                }
                snackbarResult == SnackbarResult.Dismissed -> onSnackbarMessageShown()
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
                onImageFullScreenMode = onImageFullScreenMode,
                onOpenProfile = onOpenProfile
            )
        },
        messageText = messageText,
        onMessageChanged = onMessageChanged,
        onSendButtonClicked = onSendButtonClicked,
        onSendAttachment = onSendAttachment,
        onMessageComposerError = onMessageComposerError,
        onMessageComposerInputStateChange = { messageComposerState ->
            if (messageComposerState.to == MessageComposeInputState.Active &&
                messageComposerState.from == MessageComposeInputState.Enabled
            ) {
                coroutineScope.launch { lazyListState.animateScrollToItem(messages.size) }
            }
        },
        isFileSharingEnabled = isFileSharingEnabled,
        tempCachePath = tempCachePath
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
        ErrorDeletingMessage -> stringResource(R.string.error_conversation_deleting_message)
        ErrorPickingAttachment -> stringResource(R.string.error_conversation_generic)
    }
    val actionLabel = when (messageCode) {
        is OnFileDownloaded -> stringResource(R.string.label_show)
        else -> null
    }
    return msg to actionLabel
}

@Composable
fun MessageList(
    messages: List<UIMessage>,
    lazyListState: LazyListState,
    onShowContextMenu: (UIMessage) -> Unit,
    onDownloadAsset: (String) -> Unit,
    onImageFullScreenMode: (String, Boolean) -> Unit,
    onOpenProfile: (MessageSource, UserId) -> Unit
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
            if (message.messageContent is MessageContent.SystemMessage) {
                SystemMessageItem(message = message.messageContent)
            } else {
                MessageItem(
                    message = message,
                    onLongClicked = onShowContextMenu,
                    onAssetMessageClicked = onDownloadAsset,
                    onImageMessageClicked = onImageFullScreenMode,
                    onAvatarClicked = onOpenProfile
                )
            }
        }
    }
}

@Preview
@Composable
fun ConversationScreenPreview() {
    ConversationScreen(
        conversationViewState = ConversationViewState(
            conversationName = UIText.DynamicString("Some test conversation"),
            messages = getMockedMessages(),
        ),
        onMessageChanged = {},
        onSendButtonClicked = {},
        onSendAttachment = {},
        onDownloadAsset = {},
        onImageFullScreenMode = { _, _ -> },
        onBackButtonClick = {},
        onDeleteMessage = { _, _ -> },
        onStartCall = {},
        onJoinCall = {},
        onSnackbarMessage = {},
        onSnackbarMessageShown = {},
        onDropDownClick = {},
        tempCachePath =  "".toPath(),
        onOpenProfile = {_, _ -> }
    )
}
