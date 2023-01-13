package com.wire.android.ui.home.conversations

import android.app.DownloadManager
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.wire.android.R
import com.wire.android.media.AudioMediaPlayingState
import com.wire.android.media.AudioState
import com.wire.android.model.Clickable
import com.wire.android.model.SnackBarMessage
import com.wire.android.navigation.hiltSavedStateViewModel
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.dialogs.CallingFeatureUnavailableDialog
import com.wire.android.ui.common.dialogs.OngoingActiveCallDialog
import com.wire.android.ui.common.error.CoreFailureErrorDialog
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topappbar.CommonTopAppBar
import com.wire.android.ui.common.topappbar.CommonTopAppBarViewModel
import com.wire.android.ui.common.topappbar.ConnectivityUIState
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorDownloadingAsset
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
import com.wire.android.ui.home.conversations.model.MessageGenericAsset
import com.wire.android.ui.home.conversations.model.MessageImage
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.messagetypes.audio.AudioMessage
import com.wire.android.ui.home.conversations.model.messagetypes.image.ImageMessageParams
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
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.ConferenceCallingResult
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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
    commonTopAppBarViewModel: CommonTopAppBarViewModel = hiltViewModel(),
    conversationInfoViewModel: ConversationInfoViewModel = hiltSavedStateViewModel(backNavArgs = backNavArgs),
    conversationBannerViewModel: ConversationBannerViewModel = hiltSavedStateViewModel(backNavArgs = backNavArgs),
    conversationCallViewModel: ConversationCallViewModel = hiltSavedStateViewModel(backNavArgs = backNavArgs),
    conversationMessagesViewModel: ConversationMessagesViewModel = hiltSavedStateViewModel(backNavArgs = backNavArgs),
    messageComposerViewModel: MessageComposerViewModel = hiltSavedStateViewModel(backNavArgs = backNavArgs)
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
        tempCachePath = messageComposerViewModel.provideTempCachePath(),
        bannerMessage = conversationBannerViewModel.bannerState,
        connectivityUIState = commonTopAppBarViewModel.connectivityState,
        interactionAvailability = messageComposerViewModel.interactionAvailability,
        membersToMention = messageComposerViewModel.mentionsToSelect,
        conversationViewState = uiState,
        conversationCallViewState = conversationCallViewModel.conversationCallViewState,
        conversationInfoViewState = conversationInfoViewModel.conversationInfoViewState,
        conversationMessagesViewState = conversationMessagesViewModel.conversationViewState,
        onOpenProfile = conversationInfoViewModel::navigateToProfile,
        onMessageDetailsClick = conversationMessagesViewModel::openMessageDetails,
        onSendMessage = messageComposerViewModel::sendMessage,
        onDeleteMessage = messageComposerViewModel::showDeleteMessageDialog,
        onSendAttachment = messageComposerViewModel::sendAttachmentMessage,
        onDownloadAsset = conversationMessagesViewModel::downloadOrFetchAssetToInternalStorage,
        onImageFullScreenMode = messageComposerViewModel::navigateToGallery,
        onOpenOngoingCallScreen = commonTopAppBarViewModel::openOngoingCallScreen,
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
        onReactionClick = conversationMessagesViewModel::toggleReaction,
        onResetSessionClick = conversationMessagesViewModel::onResetSession,
        onMentionMember = messageComposerViewModel::mentionMember,
        onUpdateConversationReadDate = messageComposerViewModel::updateConversationReadDate,
        onDropDownClick = conversationInfoViewModel::navigateToDetails,
        onSnackbarMessage = messageComposerViewModel::onSnackbarMessage,
        onBackButtonClick = messageComposerViewModel::navigateBack,
        onAudioClick = conversationMessagesViewModel::audioClick,
        composerMessages = messageComposerViewModel.infoMessage,
        conversationMessages = conversationMessagesViewModel.infoMessage
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class, ExperimentalLayoutApi::class)
@Suppress("LongParameterList")
@Composable
private fun ConversationScreen(
    tempCachePath: Path,
    bannerMessage: UIText?,
    connectivityUIState: ConnectivityUIState,
    interactionAvailability: InteractionAvailability,
    membersToMention: List<Contact>,
    conversationViewState: ConversationViewState,
    conversationCallViewState: ConversationCallViewState,
    conversationInfoViewState: ConversationInfoViewState,
    conversationMessagesViewState: ConversationMessagesViewState,
    onOpenProfile: (String) -> Unit,
    onMessageDetailsClick: (messageId: String, isSelfMessage: Boolean) -> Unit,
    onSendMessage: (String, List<UiMention>, String?) -> Unit,
    onDeleteMessage: (String, Boolean) -> Unit,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onDownloadAsset: (String) -> Unit,
    onImageFullScreenMode: (String, Boolean) -> Unit,
    onOpenOngoingCallScreen: () -> Unit,
    onStartCall: () -> Unit,
    onAudioClick: (String) -> Unit,
    onJoinCall: () -> Unit,
    onReactionClick: (messageId: String, reactionEmoji: String) -> Unit,
    onResetSessionClick: (senderUserId: UserId, clientId: String?) -> Unit,
    onMentionMember: (String?) -> Unit,
    onUpdateConversationReadDate: (String) -> Unit,
    onDropDownClick: () -> Unit,
    onSnackbarMessage: (ConversationSnackbarMessages) -> Unit,
    onBackButtonClick: () -> Unit,
    composerMessages: SharedFlow<SnackBarMessage>,
    conversationMessages: SharedFlow<SnackBarMessage>,
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
                    conversationScreenState.selectedMessage?.messageHeader!!.messageId,
                    conversationScreenState.isMyMessage,
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
            isAvailable = conversationScreenState.selectedMessage?.isAvailable ?: false,
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
                        interactionAvailability = interactionAvailability,
                        tempCachePath = tempCachePath,
                        membersToMention = membersToMention,
                        isFileSharingEnabled = conversationViewState.isFileSharingEnabled,
                        onAudioClick = onAudioClick,
                        lastUnreadMessageInstant = conversationMessagesViewState.firstUnreadInstant,
                        audioMessagesState = conversationMessagesViewState.audioMessagesState,
                        conversationState = conversationViewState,
                        messageComposerInnerState = messageComposerInnerState,
                        messages = conversationMessagesViewState.messages,
                        onSendMessage = onSendMessage,
                        onSendAttachment = onSendAttachment,
                        onMentionMember = onMentionMember,
                        onDownloadAsset = onDownloadAsset,
                        onImageFullScreenMode = onImageFullScreenMode,
                        onReactionClicked = onReactionClick,
                        onResetSessionClicked = onResetSessionClick, onOpenProfile = onOpenProfile,
                        onUpdateConversationReadDate = onUpdateConversationReadDate,
                        onMessageComposerError = onSnackbarMessage,
                        onShowContextMenu = conversationScreenState::showEditContextMenu
                    )
                }
            }
        )
    }
    SnackBarMessage(composerMessages, conversationMessages, conversationScreenState)
}

