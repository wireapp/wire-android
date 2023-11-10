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
 */
package com.wire.android.di.accountScoped

import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.ScheduleNewAssetMessageUseCase
import com.wire.kalium.logic.feature.asset.UpdateAssetMessageDownloadStatusUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.GetConversationMessagesFromSearchQueryUseCase
import com.wire.kalium.logic.feature.message.GetMessageByIdUseCase
import com.wire.kalium.logic.feature.message.GetNotificationsUseCase
import com.wire.kalium.logic.feature.message.GetPaginatedFlowOfMessagesByConversationUseCase
import com.wire.kalium.logic.feature.message.GetSearchedConversationMessagePositionUseCase
import com.wire.kalium.logic.feature.message.MarkMessagesAsNotifiedUseCase
import com.wire.kalium.logic.feature.message.MessageScope
import com.wire.kalium.logic.feature.message.ObserveMessageReactionsUseCase
import com.wire.kalium.logic.feature.message.ObserveMessageReceiptsUseCase
import com.wire.kalium.logic.feature.message.RetryFailedMessageUseCase
import com.wire.kalium.logic.feature.message.SendEditTextMessageUseCase
import com.wire.kalium.logic.feature.message.SendKnockUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import com.wire.kalium.logic.feature.message.ToggleReactionUseCase
import com.wire.kalium.logic.feature.message.composite.SendButtonActionMessageUseCase
import com.wire.kalium.logic.feature.message.ephemeral.EnqueueMessageSelfDeletionUseCase
import com.wire.kalium.logic.feature.message.getPaginatedFlowOfMessagesByConversation
import com.wire.kalium.logic.feature.sessionreset.ResetSessionUseCase
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
@Suppress("TooManyFunctions")
class MessageModule {

    @ViewModelScoped
    @Provides
    fun provideMessageScope(
        @CurrentAccount currentAccount: UserId,
        @KaliumCoreLogic coreLogic: CoreLogic
    ): MessageScope = coreLogic.getSessionScope(currentAccount).messages

    @ViewModelScoped
    @Provides
    fun provideSendButtonActionMessageUseCase(messageScope: MessageScope): SendButtonActionMessageUseCase =
        messageScope.sendButtonActionMessage

    @ViewModelScoped
    @Provides
    fun provideEnqueueMessageSelfDeletionUseCase(messageScope: MessageScope): EnqueueMessageSelfDeletionUseCase =
        messageScope.enqueueMessageSelfDeletion

    @ViewModelScoped
    @Provides
    fun provideResetSessionUseCase(messageScope: MessageScope): ResetSessionUseCase =
        messageScope.resetSession

    @ViewModelScoped
    @Provides
    fun provideDeleteMessageUseCase(messageScope: MessageScope): DeleteMessageUseCase =
        messageScope.deleteMessage

    @ViewModelScoped
    @Provides
    fun provideMarkMessagesAsNotifiedUseCase(messageScope: MessageScope): MarkMessagesAsNotifiedUseCase =
        messageScope.markMessagesAsNotified

    @ViewModelScoped
    @Provides
    fun provideUpdateAssetMessageDownloadStatusUseCase(messageScope: MessageScope): UpdateAssetMessageDownloadStatusUseCase =
        messageScope.updateAssetMessageDownloadStatus

    @ViewModelScoped
    @Provides
    fun provideSendTextMessageUseCase(messageScope: MessageScope): SendTextMessageUseCase = messageScope.sendTextMessage

    @ViewModelScoped
    @Provides
    fun provideSendEditTextMessageUseCase(messageScope: MessageScope): SendEditTextMessageUseCase =
        messageScope.sendEditTextMessage

    @ViewModelScoped
    @Provides
    fun provideRetryFailedMessageUseCase(messageScope: MessageScope): RetryFailedMessageUseCase =
        messageScope.retryFailedMessage

    @ViewModelScoped
    @Provides
    fun provideSendKnockUseCase(messageScope: MessageScope): SendKnockUseCase =
        messageScope.sendKnock

    @ViewModelScoped
    @Provides
    fun provideToggleReactionUseCase(messageScope: MessageScope): ToggleReactionUseCase =
        messageScope.toggleReaction

    @ViewModelScoped
    @Provides
    fun provideObserveMessageReactionsUseCase(messageScope: MessageScope): ObserveMessageReactionsUseCase =
        messageScope.observeMessageReactions

    @ViewModelScoped
    @Provides
    fun provideObserveMessageReceiptsUseCase(messageScope: MessageScope): ObserveMessageReceiptsUseCase =
        messageScope.observeMessageReceipts

    @ViewModelScoped
    @Provides
    fun providesSendAssetMessageUseCase(messageScope: MessageScope): ScheduleNewAssetMessageUseCase =
        messageScope.sendAssetMessage

    @ViewModelScoped
    @Provides
    fun provideGetPrivateAssetUseCase(messageScope: MessageScope): GetMessageAssetUseCase =
        messageScope.getAssetMessage

    @ViewModelScoped
    @Provides
    fun provideGetNotificationsUseCase(messageScope: MessageScope): GetNotificationsUseCase =
        messageScope.getNotifications

    @ViewModelScoped
    @Provides
    fun provideGetMessageByIdUseCase(messageScope: MessageScope): GetMessageByIdUseCase =
        messageScope.getMessageById

    @ViewModelScoped
    @Provides
    fun provideGetPaginatedMessagesUseCase(messageScope: MessageScope): GetPaginatedFlowOfMessagesByConversationUseCase =
        messageScope.getPaginatedFlowOfMessagesByConversation

    @ViewModelScoped
    @Provides
    fun provideGetConversationMessagesFromSearchQueryUseCase(messageScope: MessageScope): GetConversationMessagesFromSearchQueryUseCase =
        messageScope.getConversationMessagesFromSearchQuery

    @ViewModelScoped
    @Provides
    fun provideGetSearchedConversationMessagePositionUseCase(messageScope: MessageScope): GetSearchedConversationMessagePositionUseCase =
        messageScope.getSearchedConversationMessagePosition
}
