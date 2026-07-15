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
package com.wire.android.tests.core.pages

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import java.io.File
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class CallingPage(private val device: UiDevice) {
    private val acceptCallButton = UiSelectorParams(description = "Accept call")

    private val hangUpCallButton = UiSelectorParams(description = "Hang up call")

    private val minimiseCallButton = UiSelectorParams(description = "Drop down arrow")

    private val restoreCallButton = UiSelectorParams(text = "RETURN TO CALL")

    private val joinCallButton = UiSelectorParams(text = "Join")

    private val turnCameraOnButton = UiSelectorParams(description = "Turn camera on")

    private val unmuteCallButton = UiSelectorParams(description = "Unmute call")

    fun iAcceptCall(): CallingPage {
        UiWaitUtils.waitElement(acceptCallButton, timeout = UiWaitUtils.VERY_LONG_TIMEOUT).click()
        return this
    }

    fun iSeeParticipantInOngoingOneOnOneCall(participantName: String): CallingPage {
        try {
            UiWaitUtils.waitElement(hangUpCallButton)
        } catch (e: AssertionError) {
            throw AssertionError("Ongoing 1:1 call not displayed", e)
        }
        try {
            UiWaitUtils.waitElement(
                UiSelectorParams(text = participantName),
                timeout = UiWaitUtils.VERY_LONG_TIMEOUT
            )
        } catch (e: AssertionError) {
            throw AssertionError("User '$participantName' is not visible in the ongoing 1:1 call", e)
        }
        return this
    }

    fun iSeeParticipantInOngoingOneOnOneVideoCall(participantName: String): CallingPage {
        try {
            UiWaitUtils.waitElement(hangUpCallButton)
            UiWaitUtils.waitElement(
                UiSelectorParams(text = participantName),
                timeout = UiWaitUtils.VERY_LONG_TIMEOUT
            )
        } catch (e: AssertionError) {
            throw AssertionError("User '$participantName' is not visible in the ongoing 1:1 video call", e)
        }
        return this
    }

    fun iDoubleTapToMaximizeCallTile(participantName: String): CallingPage {
        val participantTile = UiWaitUtils.waitElement(
            UiSelectorParams(text = participantName),
            timeout = UiWaitUtils.VERY_LONG_TIMEOUT
        )
        val bounds = participantTile.visibleBounds
        repeat(2) {
            device.click(bounds.centerX(), bounds.centerY())
            UiWaitUtils.waitFor(100.milliseconds)
        }
        return this
    }

    fun iDoubleTapToMinimizeCallTile(participantName: String): CallingPage {
        return iDoubleTapToMaximizeCallTile(participantName)
    }

    fun iDoNotSeeUserInCallTile(userName: String): CallingPage {
        UiWaitUtils.waitUntilGoneOrThrow(
            selector = By.hasChild(By.res("User avatar"))
                .hasDescendant(By.text(" $userName")),
            timeout = UiWaitUtils.MEDIUM_TIMEOUT,
            errorMessage = "User '$userName' is still visible in call tile"
        )
        return this
    }

    fun iSeeQrCodeContaining(expectedValue: String): CallingPage {
        val decoded = mutableSetOf<String>()
        val qrCodeFound = UiWaitUtils.retryUntilTimeout(
            timeout = UiWaitUtils.VERY_LONG_TIMEOUT,
            pollingInterval = 1.seconds
        ) {
            decoded.clear()
            decoded.addAll(readQrCodesFromScreenshot())
            decoded.contains(expectedValue)
        }

        if (!qrCodeFound) {
            throw AssertionError("QR code '$expectedValue' is not visible in the video grid. Found: $decoded")
        }
        return this
    }

    // Maximize the tile first because small screens make QR codes hard to decode.
    fun iSeeQrCodeContaining(userName: String, expectedValue: String): CallingPage {
        iDoubleTapToMaximizeCallTile(userName)
        iSeeQrCodeContaining(expectedValue)
        return iDoubleTapToMinimizeCallTile(userName)
    }

    private fun readQrCodesFromScreenshot(): List<String> {
        val screenshotFile = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "calling-video-grid.png"
        )
        val screenshot = if (device.takeScreenshot(screenshotFile)) {
            BitmapFactory.decodeFile(screenshotFile.absolutePath)
        } else {
            null
        }

        return screenshot?.use { bitmap ->
            readQrCodes(bitmap)
        }.orEmpty()
    }

    fun iSeeOngoingGroupCall(): CallingPage {
        try {
            UiWaitUtils.waitElement(hangUpCallButton)
        } catch (e: AssertionError) {
            throw AssertionError("Ongoing call not displayed", e)
        }
        return this
    }

    fun iMinimiseOngoingCall(): CallingPage {
        UiWaitUtils.waitElement(minimiseCallButton).click()
        return this
    }

    fun iRestoreOngoingCall(): CallingPage {
        UiWaitUtils.waitElement(restoreCallButton).click()
        return this
    }

    fun iTurnCameraOn(): CallingPage {
        UiWaitUtils.waitElement(turnCameraOnButton).click()
        return this
    }

    fun iUnmuteMyself(): CallingPage {
        UiWaitUtils.waitElement(unmuteCallButton).click()
        return this
    }

    fun iTapOnHangUpButton(): CallingPage {
        UiWaitUtils.waitElement(hangUpCallButton).click()
        return this
    }

    fun iSeeJoinButtonInGroupConversationView(): CallingPage {
        try {
            UiWaitUtils.waitElement(joinCallButton)
        } catch (e: AssertionError) {
            throw AssertionError("Join button is not visible in group conversation view", e)
        }
        return this
    }

    fun iDoNotSeeOngoingGroupCall(): CallingPage {
        try {
            UiWaitUtils.waitElement(hangUpCallButton, timeout = 15_000.milliseconds)
        } catch (e: AssertionError) {
            return this
        }
        throw AssertionError("Ongoing call still displayed")
    }

    fun iDoNotSeeOngoingOneOnOneCall(): CallingPage {
        return iDoNotSeeOngoingGroupCall()
    }

    private fun readQrCodes(bitmap: Bitmap): List<String> {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val hints = mapOf(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.ALSO_INVERTED to true
        )

        return runCatching {
            QRCodeMultiReader()
                .decodeMultiple(binaryBitmap, hints)
                .map { it.text }
        }.getOrDefault(emptyList())
    }

    private inline fun <T> Bitmap.use(block: (Bitmap) -> T): T {
        return try {
            block(this)
        } finally {
            recycle()
        }
    }
}
