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

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemsIndexed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.result.NavResult.Canceled
import com.ramcosta.composedestinations.result.NavResult.Value
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.ramcosta.composedestinations.result.ResultRecipient
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.model.SnackBarMessage
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.dialogs.InvalidLinkDialog
import com.wire.android.ui.common.dialogs.VisitLinkDialog
import com.wire.android.ui.common.dialogs.calling.CallingFeatureUnavailableDialog
import com.wire.android.ui.common.dialogs.calling.JoinAnywayDialog
import com.wire.android.ui.common.dialogs.calling.OngoingActiveCallDialog
import com.wire.android.ui.common.error.CoreFailureErrorDialog
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.destinations.GroupConversationDetailsScreenDestination
import com.wire.android.ui.destinations.InitiatingCallScreenDestination
import com.wire.android.ui.destinations.MediaGalleryScreenDestination
import com.wire.android.ui.destinations.MessageDetailsScreenDestination
import com.wire.android.ui.destinations.OngoingCallScreenDestination
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import com.wire.android.ui.destinations.SelfUserProfileScreenDestination
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.OnFileDownloaded
import com.wire.android.ui.home.conversations.banner.ConversationBanner
import com.wire.android.ui.home.conversations.banner.ConversationBannerViewModel
import com.wire.android.ui.home.conversations.call.ConversationCallViewModel
import com.wire.android.ui.home.conversations.call.ConversationCallViewState
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialog
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsNavBackArgs
import com.wire.android.ui.home.conversations.edit.EditMessageMenuItems
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.info.ConversationInfoViewModel
import com.wire.android.ui.home.conversations.info.ConversationInfoViewState
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewModel
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewState
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMapper.toSelfDeletionDuration
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMenuItems
import com.wire.android.ui.home.gallery.MediaGalleryActionType
import com.wire.android.ui.home.gallery.MediaGalleryNavBackArgs
import com.wire.android.ui.home.messagecomposer.MessageComposer
import com.wire.android.ui.home.messagecomposer.state.MessageBundle
import com.wire.android.ui.home.messagecomposer.state.MessageComposerStateHolder
import com.wire.android.ui.home.messagecomposer.state.rememberMessageComposerStateHolder
import com.wire.android.util.normalizeLink
import com.wire.android.util.permission.CallingAudioRequestFlow
import com.wire.android.util.permission.rememberCallingRecordAudioBluetoothRequestFlow
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.openDownloadFolder
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.ConferenceCallingResult
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
import com.wire.kalium.util.DateTimeUtil
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
private const val AGGREGATION_TIME_WINDOW: Int = 30000