@Suppress("LongParameterList")
@Composable
private fun ConversationScreenContent(
    interactionAvailability: InteractionAvailability,
    tempCachePath: Path,
    membersToMention: List<Contact>,
    isFileSharingEnabled: Boolean,
    lastUnreadMessageInstant: Instant?,
    conversationState: ConversationViewState,
    audioMessagesState: Map<String, AudioState>,
    messageComposerInnerState: MessageComposerInnerState,
    messages: Flow<PagingData<UIMessage>>,
    onSendMessage: (String, List<UiMention>, String?) -> Unit,
    onSendAttachment: (AttachmentBundle?) -> Unit,
    onAudioClick: (String) -> Unit,
    onMentionMember: (String?) -> Unit,
    onDownloadAsset: (String) -> Unit,
    onImageFullScreenMode: (String, Boolean) -> Unit,
    onReactionClicked: (String, String) -> Unit,
    onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit,
    onOpenProfile: (String) -> Unit,
    onUpdateConversationReadDate: (String) -> Unit,
    onMessageComposerError: (ConversationSnackbarMessages) -> Unit,
    onShowContextMenu: (UIMessage) -> Unit,
) {
    val scope = rememberCoroutineScope()

    val lazyPagingMessages = messages.collectAsLazyPagingItems()

    val lazyListState = rememberSaveable(lazyPagingMessages, saver = LazyListState.Saver) {
        // TODO: Autoscroll to last unread message
        LazyListState(0)
    }

    MessageComposer(
        messageComposerState = messageComposerInnerState,
        messageContent = {
            MessageList(
                lazyPagingMessages = lazyPagingMessages,
                lazyListState = lazyListState,
                lastUnreadMessageInstant = lastUnreadMessageInstant,
                audioMessagesState = audioMessagesState,
                onUpdateConversationReadDate = onUpdateConversationReadDate,
                onDownloadAsset = onDownloadAsset,
                onImageFullScreenMode = onImageFullScreenMode,
                onAudioClick = onAudioClick,
                onOpenProfile = onOpenProfile,
                onReactionClicked = onReactionClicked,
                onResetSessionClicked = onResetSessionClicked,
                onShowContextMenu = onShowContextMenu
            )
        },
        onSendTextMessage = { message, mentions, messageId ->
            scope.launch {
                lazyListState.scrollToItem(0)
            }
            onSendMessage(message, mentions, messageId)
        },
        onSendAttachment = {
            scope.launch {
                lazyListState.scrollToItem(0)
            }
            onSendAttachment(it)
        },
        onMentionMember = onMentionMember,
        onMessageComposerError = onMessageComposerError,
        isFileSharingEnabled = isFileSharingEnabled,
        tempCachePath = tempCachePath,
        interactionAvailability = interactionAvailability,
        securityClassificationType = conversationState.securityClassificationType,
        membersToMention = membersToMention
    )
}

