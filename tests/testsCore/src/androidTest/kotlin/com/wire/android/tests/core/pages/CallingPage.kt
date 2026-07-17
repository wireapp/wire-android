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
import android.graphics.Rect
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class CallingPage(private val device: UiDevice) {
    private val acceptCallButton = UiSelectorParams(description = "Accept call")

    private val hangUpCallButton = UiSelectorParams(description = "Hang up call")

    private val minimiseCallButton = UiSelectorParams(description = "Drop down arrow")

    private val restoreCallButton = UiSelectorParams(text = "RETURN TO CALL")

    private val joinCallButton = UiSelectorParams(text = "Join")

    private val collapsedParticipantsList = UiSelectorParams(description = "Collapsed list of participants")

    private val expandedParticipantsList = UiSelectorParams(description = "Expanded list of participants")

    private val muteCallButton = UiSelectorParams(description = "Mute call")

    private val turnCameraOnButton = UiSelectorParams(description = "Turn camera on")

    private val turnCameraOffButton = UiSelectorParams(description = "Turn camera off")

    private val unmuteCallButton = UiSelectorParams(description = "Unmute call")

    private val turnSpeakerOnButton = UiSelectorParams(description = "Turn speaker on")

    private val turnSpeakerOffButton = UiSelectorParams(description = "Turn speaker off")

    private val showInCallReactionsButton = UiSelectorParams(description = "Show in call reactions panel")

    private val hideInCallReactionsButton = UiSelectorParams(description = "Hide in call reactions panel")

    private val participantCameraOnIcon = UiSelectorParams(description = "Camera on")

    private val participantMicrophoneOnIcon = UiSelectorParams(description = "Microphone on")

    private val participantMicrophoneOffIcon = UiSelectorParams(description = "Microphone off")

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

    fun iExpandParticipantSheet(): CallingPage {
        UiWaitUtils.waitElement(collapsedParticipantsList, timeout = UiWaitUtils.VERY_SHORT_TIMEOUT).click()
        UiWaitUtils.waitElement(expandedParticipantsList, timeout = UiWaitUtils.VERY_SHORT_TIMEOUT)
        return this
    }

    fun iSeeParticipantsCount(
        expectedCount: Int,
        timeout: Duration = UiWaitUtils.VERY_SHORT_TIMEOUT
    ): CallingPage {
        try {
            UiWaitUtils.waitElement(
                UiSelectorParams(text = "PARTICIPANTS ($expectedCount)"),
                timeout = timeout
            )
        } catch (e: AssertionError) {
            throw AssertionError("Participants count '$expectedCount' is not visible", e)
        }
        return this
    }

    fun iSeeUserInParticipantList(
        userName: String,
        timeout: Duration = UiWaitUtils.VERY_SHORT_TIMEOUT
    ): CallingPage {
        try {
            UiWaitUtils.waitElement(
                UiSelectorParams(text = userName),
                timeout = timeout
            )
        } catch (e: AssertionError) {
            throw AssertionError("User '$userName' is not visible in participant list", e)
        }
        return this
    }

    fun iDoNotSeeUserInParticipantList(userName: String): CallingPage {
        val userIsGone = UiWaitUtils.retryUntilTimeout(
            timeout = UiWaitUtils.MEDIUM_TIMEOUT,
            pollingInterval = UiWaitUtils.POLLING_DEFAULT
        ) {
            UiWaitUtils.findElementOrNull(UiSelectorParams(text = userName)) == null
        }

        if (!userIsGone) {
            throw AssertionError("User '$userName' is still visible in participant list")
        }
        return this
    }

    fun iSeeGuestBadgeForUser(userName: String): CallingPage {
        val guestBadgeVisible = UiWaitUtils.retryUntilTimeout(
            timeout = UiWaitUtils.VERY_SHORT_TIMEOUT,
            pollingInterval = UiWaitUtils.POLLING_DEFAULT
        ) {
            isGuestBadgeVisibleForUser(userName)
        }

        if (!guestBadgeVisible) {
            throw AssertionError("Guest badge is not visible for user '$userName'")
        }
        return this
    }

    fun iTapParticipantInList(userName: String): CallingPage {
        UiWaitUtils.waitElement(
            UiSelectorParams(text = userName),
            timeout = UiWaitUtils.VERY_SHORT_TIMEOUT
        ).click()
        return this
    }

    fun iSeeParticipantsOrderedAlphabetically(participantNames: List<String>): CallingPage {
        val actualOrder = participantNames
            .map { participantName ->
                val participant = UiWaitUtils.waitElement(
                    UiSelectorParams(text = participantName),
                    timeout = UiWaitUtils.VERY_SHORT_TIMEOUT
                )
                participantName to participant.visibleBounds.top
            }
            .sortedBy { it.second }
            .map { it.first }
        val expectedOrder = participantNames.sorted()

        if (actualOrder != expectedOrder) {
            throw AssertionError("Participants are not ordered alphabetically. Expected: $expectedOrder, actual: $actualOrder")
        }
        return this
    }

    fun iSeeParticipantMuted(userName: String): CallingPage {
        return iSeeParticipantStatusIcon(userName, participantMicrophoneOffIcon.description ?: "", "muted")
    }

    fun iSeeParticipantUnmuted(userName: String): CallingPage {
        return iSeeParticipantStatusIcon(userName, participantMicrophoneOnIcon.description ?: "", "unmuted")
    }

    fun iSeeParticipantCameraOn(userName: String): CallingPage {
        return iSeeParticipantStatusIcon(userName, participantCameraOnIcon.description ?: "", "camera on")
    }

    fun iSeeParticipantCameraOff(userName: String): CallingPage {
        return iDoNotSeeParticipantStatusIcon(userName, participantCameraOnIcon.description ?: "", "camera on")
    }

    fun iSeeCallControls(): CallingPage {
        val missingControls = mutableListOf<String>()

        val microphoneButtonVisible = UiWaitUtils.waitAnyVisible(
            listOf(muteCallButton, unmuteCallButton),
            timeout = UiWaitUtils.VERY_SHORT_TIMEOUT
        ) != null
        if (!microphoneButtonVisible) {
            missingControls.add("Microphone")
        }

        val cameraButtonVisible = UiWaitUtils.waitAnyVisible(
            listOf(turnCameraOnButton, turnCameraOffButton),
            timeout = UiWaitUtils.VERY_SHORT_TIMEOUT
        ) != null
        if (!cameraButtonVisible) {
            missingControls.add("Camera")
        }

        val speakerButtonVisible = UiWaitUtils.waitAnyVisible(
            listOf(turnSpeakerOnButton, turnSpeakerOffButton),
            timeout = UiWaitUtils.VERY_SHORT_TIMEOUT
        ) != null
        if (!speakerButtonVisible) {
            missingControls.add("Speaker")
        }

        val inCallReactionsButtonVisible = UiWaitUtils.waitAnyVisible(
            listOf(showInCallReactionsButton, hideInCallReactionsButton),
            timeout = UiWaitUtils.VERY_SHORT_TIMEOUT
        ) != null
        if (!inCallReactionsButtonVisible) {
            missingControls.add("In-call reactions")
        }

        try {
            UiWaitUtils.waitElement(hangUpCallButton, timeout = UiWaitUtils.VERY_SHORT_TIMEOUT)
        } catch (_: AssertionError) {
            missingControls.add("Hang up")
        }

        if (missingControls.isNotEmpty()) {
            throw AssertionError("Call controls are not visible: ${missingControls.joinToString()}")
        }
        return this
    }

    private fun iSeeParticipantStatusIcon(
        userName: String,
        statusDescription: String,
        statusName: String
    ): CallingPage {
        val statusIconVisible = UiWaitUtils.retryUntilTimeout(
            timeout = UiWaitUtils.VERY_SHORT_TIMEOUT,
            pollingInterval = UiWaitUtils.POLLING_DEFAULT
        ) {
            isParticipantStatusIconVisible(userName, statusDescription)
        }

        if (!statusIconVisible) {
            throw AssertionError("User '$userName' does not show '$statusName' in participant list")
        }
        return this
    }

    private fun iDoNotSeeParticipantStatusIcon(
        userName: String,
        statusDescription: String,
        statusName: String
    ): CallingPage {
        UiWaitUtils.waitElement(
            UiSelectorParams(text = userName),
            timeout = UiWaitUtils.VERY_SHORT_TIMEOUT
        )

        val statusIconHidden = UiWaitUtils.retryUntilTimeout(
            timeout = UiWaitUtils.VERY_SHORT_TIMEOUT,
            pollingInterval = UiWaitUtils.POLLING_DEFAULT
        ) {
            !isParticipantStatusIconVisible(userName, statusDescription)
        }

        if (!statusIconHidden) {
            throw AssertionError("User '$userName' still shows '$statusName' in participant list")
        }
        return this
    }

    private fun isParticipantStatusIconVisible(userName: String, statusDescription: String): Boolean {
        val participantBounds = runCatching {
            UiWaitUtils.findElementOrNull(UiSelectorParams(text = userName))?.visibleBounds
        }.getOrNull() ?: return false

        return device.findObjects(By.desc(statusDescription)).any { statusIcon ->
            runCatching {
                statusIcon.visibleBounds.isVerticallyAlignedWith(participantBounds)
            }.getOrDefault(false)
        }
    }

    private fun isGuestBadgeVisibleForUser(userName: String): Boolean {
        val participantBounds = runCatching {
            UiWaitUtils.findElementOrNull(UiSelectorParams(text = userName))?.visibleBounds
        }.getOrNull() ?: return false

        return device.findObjects(By.text("Guest")).any { guestBadge ->
            runCatching {
                guestBadge.visibleBounds.isVerticallyAlignedWith(participantBounds)
            }.getOrDefault(false)
        }
    }

    private fun Rect.isVerticallyAlignedWith(participantBounds: Rect): Boolean {
        val rowTop = participantBounds.top - participantBounds.height()
        val rowBottom = participantBounds.bottom + participantBounds.height()
        return centerY() in rowTop..rowBottom
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
