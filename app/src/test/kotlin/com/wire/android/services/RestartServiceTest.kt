package com.wire.android.services

import android.app.Application
import android.app.Service
import android.content.Intent
import android.content.Context
import android.content.ComponentName
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.os.Process
import android.os.RemoteException
import android.os.ResultReceiver
import com.wire.android.ui.WireActivity
import com.wire.android.ui.debug.StartServiceReceiver
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowProcess
import org.robolectric.shadow.api.Shadow
import org.robolectric.util.ReflectionHelpers.ClassParameter

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class RestartServiceTest {
    @Test
    fun waitsForMainProcessDeathBeforeRequestingServiceRecovery() = verifyRecovery(reopenApp = false)

    @Test
    fun reopensVisibleAppOnlyAfterMainProcessDeath() = verifyRecovery(reopenApp = true)

    @Test
    fun stillRecoversServicesWhenUiRelaunchFails() = verifyRecovery(reopenApp = true, launchFails = true)

    private fun verifyRecovery(reopenApp: Boolean, launchFails: Boolean = false) {
        val broadcasts = mutableListOf<Intent>()
        val service = spyk(Robolectric.buildService(RestartService::class.java).create().get())
        every { service.sendBroadcast(capture(broadcasts)) } returns Unit
        val launches = mutableListOf<Intent>()
        every { service.startActivity(capture(launches)) } answers {
            if (launchFails) throw SecurityException("Background launch denied")
            Unit
        }
        val token = mockk<IBinder>()
        val deathRecipient = slot<IBinder.DeathRecipient>()
        every { token.linkToDeath(capture(deathRecipient), 0) } returns Unit

        var ready = false
        lateinit var proceed: ResultReceiver
        val callback = object : ResultReceiver(null) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                assertTrue(deathRecipient.isCaptured)
                @Suppress("DEPRECATION")
                val command = parcelRoundTrip(requireNotNull(resultData)).getParcelable<ResultReceiver>("restart_helper_proceed")
                proceed = requireNotNull(command)
                assertEquals(ResultReceiver::class.java, proceed.javaClass)
                ready = true
            }
        }
        val intent = restartIntent(token).putExtra("restart_helper_ready", callback)
        assertEquals(Service.START_NOT_STICKY, service.onStartCommand(intent, 0, 1))
        assertTrue(ready)
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(ShadowProcess.wasKilled(Process.myPid() + 1))
        assertTrue(broadcasts.isEmpty())

        sendThroughBinder(proceed, Bundle().apply { putBoolean("reopen_app", reopenApp) })
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(ShadowProcess.wasKilled(Process.myPid() + 1))
        assertFalse(ShadowProcess.wasKilled(Process.myPid()))
        assertTrue(broadcasts.isEmpty())

        assertTrue(launches.isEmpty())
        deathRecipient.captured.binderDied()
        shadowOf(Looper.getMainLooper()).idle()

        val recovery = broadcasts.single()
        assertEquals(StartServiceReceiver::class.java.name, recovery.component?.className)
        assertEquals("com.wire.android.action.RESTORE_SERVICES_AFTER_RESTART", recovery.action)
        assertTrue(ShadowProcess.wasKilled(Process.myPid()))
        assertEquals(if (reopenApp) 1 else 0, launches.size)
        if (reopenApp) {
            assertEquals(WireActivity::class.java.name, launches.single().component?.className)
            assertTrue(launches.single().flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        }
    }

    @Test
    fun recoversImmediatelyWhenMainProcessIsAlreadyDead() {
        val broadcasts = mutableListOf<Intent>()
        val service = spyk(Robolectric.buildService(RestartService::class.java).create().get())
        every { service.sendBroadcast(capture(broadcasts)) } returns Unit
        val token = mockk<IBinder>()
        every { token.linkToDeath(any(), 0) } throws RemoteException()

        service.onStartCommand(restartIntent(token), 0, 1)

        assertEquals(1, broadcasts.size)
    }

    @Test
    fun ignoresMissingRestartArguments() {
        val broadcasts = mutableListOf<Intent>()
        val service = spyk(Robolectric.buildService(RestartService::class.java).create().get())
        every { service.sendBroadcast(capture(broadcasts)) } returns Unit
        service.onStartCommand(null, 0, 1)
        assertTrue(broadcasts.isEmpty())
    }

    @Test
    fun leavesProcessRunningWhenHelperCannotBeStarted() = runBlocking {
        val context = mockk<Context>()
        every { context.packageName } returns "com.wire.android"
        every { context.startService(any()) } returns null

        assertNull(RestartService.prepareRestart(context))
    }

    @Test
    fun stopsHelperWhenReadinessTimesOut() = runBlocking {
        val context = mockk<Context>()
        every { context.packageName } returns "com.wire.android"
        val request = slot<Intent>()
        every {
            context.startService(capture(request))
        } returns ComponentName("com.wire.android", RestartService::class.java.name)
        every { context.stopService(any()) } returns true

        assertNull(RestartService.prepareRestart(context))
        verify(exactly = 1) { context.stopService(any()) }
        @Suppress("DEPRECATION")
        val callback = parcelRoundTrip(requireNotNull(request.captured.extras))
            .getParcelable<ResultReceiver>("restart_helper_ready")!!
        assertEquals(ResultReceiver::class.java, callback.javaClass)
        // A late reply must be ignored without even trying to unpack its contents.
        sendThroughBinder(callback, mockk<Bundle>())
        shadowOf(Looper.getMainLooper()).idle()
        verify(exactly = 1) { context.stopService(any()) }
    }

    // Robolectric's default send() calls onReceiveResult directly, bypassing the parcelled Binder proxy.
    private fun sendThroughBinder(receiver: ResultReceiver, data: Bundle) {
        Shadow.directlyOn<Unit, ResultReceiver>(
            receiver,
            ResultReceiver::class.java,
            "send",
            ClassParameter.from(Int::class.javaPrimitiveType, 0),
            ClassParameter.from(Bundle::class.java, data)
        )
    }

    private fun parcelRoundTrip(bundle: Bundle): Bundle {
        val parcel = Parcel.obtain()
        return try {
            parcel.writeBundle(bundle)
            parcel.setDataPosition(0)
            requireNotNull(parcel.readBundle(ResultReceiver::class.java.classLoader))
        } finally {
            parcel.recycle()
        }
    }

    private fun restartIntent(token: IBinder) = Intent().putExtras(
        Bundle().apply {
            putParcelable("restart_helper_ready", ResultReceiver(null))
            putInt("previous_process_id", Process.myPid() + 1)
            putBinder("previous_process_binder", token)
        }
    )
}
