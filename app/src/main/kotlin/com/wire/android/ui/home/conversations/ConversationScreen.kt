/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.conversations

import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
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
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.model.SnackBarMessage
import com.wire.android.navigation.hiltSavedStateViewModel
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topappbar.CommonTopAppBarViewModel
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.ErrorDownloadingAsset
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.OnFileDownloaded
import com.wire.android.ui.home.conversations.banner.ConversationBanner
import com.wire.android.ui.home.conversations.banner.ConversationBannerViewModel
import com.wire.android.ui.home.conversations.call.ConversationCallViewModel
import com.wire.android.ui.home.conversations.call.ConversationCallViewState
import com.wire.android.ui.home.conversations.dialogs.ConversationScreenDialogType
import com.wire.android.ui.home.conversations.dialogs.ConversationScreenDialogs
import com.wire.android.ui.home.conversations.edit.EditMessageMenuItems
import com.wire.android.ui.home.conversations.info.ConversationInfoViewModel
import com.wire.android.ui.home.conversations.info.ConversationInfoViewState
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewModel
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewState
import com.wire.android.ui.home.conversations.model.EditMessageBundle
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.home.messagecomposer.MessageComposer
import com.wire.android.ui.home.messagecomposer.MessageComposerInnerState
import com.wire.android.ui.home.messagecomposer.UiMention
import com.wire.android.ui.home.messagecomposer.rememberMessageComposerInnerState
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.util.permission.CallingAudioRequestFlow
import com.wire.android.util.permission.rememberCallingRecordAudioBluetoothRequestFlow
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.ConferenceCallingResult
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.milliseconds

/**
 * The maximum number of messages the user can scroll while still
 * having autoscroll on new messages enabled.
 * Once the user scrolls further into older messages, we stop autoscroll.
 */
private const val MAXIMUM_SCROLLED_MESSAGES_UNTIL_AUTOSCROLL_STOPS = 5

// TODO: !! this screen definitely needs a refactor and some cleanup !!
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

    val startCallAudioPermissionCheck = StartCallAudioBluetoothPermissionCheckFlow {
        conversationCallViewModel.navigateToInitiatingCallScreen()
    }
    val uiState = messageComposerViewModel.messageComposerViewState

    LaunchedEffect(messageComposerViewModel.savedStateHandle) {
        messageComposerViewModel.checkPendingActions()
    }
    LaunchedEffect(Unit) {
        conversationInfoViewModel.observeConversationDetails()
    }

    val dialogStateType = remember { mutableStateOf(ConversationScreenDialogType.NONE) }
    fun updateDialogType(newDialogType: ConversationScreenDialogType) {
        dialogStateType.value = newDialogType
    }

    ConversationScreenDialogs(conversationCallViewModel, messageComposerViewModel, dialogStateType.value, conversationMessagesViewModel) {
        updateDialogType(it)
    }

    ConversationScreen(
        bannerMessage = conversationBannerViewModel.bannerState,
        messageComposerViewState = uiState,
        conversationCallViewState = conversationCallViewModel.conversationCallViewState,
        conversationInfoViewState = conversationInfoViewModel.conversationInfoViewState,
        conversationMessagesViewState = conversationMessagesViewModel.conversationViewState,
        onOpenProfile = conversationInfoViewModel::navigateToProfile,
        onMessageDetailsClick = conversationMessagesViewModel::openMessageDetails,
        onSendMessage = messageComposerViewModel::sendMessage,
        onSendEditMessage = messageComposerViewModel::sendEditMessage,
        onDeleteMessage = messageComposerViewModel::showDeleteMessageDialog,
        onAttachmentPicked = messageComposerViewModel::attachmentPicked,
        onAssetItemClicked = conversationMessagesViewModel::downloadOrFetchAssetAndShowDialog,
        onImageFullScreenMode = { message, isSelfMessage ->
            messageComposerViewModel.navigateToGallery(message.header.messageId, isSelfMessage)
            conversationMessagesViewModel.updateImageOnFullscreenMode(message)
        },
        onStartCall = {
            startCallIfPossible(
                conversationCallViewModel,
                startCallAudioPermissionCheck,
                coroutineScope,
                conversationInfoViewModel.conversationInfoViewState.conversationType,
                commonTopAppBarViewModel::openOngoingCallScreen
            ) {
                updateDialogType(it)
            }
        },
        onJoinCall = conversationCallViewModel::joinOngoingCall,
        onReactionClick = { messageId, emoji ->
            conversationMessagesViewModel.toggleReaction(messageId, emoji)
        },
        onAudioClick = conversationMessagesViewModel::audioClick,
        onChangeAudioPosition = conversationMessagesViewModel::changeAudioPosition,
        onResetSessionClick = conversationMessagesViewModel::onResetSession,
        onMentionMember = messageComposerViewModel::mentionMember,
        onUpdateConversationReadDate = messageComposerViewModel::updateConversationReadDate,
        onDropDownClick = conversationInfoViewModel::navigateToDetails,
        onBackButtonClick = messageComposerViewModel::navigateBack,
        composerMessages = messageComposerViewModel.infoMessage,
        conversationMessages = conversationMessagesViewModel.infoMessage,
        conversationMessagesViewModel = conversationMessagesViewModel,
        onPingClicked = messageComposerViewModel::sendPing,
        onSelfDeletingMessageRead = messageComposerViewModel::startSelfDeletion,
        tempWritableImageUri = messageComposerViewModel.tempWritableImageUri,
        tempWritableVideoUri = messageComposerViewModel.tempWritableVideoUri
    )
}

