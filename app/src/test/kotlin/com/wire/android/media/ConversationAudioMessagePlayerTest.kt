package com.wire.android.media

import android.content.Context
import android.media.MediaPlayer
import app.cash.turbine.test
import com.wire.android.framework.FakeKaliumFileSystem
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer
import com.wire.android.ui.home.conversations.banner.ConversationBannerViewModel
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.Before
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks

class ConversationAudioMessagePlayerTest {

    @Test
    fun test() = runTest {
        val (arrangement, conversationAudioMessagePlayer) = Arrangement().arrange()

        conversationAudioMessagePlayer.observableAudioMessagesState.test(timeoutMs = 1_0000L) {
            conversationAudioMessagePlayer.playAudio(
                ConversationId("some-dummy-value", "some.dummy.domain"),
                "some-dummy-message-id"
            )
            val test = awaitItem()
            cancelAndIgnoreRemainingEvents()
            println()
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
            every {
                mediaPlayer.setOnCompletionListener(any())
            } just runs
            coEvery { getMessageAssetUseCase.invoke(any(), any()) } returns CompletableDeferred(
                MessageAssetResult.Success(
                    decodedAssetPath = FakeKaliumFileSystem().selfUserAvatarPath(),
                    assetSize = 0,
                    assetName = "some-dummy-asset-name"
                )
            )
        }

        fun arrange() = this to conversationAudioMessagePlayer
    }
}
