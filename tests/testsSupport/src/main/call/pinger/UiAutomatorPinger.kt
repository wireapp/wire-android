/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package call.pinger

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Logger

object UiAutomatorPinger {
    private val log = Logger.getLogger("UiAutomatorPinger")
    private val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    private const val PINGER_INTERVAL_SECONDS = 30L
    private val pingerJob = AtomicReference<Job?>()

    fun startPinging() {
        if (pingerJob.get() != null) {
            log.warning("Pinger already running — stop it before starting again.")
            return
        }

        log.info("Starting UIAutomator pinger every $PINGER_INTERVAL_SECONDS seconds")

        val job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    log.info("Pinging device (waking up screen)")
                    wakeUpScreen()
                } catch (e: Exception) {
                    log.warning("Failed to ping device: ${e.message}")
                }
                delay(PINGER_INTERVAL_SECONDS * 1000)
            }
        }

        pingerJob.set(job)
    }

    fun stopPinging() {
        val job = pingerJob.getAndSet(null)
        if (job != null && job.isActive) {
            log.info("Stopping UIAutomator pinger")
            job.cancel()
        } else {
            log.warning("No active pinger to stop")
        }
    }

    private fun wakeUpScreen() {
        if (!device.isScreenOn) {
            device.wakeUp()
            log.info("Device screen awakened")
        } else {
            log.info("Device screen already on")
        }
    }
}
