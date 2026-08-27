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
package com.wire.android.media.audiomessage

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.mediaplayer.AndroidMediaPlayerPlaybackEngineFactory
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.common.error.StorageFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.message.GetNextAudioMessageInConversationUseCase
import com.wire.kalium.logic.feature.message.GetSenderNameByMessageIdUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.media.player.AudioMediaPlayingState
import com.wire.media.player.AudioState
import com.wire.media.player.MediaPlaybackEngine
import com.wire.media.player.PlaybackCommand
import com.wire.media.player.PlaybackCommandResult
import com.wire.media.player.PlaybackEvent
import com.wire.media.player.PlaybackSnapshot
import com.wire.media.player.PlaybackSource
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.Path
import okio.Path.Companion.toOkioPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

private val dispatcher = UnconfinedTestDispatcher()

class ConversationAudioMessagePlayerTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun givenSuccessfulFetch_whenNativeEngineIsReady_thenPlayingStateAndPlatformEffectsAreEmitted() = runTest(dispatcher) {
        val conversationId = testConversationId()
        val messageId = "message"
        val key = ConversationAudioMessagePlayer.MessageIdWrapper(conversationId, messageId)
        val (arrangement, player) = Arrangement(tempDir, backgroundScope)
            .withCurrentSession()
            .withSuccessfulAssetFetch()
            .arrange()

