/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

package com.wire.android.ui.home.conversations

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.result.NavResult.Canceled
import com.ramcosta.composedestinations.result.NavResult.Value
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.ramcosta.composedestinations.result.ResultRecipient
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.mapper.MessageDateTimeGroup
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.model.Clickable
import com.wire.android.model.SnackBarMessage
import com.wire.android.navigation.ArgsSerializer
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.ui.LocalActivity
import com.wire.android.ui.calling.ongoing.getOngoingCallIntent
import com.wire.android.ui.calling.getOutgoingCallIntent
import com.wire.android.navigation.WireDestination
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dialogs.ConfirmSendingPingDialog
import com.wire.android.ui.common.dialogs.InvalidLinkDialog
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.dialogs.SureAboutMessagingInDegradedConversationDialog
import com.wire.android.ui.common.dialogs.VisitLinkDialog
import com.wire.android.ui.common.dialogs.calling.CallingFeatureUnavailableDialog
import com.wire.android.ui.common.dialogs.calling.ConfirmStartCallDialog
import com.wire.android.ui.common.dialogs.calling.JoinAnywayDialog
import com.wire.android.ui.common.dialogs.calling.OngoingActiveCallDialog
import com.wire.android.ui.common.dialogs.calling.SureAboutCallingInDegradedConversationDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.error.CoreFailureErrorDialog
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.snackbar.SwipeableSnackbar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.GroupConversationDetailsScreenDestination
import com.wire.android.ui.destinations.ImagesPreviewScreenDestination
import com.wire.android.ui.destinations.MediaGalleryScreenDestination
import com.wire.android.ui.destinations.MessageDetailsScreenDestination
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import com.wire.android.ui.destinations.SelfUserProfileScreenDestination
import com.wire.android.ui.home.conversations.AuthorHeaderHelper.rememberShouldHaveSmallBottomPadding
import com.wire.android.ui.home.conversations.AuthorHeaderHelper.rememberShouldShowHeader
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.OnFileDownloaded
import com.wire.android.ui.home.conversations.banner.ConversationBanner
import com.wire.android.ui.home.conversations.banner.ConversationBannerViewModel
import com.wire.android.ui.home.conversations.call.ConversationCallViewState
import com.wire.android.ui.home.conversations.call.ConversationListCallViewModel
import com.wire.android.ui.home.conversations.composer.MessageComposerViewModel
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialog
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsNavBackArgs
import com.wire.android.ui.home.conversations.edit.editMessageMenuItems
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.info.ConversationInfoViewModel
import com.wire.android.ui.home.conversations.info.ConversationInfoViewState
import com.wire.android.ui.home.conversations.media.preview.ImagesPreviewNavBackArgs
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewModel
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewState
import com.wire.android.ui.home.conversations.messages.draft.MessageDraftViewModel
import com.wire.android.ui.home.conversations.messages.item.MessageContainerItem
import com.wire.android.ui.home.conversations.messages.item.SwipableMessageConfiguration
import com.wire.android.ui.home.conversations.migration.ConversationMigrationViewModel
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMapper.toSelfDeletionDuration
import com.wire.android.ui.home.conversations.selfdeletion.selfDeletionMenuItems
import com.wire.android.ui.home.conversations.sendmessage.SendMessageViewModel
import com.wire.android.ui.home.gallery.MediaGalleryActionType
import com.wire.android.ui.home.gallery.MediaGalleryNavBackArgs
import com.wire.android.ui.home.messagecomposer.MessageComposer
import com.wire.android.ui.home.messagecomposer.model.ComposableMessageBundle
import com.wire.android.ui.home.messagecomposer.model.MessageBundle
import com.wire.android.ui.home.messagecomposer.model.MessageComposition
import com.wire.android.ui.home.messagecomposer.model.Ping
import com.wire.android.ui.home.messagecomposer.state.MessageComposerStateHolder
import com.wire.android.ui.home.messagecomposer.state.rememberMessageComposerStateHolder
import com.wire.android.ui.legalhold.dialog.subject.LegalHoldSubjectMessageDialog
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.normalizeLink
import com.wire.android.util.serverDate
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.openDownloadFolder
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.Conversation.TypingIndicatorMode
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.MessageAssetStatus
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.ConferenceCallingResult
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import kotlinx.collections.immutable.PersistentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import java.util.Date
import java.util.Locale
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

/**
 * The maximum number of participants to send a ping without showing a confirmation dialog.
 */
private const val MAX_GROUP_SIZE_FOR_PING = 3