@Composable
private fun SnackBarMessage(
    composerMessages: SharedFlow<SnackBarMessage>,
    conversationMessages: SharedFlow<SnackBarMessage>,
    conversationScreenState: ConversationScreenState
) {
    val showLabel = stringResource(R.string.label_show)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        composerMessages.collect { conversationScreenState.snackBarHostState.showSnackbar(message = it.uiText.asString(context.resources)) }
    }

    LaunchedEffect(Unit) {
        conversationMessages.collect {
            val actionLabel = if (it is OnFileDownloaded) showLabel else null
            val snackbarResult = conversationScreenState.snackBarHostState.showSnackbar(
                message = it.uiText.asString(context.resources),
                actionLabel = actionLabel
            )
            // Show downloads folder when clicking on Snackbar cta button
            if (it is OnFileDownloaded && snackbarResult == SnackbarResult.ActionPerformed) {
                context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
            }
        }
    }
}

@Composable
fun MessageList(
    lazyPagingMessages: LazyPagingItems<UIMessage>,
    lazyListState: LazyListState,
    lastUnreadMessageInstant: Instant?,
    audioMessagesState: Map<String, AudioState>,
    onUpdateConversationReadDate: (String) -> Unit,
    onDownloadAsset: (String) -> Unit,
    onAudioClick: (String) -> Unit,
    onImageFullScreenMode: (String, Boolean) -> Unit,
    onOpenProfile: (String) -> Unit,
    onReactionClicked: (String, String) -> Unit,
    onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit,
    onShowContextMenu: (UIMessage) -> Unit
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
            if (lastVisibleMessageInstant > (lastUnreadMessageInstant ?: Instant.DISTANT_FUTURE)) {
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
                MessageItemTest(
                    message = message,
                    onOpenProfile = onOpenProfile,
                    onLongClicked = onShowContextMenu,
                    onReactionClicked = onReactionClicked,
                    onResetSessionClicked = onResetSessionClicked,
                    messageContent = {
                        when (val messageContent = message.messageContent) {
                            is UIMessageContent.ImageMessage -> {
                                val imageClickable = remember {
                                    Clickable(enabled = true, onClick = {
                                        onImageFullScreenMode(
                                            message.messageHeader.messageId,
                                            message.messageSource == MessageSource.Self
                                        )
                                    }, onLongClick = {
                                        onShowContextMenu(message)
                                    })
                                }

                                MessageImage(
                                    asset = messageContent.asset,
                                    imgParams = ImageMessageParams(messageContent.width, messageContent.height),
                                    uploadStatus = messageContent.uploadStatus,
                                    downloadStatus = messageContent.downloadStatus,
                                    onImageClick = imageClickable
                                )
                            }

                            is UIMessageContent.TextMessage -> {
                                MessageText(
                                    messageBody = messageContent.messageBody,
                                    onLongClick = { onShowContextMenu(message) },
                                    onOpenProfile = onOpenProfile
                                )
                            }

                            is UIMessageContent.AssetMessage -> {
                                val assetClickable = remember {
                                    Clickable(enabled = true, onClick = {
                                        onDownloadAsset(message.messageHeader.messageId)
                                    }, onLongClick = {
                                        onShowContextMenu(message)
                                    })
                                }

                                MessageGenericAsset(
                                    assetName = messageContent.assetName,
                                    assetExtension = messageContent.assetExtension,
                                    assetSizeInBytes = messageContent.assetSizeInBytes,
                                    assetUploadStatus = messageContent.uploadStatus,
                                    assetDownloadStatus = messageContent.downloadStatus,
                                    onAssetClick = assetClickable
                                )
                            }

                            is UIMessageContent.RestrictedAsset -> {
                                MessageRestrictedAsset(
                                    assetName = messageContent.assetName,
                                    assetSizeInBytes = messageContent.assetSizeInBytes,
                                    mimeType = messageContent.mimeType
                                )
                            }

                            is UIMessageContent.AudioAssetMessage -> {
                                AudioMessage(
                                    audioState = audioMessagesState[message.messageHeader.messageId]
                                        ?: AudioState(AudioMediaPlayingState.Paused, 0),
                                    onPlayAudioMessage = { onAudioClick(message.messageHeader.messageId) }
                                )
                            }

                            is UIMessageContent.SystemMessage.MemberAdded -> {}
                            is UIMessageContent.SystemMessage.MemberLeft -> {}
                            is UIMessageContent.SystemMessage.MemberRemoved -> {}
                            is UIMessageContent.SystemMessage.RenamedConversation -> {}
                            is UIMessageContent.SystemMessage.TeamMemberRemoved -> {}
                            is UIMessageContent.SystemMessage.CryptoSessionReset -> {}
                            is UIMessageContent.PreviewAssetMessage -> {}
                            is UIMessageContent.SystemMessage.MissedCall.YouCalled -> {}
                            is UIMessageContent.SystemMessage.MissedCall.OtherCalled -> {}
                            null -> {
                                throw NullPointerException("messageContent is null")
                            }
                        }
                    },
                )
            }
        }
    }
}

