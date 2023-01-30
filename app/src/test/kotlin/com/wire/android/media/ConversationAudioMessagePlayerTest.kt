package com.wire.android.media

import android.content.Context
import android.media.MediaPlayer
import app.cash.turbine.test
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.media.audiomessage.AudioMediaPlayingState
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test

class ConversationAudioMessagePlayerTest {

    @Test
    fun test() = runTest {
        val (_, conversationAudioMessagePlayer) = Arrangement()
            .withSuccessFullAssetFetch()
            .withAudioMediaPlayerReturningCurrentPosition(100)
            .arrange()

        val testAudioMessageId = "some-dummy-message-id"

        conversationAudioMessagePlayer.observableAudioMessagesState.test {
            conversationAudioMessagePlayer.playAudio(
                ConversationId("some-dummy-value", "some.dummy.domain"),
                testAudioMessageId
            )

            //Fetching
            val test1 = awaitItem()
            assertEquals(
                expected = test1[testAudioMessageId],
                actual = AudioState(AudioMediaPlayingState.Fetching, 0, 0)
            )
            //SuccessFullFetching
            val test2 = awaitItem()
            assertEquals(
                expected = test1[testAudioMessageId],
                actual = AudioState(AudioMediaPlayingState.SuccessFullFetching, 0, 0)
            )
            //Playing
            val test3 = awaitItem()
            assertEquals(
                expected = test1[testAudioMessageId],
                actual = AudioState(AudioMediaPlayingState.Playing, 0, 0)
            )
            // TotalTimeUpdate
            val test4 = awaitItem()
            assertEquals(
                expected = test1[testAudioMessageId],
                actual = AudioState(AudioMediaPlayingState.Playing, 0, 0)
            )
            cancelAndIgnoreRemainingEvents()
        }

    }

    class Arrangement {

        @MockK
        private lateinit var context: Context

        @MockK
        private lateinit var getMessageAssetUseCase: GetMessageAssetUseCase

        @MockK
        private lateinit var mediaPlayer: MediaPlayer

        private val conversationAudioMessagePlayer by lazy {
            ConversationAudioMessagePlayer(
                context,
                mediaPlayer,
                getMessageAssetUseCase,
            )
        }

        init {
            MockKAnnotations.init(this, relaxed = true)
        }

        fun withSuccessFullAssetFetch() = apply {
            coEvery { getMessageAssetUseCase.invoke(any(), any()) } returns CompletableDeferred(
                MessageAssetResult.Success(
                    decodedAssetPath = FakeKaliumFileSystem().selfUserAvatarPath(),
                    assetSize = 0,
                    assetName = "some-dummy-asset-name"
                )
            )
        }

        fun withFailingAssetFetch() = apply {
            coEvery { getMessageAssetUseCase.invoke(any(), any()) } returns CompletableDeferred(
                MessageAssetResult.Failure(CoreFailure.Unknown(IllegalStateException()))
            )
        }

        fun withAudioMediaPlayerReturningCurrentPosition(currentPosition: Int) = apply {
            every { mediaPlayer.currentPosition } returns currentPosition
        }

        fun arrange() = this to conversationAudioMessagePlayer
    }
}
