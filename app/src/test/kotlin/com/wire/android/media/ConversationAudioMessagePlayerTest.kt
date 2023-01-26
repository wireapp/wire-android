package com.wire.android.media

import android.content.Context
import app.cash.turbine.test
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ConversationAudioMessagePlayerTest {

    private val context: Context = mockk<Context>()

    private val getMessageAssetUseCase: GetMessageAssetUseCase = mockk<GetMessageAssetUseCase>()

    private val conversationAudioMessagePlayerTest = ConversationAudioMessagePlayer(context, getMessageAssetUseCase)

    @Test
    fun `given play is called, when audio message is playing, then stop playing`() = runTest {
        conversationAudioMessagePlayerTest.observableAudioMessagesState.test {
            val test = awaitItem()
        }
    }

}