@Preview
@Composable
fun ConversationScreenPreview() {
    ConversationScreen(
        tempCachePath = "".toPath(),
        bannerMessage = null,
        connectivityUIState = ConnectivityUIState(info = ConnectivityUIState.Info.None),
        interactionAvailability = InteractionAvailability.ENABLED,
        membersToMention = listOf(),
        conversationViewState = ConversationViewState(),
        conversationCallViewState = ConversationCallViewState(),
        conversationInfoViewState = ConversationInfoViewState(conversationName = UIText.DynamicString("Some test conversation")),
        conversationMessagesViewState = ConversationMessagesViewState(),
        onOpenProfile = { _ -> },
        onMessageDetailsClick = { _, _ -> },
        onSendMessage = { _, _, _ -> },
        onDeleteMessage = { _, _ -> },
        onSendAttachment = { },
        onDownloadAsset = { },
        onImageFullScreenMode = { _, _ -> },
        onOpenOngoingCallScreen = { },
        onStartCall = { },
        onJoinCall = { },
        onReactionClick = { _, _ -> },
        onResetSessionClick = { _, _ -> },
        onMentionMember = { },
        onUpdateConversationReadDate = { },
        onDropDownClick = { },
        onSnackbarMessage = { _ -> },
        onBackButtonClick = {},
        onAudioClick = {},
        composerMessages = MutableStateFlow(ErrorDownloadingAsset),
        conversationMessages = MutableStateFlow(ErrorDownloadingAsset),
    )
}
