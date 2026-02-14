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

package com.wire.android.ui.home.conversations.sendmessage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.media.PingRinger
import com.wire.android.media.audiomessage.toNormalizedLoudness
import com.wire.android.model.SnackBarMessage
import com.wire.android.ui.home.conversations.AssetTooLargeDialogState
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages
import com.wire.android.ui.home.conversations.MessageSharedState
import com.wire.android.ui.home.conversations.SureAboutMessagingDialogState
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.android.ui.home.messagecomposer.model.ComposableMessageBundle
import com.wire.android.ui.home.messagecomposer.model.MessageBundle
import com.wire.android.ui.home.messagecomposer.model.Ping
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.android.ui.sharing.SendMessagesSnackbarMessages
import com.wire.android.util.ImageUtil
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.getAudioLengthInMs
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.functional.Either
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.conversation.Conversation.TypingIndicatorMode
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.failure.LegalHoldEnabledForConversationFailure
import com.wire.kalium.logic.feature.asset.upload.AssetUploadParams
import com.wire.kalium.logic.feature.asset.upload.ScheduleNewAssetMessageResult
import com.wire.kalium.logic.feature.asset.upload.ScheduleNewAssetMessageUseCase
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledForConversationUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationUnderLegalHoldNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.ObserveDegradedConversationNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.SendTypingEventUseCase
import com.wire.kalium.logic.feature.conversation.SetNotifiedAboutConversationUnderLegalHoldUseCase
import com.wire.kalium.logic.feature.conversation.SetUserInformedAboutVerificationUseCase
import com.wire.kalium.logic.feature.message.RetryFailedMessageUseCase
import com.wire.kalium.logic.feature.message.SendEditMultipartMessageUseCase
import com.wire.kalium.logic.feature.message.SendEditTextMessageUseCase
import com.wire.kalium.logic.feature.message.SendKnockUseCase
import com.wire.kalium.logic.feature.message.SendLocationUseCase
import com.wire.kalium.logic.feature.message.SendMultipartMessageUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import com.wire.kalium.logic.feature.message.draft.RemoveMessageDraftUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class SendMessageViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val sendAssetMessage: ScheduleNewAssetMessageUseCase,
    private val sendTextMessage: SendTextMessageUseCase,
    private val sendMultipartMessage: SendMultipartMessageUseCase,
    private val sendEditTextMessage: SendEditTextMessageUseCase,
    private val sendEditMultipartMessage: SendEditMultipartMessageUseCase,
    private val retryFailedMessage: RetryFailedMessageUseCase,
    private val dispatchers: DispatcherProvider,
    private val kaliumFileSystem: KaliumFileSystem,
    private val handleUriAsset: HandleUriAssetUseCase,
    private val sendKnock: SendKnockUseCase,
    private val sendTypingEvent: SendTypingEventUseCase,
    private val pingRinger: PingRinger,
    private val imageUtil: ImageUtil,
    private val setUserInformedAboutVerification: SetUserInformedAboutVerificationUseCase,
    private val observeDegradedConversationNotified: ObserveDegradedConversationNotifiedUseCase,
    private val setNotifiedAboutConversationUnderLegalHold: SetNotifiedAboutConversationUnderLegalHoldUseCase,
    private val observeConversationUnderLegalHoldNotified: ObserveConversationUnderLegalHoldNotifiedUseCase,
    private val sendLocation: SendLocationUseCase,
    private val removeMessageDraft: RemoveMessageDraftUseCase,
    private val analyticsManager: AnonymousAnalyticsManager,
    private val isWireCellsEnabledForConversation: IsWireCellsEnabledForConversationUseCase,
    private val sharedState: MessageSharedState
) : ViewModel() {

    private val conversationNavArgs: ConversationNavArgs = savedStateHandle.navArgs()
    val conversationId: QualifiedID = conversationNavArgs.conversationId
    private val threadIdNavArgs: String? = conversationNavArgs.threadId

    private val _infoMessage = MutableSharedFlow<SnackBarMessage>()
    val infoMessage = _infoMessage.asSharedFlow()

    var assetTooLargeDialogState: AssetTooLargeDialogState by mutableStateOf(
        AssetTooLargeDialogState.Hidden
    )

    var sureAboutMessagingDialogState: SureAboutMessagingDialogState by mutableStateOf(
        SureAboutMessagingDialogState.Hidden
    )

    init {
        conversationNavArgs.pendingTextBundle?.let { text ->
            trySendPendingMessageBundle(text)
        }
        conversationNavArgs.pendingBundles?.let {
            handlePendingBundles(it)
        }
    }

    // for cells conversations we need to add the items to attachments list
    // for regular conversations we can send them right away
    private fun handlePendingBundles(assetBundles: ArrayList<AssetBundle>) {
        viewModelScope.launch {
            if (isWireCellsEnabledForConversation(conversationId)) {
                sharedState.postBundles(assetBundles)
            } else {
                trySendMessages(
                    assetBundles.map { assetBundle ->
                        ComposableMessageBundle.AttachmentPickedBundle(
                            conversationId,
                            assetBundle,
                            threadIdNavArgs
                        )
                    }
                )
            }
        }
    }

    private fun onSnackbarMessage(type: SnackBarMessage) = viewModelScope.launch {
        _infoMessage.emit(type)
    }

    private suspend fun shouldInformAboutDegradedBeforeSendingMessage(conversationId: ConversationId): Boolean =
        observeDegradedConversationNotified(conversationId).first().let { !it }

    private suspend fun shouldInformAboutUnderLegalHoldBeforeSendingMessage(conversationId: ConversationId) =
        observeConversationUnderLegalHoldNotified(conversationId).first().let { !it }

    private fun trySendPendingMessageBundle(pendingMessage: String) {
        viewModelScope.launch {
            sendMessage(ComposableMessageBundle.SendTextMessageBundle(conversationId, pendingMessage, emptyList()))
        }
    }

    fun trySendMessage(messageBundle: MessageBundle) {
        trySendMessages(listOf(messageBundle))
    }

    internal fun trySendMessages(messageBundleList: List<MessageBundle>) {
        if (messageBundleList.size > MAX_LIMIT_MESSAGE_SEND) {
            onSnackbarMessage(SendMessagesSnackbarMessages.MaxAmountOfAssetsReached)
        } else {
            val messageBundleMap = messageBundleList.groupBy { it.conversationId }
            messageBundleMap.forEach { (conversationId, bundles) ->
                viewModelScope.launch {
                    when {
                        shouldInformAboutDegradedBeforeSendingMessage(conversationId) ->
                            sureAboutMessagingDialogState =
                                SureAboutMessagingDialogState.Visible.ConversationVerificationDegraded(conversationId, bundles)

                        shouldInformAboutUnderLegalHoldBeforeSendingMessage(conversationId) ->
                            sureAboutMessagingDialogState =
                                SureAboutMessagingDialogState.Visible.ConversationUnderLegalHold.BeforeSending(
                                    conversationId,
                                    messageBundleList
                                )

                        else -> sendMessages(messageBundleList)
                    }
                }
            }
        }
    }

    private suspend fun sendMessages(messageBundleList: List<MessageBundle>) {
        val jobs: MutableCollection<Job> = mutableListOf()
        messageBundleList.forEach {
            val job = viewModelScope.launch {
                sendMessage(it)
            }
            jobs.add(job)
        }
        jobs.joinAll()
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private suspend fun sendMessage(messageBundle: MessageBundle) {
        when (messageBundle) {
            is ComposableMessageBundle.EditMessageBundle -> {
                removeMessageDraft(messageBundle.conversationId)
                sendTypingEvent(messageBundle.conversationId, TypingIndicatorMode.STOPPED)
                with(messageBundle) {
                    sendEditTextMessage(
                        conversationId = conversationId,
                        originalMessageId = originalMessageId,
                        text = newContent,
                        mentions = newMentions.map { it.intoMessageMention() },
                    ).toEither()
                        .handleLegalHoldFailureAfterSendingMessage(conversationId)
                        .handleNonAssetContributionEvent(messageBundle)
                }
            }

            is ComposableMessageBundle.EditMultipartMessageBundle -> {
                removeMessageDraft(messageBundle.conversationId)
                sendTypingEvent(messageBundle.conversationId, TypingIndicatorMode.STOPPED)
                with(messageBundle) {
                    sendEditMultipartMessage(
                        conversationId = conversationId,
                        originalMessageId = originalMessageId,
                        text = newContent,
                        mentions = newMentions.map { it.intoMessageMention() },
                    ).toEither()
                        .handleLegalHoldFailureAfterSendingMessage(conversationId)
                        .handleNonAssetContributionEvent(messageBundle)
                }
            }

            is ComposableMessageBundle.AttachmentPickedBundle -> {
                sendAttachment(
                    attachmentBundle = messageBundle.assetBundle,
                    conversationId = messageBundle.conversationId,
                    threadId = messageBundle.threadId ?: threadIdNavArgs,
                )
            }

            is ComposableMessageBundle.UriPickedBundle -> {
                handleAssetMessageBundle(
                    attachmentUri = messageBundle.attachmentUri,
                    conversationId = messageBundle.conversationId,
                    threadId = messageBundle.threadId ?: threadIdNavArgs,
                )
            }

            is ComposableMessageBundle.AudioMessageBundle -> {
                handleAssetMessageBundle(
                    attachmentUri = messageBundle.attachmentUri,
                    conversationId = messageBundle.conversationId,
                    threadId = messageBundle.threadId ?: threadIdNavArgs,
                )
            }

            is ComposableMessageBundle.SendTextMessageBundle -> {
                removeMessageDraft(messageBundle.conversationId)
                sendTypingEvent(messageBundle.conversationId, TypingIndicatorMode.STOPPED)
                with(messageBundle) {
                    sendTextMessage(
                        conversationId = conversationId,
                        text = message,
                        mentions = mentions.map { it.intoMessageMention() },
                        quotedMessageId = quotedMessageId,
                        threadId = threadId ?: threadIdNavArgs,
                    ).toEither()
                        .handleLegalHoldFailureAfterSendingMessage(conversationId)
                        .handleNonAssetContributionEvent(messageBundle)
                }
            }

            is ComposableMessageBundle.SendMultipartMessageBundle -> {
                removeMessageDraft(messageBundle.conversationId)
                sendTypingEvent(messageBundle.conversationId, TypingIndicatorMode.STOPPED)
                with(messageBundle) {
                    sendMultipartMessage(
                        conversationId = conversationId,
                        text = message,
                        mentions = mentions.map { it.intoMessageMention() },
                        quotedMessageId = quotedMessageId,
                        threadId = threadId ?: threadIdNavArgs,
                    ).toEither()
                        .handleLegalHoldFailureAfterSendingMessage(conversationId)
                        .handleNonAssetContributionEvent(messageBundle)
                }
            }

            is ComposableMessageBundle.LocationBundle -> {
                with(messageBundle) {
                    sendLocation(conversationId, location.latitude.toFloat(), location.longitude.toFloat(), locationName, zoom)
                        .toEither()
                        .handleLegalHoldFailureAfterSendingMessage(conversationId)
                        .handleNonAssetContributionEvent(messageBundle)
                }
            }

            is Ping -> {
                pingRinger.ping(R.raw.ping_from_me, isReceivingPing = false)
                sendKnock(conversationId = messageBundle.conversationId, hotKnock = false)
                    .toEither()
                    .handleLegalHoldFailureAfterSendingMessage(messageBundle.conversationId)
                    .handleNonAssetContributionEvent(messageBundle)
            }
        }
    }

    private suspend fun handleAssetMessageBundle(
        conversationId: ConversationId,
        attachmentUri: UriAsset,
        threadId: String?,
    ) {
        when (
            val result = handleUriAsset.invoke(
                uri = attachmentUri.uri,
                saveToDeviceIfInvalid = attachmentUri.saveToDeviceIfInvalid,
                specifiedMimeType = attachmentUri.mimeType,
                audioWavesMask = attachmentUri.audioWavesMask,
            )
        ) {
            is HandleUriAssetUseCase.Result.Failure.AssetTooLarge -> {
                assetTooLargeDialogState = AssetTooLargeDialogState.Visible(
                    assetType = result.assetBundle.assetType,
                    maxLimitInMB = result.maxLimitInMB,
                    savedToDevice = attachmentUri.saveToDeviceIfInvalid
                )
            }

            HandleUriAssetUseCase.Result.Failure.Unknown -> {
                onSnackbarMessage(ConversationSnackbarMessages.ErrorPickingAttachment)
            }

            is HandleUriAssetUseCase.Result.Success -> {
                sendAttachment(result.assetBundle, conversationId, threadId)
            }
        }
    }

    internal fun sendAttachment(
        attachmentBundle: AssetBundle?,
        conversationId: ConversationId,
        threadId: String? = threadIdNavArgs
    ) {
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                attachmentBundle?.run {
                    when (assetType) {
                        AttachmentType.IMAGE -> {
                            if (kaliumFileSystem.exists(attachmentBundle.dataPath)) {
                                val (imgWidth, imgHeight) = imageUtil.extractImageWidthAndHeight(
                                    kaliumFileSystem,
                                    attachmentBundle.dataPath
                                )
                                sendAssetMessage(attachmentBundle.uploadParams(imgHeight, imgWidth, threadId = threadId))
                                    .handleLegalHoldFailureAfterSendingMessage(conversationId)
                                    .handleAssetContributionEvent(assetType)
                            } else {
                                appLogger.e("There was a FileNotFoundException error while sending image asset")
                                onSnackbarMessage(ConversationSnackbarMessages.ErrorSendingImage)
                            }
                        }

                        AttachmentType.VIDEO,
                        AttachmentType.GENERIC_FILE,
                        AttachmentType.AUDIO -> {
                            try {
                                sendAssetMessage(
                                    attachmentBundle.uploadParams(
                                        audioLengthInMs = getAudioLengthInMs(
                                            dataPath = dataPath,
                                            mimeType = mimeType
                                        ),
                                        threadId = threadId,
                                    )
                                ).handleLegalHoldFailureAfterSendingMessage(conversationId)
                                    .handleAssetContributionEvent(assetType)
                            } catch (e: OutOfMemoryError) {
                                appLogger.e("There was an OutOfMemory error while uploading the asset")
                                onSnackbarMessage(ConversationSnackbarMessages.ErrorSendingAsset)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Either<CoreFailure?, Unit>.handleAssetContributionEvent(
        assetType: AttachmentType
    ) = also {
        onSuccess {
            val event = when (assetType) {
                AttachmentType.IMAGE -> AnalyticsEvent.Contributed.Photo
                AttachmentType.VIDEO -> AnalyticsEvent.Contributed.Video
                AttachmentType.GENERIC_FILE -> AnalyticsEvent.Contributed.File
                AttachmentType.AUDIO -> AnalyticsEvent.Contributed.Audio
            }
            analyticsManager.sendEvent(event)
        }
    }

    private fun Either<CoreFailure, Unit>.handleNonAssetContributionEvent(messageBundle: MessageBundle) = also {
        onSuccess {
            val event = when (messageBundle) {
                // assets are not handled here, as they need extra processing
                is ComposableMessageBundle.UriPickedBundle,
                is ComposableMessageBundle.AudioMessageBundle,
                is ComposableMessageBundle.AttachmentPickedBundle -> return@also

                is ComposableMessageBundle.LocationBundle -> AnalyticsEvent.Contributed.Location
                is Ping -> AnalyticsEvent.Contributed.Ping
                is ComposableMessageBundle.EditMessageBundle,
                is ComposableMessageBundle.SendTextMessageBundle,
                is ComposableMessageBundle.EditMultipartMessageBundle,
                is ComposableMessageBundle.SendMultipartMessageBundle -> AnalyticsEvent.Contributed.Text
            }
            analyticsManager.sendEvent(event)
        }
    }

    private fun CoreFailure.handleLegalHoldFailureAfterSendingMessage(conversationId: ConversationId) = also {
        if (this is LegalHoldEnabledForConversationFailure) {
            sureAboutMessagingDialogState = when (val currentState = sureAboutMessagingDialogState) {
                // if multiple messages will fail, update messageIdList to retry sending all of them
                is SureAboutMessagingDialogState.Visible.ConversationUnderLegalHold.AfterSending -> currentState.copy(
                    messageIdList = currentState.messageIdList.plus(messageId)
                )

                SureAboutMessagingDialogState.Hidden,
                is SureAboutMessagingDialogState.Visible.ConversationUnderLegalHold.BeforeSending,
                is SureAboutMessagingDialogState.Visible.ConversationVerificationDegraded ->
                    SureAboutMessagingDialogState.Visible.ConversationUnderLegalHold.AfterSending(conversationId, listOf(messageId))
            }
        }
    }

    private fun Either<CoreFailure, Unit>.handleLegalHoldFailureAfterSendingMessage(
        conversationId: ConversationId
    ): Either<CoreFailure, Unit> =
        onFailure { it.handleLegalHoldFailureAfterSendingMessage(conversationId) }

    private fun ScheduleNewAssetMessageResult.handleLegalHoldFailureAfterSendingMessage(
        conversationId: ConversationId
    ): Either<CoreFailure?, Unit> =
        let {
            when (this) {
                is ScheduleNewAssetMessageResult.Success -> Either.Right(Unit)
                ScheduleNewAssetMessageResult.Failure.DisabledByTeam,
                ScheduleNewAssetMessageResult.Failure.RestrictedFileType -> {
                    onSnackbarMessage(ConversationSnackbarMessages.ErrorAssetRestriction)
                    Either.Left(null)
                }

                is ScheduleNewAssetMessageResult.Failure.Generic -> {
                    this.coreFailure.handleLegalHoldFailureAfterSendingMessage(conversationId)
                    Either.Left(coreFailure)
                }
            }
        }

    fun retrySendingMessages(messageIdList: List<String>, conversationId: ConversationId) {
        messageIdList.forEach {
            retrySendingMessage(it, conversationId)
        }
    }

    fun retrySendingMessage(messageId: String, conversationId: ConversationId) {
        viewModelScope.launch {
            retryFailedMessage(messageId = messageId, conversationId = conversationId)
        }
    }

    fun hideAssetTooLargeError() {
        assetTooLargeDialogState = AssetTooLargeDialogState.Hidden
    }

    fun acceptSureAboutSendingMessage() {
        (sureAboutMessagingDialogState as? SureAboutMessagingDialogState.Visible)?.let {
            viewModelScope.launch {
                it.markAsNotified(it.conversationId)
                when (it) {
                    is SureAboutMessagingDialogState.Visible.ConversationVerificationDegraded ->
                        trySendMessages(it.messageBundleListToSend)

                    is SureAboutMessagingDialogState.Visible.ConversationUnderLegalHold.BeforeSending ->
                        trySendMessages(it.messageBundleListToSend)

                    is SureAboutMessagingDialogState.Visible.ConversationUnderLegalHold.AfterSending ->
                        retrySendingMessages(it.messageIdList, it.conversationId)
                }
            }
        }
    }

    fun dismissSureAboutSendingMessage() {
        sureAboutMessagingDialogState = SureAboutMessagingDialogState.Hidden
    }

    private suspend fun SureAboutMessagingDialogState.markAsNotified(conversationId: ConversationId) {
        when (this) {
            is SureAboutMessagingDialogState.Visible.ConversationUnderLegalHold ->
                setNotifiedAboutConversationUnderLegalHold(conversationId)

            is SureAboutMessagingDialogState.Visible.ConversationVerificationDegraded ->
                setUserInformedAboutVerification(conversationId)

            SureAboutMessagingDialogState.Hidden -> {
                /* do nothing */
            }
        }
        sureAboutMessagingDialogState = SureAboutMessagingDialogState.Hidden
    }

    private fun AssetBundle.uploadParams(
        assetHeight: Int? = null,
        assetWidth: Int? = null,
        audioLengthInMs: Long = 0L,
        threadId: String? = null,
    ) = AssetUploadParams(
        conversationId = conversationId,
        assetDataPath = dataPath,
        assetName = fileName,
        assetMimeType = mimeType,
        assetDataSize = dataSize,
        assetHeight = assetHeight,
        assetWidth = assetWidth,
        audioLengthInMs = audioLengthInMs,
        audioNormalizedLoudness = audioWavesMask?.toNormalizedLoudness(),
        threadId = threadId,
    )

    private companion object {
        const val MAX_LIMIT_MESSAGE_SEND = 20
    }
}
