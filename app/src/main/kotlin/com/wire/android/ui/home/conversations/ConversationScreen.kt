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

import SwipeableSnackbar
import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
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
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.ui.calling.common.MicrophoneBTPermissionsDeniedDialog
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dialogs.InvalidLinkDialog
import com.wire.android.ui.common.dialogs.VisitLinkDialog
import com.wire.android.ui.common.dialogs.calling.CallingFeatureUnavailableDialog
import com.wire.android.ui.common.dialogs.calling.ConfirmStartCallDialog
import com.wire.android.ui.common.dialogs.calling.JoinAnywayDialog
import com.wire.android.ui.common.dialogs.calling.OngoingActiveCallDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.error.CoreFailureErrorDialog
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.GroupConversationDetailsScreenDestination
import com.wire.android.ui.destinations.InitiatingCallScreenDestination
import com.wire.android.ui.destinations.MediaGalleryScreenDestination
import com.wire.android.ui.destinations.MessageDetailsScreenDestination
import com.wire.android.ui.destinations.OngoingCallScreenDestination
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import com.wire.android.ui.destinations.SelfUserProfileScreenDestination
import com.wire.android.ui.home.conversations.AuthorHeaderHelper.rememberShouldHaveSmallBottomPadding
import com.wire.android.ui.home.conversations.AuthorHeaderHelper.rememberShouldShowHeader
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
import com.wire.android.ui.home.conversations.migration.ConversationMigrationViewModel
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
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.extension.openAppInfoScreen
import com.wire.android.util.normalizeLink
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.openDownloadFolder
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.Conversation.TypingIndicatorMode
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.ConferenceCallingResult
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
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

/**
 * The maximum number of participants to start a call without showing a confirmation dialog.
 */
private const val MAX_GROUP_SIZE_FOR_CALL_WITHOUT_ALERT = 5

// TODO: !! this screen definitely needs a refactor and some cleanup !!
@Suppress("ComplexMethod")
@RootNavGraph
@Destination(
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
    conversationMigrationViewModel: ConversationMigrationViewModel = hiltViewModel(),
    groupDetailsScreenResultRecipient: ResultRecipient<GroupConversationDetailsScreenDestination, GroupConversationDetailsNavBackArgs>,
    mediaGalleryScreenResultRecipient: ResultRecipient<MediaGalleryScreenDestination, MediaGalleryNavBackArgs>,
    resultNavigator: ResultBackNavigator<GroupConversationDetailsNavBackArgs>,
) {
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val showDialog = remember { mutableStateOf(ConversationScreenDialogType.NONE) }
    val focusManager = LocalFocusManager.current
    val conversationScreenState = rememberConversationScreenState()
    val messageComposerViewState = messageComposerViewModel.messageComposerViewState
    val messageComposerStateHolder = rememberMessageComposerStateHolder(
        messageComposerViewState = messageComposerViewState,
        modalBottomSheetState = conversationScreenState.modalBottomSheetState
    )

    // this is to prevent from double navigating back after user deletes a group on group details screen
    // then ViewModel also detects it's removed and calls onNotFound which can execute navigateBack again and close the app
    var alreadyDeletedByUser by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(alreadyDeletedByUser) {
        if (!alreadyDeletedByUser) {
            conversationInfoViewModel.observeConversationDetails(navigator::navigateBack)
        }
    }
    val context = LocalContext.current

    conversationMigrationViewModel.migratedConversationId?.let { migratedConversationId ->
        navigator.navigate(
            NavigationCommand(
                ConversationScreenDestination(migratedConversationId),
                BackStackMode.REMOVE_CURRENT
            )
        )
    }

    with(conversationCallViewModel) {
        if (conversationCallViewState.shouldShowJoinAnywayDialog) {
            appLogger.i("showing showJoinAnywayDialog..")
            JoinAnywayDialog(
                onDismiss = ::dismissJoinCallAnywayDialog,
                onConfirm = { joinAnyway { navigator.navigate(NavigationCommand(OngoingCallScreenDestination(it))) } }
            )
        }

        MicrophoneBTPermissionsDeniedDialog(
            shouldShow = conversationCallViewState.shouldShowCallingPermissionDialog,
            onDismiss = ::dismissCallingPermissionDialog,
            onOpenSettings = {
                context.openAppInfoScreen()
            }
        )
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

        ConversationScreenDialogType.CALL_CONFIRMATION -> {
            ConfirmStartCallDialog(
                participantsCount = conversationCallViewModel.conversationCallViewState.participantsCount - 1,
                onConfirm = {
                    startCallIfPossible(
                        conversationCallViewModel,
                        showDialog,
                        coroutineScope,
                        conversationInfoViewModel.conversationInfoViewState.conversationType,
                        onOpenInitiatingCallScreen = {
                            navigator.navigate(NavigationCommand(InitiatingCallScreenDestination(it)))
                        }
                    ) { navigator.navigate(NavigationCommand(OngoingCallScreenDestination(it))) }
                },
                onDialogDismiss = {
                    showDialog.value = ConversationScreenDialogType.NONE
                }
            )
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
                coroutineScope,
                conversationInfoViewModel.conversationInfoViewState.conversationType,
                onOpenInitiatingCallScreen = {
                    navigator.navigate(NavigationCommand(InitiatingCallScreenDestination(it)))
                }
            ) { navigator.navigate(NavigationCommand(OngoingCallScreenDestination(it))) }
        },
        onJoinCall = {
            conversationCallViewModel.joinOngoingCall { navigator.navigate(NavigationCommand(OngoingCallScreenDestination(it))) }
        },
        onPermanentPermissionDecline = conversationCallViewModel::showCallingPermissionDialog,
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
        onBackButtonClick = { conversationScreenOnBackButtonClick(messageComposerViewModel, focusManager, navigator) },
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
        onTypingEvent = messageComposerViewModel::sendTypingEvent
    )
    BackHandler { conversationScreenOnBackButtonClick(messageComposerViewModel, focusManager, navigator) }
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

