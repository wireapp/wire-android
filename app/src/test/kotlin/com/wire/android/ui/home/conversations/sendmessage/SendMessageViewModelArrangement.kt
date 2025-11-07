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

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.media.PingRinger
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.MessageSharedState
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.android.ui.navArgs
import com.wire.android.util.ImageUtil
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.feature.asset.upload.ScheduleNewAssetMessageResult
import com.wire.kalium.logic.feature.asset.upload.ScheduleNewAssetMessageUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOngoingCallsUseCase
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
import com.wire.kalium.common.functional.Either
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledForConversationUseCase
import com.wire.kalium.logic.feature.message.SendMultipartMessageUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import okio.Path
import okio.buffer

internal class SendMessageViewModelArrangement {

    val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")

    init {
        // Tests setup
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockUri()
        every { savedStateHandle.navArgs<ConversationNavArgs>() } returns ConversationNavArgs(
            conversationId = conversationId
        )
        // Default empty values
        coEvery { observeOngoingCallsUseCase() } returns flowOf(listOf())
        coEvery { observeEstablishedCallsUseCase() } returns flowOf(listOf())
        coEvery { observeSyncState() } returns flowOf(SyncState.Live)
        every { pingRinger.ping(any(), any()) } returns Unit
        coEvery { sendKnockUseCase(any(), any()) } returns Either.Right(Unit)
        coEvery { setUserInformedAboutVerificationUseCase(any()) } returns Unit
        coEvery { observeDegradedConversationNotifiedUseCase(any()) } returns flowOf(true)
        coEvery { setNotifiedAboutConversationUnderLegalHold(any()) } returns Unit
        coEvery { observeConversationUnderLegalHoldNotified(any()) } returns flowOf(true)
    }

    @MockK
    lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var sendTextMessage: SendTextMessageUseCase

    @MockK
    lateinit var sendMultipartMessage: SendMultipartMessageUseCase

    @MockK
    lateinit var sendEditTextMessage: SendEditTextMessageUseCase

    @MockK
    lateinit var sendAssetMessage: ScheduleNewAssetMessageUseCase

    @MockK
    lateinit var observeOngoingCallsUseCase: ObserveOngoingCallsUseCase

    @MockK
    private lateinit var observeEstablishedCallsUseCase: ObserveEstablishedCallsUseCase

    @MockK
    lateinit var sendKnockUseCase: SendKnockUseCase

    @MockK
    private lateinit var observeSyncState: ObserveSyncStateUseCase

    @MockK
    lateinit var pingRinger: PingRinger

    @MockK
    private lateinit var imageUtil: ImageUtil

    @MockK
    private lateinit var handleUriAssetUseCase: HandleUriAssetUseCase

    @MockK
    lateinit var retryFailedMessageUseCase: RetryFailedMessageUseCase

    @MockK
    lateinit var sendTypingEvent: SendTypingEventUseCase

    @MockK
    lateinit var setUserInformedAboutVerificationUseCase: SetUserInformedAboutVerificationUseCase

    @MockK
    lateinit var observeDegradedConversationNotifiedUseCase: ObserveDegradedConversationNotifiedUseCase

    @MockK
    lateinit var setNotifiedAboutConversationUnderLegalHold: SetNotifiedAboutConversationUnderLegalHoldUseCase

    @MockK
    lateinit var observeConversationUnderLegalHoldNotified: ObserveConversationUnderLegalHoldNotifiedUseCase

    @MockK
    lateinit var removeMessageDraftUseCase: RemoveMessageDraftUseCase

    @MockK
    lateinit var sendLocation: SendLocationUseCase

    private val fakeKaliumFileSystem = FakeKaliumFileSystem()

    @MockK
    lateinit var analyticsManager: AnonymousAnalyticsManager

    @MockK
    lateinit var isWireCellsEnabledForConversation: IsWireCellsEnabledForConversationUseCase

    @MockK
    lateinit var sharedState: MessageSharedState