// TODO: !! this screen definitely needs a refactor and some cleanup !!
@Suppress("ComplexMethod")
@RootNavGraph
@WireDestination(
    navArgsDelegate = ConversationNavArgs::class
)
@Composable
fun ConversationScreen(
    navigator: Navigator,
    groupDetailsScreenResultRecipient: ResultRecipient<GroupConversationDetailsScreenDestination, GroupConversationDetailsNavBackArgs>,
    mediaGalleryScreenResultRecipient: ResultRecipient<MediaGalleryScreenDestination, MediaGalleryNavBackArgs>,
    imagePreviewScreenResultRecipient: ResultRecipient<ImagesPreviewScreenDestination, String>,
    resultNavigator: ResultBackNavigator<GroupConversationDetailsNavBackArgs>,
    conversationInfoViewModel: ConversationInfoViewModel = hiltViewModel(),
    conversationBannerViewModel: ConversationBannerViewModel = hiltViewModel(),
    conversationListCallViewModel: ConversationListCallViewModel = hiltViewModel(),
    conversationMessagesViewModel: ConversationMessagesViewModel = hiltViewModel(),
    messageComposerViewModel: MessageComposerViewModel = hiltViewModel(),
    sendMessageViewModel: SendMessageViewModel = hiltViewModel(),
    conversationMigrationViewModel: ConversationMigrationViewModel = hiltViewModel(),
    messageDraftViewModel: MessageDraftViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val resources = LocalContext.current.resources
    val showDialog = remember { mutableStateOf(ConversationScreenDialogType.NONE) }
    val conversationScreenState = rememberConversationScreenState()
    val messageComposerViewState = messageComposerViewModel.messageComposerViewState
    val messageComposerStateHolder = rememberMessageComposerStateHolder(
        messageComposerViewState = messageComposerViewState,
        modalBottomSheetState = conversationScreenState.modalBottomSheetState,
        draftMessageComposition = messageDraftViewModel.state.value,
        onSaveDraft = messageComposerViewModel::saveDraft,
        onSearchMentionQueryChanged = messageComposerViewModel::searchMembersToMention,
        onTypingEvent = messageComposerViewModel::sendTypingEvent,
        onClearMentionSearchResult = messageComposerViewModel::clearMentionSearchResult
    )
    val permissionPermanentlyDeniedDialogState =
        rememberVisibilityState<PermissionPermanentlyDeniedDialogState>()

    // this is to prevent from double navigating back after user deletes a group on group details screen
    // then ViewModel also detects it's removed and calls onNotFound which can execute navigateBack again and close the app
    var alreadyDeletedByUser by rememberSaveable { mutableStateOf(false) }

    val activity = LocalActivity.current

    LaunchedEffect(alreadyDeletedByUser) {
        if (!alreadyDeletedByUser) {
            conversationInfoViewModel.observeConversationDetails(navigator::navigateBack)
        }
    }

    // set message composer input to edit mode when editMessage is not null from MessageDraft
    LaunchedEffect(messageDraftViewModel.state.value.editMessageId) {
        val compositionState = messageDraftViewModel.state.value
        if (compositionState.editMessageId != null) {
            messageComposerStateHolder.toEdit(
                messageId = compositionState.editMessageId,
                editMessageText = messageDraftViewModel.state.value.draftText,
                mentions = compositionState.selectedMentions.map {
                    it.intoMessageMention()
                }
            )
        }
    }

    conversationMigrationViewModel.migratedConversationId?.let { migratedConversationId ->
        navigator.navigate(
            NavigationCommand(
                ConversationScreenDestination(migratedConversationId),
                BackStackMode.REMOVE_CURRENT
            )
        )
    }

    with(conversationListCallViewModel) {
        if (conversationCallViewState.shouldShowJoinAnywayDialog) {
            appLogger.i("showing showJoinAnywayDialog..")
            JoinAnywayDialog(
                onDismiss = ::dismissJoinCallAnywayDialog,
                onConfirm = {
                    joinAnyway {
                        getOngoingCallIntent(activity, it.toString()).run {
                            activity.startActivity(this)
                        }
                    }
                }
            )
        }
    }

    when (showDialog.value) {
        ConversationScreenDialogType.ONGOING_ACTIVE_CALL -> {
            OngoingActiveCallDialog(onJoinAnyways = {
                conversationListCallViewModel.endEstablishedCallIfAny {
                    getOutgoingCallIntent(activity, conversationListCallViewModel.conversationId.toString()).run {
                        activity.startActivity(this)
                    }
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
                participantsCount = conversationListCallViewModel.conversationCallViewState.participantsCount,
                onConfirm = {
                    startCallIfPossible(
                        conversationListCallViewModel,
                        showDialog,
                        coroutineScope,
                        conversationInfoViewModel.conversationInfoViewState.conversationType,
                        onOpenOutgoingCallScreen = {
                            getOutgoingCallIntent(activity, it.toString()).run {
                                activity.startActivity(this)
                            }
                        }
                    ) {
                        getOngoingCallIntent(activity, it.toString()).run {
                            activity.startActivity(this)
                        }
                    }
                },
                onDialogDismiss = {
                    showDialog.value = ConversationScreenDialogType.NONE
                }
            )
        }

        ConversationScreenDialogType.PING_CONFIRMATION -> {
            ConfirmSendingPingDialog(
                participantsCount = conversationListCallViewModel.conversationCallViewState.participantsCount,
                onConfirm = {
                    showDialog.value = ConversationScreenDialogType.NONE
                    sendMessageViewModel.trySendMessage(Ping(conversationMessagesViewModel.conversationId))
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

        ConversationScreenDialogType.VERIFICATION_DEGRADED -> {
            SureAboutCallingInDegradedConversationDialog(
                callAnyway = {
                    conversationListCallViewModel.onApplyConversationDegradation()
                    startCallIfPossible(
                        conversationListCallViewModel,
                        showDialog,
                        coroutineScope,
                        conversationInfoViewModel.conversationInfoViewState.conversationType,
                        onOpenOutgoingCallScreen = {
                            getOutgoingCallIntent(activity, it.toString()).run {
                                activity.startActivity(this)
                            }
                        }
                    ) {
                        getOngoingCallIntent(activity, it.toString()).run {
                            activity.startActivity(this)
                        }
                    }
                },
                onDialogDismiss = { showDialog.value = ConversationScreenDialogType.NONE }
            )
        }

        ConversationScreenDialogType.NONE -> {}
    }

    ConversationScreen(
        bannerMessage = conversationBannerViewModel.bannerState,
        messageComposerViewState = messageComposerViewState.value,
        conversationCallViewState = conversationListCallViewModel.conversationCallViewState,
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
        onSendMessage = sendMessageViewModel::trySendMessage,
        onPingOptionClicked = {
            if (conversationListCallViewModel.conversationCallViewState.participantsCount > MAX_GROUP_SIZE_FOR_PING) {
                showDialog.value = ConversationScreenDialogType.PING_CONFIRMATION
            } else {
                showDialog.value = ConversationScreenDialogType.NONE
                sendMessageViewModel.trySendMessage(Ping(conversationMessagesViewModel.conversationId))
            }
        },
        onImagesPicked = {
            navigator.navigate(
                NavigationCommand(
                    ImagesPreviewScreenDestination(
                        conversationId = conversationInfoViewModel.conversationInfoViewState.conversationId,
                        conversationName = conversationInfoViewModel.conversationInfoViewState.conversationName.asString(resources),
                        assetUriList = ArrayList(it)
                    )
                )
            )
        },
        onDeleteMessage = conversationMessagesViewModel::showDeleteMessageDialog,
        onAssetItemClicked = conversationMessagesViewModel::downloadOrFetchAssetAndShowDialog,
        onImageFullScreenMode = { message, isSelfMessage ->
            with(conversationMessagesViewModel) {
                navigator.navigate(
                    NavigationCommand(
                        MediaGalleryScreenDestination(
                            conversationId = conversationId,
                            messageId = message.header.messageId,
                            isSelfAsset = isSelfMessage,
                            isEphemeral = message.header.messageStatus.expirationStatus is ExpirationStatus.Expirable,
                            messageOptionsEnabled = true
                        )
                    )
                )
                updateImageOnFullscreenMode(message)
            }
        },
        onStartCall = {
            startCallIfPossible(
                conversationListCallViewModel,
                showDialog,
                coroutineScope,
                conversationInfoViewModel.conversationInfoViewState.conversationType,
                onOpenOutgoingCallScreen = {
                    getOutgoingCallIntent(activity, it.toString()).run {
                        activity.startActivity(this)
                    }
                }
            ) {
                getOngoingCallIntent(activity, it.toString()).run {
                    activity.startActivity(this)
                }
            }
        },
        onJoinCall = {
            conversationListCallViewModel.joinOngoingCall {
                getOngoingCallIntent(activity, it.toString()).run {
                    activity.startActivity(this)
                }
            }
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

                    is ConversationDetailsData.None -> { /* do nothing */
                    }
                }
            }
        },
        onBackButtonClick = {
            conversationScreenOnBackButtonClick(messageComposerViewModel, messageComposerStateHolder, navigator)
        },
        composerMessages = sendMessageViewModel.infoMessage,
        conversationMessages = conversationMessagesViewModel.infoMessage,
        shareAsset = conversationMessagesViewModel::shareAsset,
        onDownloadAssetClick = conversationMessagesViewModel::downloadOrFetchAssetAndShowDialog,
        onOpenAssetClick = conversationMessagesViewModel::downloadAndOpenAsset,
        onNavigateToReplyOriginalMessage = conversationMessagesViewModel::navigateToReplyOriginalMessage,
        onSelfDeletingMessageRead = messageComposerViewModel::startSelfDeletion,
        onNewSelfDeletingMessagesStatus = messageComposerViewModel::updateSelfDeletingMessages,
        tempWritableImageUri = messageComposerViewModel.tempWritableImageUri,
        tempWritableVideoUri = messageComposerViewModel.tempWritableVideoUri,
        onFailedMessageRetryClicked = sendMessageViewModel::retrySendingMessage,
        onClearMentionSearchResult = messageComposerViewModel::clearMentionSearchResult,
        onPermissionPermanentlyDenied = {
            val description = when (it) {
                ConversationActionPermissionType.CaptureVideo -> R.string.record_video_permission_dialog_description
                ConversationActionPermissionType.TakePicture -> R.string.take_picture_permission_dialog_description
                ConversationActionPermissionType.ChooseImage -> R.string.open_gallery_permission_dialog_description
                ConversationActionPermissionType.ChooseFile -> R.string.attach_file_permission_dialog_description
                ConversationActionPermissionType.CallAudio -> R.string.call_permission_dialog_description
            }
            permissionPermanentlyDeniedDialogState.show(
                PermissionPermanentlyDeniedDialogState.Visible(
                    title = R.string.app_permission_dialog_title,
                    description = description
                )
            )
        },
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
        currentTimeInMillisFlow = conversationMessagesViewModel.currentTimeInMillisFlow
    )
    BackHandler { conversationScreenOnBackButtonClick(messageComposerViewModel, messageComposerStateHolder, navigator) }
    DeleteMessageDialog(
        state = conversationMessagesViewModel.deleteMessageDialogsState,
        actions = conversationMessagesViewModel.deleteMessageHelper
    )
    DownloadedAssetDialog(
        downloadedAssetDialogState = conversationMessagesViewModel.conversationViewState.downloadedAssetDialogState,
        onSaveFileToExternalStorage = conversationMessagesViewModel::downloadAssetExternally,
        onOpenFileWithExternalApp = conversationMessagesViewModel::downloadAndOpenAsset,
        hideOnAssetDownloadedDialog = conversationMessagesViewModel::hideOnAssetDownloadedDialog,
        onPermissionPermanentlyDenied = {
            permissionPermanentlyDeniedDialogState.show(
                PermissionPermanentlyDeniedDialogState.Visible(
                    title = R.string.app_permission_dialog_title,
                    description = R.string.save_permission_dialog_description
                )
            )
        }
    )
    AssetTooLargeDialog(
        dialogState = sendMessageViewModel.assetTooLargeDialogState,
        hideDialog = sendMessageViewModel::hideAssetTooLargeError
    )
    VisitLinkDialog(
        dialogState = messageComposerViewModel.visitLinkDialogState,
        hideDialog = messageComposerViewModel::hideVisitLinkDialog
    )

    InvalidLinkDialog(
        dialogState = messageComposerViewModel.invalidLinkDialogState,
        hideDialog = messageComposerViewModel::hideInvalidLinkError
    )

    PermissionPermanentlyDeniedDialog(
        dialogState = permissionPermanentlyDeniedDialogState,
        hideDialog = permissionPermanentlyDeniedDialogState::dismiss
    )

    SureAboutMessagingInDegradedConversationDialog(
        dialogState = sendMessageViewModel.sureAboutMessagingDialogState,
        sendAnyway = sendMessageViewModel::acceptSureAboutSendingMessage,
        hideDialog = sendMessageViewModel::dismissSureAboutSendingMessage
    )

    (sendMessageViewModel.sureAboutMessagingDialogState as? SureAboutMessagingDialogState.Visible.ConversationUnderLegalHold)?.let {
        LegalHoldSubjectMessageDialog(
            dialogDismissed = sendMessageViewModel::dismissSureAboutSendingMessage,
            sendAnywayClicked = sendMessageViewModel::acceptSureAboutSendingMessage,
        )
    }

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

    imagePreviewScreenResultRecipient.onNavResult { result ->
        when (result) {
            Canceled -> {}
            is Value -> {
                val pendingBundles = ArgsSerializer().decodeFromString<ImagesPreviewNavBackArgs>(result.value).pendingBundles
                sendMessageViewModel.trySendMessages(
                    pendingBundles.map { assetBundle ->
                        ComposableMessageBundle.AttachmentPickedBundle(
                            conversationId = conversationMessagesViewModel.conversationId,
                            assetBundle = assetBundle
                        )
                    }
                )
            }
        }
    }
}

private fun conversationScreenOnBackButtonClick(
    messageComposerViewModel: MessageComposerViewModel,
    messageComposerStateHolder: MessageComposerStateHolder,
    navigator: Navigator
) {
    messageComposerViewModel.sendTypingEvent(TypingIndicatorMode.STOPPED)
    messageComposerStateHolder.messageCompositionInputStateHolder.collapseComposer(null)
    navigator.navigateBack()
}

@Suppress("LongParameterList")
private fun startCallIfPossible(
    conversationListCallViewModel: ConversationListCallViewModel,
    showDialog: MutableState<ConversationScreenDialogType>,
    coroutineScope: CoroutineScope,
    conversationType: Conversation.Type,
    onOpenOutgoingCallScreen: (ConversationId) -> Unit,
    onOpenOngoingCallScreen: (ConversationId) -> Unit
) {
    coroutineScope.launch {
        if (!conversationListCallViewModel.hasStableConnectivity()) {
            showDialog.value = ConversationScreenDialogType.NO_CONNECTIVITY
        } else if (conversationListCallViewModel.shouldInformAboutVerification.value) {
            showDialog.value = ConversationScreenDialogType.VERIFICATION_DEGRADED
        } else {
            val dialogValue = when (conversationListCallViewModel.isConferenceCallingEnabled(conversationType)) {
                ConferenceCallingResult.Enabled -> {
                    if (
                        showDialog.value != ConversationScreenDialogType.CALL_CONFIRMATION &&
                        conversationListCallViewModel.conversationCallViewState.participantsCount > MAX_GROUP_SIZE_FOR_CALL_WITHOUT_ALERT
                    ) {
                        ConversationScreenDialogType.CALL_CONFIRMATION
                    } else {
                        conversationListCallViewModel.endEstablishedCallIfAny {
                            onOpenOutgoingCallScreen(conversationListCallViewModel.conversationId)
                        }
                        ConversationScreenDialogType.NONE
                    }
                }

                ConferenceCallingResult.Disabled.Established -> {
                    onOpenOngoingCallScreen(conversationListCallViewModel.conversationId)
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
    messageComposerViewState: MessageComposerViewState,
    conversationCallViewState: ConversationCallViewState,
    conversationInfoViewState: ConversationInfoViewState,
    conversationMessagesViewState: ConversationMessagesViewState,
    onOpenProfile: (String) -> Unit,
    onMessageDetailsClick: (messageId: String, isSelfMessage: Boolean) -> Unit,
    onSendMessage: (MessageBundle) -> Unit,
    onPingOptionClicked: () -> Unit,
    onImagesPicked: (List<Uri>) -> Unit,
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
    shareAsset: (Context, messageId: String) -> Unit,
    onDownloadAssetClick: (messageId: String) -> Unit,
    onOpenAssetClick: (messageId: String) -> Unit,
    onNavigateToReplyOriginalMessage: (UIMessage) -> Unit,
    onSelfDeletingMessageRead: (UIMessage) -> Unit,
    onNewSelfDeletingMessagesStatus: (SelfDeletionTimer) -> Unit,
    tempWritableImageUri: Uri?,
    tempWritableVideoUri: Uri?,
    onFailedMessageRetryClicked: (String, ConversationId) -> Unit,
    onClearMentionSearchResult: () -> Unit,
    onPermissionPermanentlyDenied: (type: ConversationActionPermissionType) -> Unit,
    conversationScreenState: ConversationScreenState,
    messageComposerStateHolder: MessageComposerStateHolder,
    onLinkClick: (String) -> Unit,
    currentTimeInMillisFlow: Flow<Long> = flow { },
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
            editMessageMenuItems(
                message = menuType.selectedMessage,
                messageOptionsEnabled = true,
                hideEditMessageMenu = conversationScreenState::hideContextMenu,
                onCopyClick = conversationScreenState::copyMessage,
                onDeleteClick = onDeleteMessage,
                onReactionClick = onReactionClick,
                onDetailsClick = onMessageDetailsClick,
                onReplyClick = messageComposerStateHolder::toReply,
                onEditClick = { messageId, messageText, mentions -> messageComposerStateHolder.toEdit(messageId, messageText, mentions) },
                onShareAssetClick = {
                    menuType.selectedMessage.header.messageId.let {
                        shareAsset(context, it)
                        conversationScreenState.hideContextMenu()
                    }
                },
                onDownloadAssetClick = onDownloadAssetClick,
                onOpenAssetClick = onOpenAssetClick
            )
        }

        is ConversationScreenState.BottomSheetMenuType.SelfDeletion -> {
            selfDeletionMenuItems(
                hideEditMessageMenu = conversationScreenState::hideContextMenu,
                currentlySelected = menuType.currentlySelected.duration.toSelfDeletionDuration(),
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
                    onAudioPermissionPermanentlyDenied = {
                        onPermissionPermanentlyDenied(ConversationActionPermissionType.CallAudio)
                    },
                    isInteractionEnabled = messageComposerViewState.interactionAvailability == InteractionAvailability.ENABLED
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
                    assetStatuses = conversationMessagesViewState.assetStatuses,
                    lastUnreadMessageInstant = conversationMessagesViewState.firstUnreadInstant,
                    unreadEventCount = conversationMessagesViewState.firstuUnreadEventIndex,
                    conversationDetailsData = conversationInfoViewState.conversationDetailsData,
                    selectedMessageId = conversationMessagesViewState.searchedMessageId,
                    messageComposerStateHolder = messageComposerStateHolder,
                    messages = conversationMessagesViewState.messages,
                    onSendMessage = onSendMessage,
                    onPingOptionClicked = onPingOptionClicked,
                    onImagesPicked = onImagesPicked,
                    onAssetItemClicked = onAssetItemClicked,
                    onAudioItemClicked = onAudioClick,
                    onChangeAudioPosition = onChangeAudioPosition,
                    onImageFullScreenMode = onImageFullScreenMode,
                    onReactionClicked = onReactionClick,
                    onResetSessionClicked = onResetSessionClick,
                    onOpenProfile = onOpenProfile,
                    onUpdateConversationReadDate = onUpdateConversationReadDate,
                    onShowEditingOptions = conversationScreenState::showEditContextMenu,
                    onSwipedToReply = messageComposerStateHolder::toReply,
                    onSelfDeletingMessageRead = onSelfDeletingMessageRead,
                    onFailedMessageCancelClicked = remember { { onDeleteMessage(it, false) } },
                    onFailedMessageRetryClicked = onFailedMessageRetryClicked,
                    onChangeSelfDeletionClicked = conversationScreenState::showSelfDeletionContextMenu,
                    onClearMentionSearchResult = onClearMentionSearchResult,
                    onPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
                    tempWritableImageUri = tempWritableImageUri,
                    tempWritableVideoUri = tempWritableVideoUri,
                    onLinkClick = onLinkClick,
                    onNavigateToReplyOriginalMessage = onNavigateToReplyOriginalMessage,
                    currentTimeInMillisFlow = currentTimeInMillisFlow
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
    audioMessagesState: PersistentMap<String, AudioState>,
    assetStatuses: PersistentMap<String, MessageAssetStatus>,
    selectedMessageId: String?,
    messageComposerStateHolder: MessageComposerStateHolder,
    messages: Flow<PagingData<UIMessage>>,
    onSendMessage: (MessageBundle) -> Unit,
    onPingOptionClicked: () -> Unit,
    onImagesPicked: (List<Uri>) -> Unit,
    onAssetItemClicked: (String) -> Unit,
    onAudioItemClicked: (String) -> Unit,
    onChangeAudioPosition: (String, Int) -> Unit,
    onImageFullScreenMode: (UIMessage.Regular, Boolean) -> Unit,
    onReactionClicked: (String, String) -> Unit,
    onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit,
    onOpenProfile: (String) -> Unit,
    onUpdateConversationReadDate: (String) -> Unit,
    onShowEditingOptions: (UIMessage.Regular) -> Unit,
    onSwipedToReply: (UIMessage.Regular) -> Unit,
    onSelfDeletingMessageRead: (UIMessage) -> Unit,
    conversationDetailsData: ConversationDetailsData,
    onFailedMessageRetryClicked: (String, ConversationId) -> Unit,
    onFailedMessageCancelClicked: (String) -> Unit,
    onChangeSelfDeletionClicked: (SelfDeletionTimer) -> Unit,
    onClearMentionSearchResult: () -> Unit,
    onPermissionPermanentlyDenied: (type: ConversationActionPermissionType) -> Unit,
    tempWritableImageUri: Uri?,
    tempWritableVideoUri: Uri?,
    onLinkClick: (String) -> Unit,
    onNavigateToReplyOriginalMessage: (UIMessage) -> Unit,
    currentTimeInMillisFlow: Flow<Long> = flow {},
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
                assetStatuses = assetStatuses,
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
                onSwipedToReply = onSwipedToReply,
                conversationDetailsData = conversationDetailsData,
                onFailedMessageCancelClicked = onFailedMessageCancelClicked,
                onFailedMessageRetryClicked = onFailedMessageRetryClicked,
                onLinkClick = onLinkClick,
                selectedMessageId = selectedMessageId,
                onNavigateToReplyOriginalMessage = onNavigateToReplyOriginalMessage,
                interactionAvailability = messageComposerStateHolder.messageComposerViewState.value.interactionAvailability,
                currentTimeInMillisFlow = currentTimeInMillisFlow
            )
        },
        onChangeSelfDeletionClicked = onChangeSelfDeletionClicked,
        onClearMentionSearchResult = onClearMentionSearchResult,
        onSendMessageBundle = onSendMessage,
        onPingOptionClicked = onPingOptionClicked,
        onPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
        tempWritableVideoUri = tempWritableVideoUri,
        tempWritableImageUri = tempWritableImageUri,
        onImagesPicked = onImagesPicked
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
                actionLabel = actionLabel,
                duration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Long,
            )
            // Show downloads folder when clicking on Snackbar cta button
            if (it is OnFileDownloaded && snackbarResult == SnackbarResult.ActionPerformed) {
                openDownloadFolder(context)
            }
        }
    }
}

@Suppress("ComplexMethod", "ComplexCondition")
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MessageList(
    lazyPagingMessages: LazyPagingItems<UIMessage>,
    lazyListState: LazyListState,
    lastUnreadMessageInstant: Instant?,
    audioMessagesState: PersistentMap<String, AudioState>,
    assetStatuses: PersistentMap<String, MessageAssetStatus>,
    onUpdateConversationReadDate: (String) -> Unit,
    onAssetItemClicked: (String) -> Unit,
    onImageFullScreenMode: (UIMessage.Regular, Boolean) -> Unit,
    onOpenProfile: (String) -> Unit,
    onAudioItemClicked: (String) -> Unit,
    onChangeAudioPosition: (String, Int) -> Unit,
    onReactionClicked: (String, String) -> Unit,
    onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit,
    onShowEditingOption: (UIMessage.Regular) -> Unit,
    onSwipedToReply: (UIMessage.Regular) -> Unit,
    onSelfDeletingMessageRead: (UIMessage) -> Unit,
    conversationDetailsData: ConversationDetailsData,
    onFailedMessageRetryClicked: (String, ConversationId) -> Unit,
    onFailedMessageCancelClicked: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    selectedMessageId: String?,
    onNavigateToReplyOriginalMessage: (UIMessage) -> Unit,
    interactionAvailability: InteractionAvailability,
    modifier: Modifier = Modifier,
    currentTimeInMillisFlow: Flow<Long> = flow { }
) {
    val prevItemCount = remember { mutableStateOf(lazyPagingMessages.itemCount) }
    val readLastMessageAtStartTriggered = remember { mutableStateOf(false) }
    val currentTime by currentTimeInMillisFlow.collectAsState(initial = System.currentTimeMillis())

    LaunchedEffect(lazyPagingMessages.itemCount) {
        if (lazyPagingMessages.itemCount > prevItemCount.value && selectedMessageId == null) {
            val canScrollToLastMessage = prevItemCount.value > 0
                    && lazyListState.firstVisibleItemIndex > 0
                    && lazyListState.firstVisibleItemIndex <= MAXIMUM_SCROLLED_MESSAGES_UNTIL_AUTOSCROLL_STOPS
            if (canScrollToLastMessage) {
                lazyListState.animateScrollToItem(0)
            }
            prevItemCount.value = lazyPagingMessages.itemCount
        }
    }

    // update last read message when scroll ends
    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress && lazyPagingMessages.itemCount > 0) {
            val lastVisibleMessage = lazyPagingMessages[lazyListState.firstVisibleItemIndex] ?: return@LaunchedEffect
            updateLastReadMessage(lastVisibleMessage, lastUnreadMessageInstant, onUpdateConversationReadDate)
        }
    }

    // update last read message on start or when list is not scrollable
    LaunchedEffect(lazyPagingMessages.itemCount) {
        if ((!readLastMessageAtStartTriggered.value || (!lazyListState.canScrollBackward && !lazyListState.canScrollForward))
            && lazyPagingMessages.itemSnapshotList.items.isNotEmpty()
        ) {
            val lastVisibleMessage = lazyPagingMessages[lazyListState.firstVisibleItemIndex] ?: return@LaunchedEffect
            if (!readLastMessageAtStartTriggered.value) {
                readLastMessageAtStartTriggered.value = true
            }
            updateLastReadMessage(lastVisibleMessage, lastUnreadMessageInstant, onUpdateConversationReadDate)
        }
    }

    Box(
        contentAlignment = Alignment.BottomEnd,
        modifier = modifier
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
                        ?: return@items Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(dimensions().spacing56x),
                        ) {
                            WireCircularProgressIndicator(
                                progressColor = MaterialTheme.wireColorScheme.secondaryText,
                                size = dimensions().spacing24x
                            )
                        }

                    val showAuthor = rememberShouldShowHeader(index, message, lazyPagingMessages)
                    val useSmallBottomPadding = rememberShouldHaveSmallBottomPadding(index, message, lazyPagingMessages)

                    if (index > 0) {
                        val previousMessage = lazyPagingMessages[index - 1] ?: message
                        val shouldDisplayDateTimeDivider = message.header.messageTime.shouldDisplayDatesDifferenceDivider(
                            previousDate = previousMessage.header.messageTime.utcISO
                        )

                        if (shouldDisplayDateTimeDivider) {
                            val previousGroup = previousMessage.header.messageTime.getFormattedDateGroup(now = currentTime)
                            previousMessage.header.messageTime.utcISO.serverDate()?.let { serverDate ->
                                MessageGroupDateTime(
                                    messageDateTime = serverDate,
                                    messageDateTimeGroup = previousGroup,
                                    now = currentTime
                                )
                            }
                        }
                    }
                    val swipableConfiguration = remember(message) {
                        SwipableMessageConfiguration.SwipableToReply {
                            onSwipedToReply(it)
                        }
                    }

                    MessageContainerItem(
                        message = message,
                        conversationDetailsData = conversationDetailsData,
                        showAuthor = showAuthor,
                        useSmallBottomPadding = useSmallBottomPadding,
                        audioMessagesState = audioMessagesState,
                        assetStatus = assetStatuses[message.header.messageId]?.transferStatus,
                        onAudioClick = onAudioItemClicked,
                        onChangeAudioPosition = onChangeAudioPosition,
                        onLongClicked = onShowEditingOption,
                        swipableMessageConfiguration = swipableConfiguration,
                        onAssetMessageClicked = onAssetItemClicked,
                        onImageMessageClicked = onImageFullScreenMode,
                        onOpenProfile = onOpenProfile,
                        onReactionClicked = onReactionClicked,
                        onResetSessionClicked = onResetSessionClicked,
                        onSelfDeletingMessageRead = onSelfDeletingMessageRead,
                        onFailedMessageCancelClicked = onFailedMessageCancelClicked,
                        onFailedMessageRetryClicked = onFailedMessageRetryClicked,
                        onLinkClick = onLinkClick,
                        onReplyClickable = Clickable(
                            enabled = true,
                            onClick = {
                                onNavigateToReplyOriginalMessage(message)
                            }
                        ),
                        isSelectedMessage = (message.header.messageId == selectedMessageId),
                        isInteractionAvailable = interactionAvailability == InteractionAvailability.ENABLED
                    )

                    val isTheOnlyItem = index == 0 && lazyPagingMessages.itemCount == 1
                    val isTheLastItem = (index + 1) == lazyPagingMessages.itemCount
                    if (isTheOnlyItem || isTheLastItem) {
                        val currentGroup = message.header.messageTime.getFormattedDateGroup(now = currentTime)
                        message.header.messageTime.utcISO.serverDate()?.let { serverDate ->
                            MessageGroupDateTime(
                                messageDateTime = serverDate,
                                messageDateTimeGroup = currentGroup,
                                now = currentTime
                            )
                        }
                    }
                }
            }
            JumpToLastMessageButton(lazyListState = lazyListState)
        }
    )
}

