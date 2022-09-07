package com.wire.android.ui.home.conversations

import android.app.DownloadManager
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.dialogs.OngoingActiveCallDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.error.CoreFailureErrorDialog
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topappbar.CommonTopAppBar
import com.wire.android.ui.common.topappbar.CommonTopAppBarBaseViewModel
import com.wire.android.ui.common.topappbar.CommonTopAppBarViewModel
import com.wire.android.ui.common.topappbar.ConnectivityUIState
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorDeletingMessage
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorDownloadingAsset
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorMaxAssetSize
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorMaxImageSize
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorOpeningAssetFile
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorPickingAttachment
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorSendingAsset
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorSendingImage
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.OnFileDownloaded
import com.wire.android.ui.home.conversations.call.ConversationCallViewModel
import com.wire.android.ui.home.conversations.call.ConversationCallViewState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialog
import com.wire.android.ui.home.conversations.info.ConversationInfoViewModel
import com.wire.android.ui.home.conversations.info.ConversationInfoViewState
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewModel
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewState
import com.wire.android.ui.home.conversations.mock.getMockedMessages
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.MessageContent
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.messagecomposer.KeyboardHeight
import com.wire.android.ui.home.messagecomposer.MessageComposeInputState
import com.wire.android.ui.home.messagecomposer.MessageComposer
import com.wire.android.util.permission.CallingAudioRequestFlow
import com.wire.android.util.permission.rememberCallingRecordAudioBluetoothRequestFlow
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import okio.Path
import okio.Path.Companion.toPath

@Composable
fun ConversationScreen(
    messageComposerViewModel: MessageComposerViewModel,
    conversationCallViewModel: ConversationCallViewModel,
    conversationInfoViewModel: ConversationInfoViewModel,
    conversationMessagesViewModel: ConversationMessagesViewModel,
    commonTopAppBarViewModel: CommonTopAppBarViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val showDialog = remember { mutableStateOf(ConversationScreenDialogType.NONE) }

    val startCallAudioPermissionCheck = StartCallAudioBluetoothPermissionCheckFlow {
        conversationCallViewModel.navigateToInitiatingCallScreen()
    }
    val uiState = messageComposerViewModel.conversationViewState

    LaunchedEffect(messageComposerViewModel.savedStateHandle) {
        messageComposerViewModel.checkPendingActions()
    }

    when (showDialog.value) {
        ConversationScreenDialogType.ONGOING_ACTIVE_CALL -> {
            OngoingActiveCallDialog(onJoinAnyways = {
                conversationCallViewModel.navigateToInitiatingCallScreen()
                showDialog.value = ConversationScreenDialogType.NONE
            }, onDialogDismiss = {
                showDialog.value = ConversationScreenDialogType.NONE
            })
        }

        ConversationScreenDialogType.NO_CONNECTIVITY -> {
            CoreFailureErrorDialog(coreFailure = NetworkFailure.NoNetworkConnection(null)) {
                showDialog.value = ConversationScreenDialogType.NONE
            }
        }

        ConversationScreenDialogType.NONE -> {}
    }

    ConversationScreen(
        conversationViewState = uiState,
        conversationCallViewState = conversationCallViewModel.conversationCallViewState,
        conversationInfoViewState = conversationInfoViewModel.conversationInfoViewState,
        conversationMessagesViewState = conversationMessagesViewModel.conversationViewState,
        connectivityUIState = commonTopAppBarViewModel.connectivityState,
        onOpenOngoingCallScreen = commonTopAppBarViewModel::openOngoingCallScreen,
        onSendMessage = messageComposerViewModel::sendMessage,
        onSendAttachment = messageComposerViewModel::sendAttachmentMessage,
        onDownloadAsset = conversationMessagesViewModel::downloadOrFetchAssetToInternalStorage,
        onImageFullScreenMode = messageComposerViewModel::navigateToGallery,
        onBackButtonClick = messageComposerViewModel::navigateBack,
        onDeleteMessage = messageComposerViewModel::showDeleteMessageDialog,
        onStartCall = { startCallIfPossible(conversationCallViewModel, showDialog, startCallAudioPermissionCheck, coroutineScope) },
        onJoinCall = conversationCallViewModel::joinOngoingCall,
        onSnackbarMessage = messageComposerViewModel::onSnackbarMessage,
        onSnackbarMessageShown = messageComposerViewModel::clearSnackbarMessage,
        onDropDownClick = conversationInfoViewModel::navigateToDetails,
        tempCachePath = messageComposerViewModel.provideTempCachePath(),
        onOpenProfile = conversationInfoViewModel::navigateToProfile,
        onUpdateConversationReadDate = messageComposerViewModel::updateConversationReadDate,
        isSendingMessagesAllowed = messageComposerViewModel.isSendingMessagesAllowed,
    )

    DeleteMessageDialog(
        state = messageComposerViewModel.deleteMessageDialogsState,
        actions = messageComposerViewModel.deleteMessageHelper
    )
    DownloadedAssetDialog(
        downloadedAssetDialogState = conversationMessagesViewModel.conversationViewState.downloadedAssetDialogState,
        onSaveFileToExternalStorage = conversationMessagesViewModel::onSaveFile,
        onOpenFileWithExternalApp = conversationMessagesViewModel::onOpenFileWithExternalApp,
        hideOnAssetDownloadedDialog = conversationMessagesViewModel::hideOnAssetDownloadedDialog
    )
}

