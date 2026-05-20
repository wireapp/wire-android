/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.media.PingRinger
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.MessageSharedState
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.android.util.ImageUtil
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.asset.KaliumFileSystem
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
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class SendMessageViewModelFactory(
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
    private val sharedState: MessageSharedState,
) {
    fun create(conversationNavArgs: ConversationNavArgs): SendMessageViewModel = SendMessageViewModel(
        conversationNavArgs = conversationNavArgs,
        sendAssetMessage = sendAssetMessage,
        sendTextMessage = sendTextMessage,
        sendMultipartMessage = sendMultipartMessage,
        sendEditTextMessage = sendEditTextMessage,
        sendEditMultipartMessage = sendEditMultipartMessage,
        retryFailedMessage = retryFailedMessage,
        dispatchers = dispatchers,
        kaliumFileSystem = kaliumFileSystem,
        handleUriAsset = handleUriAsset,
        sendKnock = sendKnock,
        sendTypingEvent = sendTypingEvent,
        pingRinger = pingRinger,
        imageUtil = imageUtil,
        setUserInformedAboutVerification = setUserInformedAboutVerification,
        observeDegradedConversationNotified = observeDegradedConversationNotified,
        setNotifiedAboutConversationUnderLegalHold = setNotifiedAboutConversationUnderLegalHold,
        observeConversationUnderLegalHoldNotified = observeConversationUnderLegalHoldNotified,
        sendLocation = sendLocation,
        removeMessageDraft = removeMessageDraft,
        analyticsManager = analyticsManager,
        isWireCellsEnabledForConversation = isWireCellsEnabledForConversation,
        sharedState = sharedState,
    )
}