@Composable
private fun MessageGroupDateTime(
    now: Long,
    messageDateTime: Date,
    messageDateTimeGroup: MessageDateTimeGroup?
) {
    val context = LocalContext.current

    val timeString = when (messageDateTimeGroup) {
        is MessageDateTimeGroup.Now -> context.resources.getString(R.string.message_datetime_now)
        is MessageDateTimeGroup.Within30Minutes -> DateUtils.getRelativeTimeSpanString(
            messageDateTime.time,
            now,
            DateUtils.MINUTE_IN_MILLIS
        ).toString()

        is MessageDateTimeGroup.Daily -> {
            when (messageDateTimeGroup.type) {
                MessageDateTimeGroup.Daily.Type.Today -> DateUtils.getRelativeDateTimeString(
                    context,
                    messageDateTime.time,
                    DateUtils.DAY_IN_MILLIS,
                    DateUtils.DAY_IN_MILLIS,
                    0
                ).toString()

                MessageDateTimeGroup.Daily.Type.Yesterday ->
                    DateUtils.getRelativeDateTimeString(
                        context,
                        messageDateTime.time,
                        DateUtils.DAY_IN_MILLIS,
                        DateUtils.DAY_IN_MILLIS * 2,
                        0
                    ).toString()

                MessageDateTimeGroup.Daily.Type.WithinWeek -> DateUtils.formatDateTime(
                    context,
                    messageDateTime.time,
                    DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
                )

                MessageDateTimeGroup.Daily.Type.NotWithinWeekButSameYear -> DateUtils.formatDateTime(
                    context,
                    messageDateTime.time,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
                )

                MessageDateTimeGroup.Daily.Type.Other -> DateUtils.formatDateTime(
                    context,
                    messageDateTime.time,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_TIME
                )
            }
        }

        null -> ""
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                top = dimensions().spacing4x,
                bottom = dimensions().spacing8x
            )
            .background(color = colorsScheme().divider)
            .padding(
                top = dimensions().spacing6x,
                bottom = dimensions().spacing6x,
                start = dimensions().spacing56x
            )
    ) {
        Text(
            text = timeString.uppercase(Locale.getDefault()),
            color = colorsScheme().secondaryText,
            style = MaterialTheme.wireTypography.title03,
        )
    }
}

