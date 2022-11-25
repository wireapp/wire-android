package com.wire.android.ui.home.conversations

import android.app.DownloadManager
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.ExperimentalMaterialApi
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.wire.android.R
import com.wire.android.navigation.hiltSavedStateViewModel
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.dialogs.CallingFeatureUnavailableDialog
import com.wire.android.ui.common.dialogs.OngoingActiveCallDialog
import com.wire.android.ui.common.error.CoreFailureErrorDialog
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topappbar.CommonTopAppBar
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
import com.wire.android.ui.home.conversations.banner.ConversationBanner
import com.wire.android.ui.home.conversations.banner.ConversationBannerViewModel
import com.wire.android.ui.home.conversations.call.ConversationCallViewModel
import com.wire.android.ui.home.conversations.call.ConversationCallViewState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialog
import com.wire.android.ui.home.conversations.edit.EditMessageMenuItems
import com.wire.android.ui.home.conversations.info.ConversationInfoViewModel
import com.wire.android.ui.home.conversations.info.ConversationInfoViewState
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewModel
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewState
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.messagecomposer.KeyboardHeight
import com.wire.android.ui.home.messagecomposer.MessageComposer
import com.wire.android.ui.home.messagecomposer.MessageComposerInnerState
import com.wire.android.ui.home.messagecomposer.UiMention
import com.wire.android.ui.home.messagecomposer.rememberMessageComposerInnerState
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.util.debug.LocalFeatureVisibilityFlags
import com.wire.android.util.permission.CallingAudioRequestFlow
import com.wire.android.util.permission.rememberCallingRecordAudioBluetoothRequestFlow
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.feature.call.usecase.ConferenceCallingResult
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import okio.Path
import okio.Path.Companion.toPath

/**
 * The maximum number of messages the user can scroll while still
 * having autoscroll on new messages enabled.
 * Once the user scrolls further into older messages, we stop autoscroll.
 */
private const val MAXIMUM_SCROLLED_MESSAGES_UNTIL_AUTOSCROLL_STOPS = 5

