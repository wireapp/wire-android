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
@file:Suppress("TooManyFunctions")

package com.wire.android.ui.home.conversations

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.ramcosta.composedestinations.generated.app.destinations.ConversationScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.GroupConversationDetailsScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.ImagesPreviewScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.ImportMediaScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.MediaGalleryScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.MessageDetailsScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.OtherUserProfileScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.SelfUserProfileScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.ServiceDetailsScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.ThreadConversationScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.VideoPlayerScreenDestination
import com.ramcosta.composedestinations.generated.sketch.destinations.DrawingCanvasScreenDestination
import com.ramcosta.composedestinations.result.OpenResultRecipient
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.ramcosta.composedestinations.result.ResultRecipient
import com.wire.android.BuildConfig.IS_BUBBLE_UI_ENABLED
import com.wire.android.BuildConfig.REPLY_AS_THREAD_ENABLED
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.feature.sketch.model.DrawingCanvasNavArgs
import com.wire.android.feature.sketch.model.DrawingCanvasNavBackArgs
import com.wire.android.model.SnackBarMessage
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireRootDestination
import com.wire.android.ui.calling.conversationCallViewModel
import com.wire.android.ui.common.attachmentdraft.model.AttachmentDraftUi
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dialogs.ConfirmSendingPingDialog
import com.wire.android.ui.common.dialogs.calling.CallingFeatureActivatedDialog
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.snackbar.SwipeableSnackbar
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.OnFileDownloaded
import com.wire.android.ui.home.conversations.attachment.MessageAttachmentsViewModel
import com.wire.android.ui.home.conversations.banner.ConversationBanner
import com.wire.android.ui.home.conversations.banner.ConversationBannerViewModel
import com.wire.android.ui.home.conversations.call.ConversationCallViewModel
import com.wire.android.ui.home.conversations.call.ConversationCallViewState
import com.wire.android.ui.home.conversations.call.HandleActions
import com.wire.android.ui.home.conversations.call.HandleJoinOrStartCallScreenDialogs
import com.wire.android.ui.home.conversations.composer.MessageComposerViewModel
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogState
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsNavBackArgs
import com.wire.android.ui.home.conversations.edit.MessageOptionsModalSheetLayout
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.info.ConversationInfoViewModel
import com.wire.android.ui.home.conversations.info.ConversationInfoViewState
import com.wire.android.ui.home.conversations.media.preview.ImagesPreviewNavBackArgs
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewModel
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewState
import com.wire.android.ui.home.conversations.messages.ThreadSummaryUi
import com.wire.android.ui.home.conversations.messages.draft.MessageDraftViewModel
import com.wire.android.ui.home.conversations.migration.ConversationMigrationViewModel
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.android.ui.home.conversations.model.MessageSenderId
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionOptionsModalSheetLayout
import com.wire.android.ui.home.conversations.sendmessage.SendMessageViewModel
import com.wire.android.ui.home.gallery.MediaGalleryNavBackArgs
import com.wire.android.ui.home.messagecomposer.location.LocationPickerComponent
import com.wire.android.ui.home.messagecomposer.model.ComposableMessageBundle
import com.wire.android.ui.home.messagecomposer.model.MessageBundle
import com.wire.android.ui.home.messagecomposer.model.MessageComposition
import com.wire.android.ui.home.messagecomposer.model.Ping
import com.wire.android.ui.home.messagecomposer.state.MessageComposerStateHolder
import com.wire.android.ui.home.messagecomposer.state.rememberMessageComposerStateHolder
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.userprofile.service.ServiceDetailsNavArgs
import com.wire.android.util.fileShareUri
import com.wire.android.util.normalizeLink
import com.wire.android.util.openDownloadFolder
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.android.ui.sharing.ImportMediaNavArgs
import com.wire.android.ui.sharing.ImportSource
import com.wire.kalium.logic.data.conversation.Conversation.TypingIndicatorMode
import com.wire.kalium.logic.data.conversation.InteractionAvailability
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserTypeInfo
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Instant
import com.wire.android.ui.common.R as commonR

/**
 * The maximum number of participants to send a ping without showing a confirmation dialog.
 */
private const val MAX_GROUP_SIZE_FOR_PING = 3

internal enum class ConversationScreenMode {
    Main,
    Thread,
}

