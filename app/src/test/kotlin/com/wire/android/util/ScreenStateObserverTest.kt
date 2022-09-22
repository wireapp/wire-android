@file:OptIn(ExperimentalCoroutinesApi::class)

package com.wire.android.util

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import app.cash.turbine.test
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ScreenStateObserverTest {

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    suspend fun `given a intent action screen off, screenStateObserver should return false`() = runTest {
        val (arrangement, screenStateObserver) =
            Arrangement()
                .withRegisteredReceiver(Intent.ACTION_SCREEN_OFF)
                .withScreenLocked()
                .arrange()


        screenStateObserver.screenStateFlow.test {
            awaitItem()
            val intent = Intent(Intent.ACTION_SCREEN_OFF)
            screenStateObserver.onReceive(arrangement.context, intent)
            awaitItem() shouldBeEqualTo false
        }
    }

    @Test
    suspend fun `given a intent action screen on, screenStateObserver should return true`() = runTest {
        val (arrangement, screenStateObserver) =
            Arrangement()
                .withRegisteredReceiver(Intent.ACTION_SCREEN_ON)
                .withScreenLocked()
                .arrange()


        screenStateObserver.screenStateFlow.test {
            awaitItem()
            val intent = Intent(Intent.ACTION_SCREEN_ON)
            screenStateObserver.onReceive(arrangement.context, intent)
            awaitItem() shouldBeEqualTo true
        }
    }

    private class Arrangement {

        val context: Context = mockk();

        var screenStateObserver: ScreenStateObserver = ScreenStateObserver(
            context = context
        )

        fun withRegisteredReceiver(actionIntent: String) = apply {
            every { context.registerReceiver(any(), any()) } returns Intent(actionIntent)
        }

        fun withScreenLocked() = apply {
            every { (context.getSystemService(any()) as PowerManager).isInteractive } returns false
        }

        fun arrange() = this to screenStateObserver

    }

}