@Suppress("LongParameterList")
private fun startCallIfPossible(
    conversationCallViewModel: ConversationCallViewModel,
    startCallAudioPermissionCheck: CallingAudioRequestFlow,
    coroutineScope: CoroutineScope,
    conversationType: Conversation.Type,
    onOpenOngoingCallScreen: () -> Unit,
    onDialogTypeUpdated: (ConversationScreenDialogType) -> Unit,
) {
    coroutineScope.launch {
        if (!conversationCallViewModel.hasStableConnectivity()) {
            onDialogTypeUpdated(ConversationScreenDialogType.NO_CONNECTIVITY)
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

            onDialogTypeUpdated(dialogValue)
        }
    }
}

@Composable
private fun StartCallAudioBluetoothPermissionCheckFlow(
    onStartCall: () -> Unit
) = rememberCallingRecordAudioBluetoothRequestFlow(onAudioBluetoothPermissionGranted = {
    onStartCall()
}) {
    // TODO display an error dialog
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class, ExperimentalLayoutApi::class)
@Suppress("LongParameterList")
@Composable
private fun ConversationScreen(
    bannerMessage: UIText?,
    messageComposerViewState: MessageComposerViewState,
    conversationCallViewState: ConversationCallViewState,
    conversationInfoViewState: ConversationInfoViewState,
    conversationMessagesViewState: ConversationMessagesViewState,
    onOpenProfile: (String) -> Unit,
    onMessageDetailsClick: (messageId: String, isSelfMessage: Boolean) -> Unit,
    onSendMessage: (String, List<UiMention>, String?) -> Unit,
    onSendEditMessage: (EditMessageBundle) -> Unit,
    onDeleteMessage: (String, Boolean) -> Unit,
    onAttachmentPicked: (UriAsset) -> Unit,
    onAudioClick: (String) -> Unit,
    onChangeAudioPosition: (String, Int) -> Unit,
    onAssetItemClicked: (String) -> Unit,
    onImageFullScreenMode: (UIMessage.Regular, Boolean) -> Unit,
    onStartCall: () -> Unit,
    onJoinCall: () -> Unit,
    onReactionClick: (messageId: String, reactionEmoji: String) -> Unit,
    onResetSessionClick: (senderUserId: UserId, clientId: String?) -> Unit,
    onMentionMember: (String?) -> Unit,
    onUpdateConversationReadDate: (String) -> Unit,
    onDropDownClick: () -> Unit,
    onBackButtonClick: () -> Unit,
    composerMessages: SharedFlow<SnackBarMessage>,
    conversationMessages: SharedFlow<SnackBarMessage>,
    conversationMessagesViewModel: ConversationMessagesViewModel,
    onPingClicked: () -> Unit,
    onSelfDeletingMessageRead: (UIMessage.Regular) -> Unit,
    tempWritableImageUri: Uri?,
    tempWritableVideoUri: Uri?
) {
    val conversationScreenState = rememberConversationScreenState()
    val messageComposerInnerState = rememberMessageComposerInnerState()
    val context = LocalContext.current

    LaunchedEffect(conversationMessagesViewModel.savedStateHandle) {
        // We need to check if we come from the media gallery screen and the user triggered any action there like reply
        conversationMessagesViewModel.checkPendingActions(
            onMessageReply = {
                withSmoothScreenLoad {
                    messageComposerInnerState.reply(it)
                }
            })
    }
    MenuModalSheetLayout(
        sheetState = conversationScreenState.modalBottomSheetState,
        coroutineScope = conversationScreenState.coroutineScope,
        menuItems = conversationScreenState.selectedMessage?.let { message ->
            EditMessageMenuItems(
                message = message,
                hideEditMessageMenu = conversationScreenState::hideEditContextMenu,
                onCopyClick = conversationScreenState::copyMessage,
                onDeleteClick = onDeleteMessage,
                onReactionClick = onReactionClick,
                onDetailsClick = onMessageDetailsClick,
                onReplyClick = messageComposerInnerState::reply,
                onEditClick = messageComposerInnerState::toEditMessage,
                onShareAsset = {
                    conversationScreenState.selectedMessage?.header?.messageId?.let {
                        conversationMessagesViewModel.shareAsset(context, it)
                        conversationScreenState.hideEditContextMenu()
                    }
                },
                onDownloadAsset = conversationMessagesViewModel::downloadAssetExternally,
                onOpenAsset = conversationMessagesViewModel::downloadAndOpenAsset,
            )
        } ?: emptyList()
    ) {
        Scaffold(
            topBar = {
                Column {
                    ConversationScreenTopAppBar(
                        conversationInfoViewState = conversationInfoViewState,
                        onBackButtonClick = onBackButtonClick,
                        onDropDownClick = onDropDownClick,
                        isDropDownEnabled = conversationInfoViewState.hasUserPermissionToEdit,
                        onSearchButtonClick = { },
                        onPhoneButtonClick = onStartCall,
                        hasOngoingCall = conversationCallViewState.hasOngoingCall,
                        onJoinCallButtonClick = onJoinCall,
                        isInteractionEnabled = messageComposerViewState.interactionAvailability == InteractionAvailability.ENABLED
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
                        interactionAvailability = messageComposerViewState.interactionAvailability,
                        membersToMention = messageComposerViewState.mentionsToSelect,
                        audioMessagesState = conversationMessagesViewState.audioMessagesState,
                        isFileSharingEnabled = messageComposerViewState.isFileSharingEnabled,
                        lastUnreadMessageInstant = conversationMessagesViewState.firstUnreadInstant,
                        conversationState = messageComposerViewState,
                        messageComposerInnerState = messageComposerInnerState,
                        messages = conversationMessagesViewState.messages,
                        onSendMessage = onSendMessage,
                        onSendEditMessage = onSendEditMessage,
                        onAttachmentPicked = onAttachmentPicked,
                        onMentionMember = onMentionMember,
                        onAssetItemClicked = onAssetItemClicked,
                        onAudioItemClicked = onAudioClick,
                        onChangeAudioPosition = onChangeAudioPosition,
                        onImageFullScreenMode = onImageFullScreenMode,
                        onReactionClicked = onReactionClick,
                        onResetSessionClicked = onResetSessionClick,
                        onOpenProfile = onOpenProfile,
                        onUpdateConversationReadDate = onUpdateConversationReadDate,
                        onShowContextMenu = conversationScreenState::showEditContextMenu,
                        onPingClicked = onPingClicked,
                        onSelfDeletingMessageRead = onSelfDeletingMessageRead,
                        tempWritableImageUri = tempWritableImageUri,
                        tempWritableVideoUri = tempWritableVideoUri
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
    membersToMention: List<Contact>,
    isFileSharingEnabled: Boolean,
    lastUnreadMessageInstant: Instant?,
    conversationState: MessageComposerViewState,
    audioMessagesState: Map<String, AudioState>,
    messageComposerInnerState: MessageComposerInnerState,
    messages: Flow<PagingData<UIMessage>>,
    onSendMessage: (String, List<UiMention>, String?) -> Unit,
    onSendEditMessage: (EditMessageBundle) -> Unit,
    onAttachmentPicked: (UriAsset) -> Unit,
    onMentionMember: (String?) -> Unit,
    onAssetItemClicked: (String) -> Unit,
    onAudioItemClicked: (String) -> Unit,
    onChangeAudioPosition: (String, Int) -> Unit,
    onImageFullScreenMode: (UIMessage.Regular, Boolean) -> Unit,
    onReactionClicked: (String, String) -> Unit,
    onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit,
    onOpenProfile: (String) -> Unit,
    onUpdateConversationReadDate: (String) -> Unit,
    onShowContextMenu: (UIMessage.Regular) -> Unit,
    onPingClicked: () -> Unit,
    onSelfDeletingMessageRead: (UIMessage.Regular) -> Unit,
    tempWritableImageUri: Uri?,
    tempWritableVideoUri: Uri?
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
                onAssetItemClicked = onAssetItemClicked,
                onAudioItemClicked = onAudioItemClicked,
                onChangeAudioPosition = onChangeAudioPosition,
                onImageFullScreenMode = onImageFullScreenMode,
                onOpenProfile = onOpenProfile,
                onReactionClicked = onReactionClicked,
                onResetSessionClicked = onResetSessionClicked,
                onShowContextMenu = onShowContextMenu,
                onSelfDeletingMessageRead = onSelfDeletingMessageRead

            )
        },
        onSendTextMessage = { message, mentions, messageId ->
            scope.launch {
                lazyListState.scrollToItem(0)
            }
            onSendMessage(message, mentions, messageId)
        },
        onSendEditTextMessage = onSendEditMessage,
        onAttachmentPicked = remember {
            {
                scope.launch {
                    lazyListState.scrollToItem(0)
                }
                onAttachmentPicked(it)
            }
        },
        onMentionMember = onMentionMember,
        isFileSharingEnabled = isFileSharingEnabled,
        interactionAvailability = interactionAvailability,
        securityClassificationType = conversationState.securityClassificationType,
        membersToMention = membersToMention,
        onPingClicked = onPingClicked,
        tempWritableImageUri = tempWritableImageUri,
        tempWritableVideoUri = tempWritableVideoUri
    )

    // TODO: uncomment when we have the "scroll to bottom" button implemented
//    val currentEditMessageId: String? by remember(messageComposerInnerState.messageComposeInputState) {
//        derivedStateOf {
//            (messageComposerInnerState.messageComposeInputState as? MessageComposeInputState.Active)?.let {
//                (it.type as? MessageComposeInputType.EditMessage)?.messageId
//            }
//        }
//    }
//    LaunchedEffect(currentEditMessageId) {
//        // executes when the id of currently being edited message changes, if not currently editing then it's just null
//        if (currentEditMessageId != null) {
//            lazyPagingMessages.itemSnapshotList.items
//                .indexOfFirst { it.header.messageId == currentEditMessageId }
//                .let { if (it >= 0) lazyListState.animateScrollToItem(it) }
//        }
//    }
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
    onAssetItemClicked: (String) -> Unit,
    onImageFullScreenMode: (UIMessage.Regular, Boolean) -> Unit,
    onOpenProfile: (String) -> Unit,
    onAudioItemClicked: (String) -> Unit,
    onChangeAudioPosition: (String, Int) -> Unit,
    onReactionClicked: (String, String) -> Unit,
    onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit,
    onShowContextMenu: (UIMessage.Regular) -> Unit,
    onSelfDeletingMessageRead: (UIMessage.Regular) -> Unit
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

            val lastVisibleMessageInstant = Instant.parse(lastVisibleMessage.header.messageTime.utcISO)

            // TODO: This IF condition should be in the UseCase
            //       If there are no unread messages, then use distant future and don't update read date
            if (lastVisibleMessageInstant > (lastUnreadMessageInstant ?: Instant.DISTANT_FUTURE)) {
                onUpdateConversationReadDate(lastVisibleMessage.header.messageTime.utcISO)
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
            uiMessage.header.messageId
        }) { message ->
            if (message == null) {
                // We can draw a placeholder here, as we fetch the next page of messages
                return@items
            }
            when (message) {
                is UIMessage.Regular -> MessageItem(
                    message = message,
                    audioMessagesState = audioMessagesState,
                    onAudioClick = onAudioItemClicked,
                    onChangeAudioPosition = onChangeAudioPosition,
                    onLongClicked = onShowContextMenu,
                    onAssetMessageClicked = onAssetItemClicked,
                    onImageMessageClicked = onImageFullScreenMode,
                    onOpenProfile = onOpenProfile,
                    onReactionClicked = onReactionClicked,
                    onResetSessionClicked = onResetSessionClicked,
                    onSelfDeletingMessageRead = onSelfDeletingMessageRead,
                )

                is UIMessage.System -> SystemMessageItem(message = message)
            }
        }
    }
}

