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
package com.wire.android.tests.core.e2eTests

import QrCodeTestUtils.createQrImageInDeviceDownloadsFolder
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import backendUtils.team.TeamRoles
import backendUtils.team.enableCellsFeatureViaBackdoorTeam
import backendUtils.team.getTeam
import com.wire.android.testSupport.BuildConfig
import com.wire.android.testSupport.R
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import createLargePdfInDeviceDownloadsFolder
import createMultipagePdfInDeviceDownloadsFolder
import createPdfInDeviceDownloadsFolder
import deleteDeviceDownload
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import service.models.Conversation
import uiautomatorutils.UiWaitUtils
import user.utils.ClientUser
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@Suppress("LargeClass")
class WireDriveDownloadsAndOfflineTests : BaseUiTest() {
    override val deletePersonalUsersAfterTest = true

    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser
    private val deviceDownloads = mutableListOf<Uri>()
    private val documentScreenshots = mutableListOf<File>()
    private val versionReferenceScreenshots = mutableListOf<Bitmap>()
    private var originalWifiEnabled = false
    private var originalMobileDataEnabled = false
    private var restoreNetworkAfterTest = false

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        originalWifiEnabled = Settings.Global.getInt(context.contentResolver, Settings.Global.WIFI_ON, 1) == 1
        originalMobileDataEnabled = Settings.Global.getInt(context.contentResolver, "mobile_data", 1) == 1
    }

    @After
    fun tearDown() {
        try {
            if (restoreNetworkAfterTest) {
                val commonAppPage = pages.commonAppPage
                if (originalWifiEnabled) commonAppPage.enableWifiAndScanForNetworks() else commonAppPage.disableWifi()
                if (originalMobileDataEnabled) commonAppPage.enableMobileData() else commonAppPage.disableMobileData()
                if (originalWifiEnabled || originalMobileDataEnabled) commonAppPage.assertDeviceHasNetworkConnection()
            }
        } finally {
            versionReferenceScreenshots.forEach(Bitmap::recycle)
            documentScreenshots.forEach { it.delete() }
            deviceDownloads.forEach { runCatching { deleteDeviceDownload(it) } }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-12256")
    @Category("regression", "RC", "wireDrive", "wireDriveDownloadsAndOffline")
    @Test
    fun givenMemberHasEditorAccessToSharedDrive_whenSavingPdfOfflineAndRestartingWireWithoutNetwork_thenSavedPdfRemainsAvailableAndReadOnly() {
        step("Given TeamOwner creates team OfflinePersistence") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "OfflinePersistence",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }
        step("And TeamOwner configures MLS for OfflinePersistence") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "OfflinePersistence", backendClient)
        }
        step("And TeamOwner adds Member1 to OfflinePersistence with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "OfflinePersistence",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }
        step("And Shared Drive is enabled and unlocked for OfflinePersistence") {
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
            }
        }
        step("And MLS devices are registered for TeamOwner and Member1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }
        step("And I sign in as Member1 and complete the login prompts") {
            clientUserManager.setSelfUser(member1)
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(member1)
        }
        step("And TeamOwner creates OfflinePersistence Conversation with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "OfflinePersistence",
                deviceName = "Device1",
                cellsEnabled = true
            )
            testServiceHelper.waitForSharedDriveReady("user1Name", "OfflinePersistence")
        }
        val fileName = "OfflinePersistence-${UUID.randomUUID().toString().take(8)}.pdf"
        val expectedText = "Offline PDF content confirmed"
        step("And TeamOwner sends an identifiable PDF and its reference copy to OfflinePersistence Conversation") {
            val uri = createPdfInDeviceDownloadsFolder(fileName, expectedText)
            deviceDownloads += uri
            testServiceHelper.contactSendsLocalPdfConversation(context, fileName, uri, "user1Name", "Device1", "OfflinePersistence")
            testServiceHelper.contactSendsLocalPdfConversation(
                context,
                "Reference-$fileName",
                uri,
                "user1Name",
                "Device1",
                "OfflinePersistence"
            )
        }
        step("When Member1 opens Shared Drive from OfflinePersistence Conversation details") {
            pages.conversationListPage.clickGroupConversation("OfflinePersistence")
            pages.conversationViewPage.clickOnGroupConversationDetails("OfflinePersistence")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }
        step("Then Member1 sees Shared Drive with Editor access and the PDF exactly once") {
            pages.wireDrivePage.apply {
                assertSharedDriveVisibleWithoutViewerBanner()
                assertFileVisibleOnce(fileName, timeout = UiWaitUtils.SHORT_TIMEOUT)
            }
        }
        lateinit var expectedPdfScreenshot: File
        step("When Member1 opens Reference-$fileName in Shared Drive and captures its rendered first page") {
            pages.wireDrivePage.openFile("Reference-$fileName")
            pages.pdfViewerPage.assertPdfPreviewVisible("Reference-$fileName")
            expectedPdfScreenshot = pages.pdfViewerPage.savePdfPageScreenshot()
            documentScreenshots += expectedPdfScreenshot
        }
        step("And Member1 returns from the reference PDF to Shared Drive") {
            pages.pdfViewerPage.navigateBack()
        }
        step("When Member1 opens the PDF's three-dot menu in Shared Drive") {
            pages.wireDrivePage.tapFileMenu(fileName)
        }
        step("And Member1 selects Make available offline and Wire confirms File saved for offline use") {
            UiWaitUtils.assertToastDisplayed(
                text = "File saved for offline use",
                trigger = { pages.wireDrivePage.tapMakeAvailableOfflineOption() },
                timeout = UiWaitUtils.LONG_TIMEOUT
            )
        }
        step("When Member1 opens the saved PDF's three-dot menu in Shared Drive") {
            pages.wireDrivePage.tapFileMenu(fileName)
        }
        step("Then Remove offline access is available for the saved PDF") {
            pages.wireDrivePage.assertRemoveOfflineAccessOptionVisible()
        }
        step("When Member1 closes the PDF's three-dot menu and disables Wi-Fi and mobile data") {
            pages.wireDrivePage.navigateBack()
            restoreNetworkAfterTest = true
            pages.commonAppPage.apply {
                disableWifi()
                disableMobileData()
            }
        }
        step("Then the device has no active network connection") {
            pages.commonAppPage.assertDeviceHasNoNetworkConnection()
        }
        step("When Member1 force-stops Wire and launches it without network") {
            pages.commonAppPage.apply {
                forceStopWireApp()
                restartWireApp()
            }
        }
        step("Then Member1 sees Conversations without the startup service notification") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                waitUntilWireServiceNotificationDisappears()
            }
        }
        step("When Member1 opens global Drive from Conversations after restarting Wire") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                tapDriveButtonOnMenuEntry()
            }
        }
        step("Then global Drive shows the offline banner and the saved PDF exactly once") {
            pages.wireDrivePage.apply {
                assertDrivePageVisible()
                assertOfflineBannerVisible()
                assertFileVisibleOnce(fileName, timeout = UiWaitUtils.SHORT_TIMEOUT)
            }
        }
        step("When Member1 opens the saved PDF's three-dot menu in global Drive while offline") {
            pages.wireDrivePage.tapFileMenu(fileName)
        }
        step(
            "Then the offline PDF menu allows Open and Remove offline access but hides Share, link sharing, tags, move, rename and delete"
        ) {
            pages.wireDrivePage.assertOfflinePdfMenuAllowsOpenAndRemoveOfflineAccess()
        }
        step("When Member1 selects Open from the offline PDF's three-dot menu") {
            pages.wireDrivePage.tapOpenOptionOnActionsMenu()
        }
        step("Then the saved PDF matches its reference screenshot, has no edit or share controls and the device remains offline") {
            pages.pdfViewerPage.apply {
                assertPdfPreviewVisible(fileName)
                assertPdfMatchesScreenshot(expectedPdfScreenshot)
                assertPdfHasNoEditOrShareControls()
            }
            pages.commonAppPage.assertDeviceHasNoNetworkConnection()
        }
    }

    @Suppress("LongMethod", "MagicNumber")
    @TestCaseId("TC-12253")
    @Category("regression", "RC", "wireDrive", "wireDriveDownloadsAndOffline")
    @Test
    fun givenMemberHasThreePageAndFivePagePdfs_whenReadingEveryPageInSharedDrive_thenEachPageRendersCorrectContentAndBackReturnsToTheFileList() {
        step("Given TeamOwner creates team PdfPages") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "PdfPages",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }
        step("And TeamOwner configures MLS for PdfPages") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "PdfPages", backendClient)
        }
        step("And TeamOwner adds Member1 to PdfPages with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "PdfPages",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }
        step("And Shared Drive is enabled and unlocked for PdfPages") {
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
            }
        }
        step("And MLS devices are registered for TeamOwner and Member1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }
        step("And I sign in as Member1 and complete the login prompts") {
            clientUserManager.setSelfUser(member1)
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(member1)
        }
        step("And TeamOwner creates PdfPages Conversation with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "PdfPages",
                deviceName = "Device1",
                cellsEnabled = true
            )
            testServiceHelper.waitForSharedDriveReady("user1Name", "PdfPages")
        }
        val suffix = UUID.randomUUID().toString().take(8)
        val smallPdf = "SmallPages-$suffix.pdf"
        val largerPdf = "FivePages-$suffix.pdf"
        val pagesByFile = linkedMapOf(
            smallPdf to (1..3).map { "Small PDF page $it confirmed" },
            largerPdf to (1..5).map { "Larger PDF page $it confirmed" }
        )
        step("And TeamOwner sends identifiable three-page and five-page PDFs and a reference copy of each") {
            pagesByFile.forEach { (fileName, pageTexts) ->
                val uri = createMultipagePdfInDeviceDownloadsFolder(fileName, pageTexts)
                deviceDownloads += uri
                testServiceHelper.contactSendsLocalPdfConversation(context, fileName, uri, "user1Name", "Device1", "PdfPages")
                testServiceHelper.contactSendsLocalPdfConversation(
                    context,
                    "Reference-$fileName",
                    uri,
                    "user1Name",
                    "Device1",
                    "PdfPages"
                )
            }
        }
        step("When Member1 opens Shared Drive from PdfPages Conversation details") {
            pages.conversationListPage.clickGroupConversation("PdfPages")
            pages.conversationViewPage.clickOnGroupConversationDetails("PdfPages")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }
        step("Then PdfPages Shared Drive shows both PDFs exactly once") {
            pages.wireDrivePage.apply {
                assertSharedDriveVisible()
                assertFileVisibleOnce(smallPdf, timeout = UiWaitUtils.SHORT_TIMEOUT)
                assertFileVisibleOnce(largerPdf, timeout = UiWaitUtils.SHORT_TIMEOUT)
            }
        }
        pagesByFile.forEach { (fileName, pageTexts) ->
            val expectedPages = mutableListOf<File>()
            step("When Member1 opens the known-content reference PDF in Shared Drive") {
                pages.wireDrivePage.openFile("Reference-$fileName")
                pages.pdfViewerPage.assertPdfPreviewVisible("Reference-$fileName")
            }
            repeat(pageTexts.size) { index ->
                step("And Member1 scrolls to reference page ${index + 1} and captures its rendered content") {
                    pages.pdfViewerPage.scrollToPdfPage(index + 1)
                    val screenshot = pages.pdfViewerPage.savePdfPageScreenshot(index + 1)
                    documentScreenshots += screenshot
                    expectedPages += screenshot
                }
            }
            step("And Member1 returns from the reference PDF to Shared Drive") {
                pages.pdfViewerPage.navigateBack()
            }
            step("When Member1 opens $fileName from Shared Drive") {
                pages.wireDrivePage.openFile(fileName)
            }
            step("Then the in-app viewer shows $fileName with ${pageTexts.size} pages") {
                pages.pdfViewerPage.apply {
                    assertPdfPreviewVisible(fileName)
                    assertPdfPageCount(pageTexts.size)
                }
            }
            pageTexts.forEachIndexed { index, expectedText ->
                val pageNumber = index + 1
                step("When Member1 scrolls to page $pageNumber of $fileName") {
                    pages.pdfViewerPage.scrollToPdfPage(pageNumber)
                }
                step("Then page $pageNumber of $fileName matches its reference screenshot containing $expectedText") {
                    pages.pdfViewerPage.assertPdfMatchesScreenshot(expectedPages[index], pageNumber)
                }
            }
            step("When Member1 uses Back to close $fileName") {
                pages.pdfViewerPage.navigateBack()
            }
            step("Then Member1 returns to PdfPages Shared Drive with both original PDFs still listed exactly once") {
                pages.wireDrivePage.apply {
                    assertSharedDriveVisible()
                    assertFileVisibleOnce(smallPdf)
                    assertFileVisibleOnce(largerPdf)
                }
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-12252")
    @Category("regression", "RC", "wireDrive", "wireDriveDownloadsAndOffline")
    @Test
    fun givenMemberHasUnopenedVideoAndAudio_whenPlayingPausingResumingAndReopeningSavedFiles_thenCorrectPlayersWorkAndPlaybackStopsOnBack() {
        step("Given TeamOwner creates team MediaPlayback") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "MediaPlayback",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }
        step("And TeamOwner configures MLS for MediaPlayback") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "MediaPlayback", backendClient)
        }
        step("And TeamOwner adds Member1 to MediaPlayback with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "MediaPlayback",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }
        step("And Shared Drive is enabled and unlocked for MediaPlayback") {
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
            }
        }
        step("And MLS devices are registered for TeamOwner and Member1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }
        step("And I sign in as Member1 and complete the login prompts") {
            clientUserManager.setSelfUser(member1)
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(member1)
        }
        step("And TeamOwner creates MediaPlayback Conversation with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "MediaPlayback",
                deviceName = "Device1",
                cellsEnabled = true
            )
            testServiceHelper.waitForSharedDriveReady("user1Name", "MediaPlayback")
        }
        val suffix = UUID.randomUUID().toString().take(8)
        val videoFile = "PlaybackVideo-$suffix.mp4"
        val audioFile = "PlaybackAudio-$suffix.mp3"
        step("And TeamOwner sends an unopened MP4 video and MP3 audio file to MediaPlayback Conversation") {
            testServiceHelper.contactSendsLocalFileConversation(
                context,
                videoFile,
                Uri.parse("android.resource://${context.packageName}/${R.raw.drive_download_video}"),
                "user1Name",
                "Device1",
                "MediaPlayback",
                "video/mp4"
            )
            testServiceHelper.contactSendsLocalFileConversation(
                context,
                audioFile,
                Uri.parse("android.resource://${context.packageName}/${R.raw.drive_download_audio}"),
                "user1Name",
                "Device1",
                "MediaPlayback",
                "audio/mpeg"
            )
        }
        step("When Member1 opens Shared Drive from MediaPlayback Conversation details") {
            pages.conversationListPage.clickGroupConversation("MediaPlayback")
            pages.conversationViewPage.clickOnGroupConversationDetails("MediaPlayback")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }
        step("Then Shared Drive shows the named video and audio files exactly once") {
            pages.wireDrivePage.apply {
                assertSharedDriveVisible()
                assertFileVisibleOnce(videoFile, timeout = UiWaitUtils.SHORT_TIMEOUT)
                assertFileVisibleOnce(audioFile, timeout = UiWaitUtils.SHORT_TIMEOUT)
            }
        }
        listOf(videoFile, audioFile).forEach { fileName ->
            step("When Member1 opens $fileName from Shared Drive for the first time") {
                pages.wireDrivePage.openFile(fileName)
            }
            step("Then Wire's in-app player shows $fileName and its duration") {
                pages.driveMediaPlayerPage.assertPlayerReady(fileName)
            }
            step("When Member1 starts playback of $fileName") {
                pages.driveMediaPlayerPage.tapPlayPauseButton()
            }
            step("Then $fileName's elapsed playback time advances") {
                pages.driveMediaPlayerPage.assertPlaybackAdvances()
            }
            step("When Member1 pauses $fileName") {
                pages.driveMediaPlayerPage.tapPlayPauseButton()
            }
            step("Then $fileName's playback time stays fixed and audio output stops") {
                pages.driveMediaPlayerPage.assertPlaybackPaused()
            }
            step("When Member1 resumes $fileName") {
                pages.driveMediaPlayerPage.tapPlayPauseButton()
            }
            step("Then $fileName's elapsed playback time advances again") {
                pages.driveMediaPlayerPage.assertPlaybackAdvances()
            }
            step("When Member1 uses Back to leave $fileName during playback") {
                pages.driveMediaPlayerPage.navigateBack()
            }
            step("Then Shared Drive is visible and playback has stopped") {
                pages.wireDrivePage.assertSharedDriveVisible()
                pages.driveMediaPlayerPage.assertPlaybackStoppedAfterLeavingPlayer()
            }
            step("When Member1 opens $fileName's three-dot menu in Shared Drive") {
                pages.wireDrivePage.tapFileMenu(fileName)
            }
            step("And Member1 saves $fileName for offline use and sees the save confirmation") {
                UiWaitUtils.assertToastDisplayed(
                    text = "File saved for offline use",
                    trigger = { pages.wireDrivePage.tapMakeAvailableOfflineOption() },
                    timeout = UiWaitUtils.LONG_TIMEOUT
                )
            }
            step("When Member1 opens $fileName's three-dot menu after saving its local copy") {
                pages.wireDrivePage.tapFileMenu(fileName)
            }
            step("Then Remove offline access is available for $fileName") {
                pages.wireDrivePage.assertRemoveOfflineAccessOptionVisible()
            }
            step("When Member1 selects Open to play the saved copy of $fileName") {
                pages.wireDrivePage.tapOpenOptionOnActionsMenu()
            }
            step("Then Wire's player loads the same $fileName without getting stuck loading") {
                pages.driveMediaPlayerPage.assertPlayerReady(fileName)
            }
            step("When Member1 starts the saved copy of $fileName") {
                pages.driveMediaPlayerPage.tapPlayPauseButton()
            }
            step("Then the saved file's playback time advances") {
                pages.driveMediaPlayerPage.assertPlaybackAdvances()
            }
            step("When Member1 returns to Shared Drive while the saved $fileName is playing") {
                pages.driveMediaPlayerPage.navigateBack()
            }
            step("Then Shared Drive still shows $fileName once and no playback continues") {
                pages.wireDrivePage.apply {
                    assertSharedDriveVisible()
                    assertFileVisibleOnce(fileName)
                }
                pages.driveMediaPlayerPage.assertPlaybackStoppedAfterLeavingPlayer()
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-12257")
    @Category("regression", "RC", "wireDrive", "wireDriveDownloadsAndOffline")
    @Test
    fun givenPdfSavedInNestedFolders_whenMemberBrowsesAndTriesToCreateFolderOffline_thenSavedPdfOpensUncachedPdfIsHiddenAndCreationFails() {
        step("Given TeamOwner creates team OfflineFolders") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "OfflineFolders",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }
        step("And TeamOwner configures MLS for OfflineFolders") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "OfflineFolders", backendClient)
        }
        step("And TeamOwner adds Member1 to OfflineFolders with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "OfflineFolders",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }
        step("And Shared Drive is enabled and unlocked for OfflineFolders") {
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
            }
        }
        step("And MLS devices are registered for TeamOwner and Member1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }
        step("And I sign in as Member1 and complete the login prompts") {
            clientUserManager.setSelfUser(member1)
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(member1)
        }
        step("And TeamOwner creates OfflineFolders Conversation with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "OfflineFolders",
                deviceName = "Device1",
                cellsEnabled = true
            )
            testServiceHelper.waitForSharedDriveReady("user1Name", "OfflineFolders")
        }
        val suffix = UUID.randomUUID().toString().take(8)
        val savedPdf = "a-$suffix.pdf"
        val uncachedPdf = "b-$suffix.pdf"
        step("And Member1 has two identifiable PDFs in Downloads and TeamOwner sends a reference copy of the saved-file fixture") {
            val savedPdfUri = createPdfInDeviceDownloadsFolder(savedPdf, "Saved file A confirmed")
            deviceDownloads += savedPdfUri
            testServiceHelper.contactSendsLocalPdfConversation(
                context,
                "Reference-$savedPdf",
                savedPdfUri,
                "user1Name",
                "Device1",
                "OfflineFolders"
            )
            deviceDownloads += createPdfInDeviceDownloadsFolder(uncachedPdf, "Online file B confirmed")
        }
        step("When Member1 opens Shared Drive from OfflineFolders Conversation details") {
            pages.conversationListPage.clickGroupConversation("OfflineFolders")
            pages.conversationViewPage.clickOnGroupConversationDetails("OfflineFolders")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }
        lateinit var expectedPdfScreenshot: File
        step("When Member1 opens Reference-$savedPdf in Shared Drive and captures its rendered first page") {
            pages.wireDrivePage.openFile("Reference-$savedPdf")
            pages.pdfViewerPage.assertPdfPreviewVisible("Reference-$savedPdf")
            expectedPdfScreenshot = pages.pdfViewerPage.savePdfPageScreenshot()
            documentScreenshots += expectedPdfScreenshot
        }
        step("And Member1 returns from the reference PDF to Shared Drive") {
            pages.pdfViewerPage.navigateBack()
        }
        step("And Member1 creates Parent in Shared Drive, opens it and sees an empty folder") {
            pages.wireDrivePage.apply {
                createFolder("Parent")
                assertFolderVisibleOnce("Parent")
                openFolder("Parent")
                assertEmptyFolderVisible()
            }
        }
        step("And Member1 creates Child in Parent, opens it and sees an empty folder") {
            pages.wireDrivePage.apply {
                createFolder("Child")
                assertFolderVisibleOnce("Child")
                openFolder("Child")
                assertEmptyFolderVisible()
            }
        }
        listOf(savedPdf, uncachedPdf).forEach { fileName ->
            step("And Member1 uploads $fileName into Parent/Child") {
                pages.wireDrivePage.apply {
                    tapNewButton()
                    tapUploadFilesOption()
                }
                pages.documentsUIPage.selectFileInDocumentsUI(fileName)
                pages.wireDrivePage.tapUploadButtonOnUploadConfirmationDialog()
            }
            step("And the upload of $fileName completes, Member1 collapses the upload status panel and sees the file in Child") {
                pages.wireDrivePage.apply {
                    assertUploadCompleteVisible()
                    tapCollapseButtonOnUploadStatusPanel()
                    assertFolderHeadingVisible("Child")
                    assertFileVisibleOnce(fileName, timeout = UiWaitUtils.SHORT_TIMEOUT)
                }
            }
        }
        step("When Member1 opens $savedPdf's three-dot menu in Child") {
            pages.wireDrivePage.tapFileMenu(savedPdf)
        }
        step("And Member1 selects Make available offline for $savedPdf and Wire confirms the file was saved") {
            UiWaitUtils.assertToastDisplayed(
                text = "File saved for offline use",
                trigger = { pages.wireDrivePage.tapMakeAvailableOfflineOption() },
                timeout = UiWaitUtils.LONG_TIMEOUT
            )
        }
        step("And Member1 returns through Parent and Shared Drive to OfflineFolders Conversation details") {
            pages.wireDrivePage.apply {
                navigateBack()
                assertFolderHeadingVisible("Parent")
                navigateBack()
                assertFileVisibleOnce("Reference-$savedPdf")
                navigateBack()
            }
            pages.groupConversationDetailsPage.assertGroupDetailsPageVisible()
        }
        step("When Member1 disables Wi-Fi and mobile data while viewing OfflineFolders Conversation details") {
            restoreNetworkAfterTest = true
            pages.commonAppPage.apply {
                disableWifi()
                disableMobileData()
            }
        }
        step("Then the device has no active network connection") {
            pages.commonAppPage.assertDeviceHasNoNetworkConnection()
        }
        step("When Member1 opens Shared Drive from OfflineFolders Conversation details while offline") {
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }
        step("Then Shared Drive shows the offline banner and Parent exactly once") {
            pages.wireDrivePage.apply {
                assertOfflineBannerVisible()
                assertFolderVisibleOnce("Parent")
            }
        }
        step("When Member1 opens Parent's three-dot menu while offline") {
            pages.wireDrivePage.tapFolderMenu("Parent")
        }
        step("Then Parent's offline menu allows Open but hides tags, link sharing, move, rename and delete") {
            pages.wireDrivePage.assertFolderMenuAllowsOnlyOpenInSharedDrive()
        }
        step("When Member1 selects Open to navigate into Parent") {
            pages.wireDrivePage.tapOpenOptionOnActionsMenu()
        }
        step("Then Parent contains Child exactly once") {
            pages.wireDrivePage.apply {
                assertFolderHeadingVisible("Parent")
                assertFolderVisibleOnce("Child")
            }
        }
        step("When Member1 opens Child") {
            pages.wireDrivePage.openFolder("Child")
        }
        step("Then Child shows the offline banner and only the saved PDF") {
            pages.wireDrivePage.apply {
                assertFolderHeadingVisible("Child")
                assertOfflineBannerVisible()
                assertFileVisibleOnce(savedPdf)
                assertFileNotVisible(uncachedPdf)
            }
        }
        step("When Member1 opens the saved PDF's three-dot menu while offline") {
            pages.wireDrivePage.tapFileMenu(savedPdf)
        }
        step("Then the saved PDF menu allows Open and Remove offline access but hides Share, link sharing, tags, move, rename and delete") {
            pages.wireDrivePage.assertOfflinePdfMenuAllowsOpenAndRemoveOfflineAccess()
        }
        step("When Member1 selects Open for the saved PDF") {
            pages.wireDrivePage.tapOpenOptionOnActionsMenu()
        }
        step("Then the saved PDF matches its reference screenshot, has no edit or share controls and the device remains offline") {
            pages.pdfViewerPage.apply {
                assertPdfPreviewVisible(savedPdf)
                assertPdfMatchesScreenshot(expectedPdfScreenshot)
                assertPdfHasNoEditOrShareControls()
            }
            pages.commonAppPage.assertDeviceHasNoNetworkConnection()
        }
        step("When Member1 returns from the PDF through Child and Parent to the Shared Drive main folder") {
            pages.pdfViewerPage.navigateBack()
            pages.wireDrivePage.navigateBack()
            pages.wireDrivePage.navigateBack()
        }
        step("When Member1 submits Create Folder from the Shared Drive New menu while offline") {
            pages.wireDrivePage.submitNewFolder("OfflineFolder-$suffix")
        }
        step("Then Wire shows the Create Folder error and the device remains offline") {
            pages.wireDrivePage.assertCreateFolderErrorVisible()
            pages.commonAppPage.assertDeviceHasNoNetworkConnection()
        }
    }

    @Suppress("LongMethod", "MagicNumber")
    @TestCaseId("TC-12251")
    @Category("regression", "RC", "wireDrive", "wireDriveDownloadsAndOffline")
    @Test
    fun givenTwoSharedDrivesContainRecentAndOlderImages_whenMemberViewsThumbnailsAndRepeatedlyOpensImages_thenEachConversationShowsItsOwnCorrectImageWithoutBlankPreviews() {
        step("Given TeamOwner creates team ImagePreviewTeam") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "ImagePreviewTeam",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }
        step("And TeamOwner configures MLS for ImagePreviewTeam") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "ImagePreviewTeam", backendClient)
        }
        step("And TeamOwner adds Member1 to ImagePreviewTeam with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "ImagePreviewTeam",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }
        step("And Shared Drive is enabled and unlocked for ImagePreviewTeam") {
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
            }
        }
        step("And MLS devices are registered for TeamOwner and Member1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }
        step("And I sign in as Member1 and complete the login prompts") {
            clientUserManager.setSelfUser(member1)
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(member1)
        }
        step("And TeamOwner creates SendFilesHere Conversation with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "SendFilesHere",
                deviceName = "Device1",
                cellsEnabled = true
            )
            testServiceHelper.waitForSharedDriveReady("user1Name", "SendFilesHere")
        }
        step("And TeamOwner creates OtherFiles Conversation with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "OtherFiles",
                deviceName = "Device1",
                cellsEnabled = true
            )
            testServiceHelper.waitForSharedDriveReady("user1Name", "OtherFiles")
        }
        val suffix = UUID.randomUUID().toString().take(8)
        val olderImage = "DriveImage-Older-$suffix"
        val recentImage = "DriveImage-Recent-$suffix"
        val otherOlderImage = "DriveImage-OtherOlder-$suffix"
        val otherRecentImage = "DriveImage-OtherRecent-$suffix"
        val imagesByConversation = linkedMapOf(
            "SendFilesHere" to (olderImage to recentImage),
            "OtherFiles" to (otherOlderImage to otherRecentImage)
        )
        imagesByConversation.forEach { (conversationName, images) ->
            step(
                "And TeamOwner sends distinct older and recent QR images separated by eight multiline messages to $conversationName Conversation"
            ) {
                val olderFile = createQrImageInDeviceDownloadsFolder(images.first)
                val olderUri = Uri.fromFile(olderFile)
                deviceDownloads += olderUri
                testServiceHelper.contactSendsLocalFileConversation(
                    context,
                    olderFile.name,
                    olderUri,
                    "user1Name",
                    "Device1",
                    conversationName,
                    "image/png"
                )
                repeat(8) { index ->
                    testServiceHelper.userSendMessageToConversation(
                        "user1Name",
                        "Image history message ${index + 1}\nHistory line two\nHistory line three",
                        "Device1",
                        conversationName
                    )
                }
                val recentFile = createQrImageInDeviceDownloadsFolder(images.second)
                val recentUri = Uri.fromFile(recentFile)
                deviceDownloads += recentUri
                testServiceHelper.contactSendsLocalFileConversation(
                    context,
                    recentFile.name,
                    recentUri,
                    "user1Name",
                    "Device1",
                    conversationName,
                    "image/png"
                )
            }
        }
        step("When Member1 opens SendFilesHere Conversation for the first time and scrolls its first image into view") {
            pages.conversationListPage.clickGroupConversation("SendFilesHere")
            pages.conversationViewPage.scrollUntilOlderQrImageVisible(olderImage)
        }
        step("Then SendFilesHere's first unread image thumbnail shows its correct older QR image without refreshing") {
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("SendFilesHere")
                assertQrImageVisibleInMessageHistory(olderImage)
            }
        }
        step("When Member1 selects Scroll to end of conversation") {
            pages.conversationViewPage.tapScrollToEndOfConversationButton()
        }
        step("Then the recent image thumbnail shows SendFilesHere's correct QR content") {
            pages.conversationViewPage.assertQrImageVisibleInMessageHistory(recentImage)
        }
        repeat(3) { index ->
            step("When Member1 opens SendFilesHere's recent image, attempt ${index + 1}") {
                pages.conversationViewPage.tapQrImageAttachment(recentImage)
            }
            step("Then SendFilesHere's image viewer renders the recent image's correct QR content, attempt ${index + 1}") {
                pages.driveImageViewerPage.assertConversationImageVisible("SendFilesHere", recentImage)
            }
            step("When Member1 returns from the image viewer to SendFilesHere Conversation") {
                pages.driveImageViewerPage.navigateBack()
            }
            step("Then SendFilesHere's recent image thumbnail still shows its correct QR content") {
                pages.conversationViewPage.assertQrImageVisibleInMessageHistory(recentImage)
            }
        }
        step("When Member1 scrolls to older messages until the older image's correct QR thumbnail is visible") {
            pages.conversationViewPage.scrollUntilOlderQrImageVisible(olderImage)
        }
        step("And Member1 opens the older image from SendFilesHere Conversation") {
            pages.conversationViewPage.tapQrImageAttachment(olderImage)
        }
        step("Then SendFilesHere's image viewer renders the older image's distinct QR content") {
            pages.driveImageViewerPage.assertConversationImageVisible("SendFilesHere", olderImage)
        }
        step("When Member1 returns from the older image and opens SendFilesHere's Shared Drive from Conversation details") {
            pages.driveImageViewerPage.navigateBack()
            pages.conversationViewPage.clickOnGroupConversationDetails("SendFilesHere")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }
        step("Then Shared Drive lists both of SendFilesHere's images exactly once") {
            pages.wireDrivePage.apply {
                assertSharedDriveVisible()
                assertFileVisibleOnce("$olderImage.png", timeout = UiWaitUtils.SHORT_TIMEOUT)
                assertFileVisibleOnce("$recentImage.png", timeout = UiWaitUtils.SHORT_TIMEOUT)
            }
        }
        step("When Member1 opens the recent image from SendFilesHere's Shared Drive") {
            pages.wireDrivePage.openFile("$recentImage.png")
        }
        step("Then Wire's image viewer shows SendFilesHere's recent QR image") {
            pages.driveImageViewerPage.assertQrImageVisible("$recentImage.png", recentImage)
        }
        step("When Member1 returns through Shared Drive and Conversation details to the Conversation list") {
            pages.driveImageViewerPage.navigateBack()
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.tapBackButton()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }
        step("When Member1 opens OtherFiles Conversation for the first time and scrolls its first image into view") {
            pages.conversationListPage.clickGroupConversation("OtherFiles")
            pages.conversationViewPage.scrollUntilOlderQrImageVisible(otherOlderImage)
        }
        step("Then OtherFiles shows its own first unread image thumbnail rather than SendFilesHere's image") {
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("OtherFiles")
                assertQrImageVisibleInMessageHistory(otherOlderImage)
            }
        }
        step("When Member1 selects Scroll to end of OtherFiles Conversation") {
            pages.conversationViewPage.tapScrollToEndOfConversationButton()
        }
        step("Then OtherFiles' recent image thumbnail shows its distinct QR content") {
            pages.conversationViewPage.assertQrImageVisibleInMessageHistory(otherRecentImage)
        }
        step("When Member1 opens OtherFiles' recent image") {
            pages.conversationViewPage.tapQrImageAttachment(otherRecentImage)
        }
        step("Then OtherFiles' image viewer renders its own recent image's distinct QR content") {
            pages.driveImageViewerPage.assertConversationImageVisible("OtherFiles", otherRecentImage)
        }
        step("When Member1 returns to OtherFiles and opens its Shared Drive from Conversation details") {
            pages.driveImageViewerPage.navigateBack()
            pages.conversationViewPage.clickOnGroupConversationDetails("OtherFiles")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }
        step("Then OtherFiles Shared Drive lists its two images exactly once") {
            pages.wireDrivePage.apply {
                assertSharedDriveVisible()
                assertFileVisibleOnce("$otherOlderImage.png", timeout = UiWaitUtils.SHORT_TIMEOUT)
                assertFileVisibleOnce("$otherRecentImage.png", timeout = UiWaitUtils.SHORT_TIMEOUT)
            }
        }
        step("When Member1 opens OtherFiles' recent image from Shared Drive") {
            pages.wireDrivePage.openFile("$otherRecentImage.png")
        }
        step("Then Wire's image viewer again renders OtherFiles' correct QR content") {
            pages.driveImageViewerPage.assertQrImageVisible("$otherRecentImage.png", otherRecentImage)
        }
        step("When Member1 returns to the Conversation list, opens SendFilesHere's Shared Drive and selects its recent image") {
            pages.driveImageViewerPage.navigateBack()
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.tapBackButton()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
            pages.conversationListPage.clickGroupConversation("SendFilesHere")
            pages.conversationViewPage.clickOnGroupConversationDetails("SendFilesHere")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
            pages.wireDrivePage.openFile("$recentImage.png")
        }
        step("Then SendFilesHere's image still renders its original QR content after viewing OtherFiles' image") {
            pages.driveImageViewerPage.assertQrImageVisible("$recentImage.png", recentImage)
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-12258")
    @Category("regression", "RC", "wireDrive", "wireDriveDownloadsAndOffline")
    @Test
    fun givenMemberHasSavedSharedDrivePdfOffline_whenRemovingOfflineAccessWithoutNetwork_thenLocalCopyDisappearsAndBothMemberAndOwnerCanStillOpenOnlineFile() {
        step("Given TeamOwner creates team OfflineRemoval") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "OfflineRemoval",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }
        step("And TeamOwner configures MLS for OfflineRemoval") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "OfflineRemoval", backendClient)
        }
        step("And TeamOwner adds Member1 to OfflineRemoval with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "OfflineRemoval",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }
        step("And Shared Drive is enabled and unlocked for OfflineRemoval") {
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
            }
        }
        step("And MLS devices are registered for TeamOwner and Member1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }
        step("And I sign in as TeamOwner and complete the login prompts") {
            clientUserManager.setSelfUser(teamOwner)
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(teamOwner)
        }
        step("And I add Member1's account and complete the login prompts") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.tapNewAccountButton()
            clientUserManager.setSelfUser(member1)
            loginToStagingAs(member1)
        }
        step("And TeamOwner creates OfflineRemoval Conversation with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "OfflineRemoval",
                deviceName = "Device1",
                cellsEnabled = true
            )
            testServiceHelper.waitForSharedDriveReady("user1Name", "OfflineRemoval")
        }
        val fileName = "OfflineRemoval-${UUID.randomUUID().toString().take(8)}.pdf"
        val expectedText = "Online file content retained"
        step("And TeamOwner sends an identifiable PDF and its reference copy to OfflineRemoval Conversation") {
            val uri = createPdfInDeviceDownloadsFolder(fileName, expectedText)
            deviceDownloads += uri
            testServiceHelper.contactSendsLocalPdfConversation(context, fileName, uri, "user1Name", "Device1", "OfflineRemoval")
            testServiceHelper.contactSendsLocalPdfConversation(
                context,
                "Reference-$fileName",
                uri,
                "user1Name",
                "Device1",
                "OfflineRemoval"
            )
        }
        step("When Member1 opens Shared Drive from OfflineRemoval Conversation details") {
            pages.conversationListPage.clickGroupConversation("OfflineRemoval")
            pages.conversationViewPage.clickOnGroupConversationDetails("OfflineRemoval")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }
        step("Then Shared Drive shows the original PDF exactly once") {
            pages.wireDrivePage.apply {
                assertSharedDriveVisible()
                assertFileVisibleOnce(fileName, timeout = UiWaitUtils.SHORT_TIMEOUT)
            }
        }
        lateinit var expectedPdfScreenshot: File
        step("When Member1 opens Reference-$fileName in Shared Drive and captures its rendered first page") {
            pages.wireDrivePage.openFile("Reference-$fileName")
            pages.pdfViewerPage.assertPdfPreviewVisible("Reference-$fileName")
            expectedPdfScreenshot = pages.pdfViewerPage.savePdfPageScreenshot()
            documentScreenshots += expectedPdfScreenshot
        }
        step("And Member1 returns from the reference PDF to Shared Drive") {
            pages.pdfViewerPage.navigateBack()
        }
        step("When Member1 opens the PDF's three-dot menu in Shared Drive") {
            pages.wireDrivePage.tapFileMenu(fileName)
        }
        step("And Member1 selects Make available offline and Wire confirms the file was saved") {
            UiWaitUtils.assertToastDisplayed(
                text = "File saved for offline use",
                trigger = { pages.wireDrivePage.tapMakeAvailableOfflineOption() },
                timeout = UiWaitUtils.LONG_TIMEOUT
            )
        }
        step("When Member1 disables Wi-Fi and mobile data") {
            restoreNetworkAfterTest = true
            pages.commonAppPage.apply {
                disableWifi()
                disableMobileData()
            }
        }
        step("Then Shared Drive shows the offline banner and saved PDF, and the device has no active network") {
            pages.commonAppPage.assertDeviceHasNoNetworkConnection()
            pages.wireDrivePage.apply {
                assertOfflineBannerVisible()
                assertFileVisibleOnce(fileName)
            }
        }
        step("When Member1 opens the saved PDF's three-dot menu while offline") {
            pages.wireDrivePage.tapFileMenu(fileName)
        }
        step("Then the saved PDF offers Remove offline access") {
            pages.wireDrivePage.assertRemoveOfflineAccessOptionVisible()
        }
        step("When Member1 selects Remove offline access without network") {
            pages.wireDrivePage.tapRemoveOfflineAccessOption()
        }
        step("Then the PDF and its offline indicator are no longer listed in Shared Drive's offline view") {
            pages.wireDrivePage.apply {
                assertSharedDriveVisible()
                assertFileNotVisible(fileName)
            }
        }
        step("When Member1 restores the device's original Wi-Fi and mobile-data settings") {
            pages.commonAppPage.apply {
                if (originalWifiEnabled) enableWifiAndScanForNetworks()
                if (originalMobileDataEnabled) enableMobileData()
            }
        }
        step("Then the device has an active network and the same Shared Drive shows the online PDF exactly once") {
            pages.commonAppPage.assertDeviceHasNetworkConnection()
            pages.wireDrivePage.assertFileVisibleOnce(fileName, timeout = UiWaitUtils.SHORT_TIMEOUT)
        }
        step("When Member1 opens the online PDF after removing its offline copy") {
            pages.wireDrivePage.openFile(fileName)
        }
        step("Then Member1's original PDF matches its known-content reference screenshot") {
            pages.pdfViewerPage.apply {
                assertPdfPreviewVisible(fileName)
                assertPdfMatchesScreenshot(expectedPdfScreenshot)
            }
        }
        step("When Member1 returns through Shared Drive and Conversation details to the Conversation list") {
            pages.pdfViewerPage.navigateBack()
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.tapBackButton()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }
        step("And I switch to TeamOwner's account") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.tapOtherAccountByName(teamOwner.name.orEmpty())
            clientUserManager.setSelfUser(teamOwner)
        }
        step("When TeamOwner opens Shared Drive from OfflineRemoval Conversation details") {
            pages.conversationListPage.clickGroupConversation("OfflineRemoval")
            pages.conversationViewPage.clickOnGroupConversationDetails("OfflineRemoval")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }
        step("Then TeamOwner sees the original online PDF exactly once") {
            pages.wireDrivePage.apply {
                assertSharedDriveVisibleWithoutViewerBanner()
                assertFileVisibleOnce(fileName, timeout = UiWaitUtils.SHORT_TIMEOUT)
            }
        }
        step("When TeamOwner opens the original online PDF") {
            pages.wireDrivePage.openFile(fileName)
        }
        step("Then TeamOwner's original PDF matches the same known-content reference screenshot") {
            pages.pdfViewerPage.apply {
                assertPdfPreviewVisible(fileName)
                assertPdfMatchesScreenshot(expectedPdfScreenshot)
            }
        }
    }

    /**
     * Skipped on the Xiaomi test phone (f212e5d2) running MIUI 13 on Android 11.
     * MIUI's share sheet launches PDF Test Receiver under Android's system identity,
     * so Android rejects the temporary read permission for Wire's PDF.
     * The receiver opens, but cannot read the file to verify its contents.
     */
    @Suppress("LongMethod")
    @TestCaseId("TC-12254")
    @Category("regression", "RC", "wireDrive", "wireDriveDownloadsAndOffline")
    @Test
    fun givenMemberDownloadedPdfFromConversation_whenSharingItFromSharedDrive_thenReceivingAppReadsOriginalFilenameAndContent() {
        step("Given the device is eligible for the Shared Drive PDF sharing test") {
            assumeFalse(
                "Skipped on Xiaomi f212e5d2 because MIUI's share sheet fails to grant read access to the PDF.",
                device.executeShellCommand("getprop ro.boot.serialno").trim() == "f212e5d2"
            )
        }

        step("And TeamOwner creates team ShareDownloadedPdf") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "ShareDownloadedPdf",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }
        step("And TeamOwner configures MLS for ShareDownloadedPdf") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "ShareDownloadedPdf", backendClient)
        }
        step("And TeamOwner adds Member1 to ShareDownloadedPdf with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "ShareDownloadedPdf",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }
        step("And Shared Drive is enabled and unlocked for ShareDownloadedPdf") {
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
            }
        }
        step("And MLS devices are registered for TeamOwner and Member1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }
        step("And I sign in as Member1 and complete the login prompts") {
            clientUserManager.setSelfUser(member1)
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(member1)
        }
        step("And TeamOwner creates ShareDownloadedPdf Conversation with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "ShareDownloadedPdf",
                deviceName = "Device1",
                cellsEnabled = true
            )
            testServiceHelper.waitForSharedDriveReady("user1Name", "ShareDownloadedPdf")
        }

        val fileName = "ShareDownloadedPdf-${UUID.randomUUID().toString().take(8)}.pdf"
        val pageTexts = listOf("Shared PDF first page confirmed", "Shared PDF second page confirmed")
        lateinit var sourcePdf: Uri
        step("And TeamOwner sends a two-page PDF to ShareDownloadedPdf Conversation") {
            sourcePdf = createMultipagePdfInDeviceDownloadsFolder(fileName, pageTexts)
            deviceDownloads += sourcePdf
            testServiceHelper.contactSendsLocalPdfConversation(
                context,
                fileName,
                sourcePdf,
                "user1Name",
                "Device1",
                "ShareDownloadedPdf"
            )
        }
        step("When Member1 opens ShareDownloadedPdf Conversation and its PDF attachment") {
            pages.conversationListPage.clickGroupConversation("ShareDownloadedPdf")
            pages.conversationViewPage.clickFileWithName(fileName)
        }
        step("Then the downloaded PDF opens with the expected filename and two pages") {
            pages.pdfViewerPage.apply {
                assertPdfPreviewVisible(fileName)
                assertPdfPageCount(pageTexts.size)
            }
        }
        step("When Member1 returns to the Conversation and opens Shared Drive through Conversation details") {
            pages.pdfViewerPage.navigateBack()
            pages.conversationViewPage.clickOnGroupConversationDetails("ShareDownloadedPdf")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }
        step("Then Shared Drive shows the downloaded PDF exactly once") {
            pages.wireDrivePage.apply {
                assertSharedDriveVisible()
                assertFileVisibleOnce(fileName)
            }
        }
        step("When Member1 opens the downloaded PDF's three-dot menu") {
            pages.wireDrivePage.tapFileMenu(fileName)
        }
        step("Then Share is available for the locally downloaded PDF") {
            pages.wireDrivePage.assertShareOptionEnabled()
        }
        step("When Member1 shares the PDF to the PDF Test Receiver from Android's share sheet") {
            pages.wireDrivePage.tapShareOption()
            pages.pdfShareReceiverPage.selectFromShareSheet()
        }
        step("Then the receiving app reads the original filename and PDF bytes") {
            pages.pdfShareReceiverPage.assertReceivedPdf(fileName, sourcePdf)
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-12259")
    @Category("regression", "RC", "wireDrive", "wireDriveDownloadsAndOffline")
    @Test
    fun givenMemberIsEditorAndGuestIsViewer_whenInspectingOfflineActions_thenOnlyEditorPdfAndImageCanBeSavedAndConversationMenusHaveNoOfflineActions() {
        step("Given TeamOwner creates team OfflineEligibility") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "OfflineEligibility",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And TeamOwner configures MLS for OfflineEligibility") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "OfflineEligibility", backendClient)
        }

        step("And TeamOwner adds Member1 to OfflineEligibility with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "OfflineEligibility",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And Shared Drive is enabled and unlocked for OfflineEligibility") {
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
            }
        }

        step("And Guest is a personal user with a unique username outside OfflineEligibility") {
            clientUserManager.createPersonalUsersByAliases(listOf("user4Name"), backendClient)
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user4Name")
            }
        }
        val guest = clientUserManager.findUserByNameOrNameAlias("user4Name")

        step("And TeamOwner connects to Guest") {
            backendSetupHelper.userIsConnectedTo("user1Name", "user4Name")
        }

        step("And MLS devices are registered for TeamOwner, Member1 and Guest") {
            val participantAliases = listOf("user1Name", "user2Name", "user4Name")
            testServiceHelper.apply {
                participantAliases.forEach { alias -> addDevice(alias, null, "Device1") }
            }
        }

        step("And I sign in as Guest and complete the login prompts") {
            clientUserManager.setSelfUser(guest)
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(guest)
        }

        step("And I add Member1's account and complete the login prompts") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.tapNewAccountButton()
            clientUserManager.setSelfUser(member1)
            loginToStagingAs(member1)
        }

        step("And TeamOwner creates OfflineEligibility Conversation with Member1 and Guest and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name,user4Name",
                conversationName = "OfflineEligibility",
                deviceName = "Device1",
                cellsEnabled = true
            )
            testServiceHelper.waitForSharedDriveReady("user1Name", "OfflineEligibility")
        }

        val suffix = UUID.randomUUID().toString().take(8)
        val pdf = "OfflineEligibility-$suffix.pdf"
        val image = "OfflineEligibility-$suffix.png"
        val document = "OfflineEligibility-$suffix.docx"
        step("And TeamOwner sends a PDF, image and editable document to OfflineEligibility Conversation") {
            val pdfUri = createPdfInDeviceDownloadsFolder(pdf, "Offline eligibility PDF confirmed")
            deviceDownloads += pdfUri
            testServiceHelper.contactSendsLocalPdfConversation(context, pdf, pdfUri, "user1Name", "Device1", "OfflineEligibility")
            val imageUri = Uri.fromFile(createQrImageInDeviceDownloadsFolder("Offline eligibility image $suffix"))
            deviceDownloads += imageUri
            testServiceHelper.contactSendsLocalFileConversation(
                context,
                image,
                imageUri,
                "user1Name",
                "Device1",
                "OfflineEligibility",
                "image/png"
            )
            testServiceHelper.contactSendsLocalFileConversation(
                context,
                document,
                Uri.parse("android.resource://${context.packageName}/${R.raw.drive_permissions_document}"),
                "user1Name",
                "Device1",
                "OfflineEligibility",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            )
        }
        step("When Member1 opens Shared Drive from OfflineEligibility Conversation details") {
            pages.conversationListPage.clickGroupConversation("OfflineEligibility")
            pages.conversationViewPage.clickOnGroupConversationDetails("OfflineEligibility")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }
        step("Then Member1 sees all three files with Editor access") {
            pages.wireDrivePage.apply {
                assertSharedDriveVisibleWithoutViewerBanner()
                assertFileVisibleOnce(pdf, timeout = UiWaitUtils.SHORT_TIMEOUT)
                assertFileVisibleOnce(image)
                assertFileVisibleOnce(document)
            }
        }
        step("When Member1 creates EligibleFolder in Shared Drive") {
            pages.wireDrivePage.createFolder("EligibleFolder")
        }
        step("Then EligibleFolder appears exactly once without leaving Shared Drive") {
            pages.wireDrivePage.assertFolderVisibleOnce("EligibleFolder")
        }
        listOf(pdf, image).forEach { fileName ->
            step("When Member1 opens $fileName's three-dot menu in Shared Drive") {
                pages.wireDrivePage.tapFileMenu(fileName)
            }
            step("Then the supported file offers Make available offline") {
                pages.wireDrivePage.assertMakeAvailableOfflineOptionEnabled()
            }
            step("When Member1 selects Make available offline and Wire confirms the file was saved") {
                UiWaitUtils.assertToastDisplayed(
                    text = "File saved for offline use",
                    trigger = { pages.wireDrivePage.tapMakeAvailableOfflineOption() },
                    timeout = UiWaitUtils.LONG_TIMEOUT
                )
            }
            step("When Member1 opens the saved file's three-dot menu in Shared Drive") {
                pages.wireDrivePage.tapFileMenu(fileName)
            }
            step("Then Remove offline access confirms the supported file is saved offline") {
                pages.wireDrivePage.assertRemoveOfflineAccessOptionVisible()
            }
            step("When Member1 closes the file's three-dot menu") {
                pages.wireDrivePage.navigateBack()
            }
        }
        step("When Member1 opens the editable document's three-dot menu in Shared Drive") {
            pages.wireDrivePage.tapFileMenu(document)
        }
        step("Then the editable document has neither Make available offline nor Remove offline access") {
            pages.wireDrivePage.assertOfflineActionsAbsent()
        }
        step("When Member1 closes the document's three-dot menu and opens EligibleFolder's three-dot menu") {
            pages.wireDrivePage.navigateBack()
            pages.wireDrivePage.tapFolderMenu("EligibleFolder")
        }
        step("Then the folder has neither Make available offline nor Remove offline access") {
            pages.wireDrivePage.assertOfflineActionsAbsent()
        }
        step("When Member1 closes the folder's three-dot menu and returns to OfflineEligibility Conversation") {
            pages.wireDrivePage.navigateBack()
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.tapBackButton()
        }
        step("And Member1 long-presses the PDF message in OfflineEligibility Conversation") {
            pages.conversationViewPage.longPressOnMessage(pdf)
        }
        step("Then the Conversation message menu has neither Make available offline nor Remove offline access") {
            pages.conversationViewPage.assertOfflineActionsAbsentFromMessageMenu()
        }
        step("When Member1 closes the message menu and returns to the Conversation list") {
            pages.conversationViewPage.dismissMessageMenu()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }
        step("And I switch to Guest's account") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.tapOtherAccountByName(guest.name.orEmpty())
            clientUserManager.setSelfUser(guest)
        }
        step("When Guest opens Shared Drive from OfflineEligibility Conversation details") {
            pages.conversationListPage.clickGroupConversation("OfflineEligibility")
            pages.conversationViewPage.clickOnGroupConversationDetails("OfflineEligibility")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }
        step("Then Guest sees the Viewer access banner and the PDF") {
            pages.wireDrivePage.apply {
                assertViewerAccessBannerVisible("Viewer access: you can’t upload or edit files.")
                assertFileVisibleOnce(pdf, timeout = UiWaitUtils.LONG_TIMEOUT)
            }
        }
        step("When Guest opens the PDF's three-dot menu in Shared Drive") {
            pages.wireDrivePage.tapFileMenu(pdf)
        }
        step("Then the Viewer PDF has neither Make available offline nor Remove offline access") {
            pages.wireDrivePage.assertOfflineActionsAbsent()
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-12260")
    @Category("regression", "RC", "wireDrive", "wireDriveDownloadsAndOffline")
    @Test
    fun givenMemberIsSavingLargePdfOffline_whenNetworkInterruptsDownloadAndMemberRetriesOnline_thenFailureLeavesNoOfflineCopyAndRetrySavesEveryPage() {
        step("Given TeamOwner creates team OfflineRetry") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "OfflineRetry",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }
        step("And TeamOwner configures MLS for OfflineRetry") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "OfflineRetry", backendClient)
        }
        step("And TeamOwner adds Member1 to OfflineRetry with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "OfflineRetry",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }
        step("And Shared Drive is enabled and unlocked for OfflineRetry") {
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
            }
        }
        step("And MLS devices are registered for TeamOwner and Member1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }
        step("And I sign in as Member1 and complete the login prompts") {
            clientUserManager.setSelfUser(member1)
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(member1)
        }
        step("And TeamOwner creates OfflineRetry Conversation with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "OfflineRetry",
                deviceName = "Device1",
                cellsEnabled = true
            )
            testServiceHelper.waitForSharedDriveReady("user1Name", "OfflineRetry")
        }
        val fileName = "OfflineRetry-${UUID.randomUUID().toString().take(8)}.pdf"
        val pageTexts = (1..4).map { "Retry PDF page $it confirmed" }
        step("And TeamOwner sends a large four-page PDF and its reference copy to OfflineRetry Conversation") {
            val uri = createLargePdfInDeviceDownloadsFolder(fileName, pageTexts)
            deviceDownloads += uri
            testServiceHelper.contactSendsLocalPdfConversation(context, fileName, uri, "user1Name", "Device1", "OfflineRetry")
            testServiceHelper.contactSendsLocalPdfConversation(
                context,
                "Reference-$fileName",
                uri,
                "user1Name",
                "Device1",
                "OfflineRetry"
            )
        }
        step("When Member1 opens Shared Drive from OfflineRetry Conversation details") {
            pages.conversationListPage.clickGroupConversation("OfflineRetry")
            pages.conversationViewPage.clickOnGroupConversationDetails("OfflineRetry")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }
        step("Then Shared Drive shows the unopened PDF exactly once") {
            pages.wireDrivePage.apply {
                assertSharedDriveVisible()
                assertFileVisibleOnce(fileName, timeout = UiWaitUtils.SHORT_TIMEOUT)
            }
        }
        val expectedPages = mutableListOf<File>()
        step("When Member1 opens the known-content reference PDF in Shared Drive") {
            pages.wireDrivePage.openFile("Reference-$fileName")
            pages.pdfViewerPage.assertPdfPreviewVisible("Reference-$fileName")
        }
        repeat(pageTexts.size) { index ->
            step("And Member1 scrolls to reference page ${index + 1} and captures its rendered content") {
                pages.pdfViewerPage.scrollToPdfPage(index + 1)
                val screenshot = pages.pdfViewerPage.savePdfPageScreenshot(index + 1)
                documentScreenshots += screenshot
                expectedPages += screenshot
            }
        }
        step("And Member1 returns from the reference PDF to Shared Drive") {
            pages.pdfViewerPage.navigateBack()
        }
        step("And Member1 disables mobile data and confirms Wi-Fi still provides a network connection") {
            restoreNetworkAfterTest = true
            pages.commonAppPage.apply {
                disableMobileData()
                assertDeviceHasNetworkConnection()
            }
        }
        step("When Member1 opens the PDF's three-dot menu and selects Make available offline") {
            pages.wireDrivePage.apply {
                tapFileMenu(fileName)
                tapMakeAvailableOfflineOption()
            }
        }
        step("Then the PDF is still downloading for offline access") {
            pages.wireDrivePage.assertOfflineDownloadInProgress(fileName)
        }
        step("When Member1 interrupts the active download by disabling Wi-Fi, Wire reports Action failed") {
            UiWaitUtils.assertToastDisplayed(
                text = "Action failed.",
                trigger = { pages.commonAppPage.disableWifi() },
                timeout = UiWaitUtils.VERY_LONG_TIMEOUT
            )
        }
        step("Then the device is offline and the incomplete PDF is not offered as a saved offline file") {
            pages.commonAppPage.assertDeviceHasNoNetworkConnection()
            pages.wireDrivePage.apply {
                assertOfflineBannerVisible()
                assertFileNotVisible(fileName)
            }
        }
        step("When Member1 restores the original Wi-Fi and mobile-data settings") {
            pages.commonAppPage.apply {
                if (originalWifiEnabled) enableWifiAndScanForNetworks()
                if (originalMobileDataEnabled) enableMobileData()
            }
        }
        step("Then the device is online and the same Shared Drive shows the PDF again") {
            pages.commonAppPage.assertDeviceHasNetworkConnection()
            pages.wireDrivePage.assertFileVisibleOnce(fileName, timeout = UiWaitUtils.SHORT_TIMEOUT)
        }
        step("When Member1 opens the PDF's three-dot menu") {
            pages.wireDrivePage.tapFileMenu(fileName)
        }
        step("Then the failed attempt has not been marked saved and Make available offline is enabled") {
            pages.wireDrivePage.assertMakeAvailableOfflineOptionEnabled()
        }
        step("When Member1 retries Make available offline, Wire confirms the file was saved") {
            UiWaitUtils.assertToastDisplayed(
                text = "File saved for offline use",
                trigger = { pages.wireDrivePage.tapMakeAvailableOfflineOption() },
                timeout = UiWaitUtils.VERY_LONG_TIMEOUT
            )
        }
        step("When Member1 opens the PDF's three-dot menu after the successful retry") {
            pages.wireDrivePage.tapFileMenu(fileName)
        }
        step("Then Remove offline access confirms that the retry saved the PDF") {
            pages.wireDrivePage.assertRemoveOfflineAccessOptionVisible()
        }
        step("When Member1 closes the PDF's three-dot menu and disconnects again") {
            pages.wireDrivePage.navigateBack()
            pages.commonAppPage.apply {
                disableWifi()
                disableMobileData()
            }
        }
        step("Then the saved PDF is available with the offline banner and no active network") {
            pages.commonAppPage.assertDeviceHasNoNetworkConnection()
            pages.wireDrivePage.apply {
                assertOfflineBannerVisible()
                assertFileVisibleOnce(fileName)
            }
        }
        step("When Member1 opens the successfully saved PDF without network") {
            pages.wireDrivePage.openFile(fileName)
        }
        step("Then the offline PDF reports all four pages") {
            pages.pdfViewerPage.apply {
                assertPdfPreviewVisible(fileName)
                assertPdfPageCount(pageTexts.size)
            }
        }
        pageTexts.indices.forEach { index ->
            step("When Member1 scrolls to offline PDF page ${index + 1}") {
                pages.pdfViewerPage.scrollToPdfPage(index + 1)
            }
            step("Then offline PDF page ${index + 1} matches its reference screenshot") {
                pages.pdfViewerPage.assertPdfMatchesScreenshot(expectedPages[index], index + 1)
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-12255")
    @Category("regression", "RC", "wireDrive", "wireDriveDownloadsAndOffline")
    @Test
    fun givenTwoSharedDrivesHaveDifferentLargePdfsWithSameFilename_whenMemberCancelsInterruptsAndRetriesLoading_thenEachPdfRendersItsOwnCompleteContent() {
        step("Given TeamOwner creates team DownloadRetry") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "DownloadRetry",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }
        step("And TeamOwner configures MLS for DownloadRetry") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "DownloadRetry", backendClient)
        }
        step("And TeamOwner adds Member1 to DownloadRetry with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "DownloadRetry",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }
        step("And Shared Drive is enabled and unlocked for DownloadRetry") {
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
            }
        }
        step("And MLS devices are registered for TeamOwner and Member1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }
        step("And I sign in as Member1 and complete the login prompts") {
            clientUserManager.setSelfUser(member1)
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(member1)
        }
        step("And TeamOwner creates DownloadRetry Conversation with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "DownloadRetry",
                deviceName = "Device1",
                cellsEnabled = true
            )
            testServiceHelper.waitForSharedDriveReady("user1Name", "DownloadRetry")
        }
        step("And TeamOwner creates OtherFiles Conversation with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "OtherFiles",
                deviceName = "Device1",
                cellsEnabled = true
            )
            testServiceHelper.waitForSharedDriveReady("user1Name", "OtherFiles")
        }
        val fileName = "SameName-${UUID.randomUUID().toString().take(8)}.pdf"
        val firstPages = (1..4).map { "First Drive page $it confirmed" }
        val secondPages = (1..4).map { "Other Drive page $it confirmed" }
        step("And TeamOwner sends different same-named four-page PDFs and their reference copies to the two Conversations") {
            val firstUri = createLargePdfInDeviceDownloadsFolder(fileName, firstPages)
            deviceDownloads += firstUri
            testServiceHelper.contactSendsLocalPdfConversation(context, fileName, firstUri, "user1Name", "Device1", "DownloadRetry")
            testServiceHelper.contactSendsLocalPdfConversation(
                context,
                "Reference-$fileName",
                firstUri,
                "user1Name",
                "Device1",
                "DownloadRetry"
            )
            val secondUri = createLargePdfInDeviceDownloadsFolder(fileName, secondPages)
            deviceDownloads += secondUri
            testServiceHelper.contactSendsLocalPdfConversation(context, fileName, secondUri, "user1Name", "Device1", "OtherFiles")
            testServiceHelper.contactSendsLocalPdfConversation(
                context,
                "Reference-$fileName",
                secondUri,
                "user1Name",
                "Device1",
                "OtherFiles"
            )
        }
        step("When Member1 opens Shared Drive from DownloadRetry Conversation details") {
            pages.conversationListPage.clickGroupConversation("DownloadRetry")
            pages.conversationViewPage.clickOnGroupConversationDetails("DownloadRetry")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }
        step("Then DownloadRetry Shared Drive shows the unopened PDF exactly once") {
            pages.wireDrivePage.apply {
                assertSharedDriveVisible()
                assertFileVisibleOnce(fileName, timeout = UiWaitUtils.SHORT_TIMEOUT)
            }
        }
        val firstReferencePages = mutableListOf<File>()
        step("When Member1 opens the known-content reference PDF in Shared Drive") {
            pages.wireDrivePage.openFile("Reference-$fileName")
            pages.pdfViewerPage.assertPdfPreviewVisible("Reference-$fileName")
        }
        repeat(firstPages.size) { index ->
            step("And Member1 scrolls to reference page ${index + 1} and captures its rendered content") {
                pages.pdfViewerPage.scrollToPdfPage(index + 1)
                val screenshot = pages.pdfViewerPage.savePdfPageScreenshot(index + 1)
                documentScreenshots += screenshot
                firstReferencePages += screenshot
            }
        }
        step("And Member1 returns from the reference PDF to Shared Drive") {
            pages.pdfViewerPage.navigateBack()
        }
        step("When Member1 opens the large PDF from DownloadRetry Shared Drive") {
            pages.wireDrivePage.openFile(fileName)
        }
        step("Then the PDF preview is still loading and has not rendered a page") {
            pages.pdfViewerPage.assertPdfLoading(fileName)
        }
        step("When Member1 cancels the PDF loading by pressing Back") {
            pages.pdfViewerPage.navigateBack()
        }
        step("Then Member1 returns to DownloadRetry Shared Drive with the same PDF listed once") {
            pages.wireDrivePage.apply {
                assertSharedDriveVisible()
                assertFileVisibleOnce(fileName)
            }
        }
        step("When Member1 opens the PDF again after cancelling") {
            pages.wireDrivePage.openFile(fileName)
        }
        step("Then a new PDF load is active rather than an incomplete document being rendered") {
            pages.pdfViewerPage.assertPdfLoading(fileName)
        }
        step("When Member1 interrupts the active PDF load by disabling Wi-Fi and mobile data") {
            restoreNetworkAfterTest = true
            pages.commonAppPage.apply {
                disableWifi()
                disableMobileData()
            }
        }
        step("Then the device is offline and the PDF preview reports that the document could not be downloaded") {
            pages.commonAppPage.assertDeviceHasNoNetworkConnection()
            pages.pdfViewerPage.assertPdfDownloadFailedWithRetry(fileName)
        }
        step("When Member1 restores the original Wi-Fi and mobile-data settings") {
            pages.commonAppPage.apply {
                if (originalWifiEnabled) enableWifiAndScanForNetworks()
                if (originalMobileDataEnabled) enableMobileData()
            }
        }
        step("Then the device has an active network connection") {
            pages.commonAppPage.assertDeviceHasNetworkConnection()
        }
        step("When Member1 selects Try again in the PDF preview") {
            pages.pdfViewerPage.tapTryAgainButton()
        }
        step("Then the retried PDF opens with the correct filename and all four pages") {
            pages.pdfViewerPage.apply {
                assertPdfPreviewVisible(fileName)
                assertPdfPageCount(firstPages.size)
            }
        }
        firstPages.indices.forEach { index ->
            step("When Member1 scrolls to DownloadRetry PDF page ${index + 1}") {
                pages.pdfViewerPage.scrollToPdfPage(index + 1)
            }
            step("Then DownloadRetry PDF page ${index + 1} matches its reference screenshot") {
                pages.pdfViewerPage.assertPdfMatchesScreenshot(firstReferencePages[index], index + 1)
            }
        }
        step("When Member1 returns through Shared Drive and Conversation details to the Conversation list") {
            pages.pdfViewerPage.navigateBack()
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.tapBackButton()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }
        step("And Member1 opens Shared Drive from OtherFiles Conversation details") {
            pages.conversationListPage.clickGroupConversation("OtherFiles")
            pages.conversationViewPage.clickOnGroupConversationDetails("OtherFiles")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }
        step("Then OtherFiles Shared Drive shows the same filename exactly once") {
            pages.wireDrivePage.apply {
                assertSharedDriveVisible()
                assertFileVisibleOnce(fileName, timeout = UiWaitUtils.SHORT_TIMEOUT)
            }
        }
        val secondReferencePages = mutableListOf<File>()
        step("When Member1 opens the known-content reference PDF in Shared Drive") {
            pages.wireDrivePage.openFile("Reference-$fileName")
            pages.pdfViewerPage.assertPdfPreviewVisible("Reference-$fileName")
        }
        repeat(secondPages.size) { index ->
            step("And Member1 scrolls to reference page ${index + 1} and captures its rendered content") {
                pages.pdfViewerPage.scrollToPdfPage(index + 1)
                val screenshot = pages.pdfViewerPage.savePdfPageScreenshot(index + 1)
                documentScreenshots += screenshot
                secondReferencePages += screenshot
            }
        }
        step("And Member1 returns from the reference PDF to Shared Drive") {
            pages.pdfViewerPage.navigateBack()
        }
        step("When Member1 opens the same-named PDF from OtherFiles Shared Drive") {
            pages.wireDrivePage.openFile(fileName)
        }
        step("Then OtherFiles' PDF opens with all four pages") {
            pages.pdfViewerPage.apply {
                assertPdfPreviewVisible(fileName)
                assertPdfPageCount(secondPages.size)
            }
        }
        secondPages.indices.forEach { index ->
            step("When Member1 scrolls to OtherFiles PDF page ${index + 1}") {
                pages.pdfViewerPage.scrollToPdfPage(index + 1)
            }
            step("Then OtherFiles PDF page ${index + 1} matches its own reference screenshot") {
                pages.pdfViewerPage.assertPdfMatchesScreenshot(secondReferencePages[index], index + 1)
            }
        }
    }

    /**
     * Skipped on the Xiaomi test phone (f212e5d2) running MIUI 13 on Android 11.
     * MIUI's Open with dialog launches Chrome under Android's system identity,
     * so Android rejects the temporary read permission for Wire's TXT attachment.
     * Wire stays on screen and reports that Chrome cannot access the requested resource.
     */
    @Suppress("LongMethod")
    @TestCaseId("TC-12262")
    @Category("regression", "RC", "wireDrive", "wireDriveDownloadsAndOffline")
    @Test
    fun givenConversationContainsNamedVideoAudioAndTextFiles_whenMemberOpensEachAndReturns_thenFilenamesAndFileTypesRemainCorrect() {
        step("Given the device is eligible for the TXT attachment test in Chrome") {
            assumeFalse(
                "Skipped on Xiaomi f212e5d2 because MIUI's Open with dialog fails to grant Chrome read access to the TXT file.",
                device.executeShellCommand("getprop ro.boot.serialno").trim() == "f212e5d2"
            )
        }

        step("And TeamOwner creates team FileTypePreviews") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "FileTypePreviews",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }
        step("And TeamOwner configures MLS for FileTypePreviews") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "FileTypePreviews", backendClient)
        }
        step("And TeamOwner adds Member1 to FileTypePreviews with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "FileTypePreviews",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }
        step("And Shared Drive is enabled and unlocked for FileTypePreviews") {
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
            }
        }
        step("And MLS devices are registered for TeamOwner and Member1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }
        step("And I sign in as Member1 and complete the login prompts") {
            clientUserManager.setSelfUser(member1)
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(member1)
        }
        step("And TeamOwner creates FileTypePreviews Conversation with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "FileTypePreviews",
                deviceName = "Device1",
                cellsEnabled = true
            )
            testServiceHelper.waitForSharedDriveReady("user1Name", "FileTypePreviews")
        }
        val suffix = UUID.randomUUID().toString().take(8)
        val video = "NamedVideo-$suffix.mp4"
        val audio = "NamedAudio-$suffix.mp3"
        val textFile = "NamedText-$suffix.txt"
        step("And TeamOwner sends named MP4, MP3 and TXT files to FileTypePreviews Conversation") {
            testServiceHelper.contactSendsLocalFileConversation(
                context,
                video,
                Uri.parse("android.resource://${context.packageName}/${R.raw.testing}"),
                "user1Name",
                "Device1",
                "FileTypePreviews",
                "video/mp4"
            )
            testServiceHelper.contactSendsLocalFileConversation(
                context,
                audio,
                Uri.parse("android.resource://${context.packageName}/${R.raw.drive_download_audio}"),
                "user1Name",
                "Device1",
                "FileTypePreviews",
                "audio/mpeg"
            )
            testServiceHelper.contactSendsLocalFileConversation(
                context,
                textFile,
                Uri.parse("android.resource://${context.packageName}/${R.raw.testing_text}"),
                "user1Name",
                "Device1",
                "FileTypePreviews",
                "text/plain"
            )
        }
        step("When Member1 opens FileTypePreviews Conversation and scrolls to the video message") {
            pages.conversationListPage.clickGroupConversation("FileTypePreviews")
            pages.conversationViewPage.scrollToLatestFile(video.substringBeforeLast('.'))
        }
        step("Then the video message displays its correct MP4 filename") {
            pages.conversationViewPage.assertVideoFilenameAndTypeVisible(video)
        }
        step("When Member1 opens the named video message") {
            pages.conversationViewPage.openCellsVideoMessage(video.substringBeforeLast('.'))
        }
        step("Then Wire's video player is ready with the same filename and a nonzero duration") {
            pages.driveMediaPlayerPage.assertPlayerReady(video)
        }
        step("When Member1 returns to FileTypePreviews Conversation") {
            pages.driveMediaPlayerPage.navigateBack()
        }
        step("Then the video message retains its correct filename") {
            pages.conversationViewPage.assertVideoFilenameAndTypeVisible(video)
        }
        step("When Member1 scrolls to the audio message") {
            pages.conversationViewPage.scrollToLatestFile(audio)
        }
        step("Then the audio message displays its correct MP3 filename") {
            pages.conversationViewPage.assertFileWithNameIsVisible(audio)
        }
        step("When Member1 opens the named audio message") {
            pages.conversationViewPage.clickFileWithName(audio)
        }
        step("Then Wire's audio player is ready with the same filename and a nonzero duration") {
            pages.driveMediaPlayerPage.assertPlayerReady(audio)
        }
        step("When Member1 returns to FileTypePreviews Conversation") {
            pages.driveMediaPlayerPage.navigateBack()
        }
        step("Then the audio message retains its correct filename") {
            pages.conversationViewPage.assertFileWithNameIsVisible(audio)
        }
        step("When Member1 scrolls to the TXT message") {
            pages.conversationViewPage.scrollToLatestFile(textFile)
        }
        step("Then the text message displays its correct TXT filename and type header") {
            pages.conversationViewPage.apply {
                assertFileWithNameIsVisible(textFile)
                assertTxtMessageTypeVisible(textFile)
            }
        }
        step("When Member1 opens Shared Drive through FileTypePreviews Conversation details and opens the TXT file's three-dot menu") {
            pages.conversationViewPage.clickOnGroupConversationDetails("FileTypePreviews")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
            pages.wireDrivePage.assertFileVisibleOnce(textFile, timeout = UiWaitUtils.SHORT_TIMEOUT)
            pages.wireDrivePage.tapFileMenu(textFile)
        }
        step("Then the TXT file's actions bottom sheet shows the correct filename") {
            pages.wireDrivePage.assertFileNameOnActionsMenu(textFile)
        }
        step("When Member1 returns to FileTypePreviews Conversation and opens the TXT message in Chrome") {
            pages.wireDrivePage.navigateBack()
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.tapBackButton()
            pages.conversationViewPage.apply {
                clickTextFileWithName(textFile)
                tapReadyTextFileMessageIfShown(textFile)
            }
            pages.chromePage.apply {
                selectChromeFromOpenWithDialog()
                dismissNotificationsPromptIfVisible()
            }
        }
        step("Then Chrome opens the correct TXT file and renders its known content") {
            pages.chromePage.assertTextFileOpenInChrome(textFile, "Wire file-sharing test attachment.")
        }
        step("When Member1 returns from Chrome to FileTypePreviews Conversation") {
            pages.chromePage.navigateBack()
        }
        step("Then the TXT message retains its correct filename and TXT type header") {
            pages.conversationViewPage.apply {
                assertFileWithNameIsVisible(textFile)
                assertTxtMessageTypeVisible(textFile)
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-12261")
    @Category("regression", "RC", "wireDrive", "wireDriveDownloadsAndOffline")
    @Test
    fun givenMemberHasSavedPdfOffline_whenTeamOwnerUpdatesTheSameOnlinePdf_thenMemberStillReadsOriginalContentWithoutEditingWhileOffline() {
        val offlineServiceUrl = InstrumentationRegistry.getArguments().getString("offlineTestServiceUrl")
            ?: BuildConfig.TEST_SERVICE_URL
        step("Given the offline file-update request uses an ADB-forwarded device-loopback URL") {
            check(Uri.parse(offlineServiceUrl).host == "127.0.0.1") {
                "TC-12261 needs an ADB reverse connection to test service while the device is offline. " +
                    "See the TC-12261 setup in tests/testsCore/README.md."
            }
        }
        step("And TeamOwner creates team OfflineFileVersion") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "OfflineFileVersion",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }
        step("And TeamOwner configures MLS for OfflineFileVersion") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "OfflineFileVersion", backendClient)
        }
        step("And TeamOwner adds Member1 to OfflineFileVersion with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "OfflineFileVersion",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }
        step("And Shared Drive is enabled and unlocked for OfflineFileVersion") {
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
            }
        }
        step("And MLS devices are registered for TeamOwner and Member1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }
        step("And I sign in as Member1 and complete the login prompts") {
            clientUserManager.setSelfUser(member1)
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(member1)
        }
        step("And TeamOwner creates OfflineFileVersion Conversation with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "OfflineFileVersion",
                deviceName = "Device1",
                cellsEnabled = true
            )
            testServiceHelper.waitForSharedDriveReady("user1Name", "OfflineFileVersion")
        }
        val suffix = UUID.randomUUID().toString().take(8)
        val sharedPdf = "OfflineFileVersion-$suffix.pdf"
        val originalReference = "OriginalReference-$suffix.pdf"
        val updatedReference = "UpdatedReference-$suffix.pdf"
        lateinit var replacementData: ByteArray
        step("And TeamOwner sends the Original content PDF and separate Original content and Updated content reference PDFs") {
            val originalUri = createPdfInDeviceDownloadsFolder(sharedPdf, "Original content")
            deviceDownloads += originalUri
            val updatedUri = createPdfInDeviceDownloadsFolder(updatedReference, "Updated content")
            deviceDownloads += updatedUri
            replacementData = requireNotNull(context.contentResolver.openInputStream(updatedUri)).use { it.readBytes() }
            testServiceHelper.apply {
                contactSendsLocalPdfConversation(context, sharedPdf, originalUri, "user1Name", "Device1", "OfflineFileVersion")
                contactSendsLocalPdfConversation(context, originalReference, originalUri, "user1Name", "Device1", "OfflineFileVersion")
                contactSendsLocalPdfConversation(context, updatedReference, updatedUri, "user1Name", "Device1", "OfflineFileVersion")
            }
        }
        lateinit var conversation: Conversation
        lateinit var originalAttachment: JSONObject
        step("And TeamOwner's test-service device identifies the original PDF's UUID and version before Member1 disconnects") {
            conversation = testServiceHelper.toConvoObj(teamOwner, "OfflineFileVersion")
            originalAttachment = testServiceHelper.testServiceClient.getDriveFileAttachment(
                teamOwner,
                "Device1",
                conversation.qualifiedID.id,
                conversation.qualifiedID.domain,
                sharedPdf
            )
            assertTrue("Original PDF UUID is missing", originalAttachment.getString("id").isNotBlank())
            assertTrue("Original PDF version is missing", originalAttachment.getString("versionId").isNotBlank())
        }
        step("When Member1 opens Shared Drive from OfflineFileVersion Conversation details") {
            pages.conversationListPage.clickGroupConversation("OfflineFileVersion")
            pages.conversationViewPage.clickOnGroupConversationDetails("OfflineFileVersion")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }
        step("Then Member1 sees Shared Drive with Editor access and all three PDFs exactly once") {
            pages.wireDrivePage.apply {
                assertSharedDriveVisibleWithoutViewerBanner()
                assertFileVisibleOnce(sharedPdf)
                assertFileVisibleOnce(originalReference)
                assertFileVisibleOnce(updatedReference)
            }
        }
        lateinit var originalScreenshot: Bitmap
        lateinit var updatedScreenshot: Bitmap
        step("When Member1 opens the Original content reference PDF") {
            pages.wireDrivePage.openFile(originalReference)
        }
        step("Then the Original content reference PDF renders and its page is captured for comparison") {
            pages.pdfViewerPage.assertPdfPreviewVisible(originalReference)
            originalScreenshot = pages.pdfViewerPage.capturePdfPageScreenshot()
            versionReferenceScreenshots += originalScreenshot
        }
        step("When Member1 returns to Shared Drive and opens the Updated content reference PDF") {
            pages.pdfViewerPage.navigateBack()
            pages.wireDrivePage.openFile(updatedReference)
        }
        step("Then the Updated content reference PDF renders and its page differs from the Original content reference") {
            pages.pdfViewerPage.assertPdfPreviewVisible(updatedReference)
            updatedScreenshot = pages.pdfViewerPage.capturePdfPageScreenshot()
            versionReferenceScreenshots += updatedScreenshot
            assertFalse(
                "The Original content and Updated content reference pages are identical",
                originalScreenshot.sameAs(updatedScreenshot)
            )
        }
        step("When Member1 returns to Shared Drive and opens the original PDF's three-dot menu") {
            pages.pdfViewerPage.navigateBack()
            pages.wireDrivePage.tapFileMenu(sharedPdf)
        }
        step("And Member1 selects Make available offline and Wire confirms File saved for offline use") {
            UiWaitUtils.assertToastDisplayed(
                text = "File saved for offline use",
                trigger = { pages.wireDrivePage.tapMakeAvailableOfflineOption() },
                timeout = UiWaitUtils.LONG_TIMEOUT
            )
        }
        step("When Member1 disables Wi-Fi and mobile data") {
            restoreNetworkAfterTest = true
            pages.commonAppPage.apply {
                disableWifi()
                disableMobileData()
            }
        }
        step("Then the device has no active network and Shared Drive shows the offline banner and saved PDF exactly once") {
            pages.commonAppPage.assertDeviceHasNoNetworkConnection()
            pages.wireDrivePage.apply {
                assertOfflineBannerVisible()
                assertFileVisibleOnce(sharedPdf)
            }
        }
        step("When Member1 opens the saved PDF while offline") {
            pages.wireDrivePage.openFile(sharedPdf)
        }
        step("Then the offline PDF matches the Original content reference and has no editing or sharing controls") {
            pages.pdfViewerPage.apply {
                assertPdfPreviewVisible(sharedPdf)
                assertPdfMatchesScreenshot(originalScreenshot)
                assertPdfHasNoEditOrShareControls()
            }
        }
        lateinit var replacement: JSONObject
        step("When TeamOwner replaces the same online PDF with Updated content through test service while Member1 stays offline") {
            replacement = testServiceHelper.testServiceClient.replaceDriveFileContent(
                teamOwner,
                "Device1",
                conversation.qualifiedID.id,
                conversation.qualifiedID.domain,
                originalAttachment.getString("id"),
                replacementData,
                serviceUrl = offlineServiceUrl
            )
        }
        step("Then test service confirms the same file UUID with a new published version and Member1's device remains offline") {
            assertEquals("The replacement created a different file", originalAttachment.getString("id"), replacement.getString("fileUuid"))
            assertTrue("The published version ID is missing", replacement.getString("versionId").isNotBlank())
            assertNotEquals(
                "The original PDF version was not replaced",
                originalAttachment.getString("versionId"),
                replacement.getString("versionId")
            )
            pages.commonAppPage.assertDeviceHasNoNetworkConnection()
        }
        step("When Member1 closes the PDF preview and opens the saved PDF's three-dot menu in Shared Drive while offline") {
            pages.pdfViewerPage.navigateBack()
            pages.wireDrivePage.tapFileMenu(sharedPdf)
        }
        step("Then the offline PDF menu allows Open and Remove offline access but hides sharing, tags, move, rename and delete") {
            pages.wireDrivePage.assertOfflinePdfMenuAllowsOpenAndRemoveOfflineAccess()
        }
        step("When Member1 selects Open from the saved PDF's three-dot menu while still offline") {
            pages.wireDrivePage.tapOpenOptionOnActionsMenu()
        }
        step("Then the offline PDF still matches Original content rather than Updated content and has no editing or sharing controls") {
            pages.pdfViewerPage.apply {
                assertPdfPreviewVisible(sharedPdf)
                assertPdfMatchesScreenshot(originalScreenshot)
                assertPdfHasNoEditOrShareControls()
            }
            pages.commonAppPage.assertDeviceHasNoNetworkConnection()
        }
        step("When Member1 closes the PDF and restores the device's original Wi-Fi and mobile-data settings") {
            pages.pdfViewerPage.navigateBack()
            pages.commonAppPage.apply {
                if (originalWifiEnabled) enableWifiAndScanForNetworks()
                if (originalMobileDataEnabled) enableMobileData()
            }
        }
        step("Then the device has an active network connection") {
            pages.commonAppPage.assertDeviceHasNetworkConnection()
        }
        step("When Member1 returns through Conversation details to the Conversation list") {
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.tapBackButton()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }
        step("And I add TeamOwner's account and complete the login prompts") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.tapNewAccountButton()
            clientUserManager.setSelfUser(teamOwner)
            loginToStagingAs(teamOwner)
        }
        step("When TeamOwner opens Shared Drive from OfflineFileVersion Conversation details") {
            pages.conversationListPage.clickGroupConversation("OfflineFileVersion")
            pages.conversationViewPage.clickOnGroupConversationDetails("OfflineFileVersion")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }
        step("Then TeamOwner sees the original online PDF exactly once") {
            pages.wireDrivePage.apply {
                assertSharedDriveVisibleWithoutViewerBanner()
                assertFileVisibleOnce(sharedPdf)
            }
        }
        step("When TeamOwner opens the same online PDF for the first time on this app account") {
            pages.wireDrivePage.openFile(sharedPdf)
        }
        step("Then TeamOwner's PDF matches the Updated content reference, confirming the online content changed") {
            pages.pdfViewerPage.apply {
                assertPdfPreviewVisible(sharedPdf)
                assertPdfMatchesScreenshot(updatedScreenshot)
            }
        }
    }

    // Shared app login flow: opens staging, signs in as the selected user, and clears post-login prompts.
    private fun loginToStagingAs(user: ClientUser) {
        pages.loginPage.apply {
            clickStagingDeepLink()
            clickProceedButtonOnDeeplinkOverlay()
            clickContinueButtonOnBackendConfigSuccess()
            enterUserIdentifier(user.email.orEmpty())
            clickLoginButton()
            enterUserPassword(user.password.orEmpty())
            clickLoginButton()
        }
        pages.registrationPage.apply {
            waitUntilLoginFlowIsCompleted()
            clickAllowNotificationButton()
            clickDeclineShareDataAlert()
        }
        pages.conversationListPage.waitUntilWireServiceNotificationDisappears()
    }
}