// TODO: !! this screen definitely needs a refactor and some cleanup !!
@Suppress("ComplexMethod")
@WireRootDestination(
    navArgs = ConversationNavArgs::class
)
@Composable
fun ConversationScreen(
    navigator: Navigator,
    groupDetailsScreenResultRecipient:
    ResultRecipient<GroupConversationDetailsScreenDestination, GroupConversationDetailsNavBackArgs>,
    mediaGalleryScreenResultRecipient: ResultRecipient<MediaGalleryScreenDestination, MediaGalleryNavBackArgs>,
    imagePreviewScreenResultRecipient: ResultRecipient<ImagesPreviewScreenDestination, ImagesPreviewNavBackArgs>,
    drawingCanvasScreenResultRecipient: OpenResultRecipient<DrawingCanvasNavBackArgs>,
    resultNavigator: ResultBackNavigator<GroupConversationDetailsNavBackArgs>,
    conversationInfoViewModel: ConversationInfoViewModel = conversationInfoViewModel(),
    conversationBannerViewModel: ConversationBannerViewModel = conversationBannerViewModel(),
    conversationCallViewModel: ConversationCallViewModel = conversationCallViewModel(),
    conversationMessagesViewModel: ConversationMessagesViewModel = conversationMessagesViewModel(),
    messageComposerViewModel: MessageComposerViewModel = messageComposerViewModel(),
    sendMessageViewModel: SendMessageViewModel = sendMessageViewModel(),
    conversationMigrationViewModel: ConversationMigrationViewModel = conversationMigrationViewModel(),
    messageDraftViewModel: MessageDraftViewModel = messageDraftViewModel(),
    messageAttachmentsViewModel: MessageAttachmentsViewModel = messageAttachmentsViewModel(),
) {
    ConversationScreenHost(
        screenMode = ConversationScreenMode.Main,
        navigator = navigator,
        groupDetailsScreenResultRecipient = groupDetailsScreenResultRecipient,
        mediaGalleryScreenResultRecipient = mediaGalleryScreenResultRecipient,
        imagePreviewScreenResultRecipient = imagePreviewScreenResultRecipient,
        drawingCanvasScreenResultRecipient = drawingCanvasScreenResultRecipient,
        resultNavigator = resultNavigator,
        conversationInfoViewModel = conversationInfoViewModel,
        conversationBannerViewModel = conversationBannerViewModel,
        conversationCallViewModel = conversationCallViewModel,
        conversationMessagesViewModel = conversationMessagesViewModel,
        messageComposerViewModel = messageComposerViewModel,
        sendMessageViewModel = sendMessageViewModel,
        conversationMigrationViewModel = conversationMigrationViewModel,
        messageDraftViewModel = messageDraftViewModel,
        messageAttachmentsViewModel = messageAttachmentsViewModel,
    )
}