private fun CoroutineScope.withSmoothScreenLoad(block: () -> Unit) = launch {
    val smoothAnimationDuration = 200.milliseconds
    delay(smoothAnimationDuration) // we wait a bit until the whole screen is loaded to show the animation properly
    block()
}

@Preview
@Composable
fun PreviewConversationScreen() {
    ConversationScreen(
        bannerMessage = null,
        messageComposerViewState = MessageComposerViewState(),
        conversationCallViewState = ConversationCallViewState(),
        conversationInfoViewState = ConversationInfoViewState(conversationName = UIText.DynamicString("Some test conversation")),
        conversationMessagesViewState = ConversationMessagesViewState(),
        onOpenProfile = { _ -> },
        onMessageDetailsClick = { _, _ -> },
        onSendMessage = { _, _, _ -> },
        onSendEditMessage = { _ -> },
        onDeleteMessage = { _, _ -> },
        onAttachmentPicked = { },
        onAssetItemClicked = { },
        onImageFullScreenMode = { _, _ -> },
        onStartCall = { },
        onJoinCall = { },
        onReactionClick = { _, _ -> },
        onChangeAudioPosition = { _, _ -> },
        onAudioClick = { },
        onResetSessionClick = { _, _ -> },
        onMentionMember = { },
        onUpdateConversationReadDate = { },
        onDropDownClick = { },
        onBackButtonClick = {},
        composerMessages = MutableStateFlow(ErrorDownloadingAsset),
        conversationMessages = MutableStateFlow(ErrorDownloadingAsset),
        conversationMessagesViewModel = hiltViewModel(),
        onPingClicked = {},
        onSelfDeletingMessageRead = {},
        tempWritableImageUri = null,
        tempWritableVideoUri = null
    )
}
