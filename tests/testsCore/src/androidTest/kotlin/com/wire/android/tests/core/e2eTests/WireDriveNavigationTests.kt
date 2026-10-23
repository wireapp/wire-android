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

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.team.TeamRoles
import backendUtils.team.disableCellsFeatureViaBackdoorTeam
import backendUtils.team.enableCellsFeatureViaBackdoorTeam
import backendUtils.team.getTeam
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import createPdfInDeviceDownloadsFolder
import deleteDeviceDownload
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import user.utils.ClientUser
import java.time.Instant
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@Suppress("LargeClass")
class WireDriveNavigationTests : BaseUiTest() {
    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser
    private val deviceDownloads = mutableListOf<Uri>()
    private var originalTimeFormat: String? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @After
    fun tearDown() {
        try {
            originalTimeFormat?.let(UiAutomatorSetup::setTimeFormat)
        } finally {
            deviceDownloads.forEach(::deleteDeviceDownload)
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-12218")
    @Category("regression", "RC", "wireDrive", "wireDriveNavigation")
    @Test
    fun givenFilesInRootAndChildFolders_whenOpeningFoldersAndGoingBack_thenEachFolderShowsOnlyItsOwnFiles() {
        givenTeamOwnerAndMemberArePreparedForSharedDrive("DriveNavigation")

        step("And MLS devices are registered for all participants") {
            testServiceHelper.apply {
                addDevice("user1Name", null, "Device1")
                addDevice("user2Name", null, "Device1")
            }
        }

        step("And TeamOwner creates MLS group DriveNavigation with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "DriveNavigation",
                deviceName = "Device1",
                cellsEnabled = true
            )
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginAsMember1ThroughStagingDeepLink()

        step("And I see and open group conversation DriveNavigation") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("DriveNavigation")
                clickGroupConversation("DriveNavigation")
            }
            pages.conversationViewPage.assertGroupConversationInForeground("DriveNavigation")
        }

        step("And I open Shared Drive from the group conversation details") {
            pages.conversationViewPage.clickOnGroupConversationDetails("DriveNavigation")
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapSharedDriveButton()
            }
            pages.wireDrivePage.assertSharedDriveVisible()
        }

        val suffix = UUID.randomUUID().toString().take(8)
        val rootFile = "DriveRoot-$suffix.pdf"
        val childFile = "DriveChild-$suffix.pdf"

        step("And I prepare two different PDFs in Member1's device Downloads folder") {
            deviceDownloads += createPdfInDeviceDownloadsFolder(rootFile, "Root folder content")
            deviceDownloads += createPdfInDeviceDownloadsFolder(childFile, "Child folder content")
        }

        step("And I upload the root PDF to the Shared Drive main folder") {
            pages.wireDrivePage.apply {
                tapNewButton()
                tapUploadFilesOption()
            }
            pages.documentsUIPage.selectFileInDocumentsUI(rootFile)
            pages.wireDrivePage.tapUploadButtonOnUploadConfirmationDialog()
        }

        step("And I see Upload complete and collapse the upload status panel") {
            pages.wireDrivePage.apply {
                assertUploadCompleteVisible()
                tapCollapseButtonOnUploadStatusPanel()
            }
        }

        step("And I see the Upload complete banner and the root PDF in the Shared Drive main folder") {
            pages.wireDrivePage.apply {
                assertUploadCompleteVisible()
                assertFileVisibleOnce(rootFile)
            }
        }

        step("And I create folder ParentFolder in the Shared Drive main folder") {
            pages.wireDrivePage.createFolder("ParentFolder")
        }

        step("And I see folder ParentFolder once, open it, and verify that it is empty") {
            pages.wireDrivePage.apply {
                assertFolderVisibleOnce("ParentFolder")
                openFolder("ParentFolder")
                assertEmptyFolderVisible()
            }
        }

        step("And I create folder ChildFolder inside ParentFolder") {
            pages.wireDrivePage.createFolder("ChildFolder")
        }

        step("And I see folder ChildFolder once, open it, and verify that it is empty") {
            pages.wireDrivePage.apply {
                assertFolderVisibleOnce("ChildFolder")
                openFolder("ChildFolder")
                assertEmptyFolderVisible()
            }
        }

        step("And I upload the child PDF inside ChildFolder") {
            pages.wireDrivePage.apply {
                tapNewButton()
                tapUploadFilesOption()
            }
            pages.documentsUIPage.selectFileInDocumentsUI(childFile)
            pages.wireDrivePage.tapUploadButtonOnUploadConfirmationDialog()
        }

        step("And I see Upload complete and collapse the upload status panel") {
            pages.wireDrivePage.apply {
                assertUploadCompleteVisible()
                tapCollapseButtonOnUploadStatusPanel()
            }
        }

        step("And I see the Upload complete banner and the child PDF inside ChildFolder") {
            pages.wireDrivePage.apply {
                assertUploadCompleteVisible()
                assertFileVisibleOnce(childFile)
            }
        }

        step("And I return through ParentFolder to the Shared Drive main folder to begin the navigation checks") {
            pages.wireDrivePage.apply {
                navigateBack()
                assertFolderVisibleOnce("ChildFolder")
                navigateBack()
            }
        }

        step("Then the main folder shows ParentFolder and its own PDF without ChildFolder or its PDF") {
            pages.wireDrivePage.apply {
                assertFolderVisibleOnce("ParentFolder")
                assertFileVisibleOnce(rootFile)
                assertFolderNotVisible("ChildFolder")
                assertFileNotVisible(childFile)
            }
        }

        step("When I open folder ParentFolder") {
            pages.wireDrivePage.openFolder("ParentFolder")
        }

