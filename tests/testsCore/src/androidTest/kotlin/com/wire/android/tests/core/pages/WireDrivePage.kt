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

import android.graphics.Point
import android.text.format.DateFormat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.regex.Pattern
import kotlin.time.Duration

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

    fun assertViewerAccessBannerVisible(viewerAccessBannerText: String): WireDrivePage {
        UiWaitUtils.waitElement(sharedDriveHeading)
        UiWaitUtils.waitElement(UiSelectorParams(text = viewerAccessBannerText))
        return this
    }

    fun assertNewButtonAbsent(): WireDrivePage {
        UiWaitUtils.waitUntilGoneOrThrow(
            selector = By.desc("New"),
            timeout = UiWaitUtils.VERY_SHORT_TIMEOUT,
            errorMessage = "New button is visible with Viewer access"
        )
        return this
    }

    fun assertSharedDriveOptionsButtonAbsent(): WireDrivePage {
        UiWaitUtils.waitUntilGoneOrThrow(
            selector = By.desc("Open files options"),
            timeout = UiWaitUtils.VERY_SHORT_TIMEOUT,
            errorMessage = "Shared Drive options button is visible for a Viewer"
        )
        return this
    }

    fun tapSharedDriveOptionsButton(): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(description = "Open files options"), timeout = UiWaitUtils.SHORT_WAIT).click()
        return this
    }

    fun assertOpenRecycleBinOptionVisible(): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(text = "Open Recycle Bin"), timeout = UiWaitUtils.SHORT_WAIT)
        return this
    }

    fun longPressFileInSharedDrive(fileName: String): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(text = fileName), timeout = UiWaitUtils.STABLE_TIMEOUT).longClick()
        return this
    }

    fun tapFolderForBulkSelection(folderName: String): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(text = folderName), timeout = UiWaitUtils.STABLE_TIMEOUT).click()
        return this
    }

    fun longPressAndDragBetweenSharedDriveRows(startName: String, endName: String): WireDrivePage {
        val start = UiWaitUtils.waitElement(UiSelectorParams(text = startName), timeout = UiWaitUtils.STABLE_TIMEOUT)
        val end = UiWaitUtils.waitElement(UiSelectorParams(text = endName), timeout = UiWaitUtils.STABLE_TIMEOUT).visibleBounds
        // UIAutomator's drag starts with a long press; both endpoints come from the named rows.
        start.drag(Point(end.centerX(), end.centerY()))
        return this
    }

    fun assertBulkSelectionCount(expectedCount: Int): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(text = "$expectedCount selected"), timeout = UiWaitUtils.SHORT_WAIT)
        return this
    }

    fun assertBulkMoveAndDeleteActionsEnabled(): WireDrivePage {
        listOf("Move", "Delete").forEach { action ->
            UiWaitUtils.waitElement(UiSelectorParams(description = action), timeout = UiWaitUtils.SHORT_WAIT)
            val button = device.findObject(By.clickable(true).hasDescendant(By.desc(action)))
            assertTrue("Bulk action '$action' is not enabled", button?.isEnabled == true)
        }
        return this
    }

    fun tapCancelBulkSelectionButton(): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(description = "Cancel selection"), timeout = UiWaitUtils.SHORT_WAIT).click()
        return this
    }

    fun assertBulkSelectionControlsAbsent(): WireDrivePage {
        UiWaitUtils.waitElement(sharedDriveHeading, timeout = UiWaitUtils.SHORT_WAIT)
        assertFalse("Bulk selection count is visible", device.hasObject(By.text(Pattern.compile("\\d+ selected"))))
        listOf("Cancel selection", "Move", "Delete").forEach { control ->
            assertFalse("Bulk selection control '$control' is visible in Shared Drive", device.hasObject(By.desc(control)))
        }
        return this
    }

    fun createFolder(folderName: String): WireDrivePage {
        submitNewFolder(folderName)
        // The creation form's text field contains the same name as the new folder.
        UiWaitUtils.waitUntilGoneOrThrow(
            selector = By.clazz("android.widget.EditText").text(folderName),
            timeout = UiWaitUtils.MEDIUM_TIMEOUT,
            errorMessage = "The Create Folder form did not close for '$folderName'"
        )
        UiWaitUtils.waitElement(UiSelectorParams(text = folderName), timeout = UiWaitUtils.MEDIUM_TIMEOUT)
        return this
    }

    fun submitNewFolder(folderName: String): WireDrivePage {
        UiWaitUtils.waitElement(newButton).click()
        UiWaitUtils.waitElement(createFolderOption).click()
        UiWaitUtils.waitElement(folderNameInput).text = folderName
        val createSelector = By.clickable(true).enabled(true).hasChild(By.text("Create Folder"))
        val enabled = UiWaitUtils.retryUntilTimeout(UiWaitUtils.VERY_SHORT_TIMEOUT) {
            device.hasObject(createSelector)
        }
        if (!enabled) throw AssertionError("Create Folder button is not enabled for '$folderName'")
        device.findObject(createSelector).click()
        return this
    }

    fun openFolder(folderName: String): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(text = folderName)).click()
        return this
    }

    fun openFile(fileName: String): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(text = fileName)).click()
        return this
    }

    fun tapOpenOnFileReadyMessageIfShown(fileName: String): WireDrivePage {
        val readyMessage = "\"$fileName\" ready to open"
        val next = UiWaitUtils.waitAnyVisible(
            selectors = listOf(
                UiSelectorParams(text = readyMessage),
                UiSelectorParams(text = "Can't open this file. File type isn't supported."),
                UiSelectorParams(text = "Chrome"),
                UiSelectorParams(text = "Open with Chrome")
            ),
            timeout = UiWaitUtils.VERY_LONG_TIMEOUT
        )
        if (next?.text == readyMessage) {
            UiWaitUtils.waitElement(UiSelectorParams(text = "Open"), timeout = UiWaitUtils.SHORT_WAIT).click()
        }
        return this
    }

    fun assertFolderHeadingVisible(folderName: String): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(text = folderName), timeout = UiWaitUtils.SHORT_WAIT)
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

    fun assertFileVisibleOnce(fileName: String, timeout: Duration = UiWaitUtils.STABLE_TIMEOUT): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(text = fileName), timeout = timeout)
        assertEquals(
            "Expected one file named '$fileName' in the current folder",
            1,
            device.findObjects(By.text(fileName)).size
        )
        return this
    }

    fun assertSharedDriveVisibleWithoutViewerBanner(): WireDrivePage {
        assertSharedDriveVisible()
        assertFalse("Viewer banner is visible for an Editor", device.hasObject(By.text("Viewer access: you can’t upload or edit files.")))
        return this
    }

    fun tapFileMenu(fileName: String): WireDrivePage = tapNodeMenu(fileName)

    fun tapFolderMenu(folderName: String): WireDrivePage = tapNodeMenu(folderName)

    private fun tapNodeMenu(nodeName: String): WireDrivePage {
        UiWaitUtils.waitElement(
            UiSelectorParams(description = "More options for $nodeName"),
            timeout = UiWaitUtils.STABLE_TIMEOUT
        ).click()
        UiWaitUtils.waitElement(UiSelectorParams(description = "Open"), timeout = UiWaitUtils.SHORT_WAIT)
        return this
    }

    fun dismissViewerAccessBanner(): WireDrivePage {
        UiWaitUtils.waitElement(
            UiSelectorParams(description = "Close access info"),
            timeout = UiWaitUtils.SHORT_WAIT
        ).click()
        return this
    }

    fun assertViewerAccessBannerNoLongerVisible(): WireDrivePage {
        UiWaitUtils.waitElement(sharedDriveHeading, timeout = UiWaitUtils.MEDIUM_TIMEOUT)
        UiWaitUtils.waitUntilGoneOrThrow(
            selector = By.text("Viewer access: you can’t upload or edit files."),
            timeout = UiWaitUtils.SHORT_WAIT,
            errorMessage = "Viewer access banner is still visible after dismissal"
        )
        return this
    }

    fun assertEditorPdfActionsVisible(): WireDrivePage {
        val actions = listOf("Open", "Make available offline", "Add or Remove Tags", "Share via Link", "Move to folder", "Rename", "Delete")
        actions.forEach { action ->
            val option = UiWaitUtils.waitElement(UiSelectorParams(description = action), timeout = UiWaitUtils.SHORT_WAIT)
            assertTrue("Editor PDF action '$action' is not clickable", option.isClickable)
        }
        return this
    }

    fun assertEditorDocumentActionsVisible(): WireDrivePage {
        val actions = listOf(
            "Open",
            "Edit file",
            "See version history",
            "Add or Remove Tags",
            "Share via Link",
            "Move to folder",
            "Rename",
            "Delete"
        )
        actions.forEach { action ->
            val option = UiWaitUtils.waitElement(UiSelectorParams(description = action), timeout = UiWaitUtils.SHORT_WAIT)
            assertTrue("Editor document action '$action' is not clickable", option.isClickable)
        }
        return this
    }

    fun assertViewerPdfMenuActionsInSharedDrive(): WireDrivePage {
        val open = UiWaitUtils.waitElement(UiSelectorParams(description = "Open"), timeout = UiWaitUtils.SHORT_WAIT)
        assertTrue("Open is not clickable for a Viewer PDF", open.isClickable)
        val restrictedActions = listOf(
            "Make available offline",
            "Add or Remove Tags",
            "Share via Link",
            "Move to folder",
            "Rename",
            "Delete"
        )
        restrictedActions.forEach { action ->
            assertFalse(
                "Restricted action '$action' is visible in the Viewer PDF menu in Shared Drive",
                device.hasObject(By.descContains(action))
            )
        }
        return this
    }

    fun assertViewerDocumentMenuActionsInSharedDrive(): WireDrivePage {
        val open = UiWaitUtils.waitElement(UiSelectorParams(description = "Open"), timeout = UiWaitUtils.SHORT_WAIT)
        assertTrue("Open is not clickable for a Viewer document", open.isClickable)
        val restrictedActions = listOf(
            "Edit file",
            "See version history",
            "Add or Remove Tags",
            "Share via Link",
            "Move to folder",
            "Rename",
            "Delete"
        )
        restrictedActions.forEach { action ->
            assertFalse(
                "Restricted action '$action' is visible in the Viewer document menu in Shared Drive",
                device.hasObject(By.descContains(action))
            )
        }
        return this
    }

    fun assertFolderMenuAllowsOnlyOpenInSharedDrive(): WireDrivePage {
        val open = UiWaitUtils.waitElement(UiSelectorParams(description = "Open"), timeout = UiWaitUtils.SHORT_WAIT)
        assertTrue("Open is not clickable in the restricted folder menu", open.isClickable)
        val restrictedActions = listOf(
            "Add or Remove Tags",
            "Share via Link",
            "Move to folder",
            "Rename",
            "Delete"
        )
        restrictedActions.forEach { action ->
            assertFalse(
                "Restricted action '$action' is visible in the folder menu in Shared Drive",
                device.hasObject(By.descContains(action))
            )
        }
        return this
    }

    fun assertViewerShareActionAbsentInSharedDrive(): WireDrivePage {
        assertFalse(
            "Share is visible in the Viewer file menu in Shared Drive",
            device.hasObject(By.desc("Share")) || device.hasObject(By.desc("Share, disabled in viewer access"))
        )
        return this
    }

    fun tapEditFileOptionOnDocumentMenu(): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(description = "Edit file"), timeout = UiWaitUtils.SHORT_WAIT).click()
        return this
    }

    fun assertShareOptionEnabled(): WireDrivePage {
        val share = UiWaitUtils.waitElement(UiSelectorParams(description = "Share"), timeout = UiWaitUtils.SHORT_WAIT)
        assertTrue("Share is not clickable for the downloaded PDF", share.isEnabled && share.isClickable)
        return this
    }

    fun assertFileNameOnActionsMenu(fileName: String): WireDrivePage {
        val open = UiWaitUtils.waitElement(UiSelectorParams(description = "Open"))
        assertTrue("Filename '$fileName' is missing from its actions menu", open.parent.findObject(By.text(fileName)) != null)
        return this
    }

    fun tapShareOption(): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(description = "Share"), timeout = UiWaitUtils.SHORT_WAIT).click()
        return this
    }

    fun assertMakeAvailableOfflineOptionEnabled(): WireDrivePage {
        val action = UiWaitUtils.waitElement(UiSelectorParams(description = "Make available offline"))
        assertTrue("Make available offline is not enabled", action.isEnabled && action.isClickable)
        return this
    }

    fun assertOfflineActionsAbsent(): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(description = "Open"))
        listOf("Make available offline", "Remove offline access").forEach { action ->
            assertFalse("'$action' is visible in this Shared Drive menu", device.hasObject(By.desc(action)))
        }
        return this
    }

    fun assertOfflineDownloadInProgress(fileName: String): WireDrivePage {
        val configuration = Configurator.getInstance()
        val originalIdleTimeout = configuration.waitForIdleTimeout
        // Waiting for UI idle here can let the download finish before the test disconnects.
        configuration.waitForIdleTimeout = 0
        try {
            val downloading = UiWaitUtils.retryUntilTimeout(UiWaitUtils.MEDIUM_TIMEOUT) {
                device.hasObject(
                    By.clickable(true).hasDescendant(By.text(fileName)).hasDescendant(By.text("Tap to cancel download"))
                )
            }
            assertTrue("PDF '$fileName' did not remain in an active offline download; use an interruptible connection", downloading)
        } finally {
            configuration.waitForIdleTimeout = originalIdleTimeout
        }
        return this
    }

    fun tapMakeAvailableOfflineOption(): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(description = "Make available offline")).click()
        return this
    }

    fun assertRemoveOfflineAccessOptionVisible(): WireDrivePage {
        val action = UiWaitUtils.waitElement(UiSelectorParams(description = "Remove offline access"))
        assertTrue("Remove offline access is not clickable", action.isClickable)
        return this
    }

    fun tapRemoveOfflineAccessOption(): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(description = "Remove offline access")).click()
        return this
    }

    fun assertOfflineBannerVisible(): WireDrivePage {
        UiWaitUtils.waitElement(
            UiSelectorParams(text = "You're offline and can see only saved files")
        )
        return this
    }

    fun assertOfflinePdfMenuAllowsOpenAndRemoveOfflineAccess(): WireDrivePage {
        assertOpenActionEnabledOnThreeDotMenu()
        assertRemoveOfflineAccessOptionVisible()
        listOf("Share", "Share via Link", "Add or Remove Tags", "Move to folder", "Rename", "Delete").forEach { action ->
            assertFalse("Server-dependent action '$action' is visible in the offline PDF three-dot menu", device.hasObject(By.desc(action)))
        }
        return this
    }

    fun assertCreateFolderErrorVisible(): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(text = "Unable to create folder. Please try again"))
        return this
    }

    fun tapOpenOptionOnActionsMenu(): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(description = "Open"), timeout = UiWaitUtils.SHORT_WAIT).click()
        return this
    }

    fun assertOpenActionEnabledOnThreeDotMenu(): WireDrivePage {
        val open = UiWaitUtils.waitElement(UiSelectorParams(description = "Open"), timeout = UiWaitUtils.SHORT_WAIT)
        assertTrue("Open is not enabled in the three-dot menu", open.isEnabled)
        assertTrue("Open is not clickable in the three-dot menu", open.isClickable)
        return this
    }

    fun assertViewerShareLinkAndOfflineActionsDisabled(): WireDrivePage {
        listOf("Share", "Share via Link", "Make available offline").forEach { action ->
            assertViewerFileActionDisabled(action)
        }
        return this
    }

    fun assertViewerFileActionDisabled(actionName: String): WireDrivePage {
        val action = UiWaitUtils.waitElement(
            UiSelectorParams(description = "$actionName, disabled in viewer access"),
            timeout = UiWaitUtils.SHORT_WAIT
        )
        assertFalse("'$actionName' is clickable for a Viewer", action.isClickable)
        return this
    }

    fun tapDisabledViewerAction(actionName: String): WireDrivePage {
        UiWaitUtils.waitElement(
            UiSelectorParams(description = "$actionName, disabled in viewer access"),
            timeout = UiWaitUtils.SHORT_WAIT
        ).click()
        return this
    }

    fun assertPdfThreeDotMenuVisible(fileName: String): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(text = fileName), timeout = UiWaitUtils.SHORT_WAIT)
        UiWaitUtils.waitElement(UiSelectorParams(description = "Open"), timeout = UiWaitUtils.SHORT_WAIT)
        return this
    }

    fun searchInGlobalDrive(query: String): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(description = "Search"), timeout = UiWaitUtils.SHORT_WAIT).click()
        UiWaitUtils.waitElement(UiSelectorParams(className = "android.widget.EditText")).text = query
        device.pressEnter()
        UiWaitUtils.waitElement(UiSelectorParams(text = query, className = "android.widget.EditText"))
        return this
    }

    fun assertViewerFileAccessInDrive(): WireDrivePage {
        UiWaitUtils.waitElement(UiSelectorParams(text = "Viewer access"), timeout = UiWaitUtils.SHORT_WAIT)
        val link = UiWaitUtils.waitElement(
            UiSelectorParams(description = "Share via Link, disabled in viewer access"),
            timeout = UiWaitUtils.SHORT_WAIT
        )
        assertFalse("Viewer can create a public link in Drive", link.isClickable)
        val offlineAction = UiWaitUtils.waitElement(
            UiSelectorParams(description = "Make available offline, disabled in viewer access"),
            timeout = UiWaitUtils.SHORT_WAIT
        )
        assertFalse("Viewer can make a file available offline in Drive", offlineAction.isClickable)
        return this
    }

    fun assertEditorFileAccessInDrive(): WireDrivePage {
        val link = UiWaitUtils.waitElement(UiSelectorParams(description = "Share via Link"), timeout = UiWaitUtils.SHORT_WAIT)
        assertTrue("Editor cannot create a public link in Drive", link.isClickable)
        val offlineAction = UiWaitUtils.waitElement(
            UiSelectorParams(description = "Make available offline"),
            timeout = UiWaitUtils.SHORT_WAIT
        )
        assertTrue("Editor cannot make a file available offline in Drive", offlineAction.isClickable)
        assertFalse("Viewer access is shown for an Editor file", device.hasObject(By.text("Viewer access")))
        return this
    }

    fun assertViewerAccessLabelOnlyOnViewerPdf(viewerFileName: String, editorFileName: String): WireDrivePage {
        assertFileVisibleOnce(viewerFileName)
        assertFileVisibleOnce(editorFileName)
        UiWaitUtils.waitElement(
            UiSelectorParams(description = "$viewerFileName, viewer access"),
            timeout = UiWaitUtils.SHORT_WAIT
        )
        assertFalse(
            "Viewer access label is shown for Editor PDF '$editorFileName'",
            device.hasObject(By.desc("$editorFileName, viewer access"))
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