@Composable
fun ConversationScreen(
    backNavArgs: ImmutableMap<String, Any>,
    messageComposerViewModel: MessageComposerViewModel = hiltSavedStateViewModel(backNavArgs = backNavArgs),
    conversationCallViewModel: ConversationCallViewModel = hiltSavedStateViewModel(backNavArgs = backNavArgs),
    conversationInfoViewModel: ConversationInfoViewModel = hiltSavedStateViewModel(backNavArgs = backNavArgs),
    conversationMessagesViewModel: ConversationMessagesViewModel = hiltSavedStateViewModel(backNavArgs = backNavArgs),
    conversationBannerViewModel: ConversationBannerViewModel = hiltSavedStateViewModel(backNavArgs = backNavArgs),
    commonTopAppBarViewModel: CommonTopAppBarViewModel = hiltViewModel(),
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

        ConversationScreenDialogType.CALLING_FEATURE_UNAVAILABLE -> {
            CallingFeatureUnavailableDialog(onDialogDismiss = {
                showDialog.value = ConversationScreenDialogType.NONE
            })
        }

        ConversationScreenDialogType.NONE -> {}
    }

    ConversationScreen(
        conversationViewState = uiState,
        conversationCallViewState = conversationCallViewModel.conversationCallViewState,
        conversationInfoViewState = conversationInfoViewModel.conversationInfoViewState,
        conversationMessagesViewState = conversationMessagesViewModel.conversationViewState,
        bannerMessage = conversationBannerViewModel.bannerState,
        connectivityUIState = commonTopAppBarViewModel.connectivityState,
        onOpenOngoingCallScreen = commonTopAppBarViewModel::openOngoingCallScreen,
        onSendMessage = messageComposerViewModel::sendMessage,
        onSendAttachment = messageComposerViewModel::sendAttachmentMessage,
        onMentionMember = messageComposerViewModel::mentionMember,
        onDownloadAsset = conversationMessagesViewModel::downloadOrFetchAssetToInternalStorage,
        onImageFullScreenMode = messageComposerViewModel::navigateToGallery,
        onBackButtonClick = messageComposerViewModel::navigateBack,
        onDeleteMessage = messageComposerViewModel::showDeleteMessageDialog,
        onReactionClick = conversationMessagesViewModel::toggleReaction,
        onMessageDetailsClick = conversationMessagesViewModel::openMessageDetails,
        onStartCall = {
            startCallIfPossible(
                conversationCallViewModel,
                showDialog,
                startCallAudioPermissionCheck,
                coroutineScope,
                conversationInfoViewModel.conversationInfoViewState.conversationType,
                commonTopAppBarViewModel::openOngoingCallScreen
            )
        },
        onJoinCall = conversationCallViewModel::joinOngoingCall,
        onSnackbarMessage = messageComposerViewModel::onSnackbarMessage,
        onSnackbarMessageShown = messageComposerViewModel::clearSnackbarMessage,
        onDropDownClick = conversationInfoViewModel::navigateToDetails,
        tempCachePath = messageComposerViewModel.provideTempCachePath(),
        onOpenProfile = conversationInfoViewModel::navigateToProfile,
        onUpdateConversationReadDate = messageComposerViewModel::updateConversationReadDate,
        interactionAvailability = messageComposerViewModel.interactionAvailability,
        membersToMention = messageComposerViewModel.mentionsToSelect,
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

@Suppress("LongParameterList")
private fun startCallIfPossible(
    conversationCallViewModel: ConversationCallViewModel,
    showDialog: MutableState<ConversationScreenDialogType>,
    startCallAudioPermissionCheck: CallingAudioRequestFlow,
    coroutineScope: CoroutineScope,
    conversationType: Conversation.Type,
    onOpenOngoingCallScreen: () -> Unit
) {
    coroutineScope.launch {
        if (!conversationCallViewModel.hasStableConnectivity()) {
            showDialog.value = ConversationScreenDialogType.NO_CONNECTIVITY
        } else {
            val dialogValue = when (conversationCallViewModel.isConferenceCallingEnabled(conversationType)) {
                ConferenceCallingResult.Enabled -> {
                    startCallAudioPermissionCheck.launch()
                    ConversationScreenDialogType.NONE
                }

                ConferenceCallingResult.Disabled.Established -> {
                    onOpenOngoingCallScreen()
                    ConversationScreenDialogType.NONE
                }

                ConferenceCallingResult.Disabled.OngoingCall -> ConversationScreenDialogType.ONGOING_ACTIVE_CALL
                ConferenceCallingResult.Disabled.Unavailable -> ConversationScreenDialogType.CALLING_FEATURE_UNAVAILABLE
                else -> ConversationScreenDialogType.NONE
            }

            showDialog.value = dialogValue
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
    bannerMessage: UIText?,
    connectivityUIState: ConnectivityUIState,
    onOpenOngoingCallScreen: () -> Unit,
    onSendMessage: (String, List<UiMention>, String?) -> Unit,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onMentionMember: (String?) -> Unit,
    onDownloadAsset: (String) -> Unit,
    onImageFullScreenMode: (String, Boolean) -> Unit,
    onBackButtonClick: () -> Unit,
    onDeleteMessage: (String, Boolean) -> Unit,
    onReactionClick: (messageId: String, reactionEmoji: String) -> Unit,
    onMessageDetailsClick: (messageId: String) -> Unit,
    onStartCall: () -> Unit,
    onJoinCall: () -> Unit,
    onSnackbarMessage: (ConversationSnackbarMessages) -> Unit,
    onSnackbarMessageShown: () -> Unit,
    onDropDownClick: () -> Unit,
    tempCachePath: Path,
    onOpenProfile: (String) -> Unit,
    onUpdateConversationReadDate: (String) -> Unit,
    interactionAvailability: InteractionAvailability,
    membersToMention: List<Contact>
) {
    val conversationScreenState = rememberConversationScreenState()

    val messageComposerInnerState = rememberMessageComposerInnerState()

    val menuModalOnDeleteMessage = remember {
        {
            conversationScreenState.hideEditContextMenu {
                onDeleteMessage(
                    conversationScreenState.selectedMessage?.messageHeader!!.messageId,
                    conversationScreenState.isMyMessage
                )
            }
        }
    }

    val menuModalOnReactionClick = remember {
        { emoji: String ->
            conversationScreenState.hideEditContextMenu {
                onReactionClick(
                    conversationScreenState.selectedMessage?.messageHeader!!.messageId,
                    emoji
                )
            }
        }
    }

    val menuModalOnMessageDetailsClick = remember {
        {
            conversationScreenState.hideEditContextMenu {
                onMessageDetailsClick(
                    conversationScreenState.selectedMessage?.messageHeader!!.messageId
                )
            }
        }
    }

    val localFeatureVisibilityFlags = LocalFeatureVisibilityFlags.current

    MenuModalSheetLayout(
        sheetState = conversationScreenState.modalBottomSheetState,
        coroutineScope = conversationScreenState.coroutineScope,
        menuItems = EditMessageMenuItems(
            isCopyable = conversationScreenState.isTextMessage,
            isEditable = conversationScreenState.isMyMessage && localFeatureVisibilityFlags.MessageEditIcon,
            onCopyMessage = conversationScreenState::copyMessage,
            onDeleteMessage = menuModalOnDeleteMessage,
            onReactionClick = menuModalOnReactionClick,
            onMessageDetailsClick = menuModalOnMessageDetailsClick,
            onReply = {
                conversationScreenState.selectedMessage?.let {
                    messageComposerInnerState.reply(it)
                    conversationScreenState.hideEditContextMenu()
                }
            }
        )
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
                            onReturnToCallClick = onOpenOngoingCallScreen,
                        )
                        ConversationScreenTopAppBar(
                            conversationInfoViewState = conversationInfoViewState,
                            onBackButtonClick = onBackButtonClick,
                            onDropDownClick = onDropDownClick,
                            isDropDownEnabled = conversationInfoViewState.hasUserPermissionToEdit,
                            onSearchButtonClick = { },
                            onPhoneButtonClick = onStartCall,
                            hasOngoingCall = conversationCallViewState.hasOngoingCall,
                            onJoinCallButtonClick = onJoinCall,
                            isInteractionEnabled = interactionAvailability == InteractionAvailability.ENABLED
                        )
                        ConversationBanner(bannerMessage)
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
                            messageComposerInnerState = messageComposerInnerState,
                            keyboardHeight = keyboardHeight,
                            snackbarMessage = conversationViewState.snackbarMessage ?: conversationMessagesViewState.snackbarMessage,
                            messages = conversationMessagesViewState.messages,
                            lastUnreadMessageInstant = conversationMessagesViewState.lastUnreadMessageInstant,
                            onSendMessage = onSendMessage,
                            onShowContextMenu = conversationScreenState::showEditContextMenu,
                            onSendAttachment = onSendAttachment,
                            onMentionMember = onMentionMember,
                            onDownloadAsset = onDownloadAsset,
                            onReactionClicked = onReactionClick,
                            onImageFullScreenMode = onImageFullScreenMode,
                            conversationState = conversationViewState,
                            onMessageComposerError = onSnackbarMessage,
                            onSnackbarMessageShown = onSnackbarMessageShown,
                            conversationScreenState = conversationScreenState,
                            isFileSharingEnabled = conversationViewState.isFileSharingEnabled,
                            tempCachePath = tempCachePath,
                            interactionAvailability = interactionAvailability,
                            onOpenProfile = onOpenProfile,
                            onUpdateConversationReadDate = onUpdateConversationReadDate,
                            membersToMention = membersToMention
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
    messageComposerInnerState: MessageComposerInnerState,
    conversationScreenState: ConversationScreenState,
    snackbarMessage: ConversationSnackbarMessages?,
    keyboardHeight: KeyboardHeight,
    messages: Flow<PagingData<UIMessage>>,
    lastUnreadMessageInstant: Instant?,
    onSendMessage: (String, List<UiMention>, String?) -> Unit,
    onShowContextMenu: (UIMessage) -> Unit,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onMentionMember: (String?) -> Unit,
    onDownloadAsset: (String) -> Unit,
    onReactionClicked: (String, String) -> Unit,
    onImageFullScreenMode: (String, Boolean) -> Unit,
    onOpenProfile: (String) -> Unit,
    onMessageComposerError: (ConversationSnackbarMessages) -> Unit,
    conversationState: ConversationViewState,
    onSnackbarMessageShown: () -> Unit,
    isFileSharingEnabled: Boolean,
    tempCachePath: Path,
    interactionAvailability: InteractionAvailability,
    onUpdateConversationReadDate: (String) -> Unit,
    membersToMention: List<Contact>
) {
    val lazyPagingMessages = messages.collectAsLazyPagingItems()

    val lazyListState = rememberSaveable(lazyPagingMessages, saver = LazyListState.Saver) {
        // TODO: Autoscroll to last unread message
        LazyListState(0)
    }

    MessageComposer(
        messageComposerState = messageComposerInnerState,
        keyboardHeight = keyboardHeight,
        content = {
            MessageList(
                lazyPagingMessages = lazyPagingMessages,
                lazyListState = lazyListState,
                lastUnreadMessageInstant = lastUnreadMessageInstant,
                onShowContextMenu = onShowContextMenu,
                onDownloadAsset = onDownloadAsset,
                onImageFullScreenMode = onImageFullScreenMode,
                onOpenProfile = onOpenProfile,
                onUpdateConversationReadDate = onUpdateConversationReadDate,
                onReactionClicked = onReactionClicked
            )
        },
        onSendTextMessage = onSendMessage,
        onSendAttachment = onSendAttachment,
        onMentionMember = onMentionMember,
        onMessageComposerError = onMessageComposerError,
        isFileSharingEnabled = isFileSharingEnabled,
        tempCachePath = tempCachePath,
        interactionAvailability = interactionAvailability,
        securityClassificationType = conversationState.securityClassificationType,
        membersToMention = membersToMention
    )

    SnackBarMessage(snackbarMessage, conversationState, conversationScreenState, onSnackbarMessageShown)
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
    lazyPagingMessages: LazyPagingItems<UIMessage>,
    lazyListState: LazyListState,
    lastUnreadMessageInstant: Instant?,
    onShowContextMenu: (UIMessage) -> Unit,
    onDownloadAsset: (String) -> Unit,
    onImageFullScreenMode: (String, Boolean) -> Unit,
    onOpenProfile: (String) -> Unit,
    onUpdateConversationReadDate: (String) -> Unit,
    onReactionClicked: (String, String) -> Unit
) {
    val mostRecentMessage = lazyPagingMessages.itemCount.takeIf { it > 0 }?.let { lazyPagingMessages[0] }
    LaunchedEffect(mostRecentMessage) {
        // Most recent message changed, if the user didn't scroll up, we automatically scroll down to reveal the new message
        if (lazyListState.firstVisibleItemIndex < MAXIMUM_SCROLLED_MESSAGES_UNTIL_AUTOSCROLL_STOPS) {
            lazyListState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress && lazyPagingMessages.itemCount > 0) {
            val lastVisibleMessage = lazyPagingMessages[lazyListState.firstVisibleItemIndex] ?: return@LaunchedEffect

            val lastVisibleMessageInstant = Instant.parse(lastVisibleMessage.messageHeader.messageTime.utcISO)

            // TODO: This IF condition should be in the UseCase
            //       If there are no unread messages, then use distant future and don't update read date
            if (lastVisibleMessageInstant >= (lastUnreadMessageInstant ?: Instant.DISTANT_FUTURE)) {
                onUpdateConversationReadDate(lastVisibleMessage.messageHeader.messageTime.utcISO)
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
        items(lazyPagingMessages, key = { uiMessage ->
            uiMessage.messageHeader.messageId
        }) { message ->
            if (message == null) {
                // We can draw a placeholder here, as we fetch the next page of messages
                return@items
            }
            if (message.messageContent is UIMessageContent.SystemMessage) {
                SystemMessageItem(message = message.messageContent)
            } else {
                MessageItem(
                    message = message,
                    onLongClicked = onShowContextMenu,
                    onAssetMessageClicked = onDownloadAsset,
                    onImageMessageClicked = onImageFullScreenMode,
                    onOpenProfile = onOpenProfile,
                    onReactionClicked = onReactionClicked
                )
            }
        }
    }
}

@Preview
@Composable
fun ConversationScreenPreview() {
    ConversationScreen(
        conversationViewState = ConversationViewState(),
        conversationCallViewState = ConversationCallViewState(),
        conversationInfoViewState = ConversationInfoViewState(conversationName = UIText.DynamicString("Some test conversation")),
        conversationMessagesViewState = ConversationMessagesViewState(),
        connectivityUIState = ConnectivityUIState(info = ConnectivityUIState.Info.None),
        bannerMessage = null,
        onOpenOngoingCallScreen = { },
        onSendMessage = { _, _, _ -> },
        onSendAttachment = { },
        onMentionMember = { },
        onDownloadAsset = { },
        onReactionClick = { _, _ -> },
        onImageFullScreenMode = { _, _ -> },
        onBackButtonClick = { },
        onDeleteMessage = { _, _ -> },
        onStartCall = { },
        onJoinCall = { },
        onSnackbarMessage = { },
        onSnackbarMessageShown = { },
        onDropDownClick = { },
        tempCachePath = "".toPath(),
        onOpenProfile = { _ -> },
        onUpdateConversationReadDate = { },
        interactionAvailability = InteractionAvailability.ENABLED,
        membersToMention = listOf(),
        onMessageDetailsClick = { }
    )
}