        step("Then ParentFolder shows ChildFolder without either PDF") {
            pages.wireDrivePage.apply {
                assertFolderVisibleOnce("ChildFolder")
                assertFileNotVisible(rootFile)
                assertFileNotVisible(childFile)
            }
        }

        step("When I open folder ChildFolder") {
            pages.wireDrivePage.openFolder("ChildFolder")
        }

        step("Then ChildFolder shows its own PDF without the main folder's PDF") {
            pages.wireDrivePage.apply {
                assertFileVisibleOnce(childFile)
                assertFileNotVisible(rootFile)
            }
        }

        step("When I use Back to return to ParentFolder") {
            pages.wireDrivePage.navigateBack()
        }

        step("Then ParentFolder still shows ChildFolder without either PDF") {
            pages.wireDrivePage.apply {
                assertFolderVisibleOnce("ChildFolder")
                assertFileNotVisible(rootFile)
                assertFileNotVisible(childFile)
            }
        }

        step("When I use Back to return to the Shared Drive root") {
            pages.wireDrivePage.navigateBack()
        }

        step("Then the root shows ParentFolder and its own PDF without the PDF from ChildFolder") {
            pages.wireDrivePage.apply {
                assertFolderVisibleOnce("ParentFolder")
                assertFileVisibleOnce(rootFile)
                assertFolderNotVisible("ChildFolder")
                assertFileNotVisible(childFile)
            }
        }

        step("When I reopen ParentFolder and ChildFolder") {
            pages.wireDrivePage.apply {
                openFolder("ParentFolder")
                openFolder("ChildFolder")
            }
        }

        step("Then ChildFolder still shows its own PDF once without the main folder's PDF") {
            pages.wireDrivePage.apply {
                assertFileVisibleOnce(childFile)
                assertFileNotVisible(rootFile)
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-12216")
    @Category("regression", "RC", "wireDrive", "wireDriveNavigation")
    @Test
    fun givenFilesInTwoGroupConversations_whenOpeningDriveAndSharedDrive_thenDriveShowsBothFilesAndSharedDriveShowsOnlyItsConversationsFile() {
        givenTeamOwnerAndMemberArePreparedForSharedDrive("DriveAcrossGroups")

        step("And MLS devices are registered for all participants") {
            testServiceHelper.apply {
                addDevice("user1Name", null, "Device1")
                addDevice("user2Name", null, "Device1")
            }
        }

        step("And TeamOwner creates group conversation GroupFileOne with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "GroupFileOne",
                deviceName = "Device1",
                cellsEnabled = true
            )
        }

        step("And TeamOwner creates group conversation GroupFileTwo with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "GroupFileTwo",
                deviceName = "Device1",
                cellsEnabled = true
            )
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginAsMember1ThroughStagingDeepLink()

        val suffix = UUID.randomUUID().toString().take(8)
        val firstGroupFile = "FirstGroup-$suffix.pdf"
        val secondGroupFile = "SecondGroup-$suffix.pdf"

        step("And I prepare a different PDF for each group in Member1's device Downloads folder") {
            deviceDownloads += createPdfInDeviceDownloadsFolder(firstGroupFile, "GroupFileOne group content")
            deviceDownloads += createPdfInDeviceDownloadsFolder(secondGroupFile, "GroupFileTwo group content")
        }

        step("And I see and open group conversation GroupFileOne") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("GroupFileOne")
                clickGroupConversation("GroupFileOne")
            }
            pages.conversationViewPage.assertGroupConversationInForeground("GroupFileOne")
        }