    private val viewModel by lazy {
        SendMessageViewModel(
            sendTextMessage = sendTextMessage,
            sendEditTextMessage = sendEditTextMessage,
            sendAssetMessage = sendAssetMessage,
            dispatchers = TestDispatcherProvider(),
            kaliumFileSystem = fakeKaliumFileSystem,
            handleUriAsset = handleUriAssetUseCase,
            imageUtil = imageUtil,
            pingRinger = pingRinger,
            sendKnock = sendKnockUseCase,
            retryFailedMessage = retryFailedMessageUseCase,
            sendTypingEvent = sendTypingEvent,
            setUserInformedAboutVerification = setUserInformedAboutVerificationUseCase,
            observeDegradedConversationNotified = observeDegradedConversationNotifiedUseCase,
            setNotifiedAboutConversationUnderLegalHold = setNotifiedAboutConversationUnderLegalHold,
            observeConversationUnderLegalHoldNotified = observeConversationUnderLegalHoldNotified,
            sendLocation = sendLocation,
            removeMessageDraft = removeMessageDraftUseCase,
            savedStateHandle = savedStateHandle,
            analyticsManager = analyticsManager,
            sendMultipartMessage = sendMultipartMessage,
            isWireCellsEnabledForConversation = isWireCellsEnabledForConversation,
            sharedState = sharedState
        )
    }

    fun withSuccessfulViewModelInit() = apply {
        coEvery { observeOngoingCallsUseCase() } returns emptyFlow()
        coEvery { observeEstablishedCallsUseCase() } returns emptyFlow()
        coEvery { imageUtil.extractImageWidthAndHeight(any(), any()) } returns (1 to 1)
    }

    fun withStoredAsset(dataPath: Path, dataContent: ByteArray) = apply {
        fakeKaliumFileSystem.sink(dataPath).buffer().use {
            it.write(dataContent)
        }
    }

    fun withSendAttachmentMessageResult(result: ScheduleNewAssetMessageResult) = apply {
        coEvery {
            sendAssetMessage(any())
        } returns result
    }

    fun withSuccessfulSendTextMessage() = apply {
        coEvery {
            sendTextMessage(
                any(),
                any(),
                any(),
                any()
            )
        } returns Either.Right(Unit)
    }

    fun withFailedSendTextMessage(failure: CoreFailure) = apply {
        coEvery {
            sendTextMessage(
                any(),
                any(),
                any(),
                any()
            )
        } returns Either.Left(failure)
    }

    fun withSuccessfulSendEditTextMessage() = apply {
        coEvery {
            sendEditTextMessage(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Either.Right(Unit)
    }

    fun withSuccessfulSendLocationMessage() = apply {
        coEvery {
            sendLocation(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Either.Right(Unit)
    }

    fun withHandleUriAsset(result: HandleUriAssetUseCase.Result) = apply {
        coEvery { handleUriAssetUseCase.invoke(any(), any(), any()) } returns result
    }

    fun withInformAboutVerificationBeforeMessagingFlag(flag: Boolean) = apply {
        coEvery { observeDegradedConversationNotifiedUseCase(any()) } returns flowOf(flag)
    }

    fun withObserveConversationUnderLegalHoldNotified(flag: Boolean) = apply {
        coEvery { observeConversationUnderLegalHoldNotified(any()) } returns flowOf(flag)
    }

    fun withSuccessfulRetryFailedMessage() = apply {
        coEvery { retryFailedMessageUseCase(any(), any()) } returns Either.Right(Unit)
    }

    fun withPendingTextBundle(textToShare: String = "some text") = apply {
        every { savedStateHandle.navArgs<ConversationNavArgs>() } returns ConversationNavArgs(
            conversationId = conversationId,
            pendingTextBundle = textToShare
        )
    }

    fun withPendingAssetBundle(vararg assetBundle: AssetBundle) = apply {
        every { savedStateHandle.navArgs<ConversationNavArgs>() } returns ConversationNavArgs(
            conversationId = conversationId,
            pendingBundles = arrayListOf(*assetBundle)
        )
    }

    fun withCellsEnabledForConversation(result: Boolean) = apply {
        coEvery { isWireCellsEnabledForConversation.invoke(conversationId) } returns result
    }

    fun arrange() = this to viewModel
}
