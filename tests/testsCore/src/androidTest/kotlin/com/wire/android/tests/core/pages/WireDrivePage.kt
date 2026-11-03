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

import android.text.format.DateFormat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.regex.Pattern

/** UI actions shared by Wire Drive scenarios; navigation assertions stay scoped to the current folder. */
data class WireDrivePage(private val device: UiDevice) {
    private val newButton = UiSelectorParams(description = "New")
    private val createFolderOption = UiSelectorParams(text = "Create Folder")
    private val uploadFilesOption = UiSelectorParams(text = "Upload Files")
    private val folderNameInput = UiSelectorParams(className = "android.widget.EditText")
    private val uploadButton = UiSelectorParams(text = "Upload")
    private val collapseButtonOnUploadStatusPanel = UiSelectorParams(description = "Drop down arrow")
    private val driveHeading = UiSelectorParams(text = "Drive")
    private val mainNavigationButton = UiSelectorParams(description = "Main navigation")
    private val sharedDriveHeading = UiSelectorParams(text = "Shared Drive")
    private val viewerAccessBanner = UiSelectorParams(text = "Viewer access: you can’t upload or edit files.")
    private val emptyDriveTitle = UiSelectorParams(text = "There are no files or folders yet")
    private val emptyDriveMessage = UiSelectorParams(
        text = "For conversations that use file collaboration, you'll find shared files here."
    )
    private val learnMoreLinkOnEmptyDrive = UiSelectorParams(text = "Learn more")
    private val fileList = By.scrollable(true)
    private val pdfFileName = By.text(Pattern.compile(".*\\.pdf"))
    private val loadingFiles = By.text("Loading files…")

    fun assertDrivePageVisible(): WireDrivePage {
        UiWaitUtils.waitElement(mainNavigationButton)
        UiWaitUtils.waitElement(driveHeading)
        return this
    }

    fun assertEmptyDriveVisible(): WireDrivePage {
        assertDrivePageVisible()
        UiWaitUtils.waitElement(emptyDriveTitle, timeout = UiWaitUtils.MEDIUM_TIMEOUT)
        UiWaitUtils.waitElement(emptyDriveMessage, timeout = UiWaitUtils.SHORT_WAIT)
        return this
    }

    fun tapLearnMoreLinkOnEmptyDrive(): WireDrivePage {
        UiWaitUtils.waitElement(learnMoreLinkOnEmptyDrive, timeout = UiWaitUtils.SHORT_WAIT).click()
        return this
    }

    fun tapMainNavigationButtonOnDrivePage(): WireDrivePage {
        UiWaitUtils.waitElement(mainNavigationButton).click()
        return this
    }

    fun assertSharedDriveVisible(): WireDrivePage {
        UiWaitUtils.waitElement(newButton, timeout = UiWaitUtils.MEDIUM_TIMEOUT)
        return this
    }

    fun assertSharedDriveVisibleWithViewerAccess(): WireDrivePage {
        UiWaitUtils.waitElement(sharedDriveHeading)
        UiWaitUtils.waitElement(viewerAccessBanner)
        UiWaitUtils.waitUntilGoneOrThrow(
            selector = By.desc("New"),
            timeout = UiWaitUtils.VERY_SHORT_TIMEOUT,
            errorMessage = "New button is visible with Viewer access"
        )
        return this
    }

    fun createFolder(folderName: String): WireDrivePage {
        UiWaitUtils.waitElement(newButton).click()
        UiWaitUtils.waitElement(createFolderOption).click()
        UiWaitUtils.waitElement(folderNameInput).text = folderName
        val createSelector = By.clickable(true).enabled(true).hasChild(By.text("Create Folder"))
        val enabled = UiWaitUtils.retryUntilTimeout(UiWaitUtils.VERY_SHORT_TIMEOUT) {
            device.hasObject(createSelector)
        }
        if (!enabled) throw AssertionError("Create Folder button is not enabled for '$folderName'")
        device.findObject(createSelector).click()
        return assertSharedDriveVisible()
    }

