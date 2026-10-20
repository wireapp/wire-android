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
package com.wire.android.di.accountScoped

import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.di.KaliumMessageApi
import com.wire.kalium.logic.feature.asset.GetImageAssetMessagesForConversationUseCase
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.GetPaginatedFlowOfAssetMessageByConversationIdUseCase
import com.wire.kalium.logic.feature.asset.ObserveAssetStatusesUseCase
import com.wire.kalium.logic.feature.asset.ObservePaginatedAssetImageMessages
import com.wire.kalium.logic.feature.asset.UpdateAssetMessageTransferStatusUseCase
import com.wire.kalium.logic.feature.asset.UpdateAudioMessageNormalizedLoudnessUseCase
import com.wire.kalium.logic.feature.asset.upload.ScheduleNewAssetMessageUseCase
import com.wire.kalium.logic.feature.incallreaction.SendInCallReactionUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.FetchOlderNomadMessagesByConversationUseCase
import com.wire.kalium.logic.feature.message.GetNotificationsUseCase
import com.wire.kalium.logic.feature.message.GetPaginatedFlowOfMessagesByConversationUseCase
import com.wire.kalium.logic.feature.message.GetPaginatedFlowOfMessagesBySearchQueryAndConversationIdUseCase
import com.wire.kalium.logic.feature.message.GetSearchedConversationMessagePositionUseCase
import com.wire.kalium.logic.feature.message.GetSenderNameByMessageIdUseCase
import com.wire.kalium.logic.feature.message.MarkMessagesAsNotifiedUseCase
import com.wire.kalium.logic.feature.message.ObserveMessageReactionsUseCase
import com.wire.kalium.logic.feature.message.ObserveMessageReceiptsUseCase
import com.wire.kalium.logic.feature.message.RetryFailedMessageUseCase
import com.wire.kalium.logic.feature.message.SendEditMultipartMessageUseCase
import com.wire.kalium.logic.feature.message.SendEditTextMessageUseCase
import com.wire.kalium.logic.feature.message.SendKnockUseCase
import com.wire.kalium.logic.feature.message.SendLocationUseCase
import com.wire.kalium.logic.feature.message.SendMultipartMessageUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import com.wire.kalium.logic.feature.message.ToggleReactionUseCase
import com.wire.kalium.logic.feature.message.composite.SendButtonActionMessageUseCase
import com.wire.kalium.logic.feature.message.draft.GetMessageDraftUseCase
import com.wire.kalium.logic.feature.message.draft.RemoveMessageDraftUseCase
import com.wire.kalium.logic.feature.message.draft.SaveMessageDraftUseCase
import com.wire.kalium.logic.feature.message.ephemeral.EnqueueMessageSelfDeletionUseCase
import com.wire.kalium.logic.feature.message.linkpreview.DetectLinkPreviewTargetUseCase
import com.wire.kalium.logic.feature.message.linkpreview.GenerateLinkPreviewUseCase
import com.wire.kalium.logic.feature.sessionreset.ResetSessionUseCase
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides

@BindingContainer
@Suppress("TooManyFunctions")
class MessageModule {

    @Provides
    fun provideMessageApi(
        @CurrentAccount currentAccount: UserId,
        @KaliumCoreLogic coreLogic: CoreLogic
    ): KaliumMessageApi = coreLogic.getSessionScope(currentAccount).messageApi

    @Provides
    fun provideSendButtonActionMessageUseCase(api: KaliumMessageApi): SendButtonActionMessageUseCase =
        api.messageSendButtonActionMessage

    @Provides
    fun provideEnqueueMessageSelfDeletionUseCase(api: KaliumMessageApi): EnqueueMessageSelfDeletionUseCase =
        api.messageEnqueueMessageSelfDeletion

    @Provides
    fun provideResetSessionUseCase(api: KaliumMessageApi): ResetSessionUseCase =
        api.messageResetSession

    @Provides
    fun provideDeleteMessageUseCase(api: KaliumMessageApi): DeleteMessageUseCase =
        api.messageDeleteMessage

    @Provides
    fun provideMarkMessagesAsNotifiedUseCase(api: KaliumMessageApi): MarkMessagesAsNotifiedUseCase =
        api.messageMarkMessagesAsNotified

    @Provides
    fun provideUpdateAssetMessageTransferStatusUseCase(api: KaliumMessageApi): UpdateAssetMessageTransferStatusUseCase =
        api.messageUpdateAssetMessageTransferStatus

    @Provides
    fun provideSendTextMessageUseCase(api: KaliumMessageApi): SendTextMessageUseCase = api.messageSendTextMessage

    @Provides
    fun provideGenerateLinkPreviewUseCase(api: KaliumMessageApi): GenerateLinkPreviewUseCase =
        api.messageGenerateLinkPreview

    @Provides
    fun provideDetectLinkPreviewTargetUseCase(api: KaliumMessageApi): DetectLinkPreviewTargetUseCase =
        api.messageDetectLinkPreviewTarget