private fun startCallIfPossible(
    conversationCallViewModel: ConversationCallViewModel,
    showDialog: MutableState<ConversationScreenDialogType>,
    startCallAudioPermissionCheck: CallingAudioRequestFlow,
    coroutineScope: CoroutineScope
) {
    coroutineScope.launch {
        if (!conversationCallViewModel.hasStableConnectivity()) {
            showDialog.value = ConversationScreenDialogType.NO_CONNECTIVITY
        } else {
            conversationCallViewModel.establishedCallConversationId?.let {
                showDialog.value = ConversationScreenDialogType.ONGOING_ACTIVE_CALL
            } ?: run {
                startCallAudioPermissionCheck.launch()
            }
        }
    }
}

@Composable
private fun StartCallAudioBluetoothPermissionCheckFlow(
    onStartCall: () -> Unit
) = rememberCallingRecordAudioBluetoothRequestFlow(onAudioBluetoothPermissionGranted = {
    onStartCall()
}) {
    //TODO display an error dialog
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Suppress("LongParameterList")
@Composable
private fun ConversationScreen(
    conversationViewState: ConversationViewState,
    conversationCallViewState: ConversationCallViewState,
    conversationInfoViewState: ConversationInfoViewState,
    conversationMessagesViewState: ConversationMessagesViewState,
    connectivityUIState: ConnectivityUIState,
    onOpenOngoingCallScreen: () -> Unit,
    onSendMessage: (String) -> Unit,
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
    onOpenProfile: (MessageSource, UserId) -> Unit,
    onUpdateConversationReadDate: (String) -> Unit,
    isSendingMessagesAllowed: Boolean,
    commonTopAppBarViewModel: CommonTopAppBarBaseViewModel
) = with(conversationViewState) {
    val conversationScreenState = rememberConversationScreenState()

    val connectionStateOrNull = (conversationInfoViewState.conversationDetailsData as? ConversationDetailsData.OneOne)?.connectionState

    ModalBottomSheetLayout(
        sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
        sheetContent = {
            Spacer(modifier = Modifier.height(8.dp))
            Divider(
                modifier = Modifier
                    .width(width = dimensions().modalBottomSheetDividerWidth)
                    .align(alignment = Alignment.CenterHorizontally),
                thickness = 4.dp
            )
        }
    ) {
        BoxWithConstraints {
            val currentScreenHeight: Dp = with(LocalDensity.current) { constraints.maxHeight.toDp() }
            val fullScreenHeight: Dp = remember { currentScreenHeight }

            // when ConversationScreen is composed for the first time we do not know the height
            // until users opens the keyboard
            var keyboardHeight: KeyboardHeight by remember {
                mutableStateOf(KeyboardHeight.NotKnown)
            }

            // if the currentScreenHeight is smaller than the initial fullScreenHeight,
            // and we don't know the keyboard height yet
            // calculated at the first composition of the ConversationScreen, then we know the keyboard size
            if (keyboardHeight is KeyboardHeight.NotKnown && currentScreenHeight < fullScreenHeight) {
                val difference = fullScreenHeight - currentScreenHeight
                if (difference > KeyboardHeight.DEFAULT_KEYBOARD_TOP_SCREEN_OFFSET)
                    keyboardHeight = KeyboardHeight.Known(difference)
            }

            Scaffold(
                topBar = {
                    Column {
                        CommonTopAppBar(
                            connectivityUIState = connectivityUIState,
                            onReturnToCallClick = onOpenOngoingCallScreen
                        )
                        ConversationScreenTopAppBar(
                            conversationInfoViewState = conversationInfoViewState,
                            onBackButtonClick = onBackButtonClick,
                            onDropDownClick = onDropDownClick,
                            isDropDownEnabled = conversationInfoViewState.conversationDetailsData !is ConversationDetailsData.None,
                            onSearchButtonClick = { },
                            onPhoneButtonClick = onStartCall,
                            hasOngoingCall = conversationCallViewState.hasOngoingCall,
                            onJoinCallButtonClick = onJoinCall,
                            isUserBlocked = connectionStateOrNull == ConnectionState.BLOCKED
                        )
                    }
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
                            keyboardHeight = keyboardHeight,
                            snackbarMessage = conversationViewState.snackbarMessage ?: conversationMessagesViewState.snackbarMessage,
                            messages = conversationMessagesViewState.messages,
                            lastUnreadMessage = conversationMessagesViewState.lastUnreadMessage,
                            onSendMessage = onSendMessage,
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
                            isUserBlocked = connectionStateOrNull == ConnectionState.BLOCKED,
                            isSendingMessagesAllowed = isSendingMessagesAllowed,
                            onOpenProfile = onOpenProfile,
                            onUpdateConversationReadDate = onUpdateConversationReadDate
                        )
                    }
                }
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun ConversationScreenContent(
    snackbarMessage: ConversationSnackbarMessages?,
    keyboardHeight: KeyboardHeight,
    messages: List<UIMessage>,
    lastUnreadMessage: UIMessage?,
    onSendMessage: (String) -> Unit,
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
    isUserBlocked: Boolean,
    isSendingMessagesAllowed: Boolean,
    tempCachePath: Path,
    onUpdateConversationReadDate: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    SnackBarMessage(snackbarMessage, conversationState, conversationScreenState, onSnackbarMessageShown)

    val lazyListState = rememberSaveable(lastUnreadMessage, saver = LazyListState.Saver) {
        LazyListState(
            if (lastUnreadMessage != null) messages.indexOf(lastUnreadMessage) else 0,
            0
        )
    }

    LaunchedEffect(messages) {
        lazyListState.animateScrollToItem(0)
    }

    MessageComposer(
        keyboardHeight = keyboardHeight,
        content = {
            MessageList(
                messages = messages,
                lastUnreadMessage,
                lazyListState = lazyListState,
                onShowContextMenu = onShowContextMenu,
                onDownloadAsset = onDownloadAsset,
                onImageFullScreenMode = onImageFullScreenMode,
                onOpenProfile = onOpenProfile,
                onUpdateConversationReadDate = onUpdateConversationReadDate
            )
        },
        onSendTextMessage = onSendMessage,
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
        tempCachePath = tempCachePath,
        isUserBlocked = isUserBlocked,
        isSendingMessagesAllowed = isSendingMessagesAllowed,
        securityClassificationType = conversationState.securityClassificationType
    )
}

@Composable
private fun SnackBarMessage(
    snackbarMessage: ConversationSnackbarMessages?,
    conversationState: ConversationViewState,
    conversationScreenState: ConversationScreenState,
    onSnackbarMessageShown: () -> Unit
): Unit? = snackbarMessage?.let { messageCode ->
    val (message, actionLabel) = getSnackbarMessage(messageCode)
    val context = LocalContext.current
    LaunchedEffect(conversationState.snackbarMessage) {
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
    lastUnreadMessage: UIMessage?,
    lazyListState: LazyListState,
    onShowContextMenu: (UIMessage) -> Unit,
    onDownloadAsset: (String) -> Unit,
    onImageFullScreenMode: (String, Boolean) -> Unit,
    onOpenProfile: (MessageSource, UserId) -> Unit,
    onUpdateConversationReadDate: (String) -> Unit
) {
    if (messages.isNotEmpty() && lastUnreadMessage != null) {
        LaunchedEffect(lazyListState.isScrollInProgress) {
            if (!lazyListState.isScrollInProgress) {
                val lastVisibleMessage = messages[lazyListState.firstVisibleItemIndex]

                val lastVisibleMessageInstant = Instant.parse(lastVisibleMessage.messageHeader.messageTime.utcISO)
                val lastUnreadMessageInstant = Instant.parse(lastUnreadMessage.messageHeader.messageTime.utcISO)

                if (lastVisibleMessageInstant >= lastUnreadMessageInstant) {
                    onUpdateConversationReadDate(lastVisibleMessage.messageHeader.messageTime.utcISO)
                }
            }
        }
    }

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

//@Preview
//@Composable
//fun ConversationScreenPreview() {
//    ConversationScreen(
//        conversationViewState = ConversationViewState(),
//        conversationCallViewState = ConversationCallViewState(),
//        conversationInfoViewState = ConversationInfoViewState(conversationName = UIText.DynamicString("Some test conversation")),
//        conversationMessagesViewState = ConversationMessagesViewState(messages = getMockedMessages()),
//        onSendMessage = {},
//        onSendAttachment = {},
//        onDownloadAsset = {},
//        onImageFullScreenMode = { _, _ -> },
//        onBackButtonClick = {},
//        onDeleteMessage = { _, _ -> },
//        onStartCall = {},
//        onJoinCall = {},
//        onSnackbarMessage = {},
//        onSnackbarMessageShown = {},
//        onDropDownClick = {},
//        tempCachePath = "".toPath(),
//        onOpenProfile = { _, _ -> },
//        onUpdateConversationReadDate = {},
//        isSendingMessagesAllowed = true,
//        commonTopAppBarViewModel = object : CommonTopAppBarBaseViewModel() {}
//    )
//}
