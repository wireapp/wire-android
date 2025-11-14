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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.ramcosta.composedestinations.result.NavResult.Canceled
import com.ramcosta.composedestinations.result.NavResult.Value
import com.ramcosta.composedestinations.result.OpenResultRecipient
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.ramcosta.composedestinations.result.ResultRecipient
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.feature.analytics.AnonymousAnalyticsManagerImpl
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.feature.sketch.destinations.DrawingCanvasScreenDestination
import com.wire.android.feature.sketch.model.DrawingCanvasNavArgs
import com.wire.android.feature.sketch.model.DrawingCanvasNavBackArgs
import com.wire.android.mapper.MessageDateTimeGroup
import com.wire.android.media.audiomessage.PlayingAudioMessage
import com.wire.android.model.SnackBarMessage
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.ui.calling.getOutgoingCallIntent
import com.wire.android.ui.calling.ongoing.getOngoingCallIntent
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.PageLoadingIndicator
import com.wire.android.ui.common.attachmentdraft.model.AttachmentDraftUi
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dialogs.ConfirmSendingPingDialog
import com.wire.android.ui.common.dialogs.InvalidLinkDialog
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.dialogs.SureAboutMessagingInDegradedConversationDialog
import com.wire.android.ui.common.dialogs.VisitLinkDialog
import com.wire.android.ui.common.dialogs.calling.CallingFeatureActivatedDialog
import com.wire.android.ui.common.dialogs.calling.CallingFeatureUnavailableDialog
import com.wire.android.ui.common.dialogs.calling.CallingFeatureUnavailableTeamAdminDialog
import com.wire.android.ui.common.dialogs.calling.CallingFeatureUnavailableTeamMemberDialog
import com.wire.android.ui.common.dialogs.calling.ConfirmStartCallDialog
import com.wire.android.ui.common.dialogs.calling.JoinAnywayDialog
import com.wire.android.ui.common.dialogs.calling.OngoingActiveCallDialog
import com.wire.android.ui.common.dialogs.calling.SureAboutCallingInDegradedConversationDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.error.CoreFailureErrorDialog
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.snackbar.SwipeableSnackbar
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.GroupConversationDetailsScreenDestination
import com.wire.android.ui.destinations.ImagesPreviewScreenDestination
import com.wire.android.ui.destinations.MediaGalleryScreenDestination
import com.wire.android.ui.destinations.MessageDetailsScreenDestination
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import com.wire.android.ui.destinations.SelfUserProfileScreenDestination
import com.wire.android.ui.emoji.EmojiPickerBottomSheet
import com.wire.android.ui.home.conversations.AuthorHeaderHelper.rememberShouldHaveSmallBottomPadding
import com.wire.android.ui.home.conversations.AuthorHeaderHelper.rememberShouldShowHeader
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages.OnFileDownloaded
import com.wire.android.ui.home.conversations.attachment.MessageAttachmentsViewModel
import com.wire.android.ui.home.conversations.banner.ConversationBanner
import com.wire.android.ui.home.conversations.banner.ConversationBannerViewModel
import com.wire.android.ui.home.conversations.call.ConversationCallViewActions
import com.wire.android.ui.home.conversations.call.ConversationCallViewModel
import com.wire.android.ui.home.conversations.call.ConversationCallViewState
import com.wire.android.ui.home.conversations.composer.MessageComposerViewModel
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialog
import com.wire.android.ui.home.conversations.delete.DeleteMessageDialogState
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsNavBackArgs
import com.wire.android.ui.home.conversations.edit.MessageOptionsModalSheetLayout
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.info.ConversationInfoViewModel
import com.wire.android.ui.home.conversations.info.ConversationInfoViewState
import com.wire.android.ui.home.conversations.media.preview.ImagesPreviewNavBackArgs
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewModel
import com.wire.android.ui.home.conversations.messages.ConversationMessagesViewState
import com.wire.android.ui.home.conversations.messages.draft.MessageDraftViewModel
import com.wire.android.ui.home.conversations.messages.item.MessageClickActions
import com.wire.android.ui.home.conversations.messages.item.MessageContainerItem
import com.wire.android.ui.home.conversations.messages.item.SwipeableMessageConfiguration
import com.wire.android.ui.home.conversations.migration.ConversationMigrationViewModel
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionOptionsModalSheetLayout
import com.wire.android.ui.home.conversations.sendmessage.SendMessageViewModel
import com.wire.android.ui.home.gallery.MediaGalleryActionType
import com.wire.android.ui.home.gallery.MediaGalleryNavBackArgs
import com.wire.android.ui.home.messagecomposer.MessageComposer
import com.wire.android.ui.home.messagecomposer.location.LocationPickerComponent
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
import com.wire.android.util.DateAndTimeParsers
import com.wire.android.util.normalizeLink
import com.wire.android.util.serverDate
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.openDownloadFolder
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.Conversation.TypingIndicatorMode
import com.wire.kalium.logic.data.conversation.InteractionAvailability
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.MessageAssetStatus
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.isInternal
import com.wire.kalium.logic.data.user.type.isTeamAdmin
import com.wire.kalium.logic.feature.call.usecase.ConferenceCallingResult
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
@WireDestination(
    navArgsDelegate = ConversationNavArgs::class
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
    conversationInfoViewModel: ConversationInfoViewModel = hiltViewModel(),
    conversationBannerViewModel: ConversationBannerViewModel = hiltViewModel(),
    conversationCallViewModel: ConversationCallViewModel = hiltViewModel(),
    conversationMessagesViewModel: ConversationMessagesViewModel = hiltViewModel(),
    messageComposerViewModel: MessageComposerViewModel = hiltViewModel(),
    sendMessageViewModel: SendMessageViewModel = hiltViewModel(),
    conversationMigrationViewModel: ConversationMigrationViewModel = hiltViewModel(),
    messageDraftViewModel: MessageDraftViewModel = hiltViewModel(),
    messageAttachmentsViewModel: MessageAttachmentsViewModel = hiltViewModel(),
) {
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val resources = LocalContext.current.resources
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

    val context = LocalContext.current

    LaunchedEffect(conversationScreenState.isAnySheetVisible) {
        with(messageComposerStateHolder) {
            if (conversationScreenState.isAnySheetVisible) {
                messageCompositionInputStateHolder.showAttachments(false)
            }
        }
    }

    LaunchedEffect(alreadyDeletedByUser) {
        if (!alreadyDeletedByUser) {
            conversationInfoViewModel.observeConversationDetails()
        }
    }
    LaunchedEffect(conversationInfoViewModel.conversationInfoViewState.notFound) {
        if (conversationInfoViewModel.conversationInfoViewState.notFound) navigator.navigateBack()
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

    HandleActions(conversationCallViewModel.actions) { action ->
        when (action) {
            is ConversationCallViewActions.InitiatedCall -> {
                context.startActivity(getOutgoingCallIntent(context, action.conversationId.toString(), action.userId.toString()))
                AnonymousAnalyticsManagerImpl.sendEvent(event = AnalyticsEvent.CallInitiated)
            }

            is ConversationCallViewActions.JoinedCall -> {
                context.startActivity(getOngoingCallIntent(context, action.conversationId.toString(), action.userId.toString()))
                AnonymousAnalyticsManagerImpl.sendEvent(event = AnalyticsEvent.CallJoined)
            }
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

    with(conversationCallViewModel) {
        if (conversationCallViewState.shouldShowJoinAnywayDialog) {
            appLogger.i("showing showJoinAnywayDialog..")
            JoinAnywayDialog(
                onDismiss = ::dismissJoinCallAnywayDialog,
                onConfirm = ::joinAnyway,
            )
        }
    }

    when (showDialog.value) {
        ConversationScreenDialogType.ONGOING_ACTIVE_CALL -> {
            OngoingActiveCallDialog(
                onInitiateCallAnyway = {
                    conversationCallViewModel.initiateCall()
                    showDialog.value = ConversationScreenDialogType.NONE
                },
                onDialogDismiss = {
                    showDialog.value = ConversationScreenDialogType.NONE
                }
            )
        }

        ConversationScreenDialogType.NO_CONNECTIVITY -> {
            CoreFailureErrorDialog(coreFailure = NetworkFailure.NoNetworkConnection(null)) {
                showDialog.value = ConversationScreenDialogType.NONE
            }
        }

        ConversationScreenDialogType.CALL_CONFIRMATION -> {
            ConfirmStartCallDialog(
                participantsCount = conversationCallViewModel.conversationCallViewState.participantsCount,
                onConfirm = {
                    startCallIfPossible(
                        conversationCallViewModel,
                        showDialog,
                        coroutineScope,
                        conversationInfoViewModel.conversationInfoViewState.conversationType,
                        onOpenOngoingCallScreen = { conversationId, userId ->
                            getOngoingCallIntent(context, conversationId.toString(), userId.toString()).run {
                                context.startActivity(this)
                            }
                        }
                    )
                },
                onDialogDismiss = {
                    showDialog.value = ConversationScreenDialogType.NONE
                }
            )
        }

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

        ConversationScreenDialogType.CALLING_FEATURE_UNAVAILABLE -> {
            CallingFeatureUnavailableDialog {
                showDialog.value = ConversationScreenDialogType.NONE
            }
        }

        ConversationScreenDialogType.CALLING_FEATURE_UNAVAILABLE_TEAM_MEMBER -> {
            CallingFeatureUnavailableTeamMemberDialog {
                showDialog.value = ConversationScreenDialogType.NONE
            }
        }

        ConversationScreenDialogType.CALLING_FEATURE_UNAVAILABLE_TEAM_ADMIN -> {
            CallingFeatureUnavailableTeamAdminDialog(
                onUpgradeAction = uriHandler::openUri,
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

        ConversationScreenDialogType.VERIFICATION_DEGRADED -> {
            SureAboutCallingInDegradedConversationDialog(
                callAnyway = {
                    conversationCallViewModel.onApplyConversationDegradation()
                    startCallIfPossible(
                        conversationCallViewModel,
                        showDialog,
                        coroutineScope,
                        conversationInfoViewModel.conversationInfoViewState.conversationType,
                        onOpenOngoingCallScreen = { conversationId, userId ->
                            getOngoingCallIntent(context, conversationId.toString(), userId.toString()).run {
                                context.startActivity(this)
                            }
                        }
                    )
                },
                onDialogDismiss = { showDialog.value = ConversationScreenDialogType.NONE }
            )
        }

        ConversationScreenDialogType.NONE -> {}
    }

    ConversationScreen(
        bannerMessage = conversationBannerViewModel.bannerState,
        messageComposerViewState = messageComposerViewState.value,
        bottomSheetVisible = conversationScreenState.isAnySheetVisible,
        conversationCallViewState = conversationCallViewModel.conversationCallViewState,
        conversationInfoViewState = conversationInfoViewModel.conversationInfoViewState,
        conversationMessagesViewState = conversationMessagesViewModel.conversationViewState,
        attachments = messageAttachmentsViewModel.attachments,
        onOpenProfile = {
            with(conversationInfoViewModel) {
                val (mentionUserId: UserId, isSelfUser: Boolean) = mentionedUserData(it)
                if (isSelfUser) {
                    navigator.navigate(NavigationCommand(SelfUserProfileScreenDestination))
                } else {
                    (conversationInfoViewState.conversationDetailsData as? ConversationDetailsData.Group)?.conversationId.let {
                    navigator.navigate(NavigationCommand(OtherUserProfileScreenDestination(mentionUserId, it)))
                }
                }
            }
        },
        onMessageDetailsClick = { messageId: String, isSelfMessage: Boolean ->
            appLogger.i("[ConversationScreen][openMessageDetails] - isSelfMessage: $isSelfMessage")
            navigator.navigate(
                NavigationCommand(MessageDetailsScreenDestination(conversationInfoViewModel.conversationId, messageId, isSelfMessage))
            )
        },
        onSendMessage = { sendMessageViewModel.trySendMessage(it) },
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
            if (conversationInfoViewModel.conversationInfoViewState.isWireCellEnabled) {
                messageAttachmentsViewModel.onFilesSelected(listOf(it.uri))
                messageComposerStateHolder.messageCompositionInputStateHolder.showAttachments(false)
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
        onStartCall = {
            startCallIfPossible(
                conversationCallViewModel,
                showDialog,
                coroutineScope,
                conversationInfoViewModel.conversationInfoViewState.conversationType,
                onOpenOngoingCallScreen = { conversationId, userId ->
                    getOngoingCallIntent(context, conversationId.toString(), userId.toString()).run {
                        context.startActivity(this)
                    }
                }
            )
        },
        onJoinCall = conversationCallViewModel::joinOngoingCall,
        onReactionClick = { messageId, emoji ->
            conversationMessagesViewModel.toggleReaction(messageId, emoji)
        },
        onResetSessionClick = conversationMessagesViewModel::onResetSession,
        onUpdateConversationReadDate = messageComposerViewModel::updateConversationReadDate,
        onDropDownClick = {
            with(conversationInfoViewModel) {
                when (val data = conversationInfoViewState.conversationDetailsData) {
                    is ConversationDetailsData.OneOne ->
                        navigator.navigate(NavigationCommand(OtherUserProfileScreenDestination(data.otherUserId)))

                    is ConversationDetailsData.Group ->
                        navigator.navigate(NavigationCommand(GroupConversationDetailsScreenDestination(conversationId)))

                    is ConversationDetailsData.None -> {
                        /* do nothing */
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
        onDownloadAssetClick = conversationMessagesViewModel::openOrFetchAsset,
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
        onAttachmentClick = messageAttachmentsViewModel::onAttachmentClicked,
        onAttachmentMenuClick = messageAttachmentsViewModel::onAttachmentMenuClicked,
    )
    BackHandler { conversationScreenOnBackButtonClick(messageComposerViewModel, messageComposerStateHolder, navigator) }
    DeleteMessageDialog(
        dialogState = conversationMessagesViewModel.deleteMessageDialogState,
        deleteMessage = conversationMessagesViewModel::deleteMessage,
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

    FailedAttachmentDialog(
        state = messageAttachmentsViewModel.failedAttachmentDialogState,
        onRetryUpload = {
            messageAttachmentsViewModel.retryUpload()
        },
        onRemoveAttachment = {
            messageAttachmentsViewModel.remove()
        },
        onDismiss = messageAttachmentsViewModel::onFailedAttachmentDialogDismissed,
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
                sendMessageViewModel.trySendMessages(
                    result.value.pendingBundles.map { assetBundle ->
                        ComposableMessageBundle.AttachmentPickedBundle(
                            conversationId = conversationMessagesViewModel.conversationId,
                            assetBundle = assetBundle
                        )
                    }
                )
            }
        }
    }

    drawingCanvasScreenResultRecipient.onNavResult { result ->
        when (result) {
            Canceled -> {}
            is Value -> {
                sendMessageViewModel.trySendMessage(
                    ComposableMessageBundle.UriPickedBundle(
                        conversationId = conversationMessagesViewModel.conversationId,
                        attachmentUri = UriAsset(result.value.uri)
                    )
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
    conversationCallViewModel: ConversationCallViewModel,
    showDialog: MutableState<ConversationScreenDialogType>,
    coroutineScope: CoroutineScope,
    conversationType: Conversation.Type,
    onOpenOngoingCallScreen: (ConversationId, UserId) -> Unit
) {
    coroutineScope.launch {
        if (!conversationCallViewModel.hasStableConnectivity()) {
            showDialog.value = ConversationScreenDialogType.NO_CONNECTIVITY
        } else if (conversationCallViewModel.shouldInformAboutVerification.value) {
            showDialog.value = ConversationScreenDialogType.VERIFICATION_DEGRADED
        } else {
            val dialogValue = when (conversationCallViewModel.isConferenceCallingEnabled(conversationType)) {
                ConferenceCallingResult.Enabled -> {
                    if (
                        showDialog.value != ConversationScreenDialogType.CALL_CONFIRMATION &&
                        conversationCallViewModel.conversationCallViewState.participantsCount > MAX_GROUP_SIZE_FOR_CALL_WITHOUT_ALERT
                    ) {
                        ConversationScreenDialogType.CALL_CONFIRMATION
                    } else {
                        conversationCallViewModel.initiateCall()
                        ConversationScreenDialogType.NONE
                    }
                }

                ConferenceCallingResult.Disabled.Established -> {
                    onOpenOngoingCallScreen(conversationCallViewModel.conversationId, conversationCallViewModel.currentAccount)
                    AnonymousAnalyticsManagerImpl.sendEvent(event = AnalyticsEvent.CallJoined)
                    ConversationScreenDialogType.NONE
                }

                ConferenceCallingResult.Disabled.OngoingCall -> ConversationScreenDialogType.ONGOING_ACTIVE_CALL
                ConferenceCallingResult.Disabled.Unavailable -> {
                    val type = conversationCallViewModel.selfTeamRole.value
                    when {
                        type.isInternal() -> ConversationScreenDialogType.CALLING_FEATURE_UNAVAILABLE_TEAM_MEMBER
                        type.isTeamAdmin() -> ConversationScreenDialogType.CALLING_FEATURE_UNAVAILABLE_TEAM_ADMIN
                        else -> ConversationScreenDialogType.CALLING_FEATURE_UNAVAILABLE
                    }
                }

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
    attachments: List<AttachmentDraftUi>,
    bottomSheetVisible: Boolean,
    onOpenProfile: (String) -> Unit,
    onMessageDetailsClick: (messageId: String, isSelfMessage: Boolean) -> Unit,
    onSendMessage: (MessageBundle) -> Unit,
    onPingOptionClicked: () -> Unit,
    onImagesPicked: (List<Uri>, Boolean) -> Unit,
    onAttachmentPicked: (UriAsset) -> Unit,
    onAudioRecorded: (UriAsset) -> Unit,
    onDeleteMessage: (String, Boolean) -> Unit,
    onAssetItemClicked: (String) -> Unit,
    onImageFullScreenMode: (UIMessage.Regular, Boolean, String?) -> Unit,
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
    openDrawingCanvas: () -> Unit,
    onAttachmentClick: (AttachmentDraftUi) -> Unit,
    onAttachmentMenuClick: (AttachmentDraftUi) -> Unit,
    currentTimeInMillisFlow: Flow<Long> = flow { },
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    Box(modifier = Modifier) {
        // only here we will use normal Scaffold because of specific behaviour of message composer
        Scaffold(
            contentColor = if (conversationInfoViewState.isBubbleUiEnabled) {
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
            content = { internalPadding ->
                Box(
                    modifier = Modifier
                        .padding(internalPadding)
                        .consumeWindowInsets(internalPadding)
                ) {
                    ConversationScreenContent(
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
                        onSendMessage = onSendMessage,
                        onPingOptionClicked = onPingOptionClicked,
                        onImagesPicked = onImagesPicked,
                        onAttachmentPicked = onAttachmentPicked,
                        onAudioRecorded = onAudioRecorded,
                        onAssetItemClicked = onAssetItemClicked,
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
                        onLocationClicked = conversationScreenState::showLocationSheet,
                        onClearMentionSearchResult = onClearMentionSearchResult,
                        onPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
                        tempWritableImageUri = tempWritableImageUri,
                        tempWritableVideoUri = tempWritableVideoUri,
                        onLinkClick = onLinkClick,
                        onNavigateToReplyOriginalMessage = onNavigateToReplyOriginalMessage,
                        currentTimeInMillisFlow = currentTimeInMillisFlow,
                        openDrawingCanvas = openDrawingCanvas,
                        onAttachmentClick = onAttachmentClick,
                        onAttachmentMenuClick = onAttachmentMenuClick,
                        showHistoryLoadingIndicator = conversationInfoViewState.showHistoryLoadingIndicator,
                        isBubbleUiEnabled = conversationInfoViewState.isBubbleUiEnabled,
                    )
                }
            }
        )

        MessageOptionsModalSheetLayout(
            conversationId = conversationInfoViewState.conversationId,
            sheetState = conversationScreenState.editSheetState,
            onCopyClick = conversationScreenState::copyMessage,
            onDeleteClick = onDeleteMessage,
            onReactionClick = onReactionClick,
            onDetailsClick = onMessageDetailsClick,
            onReplyClick = messageComposerStateHolder::toReply,
            onEditClick = messageComposerStateHolder::toEdit,
            onShareAssetClick = { shareAsset(context, it) },
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

@Suppress("LongParameterList")
@Composable
private fun ConversationScreenContent(
    conversationId: ConversationId,
    bottomSheetVisible: Boolean,
    lastUnreadMessageInstant: Instant?,
    unreadEventCount: Int,
    playingAudioMessage: PlayingAudioMessage,
    assetStatuses: PersistentMap<String, MessageAssetStatus>,
    selectedMessageId: String?,
    messageComposerStateHolder: MessageComposerStateHolder,
    attachments: List<AttachmentDraftUi>,
    messages: Flow<PagingData<UIMessage>>,
    onSendMessage: (MessageBundle) -> Unit,
    onPingOptionClicked: () -> Unit,
    onImagesPicked: (List<Uri>, Boolean) -> Unit,
    onAttachmentPicked: (UriAsset) -> Unit,
    onAudioRecorded: (UriAsset) -> Unit,
    onAssetItemClicked: (String) -> Unit,
    onImageFullScreenMode: (UIMessage.Regular, Boolean, String?) -> Unit,
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
    onLocationClicked: () -> Unit,
    onPermissionPermanentlyDenied: (type: ConversationActionPermissionType) -> Unit,
    tempWritableImageUri: Uri?,
    tempWritableVideoUri: Uri?,
    onLinkClick: (String) -> Unit,
    onNavigateToReplyOriginalMessage: (UIMessage) -> Unit,
    openDrawingCanvas: () -> Unit,
    onAttachmentClick: (AttachmentDraftUi) -> Unit,
    onAttachmentMenuClick: (AttachmentDraftUi) -> Unit,
    currentTimeInMillisFlow: Flow<Long> = flow {},
    showHistoryLoadingIndicator: Boolean = false,
    isBubbleUiEnabled: Boolean = false
) {
    val lazyPagingMessages = messages.collectAsLazyPagingItems()

    val lazyListState = rememberSaveable(unreadEventCount, lazyPagingMessages, saver = LazyListState.Saver) {
        LazyListState(unreadEventCount)
    }

    val emojiPickerState = rememberWireModalSheetState<String>(skipPartiallyExpanded = false)

    MessageComposer(
        conversationId = conversationId,
        bottomSheetVisible = bottomSheetVisible,
        messageComposerStateHolder = messageComposerStateHolder,
        attachments = attachments,
        messageListContent = {
            MessageList(
                lazyPagingMessages = lazyPagingMessages,
                lazyListState = lazyListState,
                lastUnreadMessageInstant = lastUnreadMessageInstant,
                playingAudioMessage = playingAudioMessage,
                assetStatuses = assetStatuses,
                onUpdateConversationReadDate = onUpdateConversationReadDate,
                clickActions = MessageClickActions.Content(
                    onFullMessageLongClicked = onShowEditingOptions,
                    onProfileClicked = onOpenProfile,
                    onReactionClicked = onReactionClicked,
                    onAssetClicked = onAssetItemClicked,
                    onImageClicked = onImageFullScreenMode,
                    onLinkClicked = onLinkClick,
                    onReplyClicked = onNavigateToReplyOriginalMessage,
                    onResetSessionClicked = onResetSessionClicked,
                    onFailedMessageRetryClicked = onFailedMessageRetryClicked,
                    onFailedMessageCancelClicked = onFailedMessageCancelClicked,
                ),
                onSelfDeletingMessageRead = onSelfDeletingMessageRead,
                onSwipedToReply = onSwipedToReply,
                onSwipedToReact = { message ->
                    emojiPickerState.show(message.header.messageId)
                },
                conversationDetailsData = conversationDetailsData,
                selectedMessageId = selectedMessageId,
                interactionAvailability = messageComposerStateHolder.messageComposerViewState.value.interactionAvailability,
                currentTimeInMillisFlow = currentTimeInMillisFlow,
                showHistoryLoadingIndicator = showHistoryLoadingIndicator,
                isBubbleUiEnabled = isBubbleUiEnabled,
            )
        },
        onChangeSelfDeletionClicked = onChangeSelfDeletionClicked,
        onLocationClicked = onLocationClicked,
        onClearMentionSearchResult = onClearMentionSearchResult,
        onSendMessageBundle = onSendMessage,
        onPingOptionClicked = onPingOptionClicked,
        onPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
        tempWritableVideoUri = tempWritableVideoUri,
        tempWritableImageUri = tempWritableImageUri,
        onImagesPicked = onImagesPicked,
        openDrawingCanvas = openDrawingCanvas,
        onAttachmentClick = onAttachmentClick,
        onAttachmentMenuClick = onAttachmentMenuClick,
        onAttachmentPicked = onAttachmentPicked,
        onAudioRecorded = onAudioRecorded,
    )

    EmojiPickerBottomSheet(
        sheetState = emojiPickerState,
        onEmojiSelected = { emoji, messageId ->
            emojiPickerState.hide()
            onReactionClicked(messageId, emoji)
        },
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
    playingAudioMessage: PlayingAudioMessage,
    assetStatuses: PersistentMap<String, MessageAssetStatus>,
    onUpdateConversationReadDate: (String) -> Unit,
    onSwipedToReply: (UIMessage.Regular) -> Unit,
    onSwipedToReact: (UIMessage.Regular) -> Unit,
    onSelfDeletingMessageRead: (UIMessage) -> Unit,
    conversationDetailsData: ConversationDetailsData,
    selectedMessageId: String?,
    interactionAvailability: InteractionAvailability,
    clickActions: MessageClickActions.Content,
    modifier: Modifier = Modifier,
    currentTimeInMillisFlow: Flow<Long> = flow { },
    showHistoryLoadingIndicator: Boolean = false,
    isBubbleUiEnabled: Boolean = false
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
            .background(
                color = if (isBubbleUiEnabled) {
                    colorsScheme().bubblesBackground
                } else {
                    colorsScheme().surfaceContainerLow
                }
            ),
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

                    val showAuthor = if (!isBubbleUiEnabled || conversationDetailsData is ConversationDetailsData.Group) {
                        rememberShouldShowHeader(index, message, lazyPagingMessages)
                    } else {
                        false
                    }

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
                                    now = currentTime,
                                    isBubbleUiEnabled = isBubbleUiEnabled
                                )
                            }
                        }
                    }

                    val swipeableConfiguration = remember(message) {
                        if (message is UIMessage.Regular && message.isSwipeable) {
                            SwipeableMessageConfiguration.Swipeable(
                                onSwipedRight = { onSwipedToReply(message) }.takeIf { message.isReplyable },
                                onSwipedLeft = { onSwipedToReact(message) }.takeIf { message.isReactionAllowed },
                            )
                        } else {
                            SwipeableMessageConfiguration.NotSwipeable
                        }
                    }

                    MessageContainerItem(
                        message = message,
                        conversationDetailsData = conversationDetailsData,
                        showAuthor = showAuthor,
                        useSmallBottomPadding = useSmallBottomPadding,
                        assetStatus = assetStatuses[message.header.messageId]?.transferStatus,
                        clickActions = clickActions,
                        swipeableMessageConfiguration = swipeableConfiguration,
                        onSelfDeletingMessageRead = onSelfDeletingMessageRead,
                        isSelectedMessage = (message.header.messageId == selectedMessageId),
                        failureInteractionAvailable = interactionAvailability == InteractionAvailability.ENABLED,
                        isBubbleUiEnabled = isBubbleUiEnabled
                    )

                    val isTheOnlyItem = index == 0 && lazyPagingMessages.itemCount == 1
                    val isTheLastItem = (index + 1) == lazyPagingMessages.itemCount
                    if (isTheOnlyItem || isTheLastItem) {
                        val currentGroup = message.header.messageTime.getFormattedDateGroup(now = currentTime)
                        message.header.messageTime.utcISO.serverDate()?.let { serverDate ->
                            MessageGroupDateTime(
                                messageDateTime = serverDate,
                                messageDateTimeGroup = currentGroup,
                                now = currentTime,
                                isBubbleUiEnabled = isBubbleUiEnabled
                            )
                        }
                    }
                }
                // reverse layout, so prepend needs to be added after all messages to be displayed at the top of the list
                if (showHistoryLoadingIndicator && lazyPagingMessages.itemCount > 0) {
                    item(
                        key = "prepend_loading_indicator",
                        contentType = "prepend_loading_indicator",
                        content = {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(dimensions().spacing16x),
                            ) {
                                var allMessagesPrepended by remember { mutableStateOf(false) }
                                LaunchedEffect(lazyPagingMessages.loadState) {
                                    // When the list is being refreshed, the load state for prepend is cleared so the app doesn't know if
                                    // the end of pagination is reached or not for prepend until the refresh is done, so we don't want to
                                    // update the allMessagesPrepended state while refreshing and in that case keep the last updated state,
                                    // otherwise the indicator will flicker while refreshing.
                                    if (lazyPagingMessages.loadState.refresh is LoadState.NotLoading) {
                                        allMessagesPrepended = lazyPagingMessages.loadState.prepend.endOfPaginationReached
                                    }
                                }

                                val (text, prefixIconResId) = when (allMessagesPrepended) {
                                    true -> stringResource(R.string.conversation_history_loaded) to null
                                    false -> stringResource(R.string.conversation_history_loading) to R.drawable.ic_undo
                                }
                                PageLoadingIndicator(
                                    text = text,
                                    prefixIconResId = prefixIconResId,
                                )
                            }
                        }
                    )
                }
            }
            JumpToPlayingAudioButton(
                lazyListState = lazyListState,
                lazyPagingMessages = lazyPagingMessages,
                playingAudioMessage = playingAudioMessage
            )
            JumpToLastMessageButton(lazyListState = lazyListState)
        }
    )
}

@Composable
private fun MessageGroupDateTime(
    now: Long,
    messageDateTime: Date,
    messageDateTimeGroup: MessageDateTimeGroup?,
    isBubbleUiEnabled: Boolean
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
                MessageDateTimeGroup.Daily.Type.Today,
                MessageDateTimeGroup.Daily.Type.Yesterday ->
                    DateUtils.getRelativeTimeSpanString(
                        messageDateTime.time,
                        now,
                        DateUtils.DAY_IN_MILLIS,
                        0
                    ).toString()

                MessageDateTimeGroup.Daily.Type.WithinWeek -> DateUtils.formatDateTime(
                    context,
                    messageDateTime.time,
                    DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE
                )

                MessageDateTimeGroup.Daily.Type.NotWithinWeekButSameYear -> DateUtils.formatDateTime(
                    context,
                    messageDateTime.time,
                    DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE
                )

                MessageDateTimeGroup.Daily.Type.Other -> DateUtils.formatDateTime(
                    context,
                    messageDateTime.time,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
                )
            }
        }

        null -> ""
    }

    if (isBubbleUiEnabled) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(dimensions().spacing16x),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1F), color = colorsScheme().outline)
            HorizontalSpace.x4()
            Text(
                text = timeString.uppercase(Locale.getDefault()),
                maxLines = 1,
                color = colorsScheme().onBackground,
                style = MaterialTheme.wireTypography.label02,
            )
            HorizontalSpace.x4()
            HorizontalDivider(modifier = Modifier.weight(1F), color = colorsScheme().outline)
        }
    } else {
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
    val bottomPadding = dimensions().typingIndicatorHeight + dimensions().spacing8x
    val bottomPaddingPx = with(LocalDensity.current) { bottomPadding.toPx() }
    val showButton by remember(lazyListState, bottomPaddingPx) {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > bottomPaddingPx
        }
    }
    AnimatedVisibility(
        modifier = modifier.padding(bottom = bottomPadding, end = dimensions().spacing16x),
        visible = showButton,
        enter = scaleIn(),
        exit = scaleOut(),
    ) {
        SmallFloatingActionButton(
            onClick = { coroutineScope.launch { lazyListState.animateScrollToItem(0) } },
            containerColor = MaterialTheme.wireColorScheme.secondaryText,
            contentColor = MaterialTheme.wireColorScheme.onPrimaryButtonEnabled,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(dimensions().spacing0x),
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(id = R.string.content_description_jump_to_last_message),
                Modifier.size(dimensions().spacing32x)
            )
        }
    }
}

@Composable
fun BoxScope.JumpToPlayingAudioButton(
    lazyListState: LazyListState,
    playingAudioMessage: PlayingAudioMessage,
    lazyPagingMessages: LazyPagingItems<UIMessage>,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
) {
    if (playingAudioMessage is PlayingAudioMessage.Some && playingAudioMessage.state.isPlaying()) {
        val indexOfPlayedMessage = lazyPagingMessages.itemSnapshotList
            .indexOfFirst { playingAudioMessage.messageId == it?.header?.messageId }

        if (indexOfPlayedMessage < 0) return

        val firstVisibleIndex = lazyListState.firstVisibleItemIndex
        val lastVisibleIndex = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: firstVisibleIndex

        if (indexOfPlayedMessage in firstVisibleIndex..lastVisibleIndex) return

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .wrapContentWidth()
                .align(Alignment.TopCenter)
                .padding(all = dimensions().spacing8x)
                .clickable { coroutineScope.launch { lazyListState.animateScrollToItem(indexOfPlayedMessage) } }
                .background(
                    color = colorsScheme().secondaryText,
                    shape = RoundedCornerShape(dimensions().corner16x)
                )
                .padding(horizontal = dimensions().spacing16x, vertical = dimensions().spacing8x)
        ) {
            Icon(
                modifier = Modifier.size(dimensions().systemMessageIconSize),
                painter = painterResource(id = R.drawable.ic_play),
                contentDescription = null,
                tint = MaterialTheme.wireColorScheme.onPrimaryButtonEnabled
            )
            Text(
                modifier = Modifier
                    .padding(horizontal = dimensions().spacing8x)
                    .weight(1f, fill = false),
                text = playingAudioMessage.authorName.asString(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colorsScheme().onPrimaryButtonEnabled,
                style = MaterialTheme.wireTypography.body04,
            )
            Text(
                modifier = Modifier,
                text = DateAndTimeParsers.audioMessageTime(playingAudioMessage.state.currentPositionInMs.toLong()),
                color = colorsScheme().onPrimaryButtonEnabled,
                style = MaterialTheme.wireTypography.label03,
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
        draftMessageComposition = messageCompositionState.value,
        onClearDraft = {},
        onSaveDraft = {},
        onMessageTextUpdate = {},
        onTypingEvent = {},
        onSearchMentionQueryChanged = {},
        onClearMentionSearchResult = {},
    )
    ConversationScreen(
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
        onStartCall = { },
        onJoinCall = { },
        onReactionClick = { _, _ -> },
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
        openDrawingCanvas = {},
        onImagesPicked = { _, _ -> },
        onAttachmentClick = {},
        onAttachmentMenuClick = {},
        onAttachmentPicked = {},
        onAudioRecorded = {},
    )
}
