package com.wire.android.util

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wire.android.BuildConfig
import io.mockk.coEvery
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class WebsocketHelperTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `when MDM enforces persistent websocket, isWebsocketEnabledByDefault returns true`() {
        mockkStatic("com.wire.android.util.extension.GoogleServicesKt")
        coEvery { context.isGoogleServicesAvailable() } returns true

        val result = isWebsocketEnabledByDefault(context, persistentWebSocketEnforcedByMDM = true)
        assertTrue(result)
    }

    @Test
    fun `when GMS not available and MDM not enforced, isWebsocketEnabledByDefault returns true`() {
        mockkStatic("com.wire.android.util.extension.GoogleServicesKt")
        coEvery { context.isGoogleServicesAvailable() } returns false

        val result = isWebsocketEnabledByDefault(context, persistentWebSocketEnforcedByMDM = false)
        assertTrue(result)
    }

    @Test
    fun `when GMS available and MDM not enforced, isWebsocketEnabledByDefault matches BuildConfig flag`() {
        mockkStatic("com.wire.android.util.extension.GoogleServicesKt")
        coEvery { context.isGoogleServicesAvailable() } returns true

        val result = isWebsocketEnabledByDefault(context, persistentWebSocketEnforcedByMDM = false)
        assertEquals(BuildConfig.WEBSOCKET_ENABLED_BY_DEFAULT, result)
    }
}

