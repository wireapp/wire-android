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
package com.wire.android.tests.core.pages

import android.media.AudioManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.wire.android.tests.support.UiAutomatorSetup
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import java.util.regex.Pattern

/** Actions for Wire's in-app audio and video players. */
data class DriveMediaPlayerPage(private val device: UiDevice) {
    private val playbackTime get() = By.pkg(UiAutomatorSetup.appPackage).text(Pattern.compile("\\d{1,2}:\\d{2}"))
    private val audioManager = InstrumentationRegistry.getInstrumentation().targetContext
        .getSystemService(AudioManager::class.java)

    fun assertPlayerReady(fileName: String): DriveMediaPlayerPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = fileName))
        val ready = UiWaitUtils.retryUntilTimeout(UiWaitUtils.VERY_LONG_TIMEOUT) {
            val times = device.findObjects(playbackTime)
            times.size == 2 && timeInSeconds(times.last().text) > 0
        }
        assertTrue("The playback screen for '$fileName' did not open with a nonzero duration", ready)
        return this
    }

    // TODO: Use a play/pause accessibility locator when labelled; the control is narrow and centred in both players.
    @Suppress("MagicNumber")
    fun tapPlayPauseButton(): DriveMediaPlayerPage {
        val playerCentre = device.displayWidth / 2
        val control = device.findObjects(By.pkg(UiAutomatorSetup.appPackage).clickable(true))
            .singleOrNull {
                val bounds = it.visibleBounds
                bounds.width() < device.displayWidth / 2 && playerCentre in bounds.left..bounds.right
            }
            ?: throw AssertionError("The player's play/pause control is not visible")
        control.click()
        return this
    }

    fun assertPlaybackAdvances(): DriveMediaPlayerPage {
        val initial = elapsedSeconds()
        val advanced = UiWaitUtils.retryUntilTimeout(UiWaitUtils.SHORT_TIMEOUT) {
            elapsedSeconds() > initial
        }
        assertTrue("The media playback position did not advance", advanced)
        return this
    }

    fun assertPlaybackPaused(): DriveMediaPlayerPage {
        val stopped = UiWaitUtils.retryUntilTimeout(UiWaitUtils.SHORT_WAIT) { !audioManager.isMusicActive }
        assertTrue("Audio output is still active after pausing", stopped)
        val pausedPosition = elapsedSeconds()
        val positionChanged = UiWaitUtils.retryUntilTimeout(UiWaitUtils.VERY_SHORT_TIMEOUT) {
            elapsedSeconds() != pausedPosition
        }
        assertFalse("The playback position continued to advance after pausing", positionChanged)
        return this
    }

    fun assertPlaybackStoppedAfterLeavingPlayer(): DriveMediaPlayerPage {
        val stopped = UiWaitUtils.retryUntilTimeout(UiWaitUtils.SHORT_WAIT) { !audioManager.isMusicActive }
        assertTrue("Playback continued after leaving the media player", stopped)
        return this
    }

    fun navigateBack(): DriveMediaPlayerPage {
        device.pressBack()
        return this
    }

    private fun elapsedSeconds(): Int {
        val time = device.findObjects(playbackTime).firstOrNull()?.text
            ?: throw AssertionError("The media player's elapsed time is not visible")
        return timeInSeconds(time)
    }

    private fun timeInSeconds(time: String): Int {
        val parts = time.split(":").map(String::toInt)
        return parts.first() * SECONDS_PER_MINUTE + parts.last()
    }

    private companion object {
        const val SECONDS_PER_MINUTE = 60
    }
}