    @Provides
    fun provideSendEditTextMessageUseCase(api: KaliumMessageApi): SendEditTextMessageUseCase =
        api.messageSendEditTextMessage

    @Provides
    fun provideSendEditMultipartMessageUseCase(api: KaliumMessageApi): SendEditMultipartMessageUseCase =
        api.messageSendEditMultipartMessage

    @Provides
    fun provideRetryFailedMessageUseCase(api: KaliumMessageApi): RetryFailedMessageUseCase =
        api.messageRetryFailedMessage

    @Provides
    fun provideSendKnockUseCase(api: KaliumMessageApi): SendKnockUseCase =
        api.messageSendKnock

    @Provides
    fun provideToggleReactionUseCase(api: KaliumMessageApi): ToggleReactionUseCase =
        api.messageToggleReaction

    @Provides
    fun provideObserveMessageReactionsUseCase(api: KaliumMessageApi): ObserveMessageReactionsUseCase =
        api.messageObserveMessageReactions

    @Provides
    fun provideObserveMessageReceiptsUseCase(api: KaliumMessageApi): ObserveMessageReceiptsUseCase =
        api.messageObserveMessageReceipts

    @Provides
    fun providesSendAssetMessageUseCase(api: KaliumMessageApi): ScheduleNewAssetMessageUseCase =
        api.messageSendAssetMessage

    @Provides
    fun provideGetPrivateAssetUseCase(api: KaliumMessageApi): GetMessageAssetUseCase =
        api.messageGetAssetMessage

    @Provides
    fun provideGetNotificationsUseCase(api: KaliumMessageApi): GetNotificationsUseCase =
        api.messageGetNotifications

    @Provides
    fun provideGetPaginatedMessagesUseCase(api: KaliumMessageApi): GetPaginatedFlowOfMessagesByConversationUseCase =
        api.messageGetPaginatedFlowOfMessagesByConversation

    @Provides
    fun provideFetchOlderMessagesUseCase(api: KaliumMessageApi): FetchOlderNomadMessagesByConversationUseCase =
        api.messageFetchOlderMessagesByConversationId

    @Provides
    fun provideGetImageAssetMessagesByConversationUseCase(api: KaliumMessageApi): GetImageAssetMessagesForConversationUseCase =
        api.messageGetImageAssetMessagesByConversation

    @Provides
    fun provideGetPaginatedFlowOfAssetMessageByConversationId(
        api: KaliumMessageApi
    ): GetPaginatedFlowOfAssetMessageByConversationIdUseCase =
        api.messageGetPaginatedFlowOfAssetMessageByConversationId

    @Provides
    fun provideGetPaginatedFlowOfImageAssetMessageByConversationId(
        api: KaliumMessageApi
    ): ObservePaginatedAssetImageMessages =
        api.messageObservePaginatedImageAssetMessageByConversationId

    @Provides
    fun provideGetPaginatedFlowOfMessagesBySearchQueryAndConversation(
        api: KaliumMessageApi
    ): GetPaginatedFlowOfMessagesBySearchQueryAndConversationIdUseCase =
        api.messageGetPaginatedFlowOfMessagesBySearchQueryAndConversation

    @Provides
    fun provideGetSearchedConversationMessagePositionUseCase(api: KaliumMessageApi): GetSearchedConversationMessagePositionUseCase =
        api.messageGetSearchedConversationMessagePosition

    @Provides
    fun provideSendLocationUseCase(api: KaliumMessageApi): SendLocationUseCase =
        api.messageSendLocation

    @Provides
    fun provideObserveAssetStatusesUseCase(api: KaliumMessageApi): ObserveAssetStatusesUseCase =
        api.messageObserveAssetStatuses

    @Provides
    fun provideSaveMessageDraftUseCase(api: KaliumMessageApi): SaveMessageDraftUseCase =
        api.messageSaveMessageDraftUseCase

    @Provides
    fun provideGetMessageDraftUseCase(api: KaliumMessageApi): GetMessageDraftUseCase =
        api.messageGetMessageDraftUseCase

    @Provides
    fun provideRemoveMessageDraftUseCase(api: KaliumMessageApi): RemoveMessageDraftUseCase =
        api.messageRemoveMessageDraftUseCase

    @Provides
    fun provideSendInCallReactionUseCase(api: KaliumMessageApi): SendInCallReactionUseCase =
        api.messageSendInCallReactionUseCase

    @Provides
    fun provideGetSenderNameByMessageIdUseCase(api: KaliumMessageApi): GetSenderNameByMessageIdUseCase =
        api.messageGetSenderNameByMessageId

    @Provides
    fun provideSendMultipartMessageUseCase(api: KaliumMessageApi): SendMultipartMessageUseCase =
        api.messageSendMultipartMessage

    @Provides
    fun provideUpdateAudioMessageNormalizedLoudnessUseCase(api: KaliumMessageApi): UpdateAudioMessageNormalizedLoudnessUseCase =
        api.messageUpdateAudioMessageNormalizedLoudnessUseCase
}
