package com.wire.android.ui.debug

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.pm.PackageInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class StartServiceReceiverTest {
    private val manager = mockk<ActivityManager>()
    private val context = mockk<Context>(relaxed = true)
    private val receiver = StartServiceReceiver()

    init {
        every { context.packageName } returns PACKAGE
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns manager
        every { context.packageManager.getPackageInfo(PACKAGE, 0) } returns PackageInfo().apply { lastUpdateTime = UPDATE_TIME }
    }

    @Test
    fun reopensForegroundOrVisibleAppKilledByCurrentUpdate() {
        for (importance in listOf(RunningAppProcessInfo.IMPORTANCE_FOREGROUND, RunningAppProcessInfo.IMPORTANCE_VISIBLE)) {
            history(exit(importance = importance))
            assertTrue(receiver.wasVisibleBeforeUpdate(context))
        }
    }

    @Test
    fun doesNotTreatForegroundServiceAsVisibleUi() {
        history(exit(importance = RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE))
        assertFalse(receiver.wasVisibleBeforeUpdate(context))
    }

    @Test
    fun ignoresRestartProcessEvenWhenItsExitIsNewer() {
        history(exit(name = "$PACKAGE:restart", time = UPDATE_TIME + 10), exit())
        assertTrue(receiver.wasVisibleBeforeUpdate(context))
    }

    @Test
    fun doesNotReuseOlderUpdateExitWhenMainProcessHasExitedAgain() {
        history(exit(), exit(time = UPDATE_TIME + 10, reason = ApplicationExitInfo.REASON_CRASH))
        assertFalse(receiver.wasVisibleBeforeUpdate(context))
    }

    @Test
    fun ignoresExitFromEarlierUpdate() {
        history(exit(time = UPDATE_TIME - 300_000))
        assertFalse(receiver.wasVisibleBeforeUpdate(context))
    }

    @Test
    fun skipsUiWhenHistoryIsMissingOrUnavailable() {
        history()
        assertFalse(receiver.wasVisibleBeforeUpdate(context))
        every { manager.getHistoricalProcessExitReasons(PACKAGE, 0, 0) } throws SecurityException()
        assertFalse(receiver.wasVisibleBeforeUpdate(context))
    }

    @Test
    @Config(sdk = [33])
    fun doesNotQueryHistoryOnOlderAndroid() {
        assertFalse(receiver.wasVisibleBeforeUpdate(context))
        verify(exactly = 0) { manager.getHistoricalProcessExitReasons(any(), any(), any()) }
    }

    private fun history(vararg exits: ApplicationExitInfo) {
        every { manager.getHistoricalProcessExitReasons(PACKAGE, 0, 0) } returns exits.toList()
    }

    private fun exit(
        name: String = PACKAGE,
        time: Long = UPDATE_TIME - 100,
        reason: Int = ApplicationExitInfo.REASON_PACKAGE_UPDATED,
        importance: Int = RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    ): ApplicationExitInfo = mockk {
        every { processName } returns name
        every { timestamp } returns time
        every { getReason() } returns reason
        every { getImportance() } returns importance
    }

    private companion object {
        const val PACKAGE = "com.wire.test"
        const val UPDATE_TIME = 1_000_000L
    }
}
