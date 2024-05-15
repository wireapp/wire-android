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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.media.PingRinger
import com.wire.android.model.SnackBarMessage
import com.wire.android.ui.home.conversations.AssetTooLargeDialogState
import com.wire.android.ui.home.conversations.ConversationSnackbarMessages
import com.wire.android.ui.home.conversations.SureAboutMessagingDialogState
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.android.ui.home.messagecomposer.model.ComposableMessageBundle
import com.wire.android.ui.home.messagecomposer.model.MessageBundle
import com.wire.android.ui.home.messagecomposer.model.Ping
import com.wire.android.ui.sharing.SendMessagesSnackbarMessages
import com.wire.android.util.ImageUtil
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.getAudioLengthInMs
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.conversation.Conversation.TypingIndicatorMode
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.failure.LegalHoldEnabledForConversationFailure
import com.wire.kalium.logic.feature.asset.ScheduleNewAssetMessageResult
import com.wire.kalium.logic.feature.asset.ScheduleNewAssetMessageUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationUnderLegalHoldNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.ObserveDegradedConversationNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.SendTypingEventUseCase
import com.wire.kalium.logic.feature.conversation.SetNotifiedAboutConversationUnderLegalHoldUseCase
import com.wire.kalium.logic.feature.conversation.SetUserInformedAboutVerificationUseCase
import com.wire.kalium.logic.feature.message.RetryFailedMessageUseCase
import com.wire.kalium.logic.feature.message.SendEditTextMessageUseCase
import com.wire.kalium.logic.feature.message.SendKnockUseCase
import com.wire.kalium.logic.feature.message.SendLocationUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import com.wire.kalium.logic.feature.message.draft.RemoveMessageDraftUseCase
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.isRight
import com.wire.kalium.logic.functional.onFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path
import okio.Path.Companion.toPath
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class SendMessageViewModel @Inject constructor(
    private val sendAssetMessage: ScheduleNewAssetMessageUseCase,
    private val sendTextMessage: SendTextMessageUseCase,
    private val sendEditTextMessage: SendEditTextMessageUseCase,
    private val retryFailedMessage: RetryFailedMessageUseCase,
    private val dispatchers: DispatcherProvider,
    private val kaliumFileSystem: KaliumFileSystem,
    private val handleUriAsset: HandleUriAssetUseCase,
    private val sendKnockUseCase: SendKnockUseCase,
    private val sendTypingEvent: SendTypingEventUseCase,
    private val pingRinger: PingRinger,
    private val imageUtil: ImageUtil,
    private val setUserInformedAboutVerification: SetUserInformedAboutVerificationUseCase,
    private val observeDegradedConversationNotified: ObserveDegradedConversationNotifiedUseCase,
    private val setNotifiedAboutConversationUnderLegalHold: SetNotifiedAboutConversationUnderLegalHoldUseCase,
    private val observeConversationUnderLegalHoldNotified: ObserveConversationUnderLegalHoldNotifiedUseCase,
    private val sendLocation: SendLocationUseCase,
    private val removeMessageDraft: RemoveMessageDraftUseCase,
) : ViewModel() {

    private val _infoMessage = MutableSharedFlow<SnackBarMessage>()
    val infoMessage = _infoMessage.asSharedFlow()

    var assetTooLargeDialogState: AssetTooLargeDialogState by mutableStateOf(
        AssetTooLargeDialogState.Hidden
    )

    var sureAboutMessagingDialogState: SureAboutMessagingDialogState by mutableStateOf(
        SureAboutMessagingDialogState.Hidden
    )

    var viewState: SendMessageState by mutableStateOf(SendMessageState())

    private fun onSnackbarMessage(type: SnackBarMessage) = viewModelScope.launch {
        _infoMessage.emit(type)
    }

    private suspend fun shouldInformAboutDegradedBeforeSendingMessage(conversationId: ConversationId): Boolean =
        observeDegradedConversationNotified(conversationId).first().let { !it }

    private suspend fun shouldInformAboutUnderLegalHoldBeforeSendingMessage(conversationId: ConversationId) =
        observeConversationUnderLegalHoldNotified(conversationId).first().let { !it }

    fun trySendMessage(messageBundle: MessageBundle) {
        trySendMessages(listOf(messageBundle))
    }

    fun trySendMessages(messageBundleList: List<MessageBundle>) {
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
        beforeSendingMessage()
        messageBundleList.forEach {
            val job = viewModelScope.launch {
                sendMessage(it)
            }
            jobs.add(job)
        }
        jobs.joinAll()
        withContext(dispatchers.main()) {
            val action = messageBundleList.firstOrNull()?.let {
                SendMessageAction.NavigateToConversation(it.conversationId)
            } ?: SendMessageAction.NavigateToHome

            viewState = viewState.copy(inProgress = false, afterMessageSendAction = action)
        }
    }

    @Suppress("LongMethod")
    private suspend fun sendMessage(messageBundle: MessageBundle) {
        when (messageBundle) {
            is ComposableMessageBundle.EditMessageBundle -> {
                beforeSendingMessage()
                removeMessageDraft(messageBundle.conversationId)
                sendTypingEvent(messageBundle.conversationId, TypingIndicatorMode.STOPPED)
                with(messageBundle) {
                    sendEditTextMessage(
                        conversationId = conversationId,
                        originalMessageId = originalMessageId,
                        text = newContent,
                        mentions = newMentions.map { it.intoMessageMention() },
                    )
                        .handleLegalHoldFailureAfterSendingMessage(conversationId)
                        .handleAfterMessageResult()
                }
            }

            is ComposableMessageBundle.AttachmentPickedBundle -> {
                sendAttachment(messageBundle.assetBundle, messageBundle.conversationId)
            }

            is ComposableMessageBundle.UriPickedBundle -> {
                handleAssetMessageBundle(
                    attachmentUri = messageBundle.attachmentUri,
                    conversationId = messageBundle.conversationId
                )
            }

            is ComposableMessageBundle.AudioMessageBundle -> {
                handleAssetMessageBundle(
                    attachmentUri = messageBundle.attachmentUri,
                    audioPath = messageBundle.attachmentUri.uri.path?.toPath(),
                    conversationId = messageBundle.conversationId
                )
            }

            is ComposableMessageBundle.SendTextMessageBundle -> {
                beforeSendingMessage()
                removeMessageDraft(messageBundle.conversationId)
                sendTypingEvent(messageBundle.conversationId, TypingIndicatorMode.STOPPED)
                with(messageBundle) {
                    sendTextMessage(
                        conversationId = conversationId,
                        text = message,
                        mentions = mentions.map { it.intoMessageMention() },
                        quotedMessageId = quotedMessageId
                    )
                        .handleLegalHoldFailureAfterSendingMessage(conversationId)
                        .handleAfterMessageResult()
                }
            }

            is ComposableMessageBundle.LocationBundle -> {
                beforeSendingMessage()
                with(messageBundle) {
                    sendLocation(conversationId, location.latitude.toFloat(), location.longitude.toFloat(), locationName, zoom)
                        .handleLegalHoldFailureAfterSendingMessage(conversationId)
                        .handleAfterMessageResult()
                }
            }

            is Ping -> {
                beforeSendingMessage()
                pingRinger.ping(R.raw.ping_from_me, isReceivingPing = false)
                sendKnockUseCase(conversationId = messageBundle.conversationId, hotKnock = false)
                    .handleLegalHoldFailureAfterSendingMessage(messageBundle.conversationId)
                    .handleAfterMessageResult()
            }
        }
    }

    private suspend fun handleAssetMessageBundle(
        conversationId: ConversationId,
        attachmentUri: UriAsset,
        audioPath: Path? = null
    ) {
        when (val result = handleUriAsset.invoke(
            uri = attachmentUri.uri,
            saveToDeviceIfInvalid = attachmentUri.saveToDeviceIfInvalid,
            audioPath = audioPath
        )) {
            is HandleUriAssetUseCase.Result.Failure.AssetTooLarge -> {
                assetTooLargeDialogState = AssetTooLargeDialogState.SingleVisible(
                    assetType = result.assetBundle.assetType,
                    maxLimitInMB = result.maxLimitInMB,
                    savedToDevice = attachmentUri.saveToDeviceIfInvalid
                )
            }

            HandleUriAssetUseCase.Result.Failure.Unknown -> {
                onSnackbarMessage(ConversationSnackbarMessages.ErrorPickingAttachment)
            }

            is HandleUriAssetUseCase.Result.Success -> {
                sendAttachment(result.assetBundle, conversationId)
            }
        }
    }

    internal fun sendAttachment(attachmentBundle: AssetBundle?, conversationId: ConversationId) {
        beforeSendingMessage()
        viewModelScope.launch {
            withContext(dispatchers.io()) {
                attachmentBundle?.run {
                    when (assetType) {
                        AttachmentType.IMAGE -> {
                            val (imgWidth, imgHeight) = imageUtil.extractImageWidthAndHeight(
                                kaliumFileSystem,
                                attachmentBundle.dataPath
                            )
                            sendAssetMessage(
                                conversationId = conversationId,
                                assetDataPath = dataPath,
                                assetName = fileName,
                                assetWidth = imgWidth,
                                assetHeight = imgHeight,
                                assetDataSize = dataSize,
                                assetMimeType = mimeType,
                                audioLengthInMs = 0L
                            )
                                .handleLegalHoldFailureAfterSendingMessage(conversationId)
                                .handleAfterMessageResult()
                        }

                        AttachmentType.VIDEO,
                        AttachmentType.GENERIC_FILE,
                        AttachmentType.AUDIO -> {
                            try {
                                sendAssetMessage(
                                    conversationId = conversationId,
                                    assetDataPath = dataPath,
                                    assetName = fileName,
                                    assetMimeType = mimeType,
                                    assetDataSize = dataSize,
                                    assetHeight = null,
                                    assetWidth = null,
                                    audioLengthInMs = getAudioLengthInMs(
                                        dataPath = dataPath,
                                        mimeType = mimeType
                                    )
                                )
                                    .handleLegalHoldFailureAfterSendingMessage(conversationId)
                                    .handleAfterMessageResult()
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

    private fun Either<CoreFailure, Unit>.handleLegalHoldFailureAfterSendingMessage(conversationId: ConversationId) =
        onFailure { it.handleLegalHoldFailureAfterSendingMessage(conversationId) }

    private fun ScheduleNewAssetMessageResult.handleLegalHoldFailureAfterSendingMessage(conversationId: ConversationId) = let {
        if (it is ScheduleNewAssetMessageResult.Failure) {
            it.coreFailure.handleLegalHoldFailureAfterSendingMessage(conversationId)
        }
        when (this) {
            is ScheduleNewAssetMessageResult.Failure -> Either.Left(coreFailure)
            is ScheduleNewAssetMessageResult.Success -> Either.Right(Unit)
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

            SureAboutMessagingDialogState.Hidden -> { /* do nothing */
            }
        }
        sureAboutMessagingDialogState = SureAboutMessagingDialogState.Hidden
    }

    private fun beforeSendingMessage() {
        viewState = viewState.copy(inProgress = true)
    }

    private fun Either<CoreFailure, Unit>.handleAfterMessageResult() {
        viewState = viewState.copy(
            afterMessageSendAction = if (this.isRight()) {
                SendMessageAction.None // TODO KBX pass action
            } else {
                SendMessageAction.None
            },
            inProgress = false
        )
    }

    private companion object {
        const val MAX_LIMIT_MESSAGE_SEND = 20
    }
}