private fun conversationScreenOnBackButtonClick(
    messageComposerViewModel: MessageComposerViewModel,
    focusManager: FocusManager,
    navigator: Navigator
) {
    messageComposerViewModel.sendTypingEvent(TypingIndicatorMode.STOPPED)
    focusManager.clearFocus(true)
    navigator.navigateBack()
}

@Suppress("LongParameterList")
private fun startCallIfPossible(
    conversationCallViewModel: ConversationCallViewModel,
    showDialog: MutableState<ConversationScreenDialogType>,
    coroutineScope: CoroutineScope,
    conversationType: Conversation.Type,
    onOpenInitiatingCallScreen: (ConversationId) -> Unit,
    onOpenOngoingCallScreen: (ConversationId) -> Unit
) {
    coroutineScope.launch {
        if (!conversationCallViewModel.hasStableConnectivity()) {
            showDialog.value = ConversationScreenDialogType.NO_CONNECTIVITY
        } else {
            val dialogValue = when (conversationCallViewModel.isConferenceCallingEnabled(conversationType)) {
                ConferenceCallingResult.Enabled -> {
                    if (
                        showDialog.value != ConversationScreenDialogType.CALL_CONFIRMATION &&
                        conversationCallViewModel.conversationCallViewState.participantsCount > MAX_GROUP_SIZE_FOR_CALL_WITHOUT_ALERT
                    ) {
                        ConversationScreenDialogType.CALL_CONFIRMATION
                    } else {
                        conversationCallViewModel.endEstablishedCallIfAny {
                            onOpenInitiatingCallScreen(conversationCallViewModel.conversationId)
                        }
                        ConversationScreenDialogType.NONE
                    }
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
    onPermanentPermissionDecline: () -> Unit,
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
    onTypingEvent: (TypingIndicatorMode) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current

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
    // only here we will use normal Scaffold because of specific behaviour of message composer
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
                    onPermanentPermissionDecline = onPermanentPermissionDecline,
                    isInteractionEnabled = messageComposerViewState.value.interactionAvailability == InteractionAvailability.ENABLED
                )
                ConversationBanner(bannerMessage)
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    SwipeableSnackbar(
                        hostState = snackbarHostState,
                        data = data,
                        onDismiss = { data.dismiss() }
                    )
                }
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
                    onLinkClick = onLinkClick,
                    onTypingEvent = onTypingEvent
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
    SnackBarMessage(composerMessages, conversationMessages)
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
    onLinkClick: (String) -> Unit,
    onTypingEvent: (TypingIndicatorMode) -> Unit
) {
    val lazyPagingMessages = messages.collectAsLazyPagingItems()

    val lazyListState = rememberSaveable(unreadEventCount, lazyPagingMessages, saver = LazyListState.Saver) {
        LazyListState(unreadEventCount)
    }

    MessageComposer(
        conversationId = conversationId,
        messageComposerStateHolder = messageComposerStateHolder,
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
        tempWritableImageUri = tempWritableImageUri,
        onTypingEvent = onTypingEvent
    )
}

@Composable
private fun SnackBarMessage(
    composerMessages: SharedFlow<SnackBarMessage>,
    conversationMessages: SharedFlow<SnackBarMessage>
) {
    val showLabel = stringResource(R.string.label_show)
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(Unit) {
        composerMessages.collect {
            snackbarHostState.showSnackbar(
                message = it.uiText.asString(context.resources)
            )
        }
    }

    LaunchedEffect(Unit) {
        conversationMessages.collect {
            val actionLabel = if (it is OnFileDownloaded) showLabel else null
            val snackbarResult = snackbarHostState.showSnackbar(
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

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
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
    val mostRecentMessage by remember { derivedStateOf { lazyPagingMessages.itemCount.takeIf { it > 0 }?.let { lazyPagingMessages[0] } } }

    LaunchedEffect(mostRecentMessage) {
        // Most recent message changed, if the user didn't scroll up, we automatically scroll down to reveal the new message
        if (lazyListState.firstVisibleItemIndex < MAXIMUM_SCROLLED_MESSAGES_UNTIL_AUTOSCROLL_STOPS) {
            lazyListState.animateScrollToItem(0)
        }
    }

    val isScrollInProgress by remember { derivedStateOf { lazyListState.isScrollInProgress } }
    val currentLazyPagingItems by rememberUpdatedState(lazyPagingMessages)

    LaunchedEffect(isScrollInProgress) {
        if (!isScrollInProgress && currentLazyPagingItems.itemCount > 0) {
            val lastVisibleMessage = currentLazyPagingItems[lazyListState.firstVisibleItemIndex] ?: return@LaunchedEffect

            val lastVisibleMessageInstant = Instant.parse(lastVisibleMessage.header.messageTime.utcISO)

            // TODO: This IF condition should be in the UseCase
            //       If there are no unread messages, then use distant future and don't update read date
            if (lastVisibleMessageInstant > (lastUnreadMessageInstant ?: Instant.DISTANT_FUTURE)) {
                onUpdateConversationReadDate(lastVisibleMessage.header.messageTime.utcISO)
            }
        }
    }

    Box(
        contentAlignment = Alignment.BottomEnd,
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorsScheme().backgroundVariant),
        content = {
            LazyColumn(
                state = lazyListState,
                reverseLayout = true,
                // calculating bottom padding to have space for [UsersTypingIndicator]
                contentPadding = PaddingValues(
                    bottom = dimensions().typingIndicatorHeight - dimensions().messageItemBottomPadding
                ),
                modifier = Modifier
                    .fillMaxSize()
            ) {
                items(
                    count = lazyPagingMessages.itemCount,
                    key = lazyPagingMessages.itemKey { it.header.messageId },
                    contentType = lazyPagingMessages.itemContentType { it }
                ) { index ->
                    val message: UIMessage = lazyPagingMessages[index]
                        ?: return@items // We can draw a placeholder here, as we fetch the next page of messages

                    val showAuthor = rememberShouldShowHeader(index, message, lazyPagingMessages)
                    val useSmallBottomPadding = rememberShouldHaveSmallBottomPadding(index, message, lazyPagingMessages)

                    when (message) {
                        is UIMessage.Regular -> {
                            MessageItem(
                                message = message,
                                conversationDetailsData = conversationDetailsData,
                                showAuthor = showAuthor,
                                useSmallBottomPadding = useSmallBottomPadding,
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
            JumpToLastMessageButton(lazyListState = lazyListState)
        })
}

@Composable
fun JumpToLastMessageButton(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    lazyListState: LazyListState
) {
    AnimatedVisibility(
        visible = lazyListState.firstVisibleItemIndex > 0,
        enter = expandIn { it },
        exit = shrinkOut { it }
    ) {
        SmallFloatingActionButton(
            onClick = { coroutineScope.launch { lazyListState.animateScrollToItem(0) } },
            containerColor = MaterialTheme.wireColorScheme.scrollToBottomButtonColor,
            contentColor = MaterialTheme.wireColorScheme.onScrollToBottomButtonColor,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(dimensions().spacing0x),
            modifier = Modifier
                .padding(
                    PaddingValues(
                        bottom = dimensions().typingIndicatorHeight + dimensions().spacing8x,
                        end = dimensions().spacing16x
                    )
                )
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(id = R.string.content_description_jump_to_last_message),
                Modifier.size(dimensions().spacing32x)
            )
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
        onPermanentPermissionDecline = { },
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
        onLinkClick = { _ -> },
        onTypingEvent = {}
    )
}