// TODO: !! this screen definitely needs a refactor and some cleanup !!
@Suppress("ComplexMethod")
@RootNavGraph
@Destination( // TODO: back nav args
    navArgsDelegate = ConversationNavArgs::class
)
@Composable
fun ConversationScreen(
    navigator: Navigator,
    conversationInfoViewModel: ConversationInfoViewModel = hiltViewModel(),
    conversationBannerViewModel: ConversationBannerViewModel = hiltViewModel(),
    conversationCallViewModel: ConversationCallViewModel = hiltViewModel(),
    conversationMessagesViewModel: ConversationMessagesViewModel = hiltViewModel(),
    messageComposerViewModel: MessageComposerViewModel = hiltViewModel(),
    groupDetailsScreenResultRecipient: ResultRecipient<GroupConversationDetailsScreenDestination, GroupConversationDetailsNavBackArgs>,
    mediaGalleryScreenResultRecipient: ResultRecipient<MediaGalleryScreenDestination, MediaGalleryNavBackArgs>,
    resultNavigator: ResultBackNavigator<GroupConversationDetailsNavBackArgs>,
) {
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val showDialog = remember { mutableStateOf(ConversationScreenDialogType.NONE) }
    val conversationScreenState = rememberConversationScreenState()
    val messageComposerViewState = messageComposerViewModel.messageComposerViewState
    val messageComposerStateHolder = rememberMessageComposerStateHolder(
        messageComposerViewState = messageComposerViewState,
        modalBottomSheetState = conversationScreenState.modalBottomSheetState
    )

    val startCallAudioPermissionCheck = StartCallAudioBluetoothPermissionCheckFlow {
        conversationCallViewModel.endEstablishedCallIfAny {
            navigator.navigate(NavigationCommand(InitiatingCallScreenDestination(conversationCallViewModel.conversationId)))
        }
    }
    // this is to prevent from double navigating back after user deletes a group on group details screen
    // then ViewModel also detects it's removed and calls onNotFound which can execute navigateBack again and close the app
    var alreadyDeletedByUser by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(alreadyDeletedByUser) {
        if (!alreadyDeletedByUser) {
            conversationInfoViewModel.observeConversationDetails(navigator::navigateBack)
        }
    }

    with(conversationCallViewModel) {
        if (conversationCallViewState.shouldShowJoinAnywayDialog) {
            appLogger.i("showing showJoinAnywayDialog..")
            JoinAnywayDialog(
                onDismiss = ::dismissJoinCallAnywayDialog,
                onConfirm = { joinAnyway { navigator.navigate(NavigationCommand(OngoingCallScreenDestination(it))) } }
            )
        }
    }

    when (showDialog.value) {
        ConversationScreenDialogType.ONGOING_ACTIVE_CALL -> {
            OngoingActiveCallDialog(onJoinAnyways = {
                conversationCallViewModel.endEstablishedCallIfAny {
                    navigator.navigate(NavigationCommand(InitiatingCallScreenDestination(conversationCallViewModel.conversationId)))
                }
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
        bannerMessage = conversationBannerViewModel.bannerState,
        messageComposerViewState = messageComposerViewState,
        conversationCallViewState = conversationCallViewModel.conversationCallViewState,
        conversationInfoViewState = conversationInfoViewModel.conversationInfoViewState,
        conversationMessagesViewState = conversationMessagesViewModel.conversationViewState,
        onOpenProfile = {
            with(conversationInfoViewModel) {
                val (mentionUserId: UserId, isSelfUser: Boolean) = mentionedUserData(it)
                if (isSelfUser) navigator.navigate(NavigationCommand(SelfUserProfileScreenDestination))
                else (conversationInfoViewState.conversationDetailsData as? ConversationDetailsData.Group)?.conversationId.let {
                    navigator.navigate(NavigationCommand(OtherUserProfileScreenDestination(mentionUserId, it)))
                }
            }
        },
        onMessageDetailsClick = { messageId: String, isSelfMessage: Boolean ->
            appLogger.i("[ConversationScreen][openMessageDetails] - isSelfMessage: $isSelfMessage")
            navigator.navigate(
                NavigationCommand(MessageDetailsScreenDestination(conversationInfoViewModel.conversationId, messageId, isSelfMessage))
            )
        },
        onSendMessage = messageComposerViewModel::sendMessage,
        onDeleteMessage = messageComposerViewModel::showDeleteMessageDialog,
        onAssetItemClicked = conversationMessagesViewModel::downloadOrFetchAssetAndShowDialog,
        onImageFullScreenMode = { message, isSelfMessage ->
            with(conversationMessagesViewModel) {
                navigator.navigate(
                    NavigationCommand(
                        MediaGalleryScreenDestination(
                            conversationId = conversationId,
                            messageId = message.header.messageId,
                            isSelfAsset = isSelfMessage,
                            isEphemeral = message.header.messageStatus.expirationStatus is ExpirationStatus.Expirable
                        )
                    )
                )
                updateImageOnFullscreenMode(message)
            }
        },
        onStartCall = {
            startCallIfPossible(
                conversationCallViewModel,
                showDialog,
                startCallAudioPermissionCheck,
                coroutineScope,
                conversationInfoViewModel.conversationInfoViewState.conversationType
            ) { navigator.navigate(NavigationCommand(OngoingCallScreenDestination(it))) }
        },
        onJoinCall = {
            conversationCallViewModel.joinOngoingCall { navigator.navigate(NavigationCommand(OngoingCallScreenDestination(it))) }
        },
        onReactionClick = { messageId, emoji ->
            conversationMessagesViewModel.toggleReaction(messageId, emoji)
        },
        onAudioClick = conversationMessagesViewModel::audioClick,
        onChangeAudioPosition = conversationMessagesViewModel::changeAudioPosition,
        onResetSessionClick = conversationMessagesViewModel::onResetSession,
        onUpdateConversationReadDate = messageComposerViewModel::updateConversationReadDate,
        onDropDownClick = {
            with(conversationInfoViewModel) {
                when (val data = conversationInfoViewState.conversationDetailsData) {
                    is ConversationDetailsData.OneOne ->
                        navigator.navigate(NavigationCommand(OtherUserProfileScreenDestination(data.otherUserId)))

                    is ConversationDetailsData.Group ->
                        navigator.navigate(NavigationCommand(GroupConversationDetailsScreenDestination(conversationId)))

                    ConversationDetailsData.None -> { /* do nothing */
                    }
                }
            }
        },
        onBackButtonClick = navigator::navigateBack,
        composerMessages = messageComposerViewModel.infoMessage,
        conversationMessages = conversationMessagesViewModel.infoMessage,
        conversationMessagesViewModel = conversationMessagesViewModel,
        onSelfDeletingMessageRead = messageComposerViewModel::startSelfDeletion,
        onNewSelfDeletingMessagesStatus = messageComposerViewModel::updateSelfDeletingMessages,
        tempWritableImageUri = messageComposerViewModel.tempWritableImageUri,
        tempWritableVideoUri = messageComposerViewModel.tempWritableVideoUri,
        onFailedMessageRetryClicked = messageComposerViewModel::retrySendingMessage,
        requestMentions = messageComposerViewModel::searchMembersToMention,
        onClearMentionSearchResult = messageComposerViewModel::clearMentionSearchResult,
        conversationScreenState = conversationScreenState,
        messageComposerStateHolder = messageComposerStateHolder,
        onLinkClick = { link ->
            with(messageComposerViewModel) {
                val normalizedLink = normalizeLink(link)
                visitLinkDialogState = VisitLinkDialogState.Visible(normalizedLink) {
                    try {
                        uriHandler.openUri(normalizedLink)
                        visitLinkDialogState = VisitLinkDialogState.Hidden
                    } catch (_: Exception) {
                        visitLinkDialogState = VisitLinkDialogState.Hidden
                        invalidLinkDialogState = InvalidLinkDialogState.Visible
                    }
                }
            }
        },
    )
    DeleteMessageDialog(
        state = messageComposerViewModel.deleteMessageDialogsState,
        actions = messageComposerViewModel.deleteMessageHelper
    )
    DownloadedAssetDialog(
        downloadedAssetDialogState = conversationMessagesViewModel.conversationViewState.downloadedAssetDialogState,
        onSaveFileToExternalStorage = conversationMessagesViewModel::downloadAssetExternally,
        onOpenFileWithExternalApp = conversationMessagesViewModel::downloadAndOpenAsset,
        hideOnAssetDownloadedDialog = conversationMessagesViewModel::hideOnAssetDownloadedDialog
    )
    AssetTooLargeDialog(
        dialogState = messageComposerViewModel.assetTooLargeDialogState,
        hideDialog = messageComposerViewModel::hideAssetTooLargeError
    )
    VisitLinkDialog(
        dialogState = messageComposerViewModel.visitLinkDialogState,
        hideDialog = messageComposerViewModel::hideVisitLinkDialog
    )

    InvalidLinkDialog(
        dialogState = messageComposerViewModel.invalidLinkDialogState,
        hideDialog = messageComposerViewModel::hideInvalidLinkError
    )

    groupDetailsScreenResultRecipient.onNavResult { result ->
        when (result) {
            is Canceled -> {
                appLogger.i("Error with receiving navigation back args from groupDetails in ConversationScreen")
            }

            is Value -> {
                resultNavigator.setResult(result.value)
                resultNavigator.navigateBack()
                alreadyDeletedByUser = true
            }
        }
    }

    mediaGalleryScreenResultRecipient.onNavResult { result ->
        when (result) {
            is Canceled -> {
                appLogger.i("Error with receiving navigation back args from mediaGallery in ConversationScreen")
            }

            is Value -> {
                when (result.value.mediaGalleryActionType) {
                    MediaGalleryActionType.REPLY -> {
                        conversationMessagesViewModel.getAndResetLastFullscreenMessage(result.value.messageId)?.let {
                            coroutineScope.launch {
                                withSmoothScreenLoad {
                                    messageComposerStateHolder.toReply(it)
                                }
                            }
                        }
                    }

                    MediaGalleryActionType.REACT -> {
                        result.value.emoji?.let { conversationMessagesViewModel.toggleReaction(result.value.messageId, it) }
                    }

                    MediaGalleryActionType.DETAIL -> {
                        conversationMessagesViewModel.getAndResetLastFullscreenMessage(result.value.messageId)?.let {
                            navigator.navigate(
                                NavigationCommand(
                                    MessageDetailsScreenDestination(
                                        conversationMessagesViewModel.conversationId,
                                        result.value.messageId,
                                        result.value.isSelfAsset
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Suppress("LongParameterList")
private fun startCallIfPossible(
    conversationCallViewModel: ConversationCallViewModel,
    showDialog: MutableState<ConversationScreenDialogType>,
    startCallAudioPermissionCheck: CallingAudioRequestFlow,
    coroutineScope: CoroutineScope,
    conversationType: Conversation.Type,
    onOpenOngoingCallScreen: (ConversationId) -> Unit
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
                    onOpenOngoingCallScreen(conversationCallViewModel.conversationId)
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
    // TODO display an error dialog
}

@Suppress("LongParameterList")
@Composable
private fun ConversationScreen(
    bannerMessage: UIText?,
    messageComposerViewState: MutableState<MessageComposerViewState>,
    conversationCallViewState: ConversationCallViewState,
    conversationInfoViewState: ConversationInfoViewState,
    conversationMessagesViewState: ConversationMessagesViewState,
    onOpenProfile: (String) -> Unit,
    onMessageDetailsClick: (messageId: String, isSelfMessage: Boolean) -> Unit,
    onSendMessage: (MessageBundle) -> Unit,
    onDeleteMessage: (String, Boolean) -> Unit,
    onAudioClick: (String) -> Unit,
    onChangeAudioPosition: (String, Int) -> Unit,
    onAssetItemClicked: (String) -> Unit,
    onImageFullScreenMode: (UIMessage.Regular, Boolean) -> Unit,
    onStartCall: () -> Unit,
    onJoinCall: () -> Unit,
    onReactionClick: (messageId: String, reactionEmoji: String) -> Unit,
    onResetSessionClick: (senderUserId: UserId, clientId: String?) -> Unit,
    onUpdateConversationReadDate: (String) -> Unit,
    onDropDownClick: () -> Unit,
    onBackButtonClick: () -> Unit,
    composerMessages: SharedFlow<SnackBarMessage>,
    conversationMessages: SharedFlow<SnackBarMessage>,
    conversationMessagesViewModel: ConversationMessagesViewModel,
    onSelfDeletingMessageRead: (UIMessage) -> Unit,
    onNewSelfDeletingMessagesStatus: (SelfDeletionTimer) -> Unit,
    tempWritableImageUri: Uri?,
    tempWritableVideoUri: Uri?,
    onFailedMessageRetryClicked: (String) -> Unit,
    requestMentions: (String) -> Unit,
    onClearMentionSearchResult: () -> Unit,
    conversationScreenState: ConversationScreenState,
    messageComposerStateHolder: MessageComposerStateHolder,
    onLinkClick: (String) -> Unit,
) {
    val context = LocalContext.current

    val menuModalHeader = if (conversationScreenState.bottomSheetMenuType is ConversationScreenState.BottomSheetMenuType.SelfDeletion) {
        MenuModalSheetHeader.Visible(
            title = stringResource(R.string.automatically_delete_message_after)
        )
    } else MenuModalSheetHeader.Gone

    val menuItems = when (val menuType = conversationScreenState.bottomSheetMenuType) {
        is ConversationScreenState.BottomSheetMenuType.Edit -> {
            EditMessageMenuItems(
                message = menuType.selectedMessage,
                hideEditMessageMenu = conversationScreenState::hideContextMenu,
                onCopyClick = conversationScreenState::copyMessage,
                onDeleteClick = onDeleteMessage,
                onReactionClick = onReactionClick,
                onDetailsClick = onMessageDetailsClick,
                onReplyClick = messageComposerStateHolder::toReply,
                onEditClick = { messageId, messageText, mentions -> messageComposerStateHolder.toEdit(messageId, messageText, mentions) },
                onShareAssetClick = {
                    menuType.selectedMessage.header.messageId.let {
                        conversationMessagesViewModel.shareAsset(context, it)
                        conversationScreenState.hideContextMenu()
                    }
                },
                onDownloadAssetClick = conversationMessagesViewModel::downloadOrFetchAssetAndShowDialog,
                onOpenAssetClick = conversationMessagesViewModel::downloadAndOpenAsset
            )
        }

        is ConversationScreenState.BottomSheetMenuType.SelfDeletion -> {
            SelfDeletionMenuItems(
                hideEditMessageMenu = conversationScreenState::hideContextMenu,
                currentlySelected = messageComposerViewState.value.selfDeletionTimer.duration.toSelfDeletionDuration(),
                onSelfDeletionDurationChanged = { newTimer ->
                    onNewSelfDeletingMessagesStatus(SelfDeletionTimer.Enabled(newTimer.value))
                }
            )
        }

        ConversationScreenState.BottomSheetMenuType.None -> emptyList()
    }
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
                    isInteractionEnabled = messageComposerViewState.value.interactionAvailability == InteractionAvailability.ENABLED
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
                    conversationId = conversationInfoViewState.conversationId,
                    audioMessagesState = conversationMessagesViewState.audioMessagesState,
                    lastUnreadMessageInstant = conversationMessagesViewState.firstUnreadInstant,
                    unreadEventCount = conversationMessagesViewState.firstuUnreadEventIndex,
                    conversationDetailsData = conversationInfoViewState.conversationDetailsData,
                    messageComposerStateHolder = messageComposerStateHolder,
                    messages = conversationMessagesViewState.messages,
                    onSendMessage = onSendMessage,
                    onAssetItemClicked = onAssetItemClicked,
                    onAudioItemClicked = onAudioClick,
                    onChangeAudioPosition = onChangeAudioPosition,
                    onImageFullScreenMode = onImageFullScreenMode,
                    onReactionClicked = onReactionClick,
                    onResetSessionClicked = onResetSessionClick,
                    onOpenProfile = onOpenProfile,
                    onUpdateConversationReadDate = onUpdateConversationReadDate,
                    onShowEditingOptions = conversationScreenState::showEditContextMenu,
                    onSelfDeletingMessageRead = onSelfDeletingMessageRead,
                    onFailedMessageCancelClicked = remember { { onDeleteMessage(it, false) } },
                    onFailedMessageRetryClicked = onFailedMessageRetryClicked,
                    onChangeSelfDeletionClicked = { conversationScreenState.showSelfDeletionContextMenu() },
                    onSearchMentionQueryChanged = requestMentions,
                    onClearMentionSearchResult = onClearMentionSearchResult,
                    tempWritableImageUri = tempWritableImageUri,
                    tempWritableVideoUri = tempWritableVideoUri,
                    snackBarHostState = conversationScreenState.snackBarHostState,
                    onLinkClick = onLinkClick
                )
            }
        }
    )
    MenuModalSheetLayout(
        header = menuModalHeader,
        sheetState = conversationScreenState.modalBottomSheetState,
        coroutineScope = conversationScreenState.coroutineScope,
        menuItems = menuItems
    )
    SnackBarMessage(composerMessages, conversationMessages, conversationScreenState)
}

@Suppress("LongParameterList")
@Composable
private fun ConversationScreenContent(
    conversationId: ConversationId,
    lastUnreadMessageInstant: Instant?,
    unreadEventCount: Int,
    audioMessagesState: Map<String, AudioState>,
    messageComposerStateHolder: MessageComposerStateHolder,
    messages: Flow<PagingData<UIMessage>>,
    onSendMessage: (MessageBundle) -> Unit,
    onAssetItemClicked: (String) -> Unit,
    onAudioItemClicked: (String) -> Unit,
    onChangeAudioPosition: (String, Int) -> Unit,
    onImageFullScreenMode: (UIMessage.Regular, Boolean) -> Unit,
    onReactionClicked: (String, String) -> Unit,
    onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit,
    onOpenProfile: (String) -> Unit,
    onUpdateConversationReadDate: (String) -> Unit,
    onShowEditingOptions: (UIMessage.Regular) -> Unit,
    onSelfDeletingMessageRead: (UIMessage) -> Unit,
    conversationDetailsData: ConversationDetailsData,
    onFailedMessageRetryClicked: (String) -> Unit,
    onFailedMessageCancelClicked: (String) -> Unit,
    onChangeSelfDeletionClicked: () -> Unit,
    onSearchMentionQueryChanged: (String) -> Unit,
    onClearMentionSearchResult: () -> Unit,
    tempWritableImageUri: Uri?,
    tempWritableVideoUri: Uri?,
    snackBarHostState: SnackbarHostState,
    onLinkClick: (String) -> Unit,
) {
    val lazyPagingMessages = messages.collectAsLazyPagingItems()

    val lazyListState = rememberSaveable(unreadEventCount, lazyPagingMessages, saver = LazyListState.Saver) {
        LazyListState(unreadEventCount)
    }

    MessageComposer(
        conversationId = conversationId,
        messageComposerStateHolder = messageComposerStateHolder,
        snackbarHostState = snackBarHostState,
        messageListContent = {
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
                onSelfDeletingMessageRead = onSelfDeletingMessageRead,
                onShowEditingOption = onShowEditingOptions,
                conversationDetailsData = conversationDetailsData,
                onFailedMessageCancelClicked = onFailedMessageCancelClicked,
                onFailedMessageRetryClicked = onFailedMessageRetryClicked,
                onLinkClick = onLinkClick
            )
        },
        onChangeSelfDeletionClicked = onChangeSelfDeletionClicked,
        onSearchMentionQueryChanged = onSearchMentionQueryChanged,
        onClearMentionSearchResult = onClearMentionSearchResult,
        onSendMessageBundle = onSendMessage,
        tempWritableVideoUri = tempWritableVideoUri,
        tempWritableImageUri = tempWritableImageUri

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
        composerMessages.collect {
            conversationScreenState.snackBarHostState.showSnackbar(
                message = it.uiText.asString(context.resources)
            )
        }
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
                openDownloadFolder(context)
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
    onShowEditingOption: (UIMessage.Regular) -> Unit,
    onSelfDeletingMessageRead: (UIMessage) -> Unit,
    conversationDetailsData: ConversationDetailsData,
    onFailedMessageRetryClicked: (String) -> Unit,
    onFailedMessageCancelClicked: (String) -> Unit,
    onLinkClick: (String) -> Unit
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
        itemsIndexed(lazyPagingMessages, key = { _, uiMessage ->
            uiMessage.header.messageId
        }) { index, message ->
            if (message == null) {
                // We can draw a placeholder here, as we fetch the next page of messages
                return@itemsIndexed
            }
            when (message) {
                is UIMessage.Regular -> {
                    MessageItem(
                        message = message,
                        conversationDetailsData = conversationDetailsData,
                        showAuthor = shouldShowHeader(index, lazyPagingMessages.itemSnapshotList.items, message),
                        audioMessagesState = audioMessagesState,
                        onAudioClick = onAudioItemClicked,
                        onChangeAudioPosition = onChangeAudioPosition,
                        onLongClicked = onShowEditingOption,
                        onAssetMessageClicked = onAssetItemClicked,
                        onImageMessageClicked = onImageFullScreenMode,
                        onOpenProfile = onOpenProfile,
                        onReactionClicked = onReactionClicked,
                        onResetSessionClicked = onResetSessionClicked,
                        onSelfDeletingMessageRead = onSelfDeletingMessageRead,
                        onFailedMessageCancelClicked = onFailedMessageCancelClicked,
                        onFailedMessageRetryClicked = onFailedMessageRetryClicked,
                        onLinkClick = onLinkClick
                    )
                }

                is UIMessage.System -> SystemMessageItem(
                    message = message,
                    onFailedMessageCancelClicked = onFailedMessageCancelClicked,
                    onFailedMessageRetryClicked = onFailedMessageRetryClicked,
                    onSelfDeletingMessageRead = onSelfDeletingMessageRead
                )
            }
        }
    }
}

private fun shouldShowHeader(index: Int, messages: List<UIMessage>, currentMessage: UIMessage): Boolean {
    var showHeader = true
    val nextIndex = index + 1
    if (nextIndex < messages.size) {
        val nextUiMessage = messages[nextIndex]
        if (currentMessage.header.userId == nextUiMessage.header.userId) {
            val difference = DateTimeUtil.calculateMillisDifference(
                nextUiMessage.header.messageTime.utcISO,
                currentMessage.header.messageTime.utcISO,
            )
            showHeader = difference > AGGREGATION_TIME_WINDOW
        }
    }
    return showHeader
}

private fun CoroutineScope.withSmoothScreenLoad(block: () -> Unit) = launch {
    val smoothAnimationDuration = 200.milliseconds
    delay(smoothAnimationDuration) // we wait a bit until the whole screen is loaded to show the animation properly
    block()
}

@Preview
@Composable
fun PreviewConversationScreen() {
    val messageComposerViewState = remember { mutableStateOf(MessageComposerViewState()) }
    val conversationScreenState = rememberConversationScreenState()
    val messageComposerStateHolder = rememberMessageComposerStateHolder(
        messageComposerViewState = messageComposerViewState,
        modalBottomSheetState = conversationScreenState.modalBottomSheetState
    )
    ConversationScreen(
        bannerMessage = null,
        messageComposerViewState = messageComposerViewState,
        conversationCallViewState = ConversationCallViewState(),
        conversationInfoViewState = ConversationInfoViewState(
            conversationId = ConversationId("value", "domain"),
            conversationName = UIText.DynamicString("Some test conversation")
        ),
        conversationMessagesViewState = ConversationMessagesViewState(),
        onOpenProfile = { },
        onMessageDetailsClick = { _, _ -> },
        onSendMessage = { },
        onDeleteMessage = { _, _ -> },
        onAssetItemClicked = { },
        onImageFullScreenMode = { _, _ -> },
        onStartCall = { },
        onJoinCall = { },
        onReactionClick = { _, _ -> },
        onChangeAudioPosition = { _, _ -> },
        onAudioClick = { },
        onResetSessionClick = { _, _ -> },
        onUpdateConversationReadDate = { },
        onDropDownClick = { },
        onBackButtonClick = {},
        composerMessages = MutableStateFlow(ConversationSnackbarMessages.ErrorDownloadingAsset),
        conversationMessages = MutableStateFlow(ConversationSnackbarMessages.ErrorDownloadingAsset),
        conversationMessagesViewModel = hiltViewModel(),
        onSelfDeletingMessageRead = {},
        onNewSelfDeletingMessagesStatus = {},
        tempWritableImageUri = null,
        tempWritableVideoUri = null,
        onFailedMessageRetryClicked = {},
        requestMentions = {},
        onClearMentionSearchResult = {},
        conversationScreenState = conversationScreenState,
        messageComposerStateHolder = messageComposerStateHolder,
        onLinkClick = { _ -> }
    )
}