private fun updateLastReadMessage(
    lastVisibleMessage: UIMessage,
    lastUnreadMessageInstant: Instant?,
    onUpdateConversationReadDate: (String) -> Unit
) {
    val lastVisibleMessageInstant = Instant.parse(lastVisibleMessage.header.messageTime.utcISO)

    // TODO: This IF condition should be in the UseCase
    //       If there are no unread messages, then use distant future and don't update read date
    if (lastVisibleMessageInstant > (lastUnreadMessageInstant ?: Instant.DISTANT_FUTURE)) {
        onUpdateConversationReadDate(lastVisibleMessage.header.messageTime.utcISO)
    }
}

@Composable
fun JumpToLastMessageButton(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
) {
    AnimatedVisibility(
        modifier = modifier,
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

enum class ConversationActionPermissionType {
    CaptureVideo, TakePicture, ChooseImage, ChooseFile, CallAudio
}

@PreviewMultipleThemes
@Composable
fun PreviewConversationScreen() = WireTheme {
    val conversationId = ConversationId("value", "domain")
    val messageComposerViewState = remember { mutableStateOf(MessageComposerViewState()) }
    val messageCompositionState = remember { mutableStateOf(MessageComposition(conversationId)) }
    val conversationScreenState = rememberConversationScreenState()
    val messageComposerStateHolder = rememberMessageComposerStateHolder(
        messageComposerViewState = messageComposerViewState,
        modalBottomSheetState = conversationScreenState.modalBottomSheetState,
        draftMessageComposition = messageCompositionState.value,
        onSaveDraft = {},
        onTypingEvent = {},
        onSearchMentionQueryChanged = {},
        onClearMentionSearchResult = {},
    )
    ConversationScreen(
        bannerMessage = null,
        messageComposerViewState = messageComposerViewState.value,
        conversationCallViewState = ConversationCallViewState(),
        conversationInfoViewState = ConversationInfoViewState(
            conversationId = conversationId,
            conversationName = UIText.DynamicString("Some test conversation")
        ),
        conversationMessagesViewState = ConversationMessagesViewState(),
        onOpenProfile = { },
        onMessageDetailsClick = { _, _ -> },
        onSendMessage = { },
        onPingOptionClicked = { },
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
        shareAsset = { _, _ -> },
        onOpenAssetClick = {},
        onDownloadAssetClick = {},
        onNavigateToReplyOriginalMessage = {},
        onSelfDeletingMessageRead = {},
        onNewSelfDeletingMessagesStatus = {},
        tempWritableImageUri = null,
        tempWritableVideoUri = null,
        onFailedMessageRetryClicked = { _, _ -> },
        onClearMentionSearchResult = {},
        onPermissionPermanentlyDenied = {},
        conversationScreenState = conversationScreenState,
        messageComposerStateHolder = messageComposerStateHolder,
        onLinkClick = { _ -> },
        onImagesPicked = {}
    )
}