@Suppress("ComplexMethod")
@Composable
internal fun ConversationScreenHost(
    screenMode: ConversationScreenMode,
    navigator: Navigator,
    groupDetailsScreenResultRecipient:
    ResultRecipient<GroupConversationDetailsScreenDestination, GroupConversationDetailsNavBackArgs>,
    mediaGalleryScreenResultRecipient: ResultRecipient<MediaGalleryScreenDestination, MediaGalleryNavBackArgs>,
    imagePreviewScreenResultRecipient: ResultRecipient<ImagesPreviewScreenDestination, ImagesPreviewNavBackArgs>,
    drawingCanvasScreenResultRecipient: OpenResultRecipient<DrawingCanvasNavBackArgs>,
    resultNavigator: ResultBackNavigator<GroupConversationDetailsNavBackArgs>,
    conversationInfoViewModel: ConversationInfoViewModel,
    conversationBannerViewModel: ConversationBannerViewModel,
    conversationCallViewModel: ConversationCallViewModel,
    conversationMessagesViewModel: ConversationMessagesViewModel,
    messageComposerViewModel: MessageComposerViewModel,
    sendMessageViewModel: SendMessageViewModel,
    conversationMigrationViewModel: ConversationMigrationViewModel,
    messageDraftViewModel: MessageDraftViewModel,
    messageAttachmentsViewModel: MessageAttachmentsViewModel,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val resources = context.resources
    val isThreadMode = screenMode == ConversationScreenMode.Thread
    val showDialog = remember { mutableStateOf(ConversationScreenDialogType.NONE) }
    val messageComposerViewState = messageComposerViewModel.messageComposerViewState
    val messageComposerStateHolder = rememberMessageComposerStateHolder(
        messageComposerViewState = messageComposerViewState,
        draftMessageComposition = messageDraftViewModel.state.value,
        onClearDraft = messageDraftViewModel::clearDraft,
        onSaveDraft = messageDraftViewModel::saveDraft,
        onMessageTextUpdate = messageDraftViewModel::onMessageTextUpdate,
        onSearchMentionQueryChanged = messageComposerViewModel::searchMembersToMention,
        onTypingEvent = messageComposerViewModel::sendTypingEvent,
        onClearMentionSearchResult = messageComposerViewModel::clearMentionSearchResult
    )
    val conversationScreenState = rememberConversationScreenState(
        selfDeletingSheetState = rememberWireModalSheetState(
            onDismissAction = {
                messageComposerStateHolder.messageCompositionInputStateHolder.setFocused()
            }
        ),
        locationSheetState = rememberWireModalSheetState(
            onDismissAction = {
                messageComposerStateHolder.messageCompositionInputStateHolder.setFocused()
            }
        ),
        editSheetState = rememberWireModalSheetState(
            onDismissAction = {
                messageComposerStateHolder.messageCompositionInputStateHolder.setFocused()
            },
        ),
    )

    val permissionPermanentlyDeniedDialogState =
        rememberVisibilityState<PermissionPermanentlyDeniedDialogState>()

    // this is to prevent from double navigating back after user deletes a group on group details screen
    // then ViewModel also detects it's removed and calls onNotFound which can execute navigateBack again and close the app
    var alreadyDeletedByUser by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(conversationScreenState.isAnySheetVisible) {
        with(messageComposerStateHolder) {
            if (conversationScreenState.isAnySheetVisible) {
                messageCompositionInputStateHolder.showAttachments(false)
            }
        }
    }

    LaunchedEffect(alreadyDeletedByUser, isThreadMode) {
        if (!alreadyDeletedByUser) {
            conversationInfoViewModel.observeConversationDetails()
        }
    }
    LaunchedEffect(conversationInfoViewModel.conversationInfoViewState.notFound, isThreadMode) {
        if (!isThreadMode && conversationInfoViewModel.conversationInfoViewState.notFound) navigator.navigateBack()
    }

    // set message composer input to edit mode when editMessage is not null from MessageDraft
    LaunchedEffect(messageDraftViewModel.state.value.editMessageId) {
        val compositionState = messageDraftViewModel.state.value
        if (compositionState.editMessageId != null) {
            messageDraftViewModel.clearDraft()
            messageComposerStateHolder.toEdit(
                messageId = compositionState.editMessageId,
                editMessageText = messageDraftViewModel.state.value.draftText,
                mentions = compositionState.selectedMentions.map {
                    it.intoMessageMention()
                },
                isMultipart = compositionState.isMultipart,
            )
        }
    }
    // set message composer input to reply mode when quotedMessage is not null from MessageDraft
    LaunchedEffect(messageDraftViewModel.state.value.quotedMessageId) {
        val compositionState = messageDraftViewModel.state.value
        if (compositionState.quotedMessage != null) {
            messageComposerStateHolder.messageCompositionHolder.value.updateQuote(compositionState.quotedMessage)
        }
    }

    LaunchedEffect(Unit) {
        conversationCallViewModel.callingEnabled.collect {
            showDialog.value = ConversationScreenDialogType.CALLING_FEATURE_ACTIVATED
        }
    }

    LaunchedEffect(
        messageComposerStateHolder.messageCompositionInputStateHolder.messageTextState,
        messageAttachmentsViewModel.attachments,
    ) {
        if (messageAttachmentsViewModel.attachments.isNotEmpty()) {
            sendMessageViewModel.clearLinkPreview()
            return@LaunchedEffect
        }

        messageComposerStateHolder.messageCompositionInputStateHolder.messageTextState
            .textAsFlow()
            .distinctUntilChanged()
            .collectLatest { text ->
                sendMessageViewModel.updateLinkPreview(
                    text = text.toString(),
                    mentions = messageComposerStateHolder.messageComposition.value.selectedMentions.map {
                        it.intoMessageMention()
                    }
                )
            }
    }

    LaunchedEffect(Unit) {
        conversationMessagesViewModel.openThread.collect { threadData ->
            navigator.navigate(
                threadNavigationCommand(
                    conversationId = conversationMessagesViewModel.conversationId,
                    threadId = threadData.threadId,
                    rootMessageId = threadData.rootMessageId,
                    rootMessageSelfDeletionDurationMillis = threadData.rootMessageSelfDeletionDurationMillis,
                )
            )
        }
    }

    if (!isThreadMode) {
        conversationMigrationViewModel.migratedConversationId?.let { migratedConversationId ->
            navigator.navigate(
                NavigationCommand(
                    ConversationScreenDestination(migratedConversationId),
                    BackStackMode.REMOVE_CURRENT,
                )
            )
        }
    }

    when (showDialog.value) {
        ConversationScreenDialogType.PING_CONFIRMATION -> {
            ConfirmSendingPingDialog(
                participantsCount = conversationCallViewModel.conversationCallViewState.participantsCount,
                onConfirm = {
                    showDialog.value = ConversationScreenDialogType.NONE
                    sendMessageViewModel.trySendMessage(Ping(conversationMessagesViewModel.conversationId))
                },
                onDialogDismiss = {
                    showDialog.value = ConversationScreenDialogType.NONE
                }
            )
        }

        ConversationScreenDialogType.CALLING_FEATURE_ACTIVATED -> {
            CallingFeatureActivatedDialog {
                showDialog.value = ConversationScreenDialogType.NONE
            }
        }

        ConversationScreenDialogType.NONE -> {}
    }

    conversationCallViewModel.callManager.actions.HandleActions()
    conversationCallViewModel.callManager.HandleJoinOrStartCallScreenDialogs()

    ConversationScreenContent(
        bannerMessage = conversationBannerViewModel.bannerState,
        messageComposerViewState = messageComposerViewState.value,
        bottomSheetVisible = conversationScreenState.isAnySheetVisible,
        conversationCallViewState = conversationCallViewModel.conversationCallViewState,
        conversationInfoViewState = conversationInfoViewModel.conversationInfoViewState,
        conversationMessagesViewState = conversationMessagesViewModel.conversationViewState,
        attachments = messageAttachmentsViewModel.attachments,
        onOpenProfile = { senderId: MessageSenderId ->
            with(conversationInfoViewModel) {
                val route = when (senderId) {
                    is MessageSenderId.Bot -> ServiceDetailsScreenDestination(
                        null,
                        ServiceDetailsNavArgs.Id.BotServiceId(senderId.botService)
                    )

                    is MessageSenderId.App -> ServiceDetailsScreenDestination(
                        null,
                        ServiceDetailsNavArgs.Id.AppId(senderId.appId)
                    )

                    is MessageSenderId.User -> {
                        val (mentionUserId: UserId, isSelfUser: Boolean) = mentionedUserData(senderId.id.toString())
                        if (isSelfUser) {
                            SelfUserProfileScreenDestination
                        } else {
                            (conversationInfoViewState.conversationDetailsData as? ConversationDetailsData.Group)
                                ?.conversationId?.let { conversationId ->
                                    OtherUserProfileScreenDestination(
                                        mentionUserId,
                                        conversationId
                                    )
                            }
                        }
                    }
                }

                route?.let {
                    navigator.navigate(NavigationCommand(it))
                }
            }
        },
        onMessageDetailsClick = { messageId: String, isSelfMessage: Boolean ->
            appLogger.i("[ConversationScreen][openMessageDetails] - isSelfMessage: $isSelfMessage")
            navigator.navigate(
                NavigationCommand(MessageDetailsScreenDestination(conversationInfoViewModel.conversationId, messageId, isSelfMessage))
            )
        },
        onSendMessage = {
            sendMessageViewModel.trySendMessage(it.withPrefetchedLinkPreview(sendMessageViewModel.currentLinkPreview))
        },
        onPingOptionClicked = {
            if (conversationCallViewModel.conversationCallViewState.participantsCount > MAX_GROUP_SIZE_FOR_PING) {
                showDialog.value = ConversationScreenDialogType.PING_CONFIRMATION
            } else {
                showDialog.value = ConversationScreenDialogType.NONE
                sendMessageViewModel.trySendMessage(Ping(conversationMessagesViewModel.conversationId))
            }
        },
        onImagesPicked = { it, fromKeyboard ->
            if (conversationInfoViewModel.conversationInfoViewState.isWireCellEnabled && !fromKeyboard) {
                messageAttachmentsViewModel.onFilesSelected(it)
                messageComposerStateHolder.messageCompositionInputStateHolder.showAttachments(false)
            } else {
                navigator.navigate(
                    NavigationCommand(
                        ImagesPreviewScreenDestination(
                            conversationId = conversationInfoViewModel.conversationInfoViewState.conversationId,
                            conversationName = conversationInfoViewModel.conversationInfoViewState.conversationName.asString(resources),
                            assetUriList = ArrayList(it)
                        )
                    )
                )
            }
        },
        onAttachmentPicked = {
            if (conversationInfoViewModel.conversationInfoViewState.isWireCellEnabled) {
                messageAttachmentsViewModel.onFilesSelected(listOf(it.uri))
                messageComposerStateHolder.messageCompositionInputStateHolder.showAttachments(false)
            } else {
                val bundle = ComposableMessageBundle.UriPickedBundle(conversationInfoViewModel.conversationId, it)
                sendMessageViewModel.trySendMessage(bundle)
            }
        },
        onAudioRecorded = {
            messageComposerStateHolder.messageCompositionInputStateHolder.showAttachments(false)
            if (conversationInfoViewModel.conversationInfoViewState.isWireCellEnabled) {
                messageAttachmentsViewModel.onAudioRecorded(it.uri, it.audioWavesMask)
            } else {
                val bundle = ComposableMessageBundle.AudioMessageBundle(conversationInfoViewModel.conversationId, it)
                sendMessageViewModel.trySendMessage(bundle)
            }
        },
        onDeleteMessage = { messageId, deleteForEveryone ->
            conversationMessagesViewModel.deleteMessageDialogState
                .show(DeleteMessageDialogState(deleteForEveryone, messageId, conversationMessagesViewModel.conversationId))
        },
        onAssetItemClicked = conversationMessagesViewModel::openOrFetchAsset,
        onImageFullScreenMode = { message, isSelfMessage, cellAssetId ->
            with(conversationMessagesViewModel) {
                navigator.navigate(
                    NavigationCommand(
                        MediaGalleryScreenDestination(
                            conversationId = conversationId,
                            messageId = message.header.messageId,
                            isSelfAsset = isSelfMessage,
                            isEphemeral = message.header.messageStatus.expirationStatus is ExpirationStatus.Expirable,
                            messageOptionsEnabled = true,
                            cellAssetId = cellAssetId,
                        )
                    )
                )
                updateImageOnFullscreenMode(message)
            }
        },
        onVideoClick = { localPath, contentUrl, fileName ->
            navigator.navigate(
                NavigationCommand(
                    VideoPlayerScreenDestination(
                        localPath = localPath,
                        contentUrl = contentUrl,
                        fileName = fileName,
                    )
                )
            )
        },
        onStartCall = {
            conversationCallViewModel.startCallIfPossible(conversationInfoViewModel.conversationInfoViewState.conversationType)
        },
        onJoinCall = conversationCallViewModel::joinOngoingCall,
        onReactionClick = { messageId, emoji ->
            conversationMessagesViewModel.toggleReaction(messageId, emoji)
        },
        onResetSessionClick = conversationMessagesViewModel::onResetSession,
        onUpdateConversationReadDate = messageComposerViewModel::updateConversationReadDate,
        onDropDownClick = {
            with(conversationInfoViewModel) {
                val route = when (val data = conversationInfoViewState.conversationDetailsData) {
                    is ConversationDetailsData.OneOne -> {
                        val botService = data.botService
                        when {
                            botService != null ->
                                ServiceDetailsScreenDestination(
                                    null,
                                    ServiceDetailsNavArgs.Id.BotServiceId(botService)
                                )

                            data.userType == UserTypeInfo.App ->
                                ServiceDetailsScreenDestination(
                                    null,
                                    ServiceDetailsNavArgs.Id.AppId(data.otherUserId)
                                )

                            else -> OtherUserProfileScreenDestination(data.otherUserId)
                        }
                    }

                    is ConversationDetailsData.Group ->
                        GroupConversationDetailsScreenDestination(conversationId)

                    is ConversationDetailsData.None -> {
                        /* do nothing */
                        null
                    }
                }

                route?.let {
                    navigator.navigate(NavigationCommand(it))
                }
            }
        },
        onBackButtonClick = {
            conversationScreenOnBackButtonClick(messageComposerViewModel, messageComposerStateHolder, navigator)
        },
        composerMessages = sendMessageViewModel.infoMessage,
        conversationMessages = conversationMessagesViewModel.infoMessage,
        threadRootMessage = conversationMessagesViewModel.conversationViewState.threadRootMessage,
        threadSummaryByRootMessageId = conversationMessagesViewModel.conversationViewState.threadSummaryByRootMessageId,
        isThreadMode = isThreadMode,
        onOpenThreadParentConversation = {
            conversationMessagesViewModel.threadRootMessageId?.let { rootMessageId ->
                navigator.navigate(
                    threadParentConversationNavigationCommand(
                        conversationId = conversationMessagesViewModel.conversationId,
                        rootMessageId = rootMessageId,
                    )
                )
            }
        },
        shareAssetExternally = conversationMessagesViewModel::shareAsset,
        shareAssetViaWire = { messageId ->
            conversationMessagesViewModel.prepareAssetForWireShare(messageId) { path, assetName ->
                navigator.navigate(
                    NavigationCommand(
                        ImportMediaScreenDestination(
                            ImportMediaNavArgs(
                                source = ImportSource.INTERNAL_SHARE,
                                internalAssetUriList = arrayListOf(context.fileShareUri(path, assetName))
                            )
                        ),
                        BackStackMode.UPDATE_EXISTED
                    )
                )
            }
        },
        onDownloadAssetClick = conversationMessagesViewModel::openOrFetchAsset,
        onOpenAssetClick = conversationMessagesViewModel::downloadAndOpenAsset,
        onReplyInThreadClick = if (REPLY_AS_THREAD_ENABLED) {
            { message -> conversationMessagesViewModel.startThreadFromMessage(message) }
        } else {
            { message -> messageComposerStateHolder.toReply(message) }
        },
        onOpenThreadClick = { threadId, rootMessageId, rootMessageSelfDeletionDurationMillis ->
            navigator.navigate(
                threadNavigationCommand(
                    conversationId = conversationMessagesViewModel.conversationId,
                    threadId = threadId,
                    rootMessageId = rootMessageId,
                    rootMessageSelfDeletionDurationMillis = rootMessageSelfDeletionDurationMillis,
                )
            )
        },
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
                    title = commonR.string.app_permission_dialog_title,
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
        openDrawingCanvas = {
            navigator.navigate(
                NavigationCommand(
                    DrawingCanvasScreenDestination(
                        DrawingCanvasNavArgs(
                            conversationName = conversationInfoViewModel.conversationInfoViewState.conversationName.asString(resources),
                            tempWritableUri = messageComposerViewModel.tempWritableImageUri
                        )
                    )
                )
            )
        },
        currentTimeInMillisFlow = conversationMessagesViewModel.currentTimeInMillisFlow,
        onReachedOldestMessage = {
            conversationMessagesViewModel.fetchOlderMessagesIfNeeded()
        },
        onAttachmentClick = messageAttachmentsViewModel::onAttachmentClicked,
        onAttachmentMenuClick = messageAttachmentsViewModel::onAttachmentMenuClicked,
        isFetchingOlderMessages = conversationMessagesViewModel.conversationViewState.isFetchingOlderMessages,
        hasMoreRemoteMessages = conversationMessagesViewModel.conversationViewState.hasMoreRemoteMessages,
        onVisibleRootMessagesChanged = conversationMessagesViewModel::observeThreadSummariesForVisibleRoots,
        isWireCellsEnabled = conversationInfoViewModel.conversationInfoViewState.isWireCellEnabled,
    )
    BackHandler { conversationScreenOnBackButtonClick(messageComposerViewModel, messageComposerStateHolder, navigator) }

    // Mark conversation as read when leaving, regardless of how the user exits
    // (back button, system gesture, navigation to another screen, etc.)
    DisposableEffect(messageComposerViewModel) {
        onDispose {
            messageComposerViewModel.onConversationClosed()
        }
    }

    ConversationDialogs(
        state = ConversationDialogsState(
            deleteMessage = conversationMessagesViewModel.deleteMessageDialogState,
            downloadedAsset = conversationMessagesViewModel.conversationViewState.downloadedAssetDialogState,
            assetTooLarge = sendMessageViewModel.assetTooLargeDialogState,
            visitLink = messageComposerViewModel.visitLinkDialogState,
            invalidLink = messageComposerViewModel.invalidLinkDialogState,
            permissionPermanentlyDenied = permissionPermanentlyDeniedDialogState,
            sureAboutMessaging = sendMessageViewModel.sureAboutMessagingDialogState,
            failedAttachment = messageAttachmentsViewModel.failedAttachmentDialogState,
            incompatibleFileName = messageAttachmentsViewModel.incompatibleFileNameDialogState,
        ),
        actions = ConversationDialogActions(
            deleteMessage = conversationMessagesViewModel::deleteMessage,
            saveFileToExternalStorage = conversationMessagesViewModel::downloadAssetExternally,
            openFileWithExternalApp = conversationMessagesViewModel::downloadAndOpenAsset,
            hideDownloadedAsset = conversationMessagesViewModel::hideOnAssetDownloadedDialog,
            onAssetPermissionPermanentlyDenied = {
                permissionPermanentlyDeniedDialogState.show(
                    PermissionPermanentlyDeniedDialogState.Visible(
                        title = commonR.string.app_permission_dialog_title,
                        description = R.string.save_permission_dialog_description,
                    )
                )
            },
            hideAssetTooLarge = sendMessageViewModel::hideAssetTooLargeError,
            hideVisitLink = messageComposerViewModel::hideVisitLinkDialog,
            hideInvalidLink = messageComposerViewModel::hideInvalidLinkError,
            hidePermissionPermanentlyDenied = permissionPermanentlyDeniedDialogState::dismiss,
            acceptSureAboutMessaging = sendMessageViewModel::acceptSureAboutSendingMessage,
            dismissSureAboutMessaging = sendMessageViewModel::dismissSureAboutSendingMessage,
            retryAttachmentUpload = messageAttachmentsViewModel::retryUpload,
            removeAttachment = messageAttachmentsViewModel::remove,
            dismissFailedAttachment = messageAttachmentsViewModel::onFailedAttachmentDialogDismissed,
            replaceFileNameAutomatically = messageAttachmentsViewModel::onReplaceFileNameAutomatically,
            dismissIncompatibleFileName = messageAttachmentsViewModel::onDismissIncompatibleFileNameDialog,
        ),
    )

    ConversationNavigationResults(
        groupDetailsScreenResultRecipient = groupDetailsScreenResultRecipient,
        mediaGalleryScreenResultRecipient = mediaGalleryScreenResultRecipient,
        imagePreviewScreenResultRecipient = imagePreviewScreenResultRecipient,
        drawingCanvasScreenResultRecipient = drawingCanvasScreenResultRecipient,
        resultNavigator = resultNavigator,
        navigator = navigator,
        conversationId = conversationMessagesViewModel.conversationId,
        messageComposerStateHolder = messageComposerStateHolder,
        getAndResetLastFullscreenMessage = conversationMessagesViewModel::getAndResetLastFullscreenMessage,
        toggleReaction = conversationMessagesViewModel::toggleReaction,
        trySendMessages = sendMessageViewModel::trySendMessages,
        trySendMessage = sendMessageViewModel::trySendMessage,
        onConversationDeleted = { alreadyDeletedByUser = true },
        handleGroupDetailsResult = !isThreadMode,
    )
}