        player.observableAudioMessagesState.test {
            assertTrue(awaitItem().isEmpty())

            player.playAudio(conversationId, messageId)

            awaitState { it[key]?.audioMediaPlayingState == AudioMediaPlayingState.Fetching }
            awaitState { it[key]?.audioMediaPlayingState == AudioMediaPlayingState.SuccessfulFetching }
            val prepare = arrangement.engine.commands.single() as PlaybackCommand.Prepare
            assertInstanceOf(PlaybackSource.Local::class.java, prepare.source)
            assertTrue(prepare.playWhenReady)
            assertEquals(1, arrangement.platform.requestFocusCalls)

            arrangement.engine.emit(PlaybackEvent.Ready(durationMs = 1_000))

            awaitState { states ->
                states[key]?.let { state ->
                    state.audioMediaPlayingState == AudioMediaPlayingState.Playing &&
                        state.totalTimeInMs == AudioState.TotalTimeInMs.Known(1_000)
                } == true
            }
            assertEquals(PlaybackCommand.Play, arrangement.engine.commands.last())
            assertEquals(1, arrangement.platform.startServiceCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun givenPlayingMessage_whenSelectedAgain_thenPlaybackPausesAndResumes() = runTest(dispatcher) {
        val conversationId = testConversationId()
        val (arrangement, player) = Arrangement(tempDir, backgroundScope)
            .withCurrentSession()
            .withSuccessfulAssetFetch()
            .arrange()

        arrangement.start(player, conversationId, "message")

        player.playAudio(conversationId, "message")
        assertEquals(PlaybackCommand.Pause, arrangement.engine.commands.last())
        assertEquals(1, arrangement.platform.abandonFocusCalls)

        player.playAudio(conversationId, "message")
        assertEquals(PlaybackCommand.Play, arrangement.engine.commands.last())
        assertEquals(2, arrangement.platform.requestFocusCalls)
    }

    @Test
    fun givenFocusChanges_whenPlaying_thenNativePlaybackPausesAndResumes() = runTest(dispatcher) {
        val conversationId = testConversationId()
        val (arrangement, player) = Arrangement(tempDir, backgroundScope)
            .withCurrentSession()
            .withSuccessfulAssetFetch()
            .arrange()

        arrangement.start(player, conversationId, "message")

        arrangement.platform.loseFocus()
        assertEquals(PlaybackCommand.Pause, arrangement.engine.commands.last())

        arrangement.platform.regainFocus()
        assertEquals(PlaybackCommand.Play, arrangement.engine.commands.last())
    }

    @Test
    fun givenNextMessage_whenPlaybackCompletes_thenNextMessageIsPreparedWithoutStoppingService() = runTest(dispatcher) {
        val conversationId = testConversationId()
        val (arrangement, player) = Arrangement(tempDir, backgroundScope)
            .withCurrentSession()
            .withSuccessfulAssetFetch()
            .withNextAudioMessage("second")
            .arrange()

        arrangement.start(player, conversationId, "first")
        arrangement.engine.emit(PlaybackEvent.Completed)

        val nextPrepare = arrangement.engine.commands.last() as PlaybackCommand.Prepare
        assertEquals(PlaybackSource.Local(arrangement.assetPath), nextPrepare.source)
        coVerify(exactly = 1) { arrangement.getNextAudioMessage(conversationId, "first") }
        assertEquals(0, arrangement.platform.stopServiceCalls)
        assertEquals(0, arrangement.platform.abandonFocusCalls)
    }

    @Test
    fun givenNoNextMessage_whenPlaybackCompletes_thenPlaybackServiceAndFocusAreReleased() = runTest(dispatcher) {
        val conversationId = testConversationId()
        val (arrangement, player) = Arrangement(tempDir, backgroundScope)
            .withCurrentSession()
            .withSuccessfulAssetFetch()
            .arrange()

        arrangement.start(player, conversationId, "message")
        arrangement.engine.emit(PlaybackEvent.Completed)

        assertEquals(1, arrangement.platform.stopServiceCalls)
        assertEquals(1, arrangement.platform.abandonFocusCalls)
        assertTrue(arrangement.engine.commands.contains(PlaybackCommand.Stop))
    }

    @Test
    fun givenChangedSpeed_whenNextMessageStarts_thenSpeedIsRetainedAcrossPreparation() = runTest(dispatcher) {
        val conversationId = testConversationId()
        val (arrangement, player) = Arrangement(tempDir, backgroundScope)
            .withCurrentSession()
            .withSuccessfulAssetFetch()
            .withNextAudioMessage("second")
            .arrange()

        arrangement.start(player, conversationId, "first")
        player.setSpeed(AudioSpeed.MAX)
        arrangement.engine.emit(PlaybackEvent.Completed)
        arrangement.engine.emit(PlaybackEvent.Ready(durationMs = 2_000))

        assertEquals(
            listOf(PlaybackCommand.SetSpeed(AudioSpeed.MAX), PlaybackCommand.Play),
            arrangement.engine.commands.takeLast(2),
        )
    }

    @Test
    fun givenCachedAssetStillExists_whenMessageIsPlayedAgain_thenCachedResultIsReused() = runTest(dispatcher) {
        val conversationId = testConversationId()
        val (arrangement, player) = Arrangement(tempDir, backgroundScope)
            .withCurrentSession()
            .withSuccessfulAssetFetch(fileExists = true)
            .arrange()

        arrangement.start(player, conversationId, "message")
        player.forceToStopCurrentAudioMessage()
        player.playAudio(conversationId, "message")

        coVerify(exactly = 1) { arrangement.getAssetMessage(conversationId, "message") }
    }

    @Test
    fun givenCachedAssetWasDeleted_whenMessageIsPlayedAgain_thenAssetIsFetchedAgain() = runTest(dispatcher) {
        val conversationId = testConversationId()
        val (arrangement, player) = Arrangement(tempDir, backgroundScope)
            .withCurrentSession()
            .withSuccessfulAssetFetch(fileExists = true)
            .arrange()

        arrangement.start(player, conversationId, "message")
        player.forceToStopCurrentAudioMessage()
        arrangement.withSuccessfulAssetFetch(fileExists = false)
        player.playAudio(conversationId, "message")

        coVerify(exactly = 2) { arrangement.getAssetMessage(conversationId, "message") }
    }

    @Test
    fun givenFailedFetch_whenMessageIsPlayed_thenFailureIsExposedWithoutPreparingNativePlayback() = runTest(dispatcher) {
        val conversationId = testConversationId()
        val key = ConversationAudioMessagePlayer.MessageIdWrapper(conversationId, "message")
        val (arrangement, player) = Arrangement(tempDir, backgroundScope)
            .withCurrentSession()
            .withFailedAssetFetch()
            .arrange()

        player.observableAudioMessagesState.test {
            awaitItem()
            player.playAudio(conversationId, "message")

            awaitState { it[key]?.audioMediaPlayingState == AudioMediaPlayingState.Fetching }
            awaitState { it[key]?.audioMediaPlayingState == AudioMediaPlayingState.Failed }
            assertTrue(arrangement.engine.commands.isEmpty())
            assertEquals(0, arrangement.platform.requestFocusCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun TurbineTestContext<Map<ConversationAudioMessagePlayer.MessageIdWrapper, AudioState>>.awaitState(
        predicate: (Map<ConversationAudioMessagePlayer.MessageIdWrapper, AudioState>) -> Boolean,
    ): Map<ConversationAudioMessagePlayer.MessageIdWrapper, AudioState> {
        while (true) {
            val states = awaitItem()
            if (predicate(states)) return states
        }
    }

    private fun testConversationId() = ConversationId("conversation", "wire.test")
}

private class Arrangement(
    private val tempDir: File,
    private val testScope: CoroutineScope,
) {
    @MockK
    lateinit var coreLogic: CoreLogic

    @MockK
    lateinit var getAssetMessage: GetMessageAssetUseCase

    @MockK
    lateinit var getNextAudioMessage: GetNextAudioMessageInConversationUseCase

    @MockK
    lateinit var getSenderName: GetSenderNameByMessageIdUseCase

    @MockK
    lateinit var kaliumFileSystem: KaliumFileSystem

    val engine = FakePlaybackEngine()
    val platform = FakeConversationAudioPlatformController()
    val assetPath: Path
        get() = File(tempDir, ASSET_NAME).toOkioPath()

    private val engineFactory = mockk<AndroidMediaPlayerPlaybackEngineFactory>()
    private val player by lazy {
        ConversationAudioMessagePlayer(
            engineFactory = engineFactory,
            platformController = platform,
            coreLogic = coreLogic,
            scope = testScope,
            dispatchers = TestDispatcherProvider(dispatcher),
        )
    }

    init {
        MockKAnnotations.init(this, relaxed = true)
        every { engineFactory.create() } returns engine
        every { coreLogic.getSessionScope(any()).messages.getAssetMessage } returns getAssetMessage
        every { coreLogic.getSessionScope(any()).messages.getNextAudioMessageInConversation } returns getNextAudioMessage
        every { coreLogic.getSessionScope(any()).messages.getSenderNameByMessageId } returns getSenderName
        every { coreLogic.getSessionScope(any()).kaliumFileSystem } returns kaliumFileSystem
        every { kaliumFileSystem.exists(any()) } answers { firstArg<Path>().toFile().exists() }
        coEvery { getNextAudioMessage(any(), any()) } returns
            GetNextAudioMessageInConversationUseCase.Result.Failure(StorageFailure.DataNotFound)
        coEvery { getSenderName(any(), any()) } returns
            GetSenderNameByMessageIdUseCase.Result.Failure(StorageFailure.DataNotFound)
    }

    fun withCurrentSession() = apply {
        coEvery { coreLogic.getGlobalScope().session.currentSession() } returns CurrentSessionResult.Success(
            AccountInfo.Valid(UserId("user", "wire.test"))
        )
    }

    fun withSuccessfulAssetFetch(fileExists: Boolean = true) = apply {
        val assetFile = assetPath.toFile()
        if (fileExists) assetFile.createNewFile() else assetFile.delete()
        coEvery { getAssetMessage(any<ConversationId>(), any<String>()) } returns CompletableDeferred(
            MessageAssetResult.Success(
                decodedAssetPath = assetPath,
                assetSize = 0,
                assetName = ASSET_NAME,
            )
        )
    }

    fun withFailedAssetFetch() = apply {
        coEvery { getAssetMessage(any<ConversationId>(), any<String>()) } returns CompletableDeferred(
            MessageAssetResult.Failure(NetworkFailure.NoNetworkConnection(null), false)
        )
    }

    fun withNextAudioMessage(messageId: String) = apply {
        coEvery { getNextAudioMessage(any(), any()) } returns
            GetNextAudioMessageInConversationUseCase.Result.Success(messageId, "asset")
    }

    suspend fun start(
        player: ConversationAudioMessagePlayer,
        conversationId: ConversationId,
        messageId: String,
    ) {
        player.playAudio(conversationId, messageId)
        engine.emit(PlaybackEvent.Ready(durationMs = 1_000))
    }

    fun arrange() = this to player

    private companion object {
        const val ASSET_NAME = "audio.m4a"
    }
}

private class FakePlaybackEngine : MediaPlaybackEngine {
    val commands = mutableListOf<PlaybackCommand>()
    private var listener: ((PlaybackEvent) -> Unit)? = null
    private var prepared = false

    override fun setEventListener(listener: ((PlaybackEvent) -> Unit)?) {
        this.listener = listener
    }

    override fun execute(command: PlaybackCommand): PlaybackCommandResult {
        commands += command
        if (command in preparedCommands && !prepared) return PlaybackCommandResult.Failure("not_prepared")
        when (command) {
            is PlaybackCommand.Prepare -> prepared = false
            PlaybackCommand.Play -> emit(PlaybackEvent.Playing)
            PlaybackCommand.Pause -> emit(PlaybackEvent.Paused)
            PlaybackCommand.Stop -> {
                prepared = false
                emit(PlaybackEvent.Stopped)
            }
            is PlaybackCommand.SeekTo -> emit(PlaybackEvent.PositionChanged(command.positionMs, 0))
            is PlaybackCommand.SetMuted -> emit(PlaybackEvent.MutedChanged(command.muted))
            is PlaybackCommand.SetSpeed -> emit(PlaybackEvent.SpeedChanged(command.speed))
            PlaybackCommand.Release -> emit(PlaybackEvent.Released)
        }
        return PlaybackCommandResult.Executed
    }

    override fun snapshot(): PlaybackSnapshot? = null

    fun emit(event: PlaybackEvent) {
        if (event is PlaybackEvent.Ready) prepared = true
        listener?.invoke(event)
    }

    private companion object {
        val preparedCommands = setOf(
            PlaybackCommand.Play,
            PlaybackCommand.Pause,
        )
    }
}

private class FakeConversationAudioPlatformController : ConversationAudioPlatformController {
    var requestFocusCalls = 0
    var abandonFocusCalls = 0
    var startServiceCalls = 0
    var stopServiceCalls = 0
    private var onPause: () -> Unit = {}
    private var onResume: () -> Unit = {}

    override fun setFocusListener(onPause: () -> Unit, onResume: () -> Unit) {
        this.onPause = onPause
        this.onResume = onResume
    }

    override fun requestFocus(): Boolean {
        requestFocusCalls++
        return true
    }

    override fun abandonFocus() {
        abandonFocusCalls++
    }

    override fun startPlaybackService() {
        startServiceCalls++
    }

    override fun stopPlaybackService() {
        stopServiceCalls++
    }

    fun loseFocus() = onPause()

    fun regainFocus() = onResume()
}
