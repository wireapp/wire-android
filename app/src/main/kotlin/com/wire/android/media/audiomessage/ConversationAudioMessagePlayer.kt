/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.media.audiomessage

import com.wire.android.di.ApplicationScope
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.mediaplayer.AndroidMediaPlayerPlaybackEngineFactory
import com.wire.android.ui.common.R as commonR
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.message.GetNextAudioMessageInConversationUseCase
import com.wire.kalium.logic.feature.message.GetSenderNameByMessageIdUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.media.player.AudioMediaPlayingState
import com.wire.media.player.AudioPlaybackStateStore
import com.wire.media.player.AudioState
import com.wire.media.player.AudioStateUpdate
import com.wire.media.player.MediaPlaybackCoordinator
import com.wire.media.player.PlaybackEvent
import com.wire.media.player.PlaybackSource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@SingleIn(AppScope::class)
@Suppress("TooManyFunctions")
class ConversationAudioMessagePlayer @Inject constructor(
    engineFactory: AndroidMediaPlayerPlaybackEngineFactory,
    private val platformController: ConversationAudioPlatformController,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    @ApplicationScope private val scope: CoroutineScope,
    private val dispatchers: DispatcherProvider,
) {
    private val playbackCoordinator = MediaPlaybackCoordinator(
        engine = engineFactory.create(),
        scope = scope,
        positionPollIntervalMs = UPDATE_POSITION_INTERVAL_IN_MS,
    )
    private val stateStore = AudioPlaybackStateStore<MessageIdWrapper>()
    private var currentAudioMessageId: MessageIdWrapper? = null
    private var pendingDurationMs: Int? = null

    val observableAudioMessagesState: Flow<Map<MessageIdWrapper, AudioState>> = stateStore.states
    val audioSpeed: Flow<AudioSpeed> = playbackCoordinator.state
        .map { it.speed }
        .distinctUntilChanged()

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            playbackCoordinator.events.collect(::handlePlaybackEvent)
        }
        platformController.setFocusListener(
            onPause = { scope.launch { pauseCurrentAudioMessage() } },
            onResume = { scope.launch { resumeCurrentAudioMessage() } },
        )
    }

    val playingAudioMessageFlow: Flow<PlayingAudioMessage> = observableAudioMessagesState
        .scan(PlayingAudioMessage.None as PlayingAudioMessage) { previous, history ->
            val currentMessageId = currentAudioMessageId
            val state = currentMessageId?.let(history::get)

            when {
                state?.isPlayingOrPausedOrFetching() != true -> PlayingAudioMessage.None
                previous is PlayingAudioMessage.Some && previous.messageId == currentMessageId.messageId ->
                    PlayingAudioMessage.Some(
                        conversationId = currentMessageId.conversationId,
                        messageId = currentMessageId.messageId,
                        authorName = previous.authorName,
                        state = state,
                    )
                else -> {
                    val authorName = getSenderNameByMessageId(currentMessageId.conversationId, currentMessageId.messageId)
                        ?.let(UIText::DynamicString)
                        ?: UIText.StringResource(commonR.string.username_unavailable_label)
                    PlayingAudioMessage.Some(
                        conversationId = currentMessageId.conversationId,
                        messageId = currentMessageId.messageId,
                        authorName = authorName,
                        state = state,
                    )
                }
            }
        }
        .shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)

    suspend fun playAudio(conversationId: ConversationId, messageId: String) {
        val requested = MessageIdWrapper(conversationId, messageId)
        if (requested == currentAudioMessageId) {
            resumeOrPause()
        } else {
            stopCurrentAudioMessage()
            playAudioMessage(requested, stateStore.restoredPosition(requested))
        }
    }

    suspend fun setSpeed(speed: AudioSpeed) {
        playbackCoordinator.setSpeed(speed)
    }

    suspend fun forceToStopCurrentAudioMessage() {
        stopCurrentAudioMessage()
        platformController.stopPlaybackService()
        platformController.abandonFocus()
    }

    suspend fun resumeOrPauseCurrentAudioMessage() {
        if (currentAudioMessageId != null) resumeOrPause()
    }

    suspend fun setPosition(conversationId: ConversationId, messageId: String, position: Int) {
        val key = MessageIdWrapper(conversationId, messageId)
        if (stateStore.state(key) != null) {
            if (currentAudioMessageId == key) playbackCoordinator.seekTo(position)
            stateStore.update(key, AudioStateUpdate.PositionChanged(position))
        }
    }

    suspend fun preloadAudioMessage(conversationId: ConversationId, messageId: String) {
        currentUserId()?.let { userId -> getAssetMessage(userId, conversationId, messageId) }
    }

    private suspend fun playAudioMessage(key: MessageIdWrapper, position: Int?) {
        currentAudioMessageId = key
        pendingDurationMs = null
        val userId = currentUserId() ?: return
        stateStore.update(key, AudioStateUpdate.PlayingStateChanged(AudioMediaPlayingState.Fetching))

        when (val result = getAssetMessage(userId, key.conversationId, key.messageId)) {
            is MessageAssetResult.Success -> {
                stateStore.update(key, AudioStateUpdate.PlayingStateChanged(AudioMediaPlayingState.SuccessfulFetching))
                if (key == currentAudioMessageId) {
                    platformController.requestFocus()
                    playbackCoordinator.prepare(
                        source = PlaybackSource.Local(result.decodedAssetPath),
                        restoredPositionMs = position ?: 0,
                        playWhenReady = true,
                    )
                }
            }
            is MessageAssetResult.Failure ->
                stateStore.update(key, AudioStateUpdate.PlayingStateChanged(AudioMediaPlayingState.Failed))
        }
    }

    private suspend fun resumeOrPause() {
        if (playbackCoordinator.state.value.isPlaying) {
            pauseCurrentAudioMessage()
            platformController.abandonFocus()
        } else {
            platformController.requestFocus()
            resumeCurrentAudioMessage()
        }
    }

    private fun pauseCurrentAudioMessage() {
        currentAudioMessageId?.let { key ->
            if (stateStore.state(key)?.audioMediaPlayingState != AudioMediaPlayingState.Fetching) {
                playbackCoordinator.pause()
            }
        }
    }

    private fun resumeCurrentAudioMessage() {
        currentAudioMessageId?.let { key ->
            if (stateStore.state(key)?.audioMediaPlayingState != AudioMediaPlayingState.Fetching) {
                playbackCoordinator.play()
            }
        }
    }

    private fun stopCurrentAudioMessage() {
        currentAudioMessageId?.let { key ->
            if (stateStore.state(key)?.audioMediaPlayingState != AudioMediaPlayingState.Fetching) {
                playbackCoordinator.stop()
                stateStore.update(key, AudioStateUpdate.PlayingStateChanged(AudioMediaPlayingState.Stopped))
                stateStore.update(key, AudioStateUpdate.PlayingStateChanged(AudioMediaPlayingState.Completed))
                stateStore.update(key, AudioStateUpdate.PositionChanged(0))
                currentAudioMessageId = null
                pendingDurationMs = null
            }
        }
    }

    private suspend fun handlePlaybackEvent(event: PlaybackEvent) {
        val key = currentAudioMessageId ?: return
        when (event) {
            is PlaybackEvent.Ready -> pendingDurationMs = event.durationMs
            PlaybackEvent.Playing -> {
                stateStore.update(key, AudioStateUpdate.PlayingStateChanged(AudioMediaPlayingState.Playing))
                pendingDurationMs?.let { durationMs ->
                    stateStore.update(key, AudioStateUpdate.TotalTimeChanged(durationMs))
                    pendingDurationMs = null
                }
                platformController.startPlaybackService()
            }
            PlaybackEvent.Paused ->
                stateStore.update(key, AudioStateUpdate.PlayingStateChanged(AudioMediaPlayingState.Paused))
            is PlaybackEvent.PositionChanged ->
                stateStore.update(key, AudioStateUpdate.PositionChanged(event.positionMs))
            PlaybackEvent.Completed -> {
                stateStore.update(key, AudioStateUpdate.PlayingStateChanged(AudioMediaPlayingState.Completed))
                stateStore.update(key, AudioStateUpdate.PositionChanged(playbackCoordinator.state.value.durationMs))
                if (!tryToPlayNextAudio(key)) forceToStopCurrentAudioMessage()
            }
            is PlaybackEvent.Failed -> {
                pendingDurationMs = null
                stateStore.update(key, AudioStateUpdate.PlayingStateChanged(AudioMediaPlayingState.Failed))
            }
            else -> Unit
        }
    }

    private val getAssetMessageMutex = Mutex()
    private val getAssetMessageDeferredMap = mutableMapOf<GetAssetMessageKey, Deferred<MessageAssetResult>>()

    private suspend fun getAssetMessage(
        userId: UserId,
        conversationId: ConversationId,
        messageId: String,
    ): MessageAssetResult = withContext(dispatchers.io()) {
        val key = GetAssetMessageKey(userId, conversationId, messageId)
        getAssetMessageMutex.withLock {
            val deferredResult = getAssetMessageDeferredMap[key]
            if (deferredResult == null || deferredResult.isCompletedWithFailure()) {
                coreLogic.getSessionScope(userId).messages.getAssetMessage(conversationId, messageId).also {
                    getAssetMessageDeferredMap[key] = it
                }
            } else {
                deferredResult
            }
        }.await().let { result ->
            if (
                result is MessageAssetResult.Success &&
                !coreLogic.getSessionScope(userId).kaliumFileSystem.exists(result.decodedAssetPath)
            ) {
                getAssetMessageMutex.withLock {
                    coreLogic.getSessionScope(userId).messages.getAssetMessage(conversationId, messageId).also {
                        getAssetMessageDeferredMap[key] = it
                    }
                }.await()
            } else {
                result
            }
        }
    }

    private fun Deferred<MessageAssetResult>.isCompletedWithFailure(): Boolean =
        isCompleted && getCompleted() is MessageAssetResult.Failure

    private suspend fun tryToPlayNextAudio(current: MessageIdWrapper): Boolean {
        val userId = currentUserId() ?: return false
        return when (
            val next = coreLogic.getSessionScope(userId).messages
                .getNextAudioMessageInConversation(current.conversationId, current.messageId)
        ) {
            is GetNextAudioMessageInConversationUseCase.Result.Success -> {
                playAudio(current.conversationId, next.messageId)
                true
            }
            else -> false
        }
    }

    private suspend fun getSenderNameByMessageId(conversationId: ConversationId, messageId: String): String? {
        val userId = currentUserId() ?: return null
        return when (
            val result = coreLogic.getSessionScope(userId).messages.getSenderNameByMessageId(conversationId, messageId)
        ) {
            is GetSenderNameByMessageIdUseCase.Result.Success -> result.name
            else -> null
        }
    }

    private suspend fun currentUserId(): UserId? =
        (coreLogic.getGlobalScope().session.currentSession() as? CurrentSessionResult.Success)?.accountInfo?.userId

    internal fun clear() {
        playbackCoordinator.stop()
        currentAudioMessageId = null
        pendingDurationMs = null
        stateStore.clear()
        platformController.stopPlaybackService()
    }

    data class MessageIdWrapper(val conversationId: ConversationId, val messageId: String)

    private companion object {
        const val UPDATE_POSITION_INTERVAL_IN_MS = 1_000L
    }
}

private typealias GetAssetMessageKey = Triple<UserId, ConversationId, String>