        step("And I open Shared Drive from GroupFileOne's conversation details") {
            pages.conversationViewPage.clickOnGroupConversationDetails("GroupFileOne")
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapSharedDriveButton()
            }
            pages.wireDrivePage.assertSharedDriveVisible()
        }

        step("And I upload the first group's PDF to GroupFileOne's Shared Drive") {
            pages.wireDrivePage.apply {
                tapNewButton()
                tapUploadFilesOption()
            }
            pages.documentsUIPage.selectFileInDocumentsUI(firstGroupFile)
            pages.wireDrivePage.tapUploadButtonOnUploadConfirmationDialog()
        }

        step("And I see Upload complete for the first PDF and collapse the upload status panel") {
            pages.wireDrivePage.apply {
                assertUploadCompleteVisible()
                tapCollapseButtonOnUploadStatusPanel()
            }
        }

        step("And I see the Upload complete banner and the first PDF in GroupFileOne's Shared Drive") {
            pages.wireDrivePage.apply {
                assertUploadCompleteVisible()
                assertFileVisibleOnce(firstGroupFile)
            }
        }

        step("And I return from Shared Drive through conversation details and GroupFileOne to the conversation list") {
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapCloseButtonOnGroupConversationDetailsPage()
            }
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("GroupFileOne")
                tapBackButtonToCloseConversationViewPage()
            }
            pages.conversationListPage.assertConversationListVisible()
        }

        step("And I see and open group conversation GroupFileTwo") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("GroupFileTwo")
                clickGroupConversation("GroupFileTwo")
            }
            pages.conversationViewPage.assertGroupConversationInForeground("GroupFileTwo")
        }

        step("And I open Shared Drive from GroupFileTwo's conversation details") {
            pages.conversationViewPage.clickOnGroupConversationDetails("GroupFileTwo")
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapSharedDriveButton()
            }
            pages.wireDrivePage.assertSharedDriveVisible()
        }

        step("And I upload the second group's PDF to GroupFileTwo's Shared Drive") {
            pages.wireDrivePage.apply {
                tapNewButton()
                tapUploadFilesOption()
            }
            pages.documentsUIPage.selectFileInDocumentsUI(secondGroupFile)
            pages.wireDrivePage.tapUploadButtonOnUploadConfirmationDialog()
        }

        step("And I see Upload complete for the second PDF and collapse the upload status panel") {
            pages.wireDrivePage.apply {
                assertUploadCompleteVisible()
                tapCollapseButtonOnUploadStatusPanel()
            }
        }

        step("And I see the Upload complete banner and only the second group's PDF in GroupFileTwo's Shared Drive") {
            pages.wireDrivePage.apply {
                assertUploadCompleteVisible()
                assertFileVisibleOnce(secondGroupFile)
                assertFileNotVisible(firstGroupFile)
            }
        }

        step("And I return from Shared Drive through conversation details and GroupFileTwo to the conversation list") {
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapCloseButtonOnGroupConversationDetailsPage()
            }
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("GroupFileTwo")
                tapBackButtonToCloseConversationViewPage()
            }
            pages.conversationListPage.assertConversationListVisible()
        }

        step("When I open the main navigation menu and tap Drive") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                tapDriveButtonOnMenuEntry()
            }
        }

        step("Then Drive shows both groups' PDFs once") {
            pages.wireDrivePage.apply {
                assertDrivePageVisible()
                assertFileVisibleOnce(firstGroupFile)
                assertFileVisibleOnce(secondGroupFile)
            }
        }

        step("When I open the main navigation menu from Drive and return to Conversations") {
            pages.wireDrivePage.tapMainNavigationButtonOnDrivePage()
            pages.conversationListPage.apply {
                clickConversationsButtonOnMenuEntry()
                assertConversationListVisible()
            }
        }

        step("And I open GroupFileOne and its Shared Drive through conversation details") {
            pages.conversationListPage.clickGroupConversation("GroupFileOne")
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("GroupFileOne")
                clickOnGroupConversationDetails("GroupFileOne")
            }
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapSharedDriveButton()
            }
            pages.wireDrivePage.assertSharedDriveVisible()
        }

        step("Then GroupFileOne's Shared Drive shows its own PDF once and excludes GroupFileTwo's PDF") {
            pages.wireDrivePage.apply {
                assertFileVisibleOnce(firstGroupFile)
                assertFileNotVisible(secondGroupFile)
            }
        }

        step("When I return to GroupFileOne's conversation and reopen its Shared Drive") {
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapCloseButtonOnGroupConversationDetailsPage()
            }
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("GroupFileOne")
                clickOnGroupConversationDetails("GroupFileOne")
            }
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapSharedDriveButton()
            }
            pages.wireDrivePage.assertSharedDriveVisible()
        }

        step("Then GroupFileOne's Shared Drive still shows its own PDF once and excludes GroupFileTwo's PDF") {
            pages.wireDrivePage.apply {
                assertFileVisibleOnce(firstGroupFile)
                assertFileNotVisible(secondGroupFile)
            }
        }

        step("When I return through conversation details and GroupFileOne to the conversation list") {
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapCloseButtonOnGroupConversationDetailsPage()
            }
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("GroupFileOne")
                tapBackButtonToCloseConversationViewPage()
            }
            pages.conversationListPage.assertConversationListVisible()
        }

        step("And I open GroupFileTwo and its Shared Drive through conversation details") {
            pages.conversationListPage.clickGroupConversation("GroupFileTwo")
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("GroupFileTwo")
                clickOnGroupConversationDetails("GroupFileTwo")
            }
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapSharedDriveButton()
            }
            pages.wireDrivePage.assertSharedDriveVisible()
        }

        step("Then GroupFileTwo's Shared Drive shows its own PDF once and excludes GroupFileOne's PDF") {
            pages.wireDrivePage.apply {
                assertFileVisibleOnce(secondGroupFile)
                assertFileNotVisible(firstGroupFile)
            }
        }

        step("When I return through conversation details and GroupFileTwo to the conversation list") {
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapCloseButtonOnGroupConversationDetailsPage()
            }
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("GroupFileTwo")
                tapBackButtonToCloseConversationViewPage()
            }
            pages.conversationListPage.assertConversationListVisible()
        }

        step("And I reopen Drive from the main navigation menu") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                tapDriveButtonOnMenuEntry()
            }
        }

        step("Then Drive still shows both groups' PDFs once without mixing the Shared Drive lists") {
            pages.wireDrivePage.apply {
                assertDrivePageVisible()
                assertFileVisibleOnce(firstGroupFile)
                assertFileVisibleOnce(secondGroupFile)
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-12217")
    @Category("regression", "RC", "wireDrive", "wireDriveNavigation")
    @Test
    fun givenGuestFromTeamWithDriveDisabled_whenOpeningPdfInAnotherTeamsSharedDrive_thenPdfPreviewIsVisibleAndDriveOptionIsHiddenInMainMenu() {
        step("Given There is a team owner TeamOwner with team DriveGuestAccess") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "DriveGuestAccess",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "DriveGuestAccess", backendClient)
        }

        step("And There is a team owner GuestTeamOwner with team GuestTeam") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user3Name",
                "GuestTeam",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam("user3Name", "GuestTeam", backendClient)
        }

        step("And GuestTeamOwner adds Member1 to team GuestTeam with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user3Name",
                "user2Name",
                "GuestTeam",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And Drive is enabled for TeamOwner's team DriveGuestAccess") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
            }
        }

        step("And Drive is disabled for Member1's own team GuestTeam") {
            val guestTeamOwner = clientUserManager.findUserByNameOrNameAlias("user3Name")
            runBlocking {
                backendClient.disableCellsFeatureViaBackdoorTeam(backendClient.getTeam(guestTeamOwner))
            }
        }

        step("And TeamOwner is connected to Member1 from GuestTeam") {
            backendSetupHelper.userIsConnectedTo("user1Name", "user2Name")
        }

        step("And MLS devices are registered for TeamOwner and Member1") {
            testServiceHelper.apply {
                addDevice("user1Name", null, "Device1")
                addDevice("user2Name", null, "Device1")
            }
        }

        step("And TeamOwner creates MLS group SharedWithGuest with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "SharedWithGuest",
                deviceName = "Device1",
                cellsEnabled = true
            )
        }

        val guestFile = "GuestAccess-${UUID.randomUUID().toString().take(8)}.pdf"

        step("And TeamOwner sends a PDF to SharedWithGuest through the test service") {
            val pdfUri = createPdfInDeviceDownloadsFolder(guestFile, "Shared with Member1 from GuestTeam")
            deviceDownloads += pdfUri
            testServiceHelper.contactSendsLocalPdfConversation(
                context,
                guestFile,
                pdfUri,
                "user1Name",
                "Device1",
                "SharedWithGuest"
            )
        }

        step("And User Member1 from GuestTeam is me and is a guest in SharedWithGuest") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginAsMember1ThroughStagingDeepLink()

        step("And I wait until Wire service notification disappears") {
            pages.conversationListPage.waitUntilWireServiceNotificationDisappears()
        }

        step("When I open the main navigation menu as Member1") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                clickConversationsMenuEntry()
            }
        }

        step("Then the main navigation menu does not show Drive") {
            pages.conversationListPage.assertDriveButtonNotVisibleOnMenuEntry()
        }

        step("When I return to Conversations and open group conversation SharedWithGuest") {
            pages.conversationListPage.apply {
                clickConversationsButtonOnMenuEntry()
                assertConversationListVisible()
                assertGroupConversationVisible("SharedWithGuest")
                clickGroupConversation("SharedWithGuest")
            }
            pages.conversationViewPage.assertGroupConversationInForeground("SharedWithGuest")
        }

        step("And I open Shared Drive from SharedWithGuest's conversation details") {
            pages.conversationViewPage.clickOnGroupConversationDetails("SharedWithGuest")
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapSharedDriveButton()
            }
        }

        step("Then I see the Viewer access banner and exactly one entry for TeamOwner's PDF in Shared Drive") {
            pages.wireDrivePage.apply {
                assertSharedDriveVisibleWithViewerAccess()
                assertFileVisibleOnce(guestFile)
            }
        }

        step("When I open TeamOwner's PDF from Shared Drive, I see its filename and a rendered page in the preview") {
            pages.wireDrivePage.openFile(guestFile)
            pages.pdfViewerPage.assertPdfPreviewVisible(guestFile)
        }

        step("When I use Back to return to Shared Drive, I see the Viewer access banner and exactly one entry for TeamOwner's PDF") {
            pages.pdfViewerPage.navigateBack()
            pages.wireDrivePage.apply {
                assertSharedDriveVisibleWithViewerAccess()
                assertFileVisibleOnce(guestFile)
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-12219", "TC-12220")
    @Category("regression", "RC", "wireDrive", "wireDriveNavigation")
    @Test
    fun givenDriveHas35FilesBeforeLogin_whenMemberScrollsFromPage1ToPage2AndOpensPdfs_thenNoFilesAreMissingOrDuplicated() {
        givenTeamOwnerAndMemberArePreparedForSharedDrive("DrivePagination")

        step("And MLS devices are registered for TeamOwner and Member1") {
            testServiceHelper.apply {
                addDevice("user1Name", null, "Device1")
                addDevice("user2Name", null, "Device1")
            }
        }

        step("And TeamOwner creates MLS group ManySharedFiles with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "ManySharedFiles",
                deviceName = "Device1",
                cellsEnabled = true
            )
        }

        val suffix = UUID.randomUUID().toString().take(8)
        val expectedFileNames = (1..35).map { number ->
            "DriveFile-${number.toString().padStart(3, '0')}-$suffix.pdf"
        }

        step("And TeamOwner sends 35 uniquely named PDFs to ManySharedFiles before Member1 signs in") {
            val pdfUri = createPdfInDeviceDownloadsFolder("PaginationSource-$suffix.pdf", "Drive pagination")
            deviceDownloads += pdfUri
            expectedFileNames.forEach { fileName ->
                testServiceHelper.contactSendsLocalPdfConversation(
                    context,
                    fileName,
                    pdfUri,
                    "user1Name",
                    "Device1",
                    "ManySharedFiles"
                )
            }
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginAsMember1ThroughStagingDeepLink()

        lateinit var firstFileName: String

        // TC-12219 - Load both pages of Drive files and check for missing or duplicate entries on both passes.
        step("When I open Drive from the main menu after login and note the first PDF in the list") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertGroupConversationVisible("ManySharedFiles")
                clickConversationsMenuEntry()
                tapDriveButtonOnMenuEntry()
            }
            pages.wireDrivePage.assertDrivePageVisible()
            firstFileName = pages.wireDrivePage.getFirstVisiblePdfFileName()
        }

        step("Then I can scroll to the end of Drive and see all 35 expected PDFs without missing or duplicate entries") {
            pages.wireDrivePage.assertAllFilesAppearWithoutDuplicatesWhileScrolling(expectedFileNames)
        }

        step("When I scroll back to the first PDF in Drive") {
            pages.wireDrivePage.scrollToFirstFile(firstFileName)
        }

        step("Then I can scroll to the end again and see all 35 expected PDFs without missing or duplicate entries") {
            pages.wireDrivePage.assertAllFilesAppearWithoutDuplicatesWhileScrolling(expectedFileNames)
        }

        // TC-12220 - Open an existing PDF from Drive and Shared Drive in the same session without restarting Wire.
        val existingFile = expectedFileNames.first()

        step("When I open the oldest PDF from Drive, I see its filename and a rendered page") {
            pages.wireDrivePage.openFile(existingFile)
            pages.pdfViewerPage.assertPdfPreviewVisible(existingFile)
        }

        step("And I use Back to return to Drive and still see exactly one entry for the PDF") {
            pages.pdfViewerPage.navigateBack()
            pages.wireDrivePage.apply {
                assertDrivePageVisible()
                assertFileVisibleOnce(existingFile)
            }
        }

        step("And I return to Conversations from Drive's main navigation menu") {
            pages.wireDrivePage.tapMainNavigationButtonOnDrivePage()
            pages.conversationListPage.apply {
                clickConversationsButtonOnMenuEntry()
                assertConversationListVisible()
            }
        }

        step("And I open ManySharedFiles' Shared Drive through its conversation details") {
            pages.conversationListPage.clickGroupConversation("ManySharedFiles")
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("ManySharedFiles")
                clickOnGroupConversationDetails("ManySharedFiles")
            }
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapSharedDriveButton()
            }
        }

        step("Then Shared Drive shows exactly one entry for the same PDF without restarting Wire") {
            pages.wireDrivePage.apply {
                assertSharedDriveVisible()
                assertFileVisibleOnce(existingFile)
            }
        }

        step("When I open the same PDF from Shared Drive, I see its filename and a rendered page") {
            pages.wireDrivePage.openFile(existingFile)
            pages.pdfViewerPage.assertPdfPreviewVisible(existingFile)
        }

        step("Then I use Back to return to Shared Drive and still see exactly one entry for the PDF") {
            pages.pdfViewerPage.navigateBack()
            pages.wireDrivePage.apply {
                assertSharedDriveVisible()
                assertFileVisibleOnce(existingFile)
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    // TC-12221 currently fails due to WPB-22171: Drive file timestamps do not follow the device's 12/24-hour setting.
    @TestCaseId("TC-12221")
    @Category("regression", "RC", "wireDrive", "wireDriveNavigation")
    @Test
    fun givenPdfInDriveAndSharedDrive_whenMemberSwitchesBetween12And24HourTime_thenFileTimestampsFollowDeviceSetting() {
        givenTeamOwnerAndMemberArePreparedForSharedDrive("DriveTimeFormat")

        step("And MLS devices are registered for TeamOwner and Member1") {
            testServiceHelper.apply {
                addDevice("user1Name", null, "Device1")
                addDevice("user2Name", null, "Device1")
            }
        }

        step("And TeamOwner creates MLS group FileTimeFormat with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "FileTimeFormat",
                deviceName = "Device1",
                cellsEnabled = true
            )
        }

        val timestampFile = "TimeFormat-${UUID.randomUUID().toString().take(8)}.pdf"
        lateinit var uploadStartedAt: Instant
        lateinit var uploadFinishedAt: Instant

        step("And TeamOwner sends a PDF to FileTimeFormat and I record when the upload starts and finishes") {
            val pdfUri = createPdfInDeviceDownloadsFolder(timestampFile, "File timestamp format")
            deviceDownloads += pdfUri
            uploadStartedAt = Instant.now()
            testServiceHelper.contactSendsLocalPdfConversation(
                context,
                timestampFile,
                pdfUri,
                "user1Name",
                "Device1",
                "FileTimeFormat"
            )
            uploadFinishedAt = Instant.now()
        }

        step("And I save the original device time format and select 12-hour time") {
            originalTimeFormat = UiAutomatorSetup.getTimeFormat()
            UiAutomatorSetup.setTimeFormat("12")
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginAsMember1ThroughStagingDeepLink()

        step("When I open Drive from the main navigation menu") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                clickConversationsMenuEntry()
                tapDriveButtonOnMenuEntry()
            }
            pages.wireDrivePage.assertDrivePageVisible()
        }

        step("Then Drive shows the PDF's upload date and time in 12-hour format with AM or PM") {
            pages.wireDrivePage.assertFileTimestampMatchesDeviceTimeFormat(
                timestampFile,
                uploadStartedAt,
                uploadFinishedAt
            )
        }

        step("When I return to Conversations and open FileTimeFormat's Shared Drive through conversation details") {
            pages.wireDrivePage.tapMainNavigationButtonOnDrivePage()
            pages.conversationListPage.apply {
                clickConversationsButtonOnMenuEntry()
                assertConversationListVisible()
                assertGroupConversationVisible("FileTimeFormat")
                clickGroupConversation("FileTimeFormat")
            }
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("FileTimeFormat")
                clickOnGroupConversationDetails("FileTimeFormat")
            }
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapSharedDriveButton()
            }
            pages.wireDrivePage.assertSharedDriveVisible()
        }

        step("Then Shared Drive shows the PDF's upload date and time in 12-hour format with AM or PM") {
            pages.wireDrivePage.assertFileTimestampMatchesDeviceTimeFormat(
                timestampFile,
                uploadStartedAt,
                uploadFinishedAt
            )
        }

        step("When I return through conversation details and FileTimeFormat to the conversation list") {
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapCloseButtonOnGroupConversationDetailsPage()
            }
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("FileTimeFormat")
                tapBackButtonToCloseConversationViewPage()
            }
            pages.conversationListPage.assertConversationListVisible()
        }

        step("And I change the device to 24-hour time and reopen Drive from the main navigation menu") {
            UiAutomatorSetup.setTimeFormat("24")
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                tapDriveButtonOnMenuEntry()
            }
            pages.wireDrivePage.assertDrivePageVisible()
        }

        step("Then Drive shows the same PDF upload date and time in 24-hour format without AM or PM") {
            pages.wireDrivePage.assertFileTimestampMatchesDeviceTimeFormat(
                timestampFile,
                uploadStartedAt,
                uploadFinishedAt
            )
        }

        step("When I return to Conversations and reopen FileTimeFormat's Shared Drive through conversation details") {
            pages.wireDrivePage.tapMainNavigationButtonOnDrivePage()
            pages.conversationListPage.apply {
                clickConversationsButtonOnMenuEntry()
                assertConversationListVisible()
                clickGroupConversation("FileTimeFormat")
            }
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("FileTimeFormat")
                clickOnGroupConversationDetails("FileTimeFormat")
            }
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapSharedDriveButton()
            }
            pages.wireDrivePage.assertSharedDriveVisible()
        }

        step("Then Shared Drive shows the same PDF upload date and time in 24-hour format without AM or PM") {
            pages.wireDrivePage.assertFileTimestampMatchesDeviceTimeFormat(
                timestampFile,
                uploadStartedAt,
                uploadFinishedAt
            )
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-12222", "TC-12223")
    @Category("regression", "RC", "wireDrive", "wireDriveNavigation")
    @Test
    fun givenTeamOwnerCanCreateGroupsAndChannels_whenCreatingWithSharedDriveOffAndOn_thenDetailsShowSelectedSetting() {
        step("Given There is a team owner TeamOwner with team DriveCreation") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "DriveCreation",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "DriveCreation", backendClient)
        }

        step("And Shared Drive and channel creation are enabled for team DriveCreation") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
            }
            backendSetupHelper.userEnablesChannelFeatureForTeam(
                "user1Name",
                "DriveCreation",
                backendClient
            )
        }

        step("And User TeamOwner is me") {
            clientUserManager.setSelfUser(teamOwner)
        }

        step("And I log in as TeamOwner through the staging deep link") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
                enterTeamOwnerLoggingEmail(teamOwner.email.orEmpty())
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwner.password.orEmpty())
                clickLoginButton()
            }
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("When I start creating a new group without adding participants") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                tapStartNewConversationButton()
            }
            pages.searchPage.tapCreateNewGroupButton()
            pages.searchPage.tapContinueButtonOnAddParticipantsPage()
        }

        step("And I name the group GroupSharedDriveOn and open its settings") {
            pages.createNewGroupPage.apply {
                assertCreateNewGroupDetailsPageVisible()
                enterNewGroupName("GroupSharedDriveOn")
                tapContinueButtonOnGroupDetailsPage()
                assertCreateNewGroupSettingsPageVisible()
            }
        }

        step("Then Shared Drive is off by default for GroupSharedDriveOn") {
            pages.createNewGroupPage.assertSharedDriveIsOffOnCreationSettingsPage()
        }

        step("And I enable Shared Drive for GroupSharedDriveOn") {
            pages.createNewGroupPage.apply {
                tapSharedDriveToggleOnCreationSettingsPage()
                assertSharedDriveIsOnOnCreationSettingsPage()
            }
        }

        // TC-12223 - Open Shared Drive help and verify that returning preserves the creation state.
        step("When I open Learn more from the Shared Drive explanation for GroupSharedDriveOn") {
            pages.createNewGroupPage.tapLearnMoreLinkInSharedDriveExplanation()
        }

        step("Then the browser shows the Shared Drive support article") {
            pages.chromePage.assertSharedDriveSupportArticleVisible()
        }

        step("When I return to Wire, the group creation settings are still open and Shared Drive remains on") {
            pages.commonAppPage.apply {
                closeWebPage()
                assertWireAppIsInForeground()
            }
            pages.createNewGroupPage.apply {
                assertCreateNewGroupSettingsPageVisible()
                assertSharedDriveIsOnOnCreationSettingsPage()
            }
        }

        step("And I create GroupSharedDriveOn with Shared Drive enabled") {
            pages.createNewGroupPage.tapCreateGroupButtonOnGroupSettingsPage()
            pages.conversationViewPage.assertGroupConversationInForeground("GroupSharedDriveOn")
        }

        step("Then GroupSharedDriveOn conversation details show Shared Drive") {
            pages.conversationViewPage.clickOnGroupConversationDetails("GroupSharedDriveOn")
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                assertGroupNameVisible("GroupSharedDriveOn")
                assertSharedDriveButtonVisible()
                tapCloseButtonOnGroupConversationDetailsPage()
            }
        }

        step("And I return from GroupSharedDriveOn to the conversation list") {
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("GroupSharedDriveOn")
                tapBackButtonToCloseConversationViewPage()
            }
            pages.conversationListPage.assertConversationListVisible()
        }

        step("When I start creating a new group without adding participants") {
            pages.conversationListPage.tapStartNewConversationButton()
            pages.searchPage.tapCreateNewGroupButton()
            pages.searchPage.tapContinueButtonOnAddParticipantsPage()
        }

        step("And I name the group GroupSharedDriveOff and open its settings") {
            pages.createNewGroupPage.apply {
                assertCreateNewGroupDetailsPageVisible()
                enterNewGroupName("GroupSharedDriveOff")
                tapContinueButtonOnGroupDetailsPage()
                assertCreateNewGroupSettingsPageVisible()
            }
        }

        step("Then Shared Drive is off again for the new group creation flow") {
            pages.createNewGroupPage.assertSharedDriveIsOffOnCreationSettingsPage()
        }

        step("When I create GroupSharedDriveOff with the default Shared Drive setting") {
            pages.createNewGroupPage.tapCreateGroupButtonOnGroupSettingsPage()
            pages.conversationViewPage.assertGroupConversationInForeground("GroupSharedDriveOff")
        }

        step("Then GroupSharedDriveOff conversation details show Media and do not show Shared Drive") {
            pages.conversationViewPage.clickOnGroupConversationDetails("GroupSharedDriveOff")
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                assertGroupNameVisible("GroupSharedDriveOff")
                assertConversationMediaButtonVisible()
                assertSharedDriveButtonNotVisible()
                tapCloseButtonOnGroupConversationDetailsPage()
            }
        }

        step("And I return from GroupSharedDriveOff to the conversation list") {
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("GroupSharedDriveOff")
                tapBackButtonToCloseConversationViewPage()
            }
            pages.conversationListPage.assertConversationListVisible()
        }

        step("When I start creating a new channel without adding participants") {
            pages.conversationListPage.apply {
                tapStartNewConversationButton()
                tapCreateNewChannelButtonOnNewConversationScreen()
            }
            pages.searchPage.tapContinueButtonOnAddParticipantsPage()
        }

        step("And I name the channel ChannelSharedDriveOn and open its settings") {
            pages.createNewGroupPage.apply {
                assertCreateNewChannelDetailsPageVisible()
                enterNewChannelName("ChannelSharedDriveOn")
                tapContinueButtonOnChannelDetailsPage()
                assertCreateNewChannelSettingsPageVisible()
            }
        }

        step("Then Shared Drive is off by default for ChannelSharedDriveOn") {
            pages.createNewGroupPage.assertSharedDriveIsOffOnCreationSettingsPage()
        }

        step("And I enable Shared Drive for ChannelSharedDriveOn") {
            pages.createNewGroupPage.apply {
                tapSharedDriveToggleOnCreationSettingsPage()
                assertSharedDriveIsOnOnCreationSettingsPage()
            }
        }

        step("When I open Learn more from the Shared Drive explanation for ChannelSharedDriveOn") {
            pages.createNewGroupPage.tapLearnMoreLinkInSharedDriveExplanation()
        }

        step("Then the browser shows the Shared Drive support article for the channel creation flow") {
            pages.chromePage.assertSharedDriveSupportArticleVisible()
        }

        step("When I return to Wire, the channel creation settings are still open and Shared Drive remains on") {
            pages.commonAppPage.apply {
                closeWebPage()
                assertWireAppIsInForeground()
            }
            pages.createNewGroupPage.apply {
                assertCreateNewChannelSettingsPageVisible()
                assertSharedDriveIsOnOnCreationSettingsPage()
            }
        }

        step("And I create ChannelSharedDriveOn with Shared Drive enabled") {
            pages.createNewGroupPage.tapCreateChannelButtonOnChannelSettingsPage()
            pages.conversationViewPage.assertChannelConversationInForeground("ChannelSharedDriveOn")
        }

        step("Then ChannelSharedDriveOn conversation details show Shared Drive") {
            pages.conversationViewPage.clickOnChannelConversationDetails("ChannelSharedDriveOn")
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                assertChannelNameVisible("ChannelSharedDriveOn")
                assertSharedDriveButtonVisible()
                tapCloseButtonOnChannelConversationDetailsPage()
            }
        }

        step("And I return from ChannelSharedDriveOn to the conversation list") {
            pages.conversationViewPage.apply {
                assertChannelConversationInForeground("ChannelSharedDriveOn")
                tapBackButtonToCloseConversationViewPage()
            }
            pages.conversationListPage.assertConversationListVisible()
        }

        step("When I start creating a new channel without adding participants") {
            pages.conversationListPage.apply {
                tapStartNewConversationButton()
                tapCreateNewChannelButtonOnNewConversationScreen()
            }
            pages.searchPage.tapContinueButtonOnAddParticipantsPage()
        }

        step("And I name the channel ChannelSharedDriveOff and open its settings") {
            pages.createNewGroupPage.apply {
                assertCreateNewChannelDetailsPageVisible()
                enterNewChannelName("ChannelSharedDriveOff")
                tapContinueButtonOnChannelDetailsPage()
                assertCreateNewChannelSettingsPageVisible()
            }
        }

        step("Then Shared Drive is off again for the new channel creation flow") {
            pages.createNewGroupPage.assertSharedDriveIsOffOnCreationSettingsPage()
        }

        step("When I create ChannelSharedDriveOff with the default Shared Drive setting") {
            pages.createNewGroupPage.tapCreateChannelButtonOnChannelSettingsPage()
            pages.conversationViewPage.assertChannelConversationInForeground("ChannelSharedDriveOff")
        }

        step("Then ChannelSharedDriveOff conversation details show Media and do not show Shared Drive") {
            pages.conversationViewPage.clickOnChannelConversationDetails("ChannelSharedDriveOff")
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                assertChannelNameVisible("ChannelSharedDriveOff")
                assertConversationMediaButtonVisible()
                assertSharedDriveButtonNotVisible()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-12224")
    @Category("regression", "RC", "wireDrive", "wireDriveNavigation")
    @Test
    fun givenTeamMemberHasNoSharedDriveEnabledConversations_whenOpeningAndReopeningDrive_thenEmptyStateIsDisplayed() {
        givenTeamOwnerAndMemberArePreparedForSharedDrive("EmptyDrive")

        step("And User Member1 is me and has no Shared Drive-enabled conversations") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginAsMember1ThroughStagingDeepLink()

        step("When I open Drive from the main navigation menu") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                clickConversationsMenuEntry()
                tapDriveButtonOnMenuEntry()
            }
        }

        step("Then Drive shows the empty state for no files or folders") {
            pages.wireDrivePage.assertEmptyDriveVisible()
        }

        step("When I open Learn more from the empty Drive screen") {
            pages.wireDrivePage.tapLearnMoreLinkOnEmptyDrive()
        }

        step("Then the browser shows the Access all files shared across conversations (Drive) support article") {
            pages.chromePage.assertGlobalDriveSupportArticleVisible()
        }

        step("And I close the support article and return to the empty Drive screen") {
            pages.commonAppPage.apply {
                closeWebPage()
                assertWireAppIsInForeground()
            }
            pages.wireDrivePage.assertEmptyDriveVisible()
        }

        step("When I return to Conversations and reopen Drive") {
            pages.wireDrivePage.tapMainNavigationButtonOnDrivePage()
            pages.conversationListPage.apply {
                clickConversationsButtonOnMenuEntry()
                assertConversationListVisible()
                clickConversationsMenuEntry()
                tapDriveButtonOnMenuEntry()
            }
        }

        step("Then Drive still shows the empty state and remains usable") {
            pages.wireDrivePage.assertEmptyDriveVisible()
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-12225")
    @Category("regression", "RC", "wireDrive", "wireDriveNavigation")
    @Test
    fun givenGroupAndChannelHaveSharedDrive_whenMemberOpensDetailsRepeatedly_thenSharedDriveAppearsWithoutMedia() {
        givenTeamOwnerAndMemberArePreparedForSharedDrive("SharedDriveDetails")

        step("And channel creation is enabled for team SharedDriveDetails") {
            backendSetupHelper.userEnablesChannelFeatureForTeam(
                "user1Name",
                "SharedDriveDetails",
                backendClient
            )
        }

        step("And MLS devices are registered for TeamOwner and Member1") {
            testServiceHelper.apply {
                addDevice("user1Name", null, "Device1")
                addDevice("user2Name", null, "Device1")
            }
        }

        step("And TeamOwner creates SharedDriveDetailsGroup with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "SharedDriveDetailsGroup",
                deviceName = "Device1",
                cellsEnabled = true
            )
        }

        step("And TeamOwner creates SharedDriveDetailsChannel with Shared Drive enabled") {
            backendSetupHelper.userHasChannelConversationInTeam(
                chatOwnerNameAlias = "user1Name",
                chatName = "SharedDriveDetailsChannel",
                teamName = "SharedDriveDetails",
                cellsEnabled = true
            )
        }

        step("And TeamOwner adds Member1 to SharedDriveDetailsChannel") {
            backendSetupHelper.userXAddedContactsToGroupChat(
                "user1Name",
                "user2Name",
                "SharedDriveDetailsChannel"
            )
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginAsMember1ThroughStagingDeepLink()

        step("When I open group conversation SharedDriveDetailsGroup") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("SharedDriveDetailsGroup")
                clickGroupConversation("SharedDriveDetailsGroup")
            }
            pages.conversationViewPage.assertGroupConversationInForeground("SharedDriveDetailsGroup")
        }

        step("Then all three openings of SharedDriveDetailsGroup details show the correct Shared Drive state") {
            repeat(3) {
                pages.conversationViewPage.clickOnGroupConversationDetails("SharedDriveDetailsGroup")
                pages.groupConversationDetailsPage.apply {
                    assertSharedDriveButtonAppearsWithoutMedia()
                    assertGroupDetailsPageVisible()
                    assertGroupNameVisible("SharedDriveDetailsGroup")
                    assertSharedDriveIsPermanentlyOn()
                    assertSelfDeletingMessagesUnavailableForSharedDrive()
                    tapCloseButtonOnGroupConversationDetailsPage()
                }
                pages.conversationViewPage.assertGroupConversationInForeground("SharedDriveDetailsGroup")
            }
        }

        step("When I return to the conversation list and open channel SharedDriveDetailsChannel") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
            pages.conversationListPage.apply {
                assertChannelConversationVisible("SharedDriveDetailsChannel")
                clickChannelConversation("SharedDriveDetailsChannel")
            }
            pages.conversationViewPage.assertChannelConversationInForeground("SharedDriveDetailsChannel")
        }

        step("Then all three openings of SharedDriveDetailsChannel details show the correct Shared Drive state") {
            repeat(3) {
                pages.conversationViewPage.clickOnChannelConversationDetails("SharedDriveDetailsChannel")
                pages.groupConversationDetailsPage.apply {
                    assertSharedDriveButtonAppearsWithoutMedia()
                    assertGroupDetailsPageVisible()
                    assertChannelNameVisible("SharedDriveDetailsChannel")
                    assertSharedDriveIsPermanentlyOn()
                    assertSelfDeletingMessagesUnavailableForSharedDrive()
                    tapCloseButtonOnChannelConversationDetailsPage()
                }
                pages.conversationViewPage.assertChannelConversationInForeground("SharedDriveDetailsChannel")
            }
        }
    }

    // Shared backend setup for Wire Drive tests: prepares TeamOwner, Member1 and enables Shared Drive for their team.
    private fun givenTeamOwnerAndMemberArePreparedForSharedDrive(teamName: String) {
        step("Given There is a team owner TeamOwner with team $teamName") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                teamName,
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", teamName, backendClient)
        }

        step("And TeamOwner adds Member1 to team $teamName with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                teamName,
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And Shared Drive is enabled for team $teamName") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
            }
        }
    }

    // Shared Member1 login: selects staging, signs in and handles the post-login prompts.
    private fun loginAsMember1ThroughStagingDeepLink() {
        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("And I enter Member1's valid email and password to sign in") {
            pages.loginPage.apply {
                enterTeamMemberLoggingEmail(member1.email.orEmpty())
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamMemberLoggingPassword(member1.password.orEmpty())
                clickLoginButton()
            }
        }

        step("And I wait until Member1 is fully logged in and decline the share data alert") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }
    }
}