    fun openFolder(folderName: String): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(text = folderName)).click()
        return this
    }

    fun openFile(fileName: String): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(text = fileName)).click()
        return this
    }

    fun assertEmptyFolderVisible(): WireDrivePage {
        UiWaitUtils.waitElement(
            UiSelectorParams(text = "There are no files or folders yet"),
            timeout = UiWaitUtils.MEDIUM_TIMEOUT
        )
        return this
    }

    fun navigateBack(): WireDrivePage {
        device.pressBack()
        return this
    }

    fun tapNewButton(): WireDrivePage {
        UiWaitUtils.waitElement(newButton).click()
        return this
    }

    fun tapUploadFilesOption(): WireDrivePage {
        UiWaitUtils.waitElement(uploadFilesOption).click()
        return this
    }

    fun tapUploadButtonOnUploadConfirmationDialog(): WireDrivePage {
        UiWaitUtils.waitElement(uploadButton).click()
        return this
    }

    fun assertUploadCompleteVisible(): WireDrivePage {
        UiWaitUtils.waitElement(
            UiSelectorParams(text = "Upload complete"),
            timeout = UiWaitUtils.VERY_LONG_TIMEOUT
        )
        return this
    }

    fun tapCollapseButtonOnUploadStatusPanel(): WireDrivePage {
        UiWaitUtils.waitElement(collapseButtonOnUploadStatusPanel).click()
        UiWaitUtils.waitElement(newButton, timeout = UiWaitUtils.SHORT_WAIT)
        return this
    }

    fun assertFolderVisibleOnce(folderName: String): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(text = folderName), timeout = UiWaitUtils.STABLE_TIMEOUT)
        assertEquals(
            "Expected one folder named '$folderName' in the current folder",
            1,
            device.findObjects(By.text(folderName)).size
        )
        return this
    }

    fun assertFileVisibleOnce(fileName: String): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(text = fileName), timeout = UiWaitUtils.STABLE_TIMEOUT)
        assertEquals(
            "Expected one file named '$fileName' in the current folder",
            1,
            device.findObjects(By.text(fileName)).size
        )
        return this
    }

    fun assertFileTimestampMatchesDeviceTimeFormat(
        fileName: String,
        uploadStartedAt: Instant,
        uploadFinishedAt: Instant
    ): WireDrivePage {
        assertFileVisibleOnce(fileName)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val locale = Locale.getDefault()
        val timePattern = DateFormat.getBestDateTimePattern(locale, if (DateFormat.is24HourFormat(context)) "Hm" else "hm")
        val formatter = DateTimeFormatter.ofPattern("MMM dd, $timePattern", locale).withZone(ZoneId.systemDefault())
        val firstMinute = uploadStartedAt.truncatedTo(ChronoUnit.MINUTES)
        val lastMinute = uploadFinishedAt.truncatedTo(ChronoUnit.MINUTES)
        val expectedTimestamps = (0..ChronoUnit.MINUTES.between(firstMinute, lastMinute)).map { minute ->
            formatter.format(firstMinute.plus(minute, ChronoUnit.MINUTES))
        }
        val timestampPattern = expectedTimestamps.joinToString("|") { timestamp ->
            timestamp.split(' ', '\u00A0', '\u202F')
                .joinToString("[ \\u00A0\\u202F]", transform = Pattern::quote)
        }
        val subtitle = By.text(Pattern.compile("(?:$timestampPattern) (?:in|by) .+"))
        val fileRow = By.clickable(true).hasDescendant(By.text(fileName))
        val timestampVisible = UiWaitUtils.retryUntilTimeout(UiWaitUtils.STABLE_TIMEOUT) {
            device.findObject(fileRow)?.findObject(subtitle) != null
        }
        val rowText = device.findObject(fileRow)?.findObjects(By.clazz("android.widget.TextView"))?.map { it.text }
        assertTrue(
            "Expected '$fileName' to show one of $expectedTimestamps. Visible row text: $rowText",
            timestampVisible
        )
        return this
    }

    fun assertFolderNotVisible(folderName: String): WireDrivePage {
        UiWaitUtils.waitUntilGoneOrThrow(
            selector = By.text(folderName),
            timeout = UiWaitUtils.VERY_SHORT_TIMEOUT,
            errorMessage = "Unexpected folder '$folderName' is visible in the current folder"
        )
        return this
    }

    fun assertFileNotVisible(fileName: String): WireDrivePage {
        UiWaitUtils.waitUntilGoneOrThrow(
            selector = By.text(fileName),
            timeout = UiWaitUtils.VERY_SHORT_TIMEOUT,
            errorMessage = "Unexpected file '$fileName' is visible in the current folder"
        )
        return this
    }

    fun assertAllFilesAppearWithoutDuplicatesWhileScrolling(expectedFileNames: List<String>): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(textMatches = ".*\\.pdf"), timeout = UiWaitUtils.STABLE_TIMEOUT)
        val observedFileNames = mutableSetOf<String>()
        var previousVisibleFileNames = emptySet<String>()
        var reachedListEnd = false
        val reachedEnd = UiWaitUtils.retryUntilTimeout(UiWaitUtils.VERY_LONG_TIMEOUT) {
            UiWaitUtils.waitUntilGoneOrThrow(
                selector = loadingFiles,
                errorMessage = "Drive is still loading files"
            )
            val visibleFileNames = visiblePdfFileNames()
            assertEquals("Duplicate PDF entries are visible", visibleFileNames.distinct().size, visibleFileNames.size)
            val repeatedFileNames = visibleFileNames.filter { fileName ->
                fileName in observedFileNames && fileName !in previousVisibleFileNames
            }
            assertTrue(
                "Drive repeats PDF entries after scrolling past them: $repeatedFileNames",
                repeatedFileNames.isEmpty()
            )
            observedFileNames += visibleFileNames
            previousVisibleFileNames = visibleFileNames.toSet()
            assertTrue("Drive contains unexpected PDF files", expectedFileNames.containsAll(observedFileNames))
            if (reachedListEnd && observedFileNames == expectedFileNames.toSet()) {
                true
            } else {
                reachedListEnd = !scrollFileList(Direction.DOWN)
                false
            }
        }
        assertTrue(
            "Drive did not finish displaying all PDFs. Missing files: ${expectedFileNames - observedFileNames}",
            reachedEnd
        )
        return this
    }

    fun getFirstVisiblePdfFileName(): String {
        UiWaitUtils.waitElement(UiSelectorParams(textMatches = ".*\\.pdf"), timeout = UiWaitUtils.STABLE_TIMEOUT)
        return visiblePdfFileNames().first()
    }

    fun scrollToFirstFile(fileName: String): WireDrivePage {
        val fileVisible = UiWaitUtils.retryUntilTimeout(UiWaitUtils.VERY_LONG_TIMEOUT) {
            if (fileName in visiblePdfFileNames()) {
                true
            } else {
                scrollFileList(Direction.UP)
                false
            }
        }
        assertTrue("Could not scroll back to the first file '$fileName'", fileVisible)
        return assertFileVisibleOnce(fileName)
    }

    private fun visiblePdfFileNames(): List<String> {
        val list = requireNotNull(device.findObject(fileList)) { "Drive file list is not visible" }
        return list.findObjects(pdfFileName)
            .filter { !it.visibleBounds.isEmpty }
            .sortedBy { it.visibleBounds.top }
            .map { it.text }
    }

    private fun scrollFileList(direction: Direction): Boolean {
        val list = requireNotNull(device.findObject(fileList)) { "Drive file list is not visible" }
        return list.scroll(direction, 0.5f)
    }
}
