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
package com.wire.android.ui.home.conversations

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.media.audiomessage.AudioFocusHelper
import com.wire.android.media.audiomessage.AudioMessageArgs
import com.wire.android.media.audiomessage.AudioMessageViewModelImpl
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer
import com.wire.android.media.audiomessage.RecordAudioMessagePlayer
import com.wire.android.ui.home.conversations.edit.MessageOptionsMenuArgs
import com.wire.android.ui.home.conversations.edit.MessageOptionsMenuViewModelImpl
import com.wire.android.ui.home.conversations.messages.item.AssetLocalPathArgs
import com.wire.android.ui.home.conversations.messages.item.AssetLocalPathViewModelImpl
import com.wire.android.ui.home.conversations.model.CompositeMessageArgs
import com.wire.android.ui.home.conversations.typing.TypingIndicatorArgs
import com.wire.android.ui.home.conversations.typing.TypingIndicatorViewModelImpl
import com.wire.android.ui.home.conversations.usecase.ObserveMessageForConversationUseCase
import com.wire.android.ui.home.conversations.usecase.ObserveUsersTypingInConversationUseCase
import com.wire.android.ui.home.messagecomposer.actions.SelfDeletingMessageActionArgs
import com.wire.android.ui.home.messagecomposer.actions.SelfDeletingMessageActionViewModelImpl
import com.wire.android.ui.home.messagecomposer.attachments.IsFileSharingEnabledViewModelImpl
import com.wire.android.ui.home.messagecomposer.recordaudio.AudioMediaRecorder
import com.wire.android.ui.home.messagecomposer.recordaudio.GenerateAudioFileWithEffectsUseCase
import com.wire.android.ui.home.messagecomposer.recordaudio.RecordAudioViewModel
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.feature.asset.AudioNormalizedLoudnessBuilder
import com.wire.kalium.logic.feature.asset.GetAssetSizeLimitUseCase
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.message.ObserveMessageByIdUseCase
import com.wire.kalium.logic.feature.message.composite.SendButtonActionMessageUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.user.IsFileSharingEnabledUseCase
import com.wire.android.di.ApplicationContext
import dev.zacsweers.metro.Inject

@Suppress("LongParameterList")
class ScopedMessageViewModelFactory @Inject constructor(
    private val sendButtonActionMessage: SendButtonActionMessageUseCase,
    private val observeMessageForConversation: ObserveMessageForConversationUseCase,
    private val observeUsersTypingInConversation: ObserveUsersTypingInConversationUseCase,
    private val getMessageAsset: GetMessageAssetUseCase,
    private val dispatchers: DispatcherProvider,
    private val observeSelfDeletingMessages: ObserveSelfDeletionTimerSettingsForConversationUseCase,
    private val isFileSharingEnabled: IsFileSharingEnabledUseCase,
    @ApplicationContext private val context: Context,
    private val recordAudioMessagePlayer: RecordAudioMessagePlayer,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val getAssetSizeLimit: GetAssetSizeLimitUseCase,
    private val generateAudioFileWithEffects: GenerateAudioFileWithEffectsUseCase,
    private val currentScreenManager: CurrentScreenManager,
    private val audioMediaRecorder: AudioMediaRecorder,
    private val globalDataStore: GlobalDataStore,
    private val audioNormalizedLoudnessBuilder: AudioNormalizedLoudnessBuilder,
    private val audioFocusHelper: AudioFocusHelper,
    private val kaliumFileSystem: KaliumFileSystem,
    private val audioMessagePlayer: ConversationAudioMessagePlayer,
    private val observeMessageById: ObserveMessageByIdUseCase,
) {
    fun compositeMessageViewModel(savedStateHandle: SavedStateHandle, args: CompositeMessageArgs) =
        CompositeMessageViewModelImpl(
            sendButtonActionMessageUseCase = sendButtonActionMessage,
            savedStateHandle = savedStateHandle,
            scopedArgs = args,
        )

    fun messageOptionsMenuViewModel(args: MessageOptionsMenuArgs) =
        MessageOptionsMenuViewModelImpl(
            observeMessageForConversation = observeMessageForConversation,
            args = args,
        )

    fun typingIndicatorViewModel(args: TypingIndicatorArgs) =
        TypingIndicatorViewModelImpl(
            observeUsersTypingInConversation = observeUsersTypingInConversation,
            args = args,
        )

    internal fun assetLocalPathViewModel(args: AssetLocalPathArgs) =
        AssetLocalPathViewModelImpl(
            getMessageAsset = getMessageAsset,
            dispatchers = dispatchers,
            args = args,
        )

    fun selfDeletingMessageActionViewModel(args: SelfDeletingMessageActionArgs) =
        SelfDeletingMessageActionViewModelImpl(
            dispatchers = dispatchers,
            observeSelfDeletingMessages = observeSelfDeletingMessages,
            args = args,
        )

    fun isFileSharingEnabledViewModel() =
        IsFileSharingEnabledViewModelImpl(isFileSharingEnabledUseCase = isFileSharingEnabled)

    fun recordAudioViewModel() =
        RecordAudioViewModel(
            context = context,
            recordAudioMessagePlayer = recordAudioMessagePlayer,
            observeEstablishedCalls = observeEstablishedCalls,
            getAssetSizeLimit = getAssetSizeLimit,
            generateAudioFileWithEffects = generateAudioFileWithEffects,
            currentScreenManager = currentScreenManager,
            audioMediaRecorder = audioMediaRecorder,
            globalDataStore = globalDataStore,
            audioNormalizedLoudnessBuilder = audioNormalizedLoudnessBuilder,
            audioFocusHelper = audioFocusHelper,
            dispatchers = dispatchers,
            kaliumFileSystem = kaliumFileSystem,
        )

    fun audioMessageViewModel(args: AudioMessageArgs) =
        AudioMessageViewModelImpl(
            audioMessagePlayer = audioMessagePlayer,
            observeMessageById = observeMessageById,
            args = args,
        )
}