internal fun threadNavigationCommand(
    conversationId: ConversationId,
    threadId: String,
    rootMessageId: String,
    rootMessageSelfDeletionDurationMillis: Long?,
) = NavigationCommand(
    ThreadConversationScreenDestination(
        ThreadConversationNavArgs(
            conversationId = conversationId,
            threadId = threadId,
            threadRootMessageId = rootMessageId,
            threadRootSelfDeletionDurationMillis = rootMessageSelfDeletionDurationMillis,
        )
    ),
    launchSingleTop = false,
)

internal fun threadParentConversationNavigationCommand(
    conversationId: ConversationId,
    rootMessageId: String,
) = NavigationCommand(
    ConversationScreenDestination(
        navArgs = ConversationNavArgs(
            conversationId = conversationId,
            searchedMessageId = rootMessageId,
        )
    ),
    BackStackMode.UPDATE_EXISTED,
)

private fun MessageBundle.withPrefetchedLinkPreview(
    linkPreview: com.wire.kalium.logic.data.message.linkpreview.MessageLinkPreview?
): MessageBundle {
    return when (this) {
        is ComposableMessageBundle.SendTextMessageBundle -> copy(prefetchedLinkPreview = linkPreview)
        else -> this
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
@Composable
private fun ConversationScreenContent(
    bannerMessage: UIText?,
    messageComposerViewState: MessageComposerViewState,
    conversationCallViewState: ConversationCallViewState,
    conversationInfoViewState: ConversationInfoViewState,
    conversationMessagesViewState: ConversationMessagesViewState,
    attachments: List<AttachmentDraftUi>,
    bottomSheetVisible: Boolean,
    onOpenProfile: (senderId: MessageSenderId) -> Unit,
    onMessageDetailsClick: (messageId: String, isSelfMessage: Boolean) -> Unit,
    onSendMessage: (MessageBundle) -> Unit,
    onPingOptionClicked: () -> Unit,
    onImagesPicked: (List<Uri>, Boolean) -> Unit,
    onAttachmentPicked: (UriAsset) -> Unit,
    onAudioRecorded: (UriAsset) -> Unit,
    onDeleteMessage: (String, Boolean) -> Unit,
    onAssetItemClicked: (String) -> Unit,
    onImageFullScreenMode: (UIMessage.Regular, Boolean, String?) -> Unit,
    onVideoClick: (localPath: String?, contentUrl: String?, fileName: String?) -> Unit,
    onStartCall: () -> Unit,
    onJoinCall: () -> Unit,
    onReactionClick: (messageId: String, reactionEmoji: String) -> Unit,
    onResetSessionClick: (senderUserId: UserId, clientId: String?) -> Unit,
    onUpdateConversationReadDate: (Instant) -> Unit,
    onDropDownClick: () -> Unit,
    onBackButtonClick: () -> Unit,
    composerMessages: SharedFlow<SnackBarMessage>,
    conversationMessages: SharedFlow<SnackBarMessage>,
    shareAssetExternally: (Context, messageId: String) -> Unit,
    shareAssetViaWire: (messageId: String) -> Unit,
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
    openDrawingCanvas: () -> Unit,
    onAttachmentClick: (AttachmentDraftUi) -> Unit,
    onAttachmentMenuClick: (AttachmentDraftUi) -> Unit,
    threadRootMessage: UIMessage.Regular? = null,
    threadSummaryByRootMessageId: PersistentMap<String, ThreadSummaryUi> = persistentMapOf(),
    isThreadMode: Boolean = false,
    onOpenThreadParentConversation: () -> Unit = {},
    onReplyInThreadClick: (UIMessage.Regular) -> Unit = {},
    onOpenThreadClick: (threadId: String, rootMessageId: String, rootMessageSelfDeletionDurationMillis: Long?) -> Unit = { _, _, _ -> },
    onVisibleRootMessagesChanged: (List<String>) -> Unit = {},
    currentTimeInMillisFlow: Flow<Long> = flow { },
    onReachedOldestMessage: () -> Unit = {},
    isFetchingOlderMessages: Boolean = false,
    hasMoreRemoteMessages: Boolean = false,
    isWireCellsEnabled: Boolean = false,
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    Box(modifier = Modifier) {
        // only here we will use normal Scaffold because of specific behaviour of message composer
        Scaffold(
            contentColor = if (IS_BUBBLE_UI_ENABLED) {
                colorsScheme().primary
            } else {
                colorsScheme().background
            },
            topBar = {
                Column {
                    ConversationScreenTopAppBar(
                        conversationInfoViewState = conversationInfoViewState,
                        onBackButtonClick = onBackButtonClick,
                        onDropDownClick = onDropDownClick,
                        isDropDownEnabled = !isThreadMode && conversationInfoViewState.hasUserPermissionToEdit,
                        onSearchButtonClick = { },
                        onPhoneButtonClick = onStartCall,
                        hasOngoingCall = conversationCallViewState.hasOngoingCall,
                        onJoinCallButtonClick = onJoinCall,
                        onAudioPermissionPermanentlyDenied = {
                            onPermissionPermanentlyDenied(ConversationActionPermissionType.CallAudio)
                        },
                        isInteractionEnabled = messageComposerViewState.interactionAvailability == InteractionAvailability.ENABLED,
                        isThreadMode = isThreadMode,
                        onOpenThreadParentConversation = onOpenThreadParentConversation,
                    )

                    HorizontalDivider(color = colorsScheme().outline)

                    ConversationBanner(
                        bannerMessage = bannerMessage,
                        spannedTexts = listOf(
                            stringResource(R.string.conversation_banner_federated),
                            stringResource(R.string.conversation_banner_externals),
                            stringResource(R.string.conversation_banner_guests),
                            stringResource(R.string.conversation_banner_services)
                        )
                    )
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
            contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Vertical),
            content = { internalPadding ->
                Box(
                    modifier = Modifier
                        .padding(internalPadding)
                        .consumeWindowInsets(internalPadding)
                ) {
                    ConversationMessageComposer(
                        conversationId = conversationInfoViewState.conversationId,
                        bottomSheetVisible = bottomSheetVisible,
                        playingAudioMessage = conversationMessagesViewState.playingAudioMessage,
                        assetStatuses = conversationMessagesViewState.assetStatuses,
                        lastUnreadMessageInstant = conversationMessagesViewState.firstUnreadInstant,
                        unreadEventCount = conversationMessagesViewState.firstUnreadEventIndex,
                        conversationDetailsData = conversationInfoViewState.conversationDetailsData,
                        selectedMessageId = conversationMessagesViewState.searchedMessageId,
                        messageComposerStateHolder = messageComposerStateHolder,
                        attachments = attachments,
                        messages = conversationMessagesViewState.messages,
                        threadSummaryByRootMessageId = threadSummaryByRootMessageId,
                        isThreadMode = isThreadMode,
                        onSendMessage = onSendMessage,
                        onPingOptionClicked = onPingOptionClicked,
                        onImagesPicked = onImagesPicked,
                        onAttachmentPicked = onAttachmentPicked,
                        onAudioRecorded = onAudioRecorded,
                        onAssetItemClicked = onAssetItemClicked,
                        onImageFullScreenMode = onImageFullScreenMode,
                        onVideoClick = onVideoClick,
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
                        onLocationClicked = conversationScreenState::showLocationSheet,
                        onClearMentionSearchResult = onClearMentionSearchResult,
                        onPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
                        tempWritableImageUri = tempWritableImageUri,
                        tempWritableVideoUri = tempWritableVideoUri,
                        onLinkClick = onLinkClick,
                        onNavigateToReplyOriginalMessage = onNavigateToReplyOriginalMessage,
                        onOpenThreadClick = onOpenThreadClick,
                        onVisibleRootMessagesChanged = onVisibleRootMessagesChanged,
                        currentTimeInMillisFlow = currentTimeInMillisFlow,
                        onReachedOldestMessage = onReachedOldestMessage,
                        openDrawingCanvas = openDrawingCanvas,
                        onAttachmentClick = onAttachmentClick,
                        onAttachmentMenuClick = onAttachmentMenuClick,
                        showHistoryLoadingIndicator = conversationInfoViewState.showHistoryLoadingIndicator,
                        isFetchingOlderMessages = conversationMessagesViewState.isFetchingOlderMessages,
                        hasMoreRemoteMessages = conversationMessagesViewState.hasMoreRemoteMessages,
                        isBubbleUiEnabled = IS_BUBBLE_UI_ENABLED,
                        isWireCellsEnabled = isWireCellsEnabled,
                    )

                    if (isThreadMode) {
                        ThreadContextHeader(
                            rootMessage = threadRootMessage,
                            onOpenParentConversation = onOpenThreadParentConversation,
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    }
                }
            }
        )

        MessageOptionsModalSheetLayout(
            conversationId = conversationInfoViewState.conversationId,
            isThreadMode = isThreadMode,
            sheetState = conversationScreenState.editSheetState,
            isNetworkAvailable = conversationMessagesViewState.isNetworkAvailable,
            onCopyClick = conversationScreenState::copyMessage,
            onDeleteClick = onDeleteMessage,
            onReactionClick = onReactionClick,
            onDetailsClick = onMessageDetailsClick,
            onReplyClick = onReplyInThreadClick,
            onEditClick = messageComposerStateHolder::toEdit,
            onShareAssetExternallyClick = { shareAssetExternally(context, it) },
            onShareAssetViaWireClick = shareAssetViaWire,
            onDownloadAssetClick = onDownloadAssetClick,
            onOpenAssetClick = onOpenAssetClick,
        )

        SelfDeletionOptionsModalSheetLayout(
            sheetState = conversationScreenState.selfDeletingSheetState,
            onNewSelfDeletingMessagesStatus = onNewSelfDeletingMessagesStatus
        )
        LocationPickerComponent(
            sheetState = conversationScreenState.locationSheetState,
            onLocationPicked = {
                onSendMessage(
                    ComposableMessageBundle.LocationBundle(
                        conversationInfoViewState.conversationId,
                        it.getFormattedAddress(),
                        it.location
                    )
                )
            }
        )

        SnackBarMessage(composerMessages, conversationMessages)
    }
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
        draftMessageComposition = messageCompositionState.value,
        onClearDraft = {},
        onSaveDraft = {},
        onMessageTextUpdate = {},
        onTypingEvent = {},
        onSearchMentionQueryChanged = {},
        onClearMentionSearchResult = {},
    )
    ConversationScreenContent(
        bannerMessage = null,
        bottomSheetVisible = false,
        messageComposerViewState = messageComposerViewState.value,
        conversationCallViewState = ConversationCallViewState(),
        conversationInfoViewState = ConversationInfoViewState(
            conversationId = conversationId,
            conversationName = UIText.DynamicString("Some test conversation")
        ),
        conversationMessagesViewState = ConversationMessagesViewState(),
        attachments = emptyList(),
        onOpenProfile = { },
        onMessageDetailsClick = { _, _ -> },
        onSendMessage = { },
        onPingOptionClicked = { },
        onDeleteMessage = { _, _ -> },
        onAssetItemClicked = { },
        onImageFullScreenMode = { _, _, _ -> },
        onVideoClick = { _, _, _ -> },
        onStartCall = { },
        onJoinCall = { },
        onReactionClick = { _, _ -> },
        onResetSessionClick = { _, _ -> },
        onUpdateConversationReadDate = { },
        onDropDownClick = { },
        onBackButtonClick = {},
        composerMessages = MutableStateFlow(ConversationSnackbarMessages.ErrorDownloadingAsset),
        conversationMessages = MutableStateFlow(ConversationSnackbarMessages.ErrorDownloadingAsset),
        shareAssetExternally = { _, _ -> },
        shareAssetViaWire = {},
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
        openDrawingCanvas = {},
        onImagesPicked = { _, _ -> },
        onAttachmentClick = {},
        onAttachmentMenuClick = {},
        onAttachmentPicked = {},
        onAudioRecorded = {},
        isFetchingOlderMessages = false,
        hasMoreRemoteMessages = false,
    )
}
