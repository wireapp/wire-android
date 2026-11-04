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
import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.team.TeamRoles
import backendUtils.team.enableCellsFeatureViaBackdoorTeam
import backendUtils.team.getTeam
import com.wire.android.testSupport.R
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import createPdfInDeviceDownloadsFolder
import deleteDownloadedFilesContaining
import deleteDeviceDownload
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.UiWaitUtils
import user.utils.ClientUser
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@Suppress("LargeClass")
class WireDrivePermissionsTests : BaseUiTest() {
    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser
    private lateinit var sharedPdfUri: Uri
    private val deviceDownloads = mutableListOf<Uri>()
    private val documentScreenshots = mutableListOf<Bitmap>()
    override val deletePersonalUsersAfterTest = true

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @After
    fun tearDown() {
        documentScreenshots.forEach { it.recycle() }
        deviceDownloads.forEach(::deleteDeviceDownload)
        deleteDownloadedFilesContaining("PermissionImport-")
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-12226")
    @Category("regression", "RC", "wireDrive", "wireDrivePermissions")
    @Test
    fun givenOwningTeamMemberAndOutsideParticipants_whenOpeningSharedDrive_thenMemberSeesEditorActionsAndOutsideParticipantsSeeViewerAccess() {
        step("Given There is a team owner TeamOwner with team DrivePermissions") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "DrivePermissions",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And TeamOwner configures MLS for DrivePermissions") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "DrivePermissions", backendClient)
        }

        step("And TeamOwner adds Member1 to DrivePermissions with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "DrivePermissions",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And There is a team owner GuestTeamOwner with team GuestDrivePermissions") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user3Name",
                "GuestDrivePermissions",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And GuestTeamOwner configures MLS for GuestDrivePermissions") {
            backendSetupHelper.userConfiguresMLSForTeam("user3Name", "GuestDrivePermissions", backendClient)
        }

        step("And GuestTeamOwner adds GuestMember to GuestDrivePermissions with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user3Name",
                "user5Name",
                "GuestDrivePermissions",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }
        val guestMember = clientUserManager.findUserByNameOrNameAlias("user5Name")

        step("And Drive is enabled and unlocked for both teams") {
            val guestTeamOwner = clientUserManager.findUserByNameOrNameAlias("user3Name")
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(guestTeamOwner))
            }
        }

        step("And There is a personal Guest with a unique username") {
            clientUserManager.createPersonalUsersByAliases(listOf("user4Name"), backendClient)
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user4Name")
            }
        }
        val guest = clientUserManager.findUserByNameOrNameAlias("user4Name")

        step("And TeamOwner is connected to Guest and GuestMember") {
            backendSetupHelper.userIsConnectedTo("user1Name", "user4Name")
            backendSetupHelper.userIsConnectedTo("user1Name", "user5Name")
        }

        step("And MLS devices are registered for all participants") {
            val participantAliases = listOf("user1Name", "user2Name", "user3Name", "user4Name", "user5Name")
            testServiceHelper.apply {
                participantAliases.forEach { alias -> addDevice(alias, null, "Device1") }
            }
        }

        step("And I sign in as Guest and complete the login prompts") {
            clientUserManager.setSelfUser(guest)
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(guest)
        }

        step("And I add GuestMember's account and complete the login prompts") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                tapNewAccountButton()
            }
            clientUserManager.setSelfUser(guestMember)
            loginToStagingAs(guestMember)
        }

        step("And I add Member1's account and complete the login prompts") {
            pages.conversationListPage.apply {
                clickUserProfileButton()
            }
            pages.selfUserProfilePage.apply {
                tapNewAccountButton()
            }
            clientUserManager.setSelfUser(member1)
            loginToStagingAs(member1)
        }

        step("And TeamOwner creates SharedPermissions Conversation with Member1, Guest and GuestMember and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name,user4Name,user5Name",
                conversationName = "SharedPermissions",
                deviceName = "Device1",
                cellsEnabled = true
            )
        }

        step("And GuestTeamOwner creates OtherFiles Conversation with GuestMember and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user3Name",
                participantAliases = "user5Name",
                conversationName = "OtherFiles",
                deviceName = "Device1",
                cellsEnabled = true
            )
        }

        val suffix = UUID.randomUUID().toString().take(8)
        val sharedPdf = "SharedPermissions-$suffix.pdf"
        val sharedDocument = "SharedPermissions-$suffix.docx"
        val editorPdf = "EditorPermissions-$suffix.pdf"
        step("And TeamOwner sends a PDF and an editable document to SharedPermissions") {
            sharedPdfUri = createPdfInDeviceDownloadsFolder(sharedPdf, "Shared Drive permissions PDF")
            deviceDownloads += sharedPdfUri
            testServiceHelper.contactSendsLocalPdfConversation(
                context,
                sharedPdf,
                sharedPdfUri,
                "user1Name",
                "Device1",
                "SharedPermissions"
            )
            val documentUri = Uri.parse("android.resource://${context.packageName}/${R.raw.drive_permissions_document}")
            testServiceHelper.contactSendsLocalFileConversation(
                context,
                sharedDocument,
                documentUri,
                "user1Name",
                "Device1",
                "SharedPermissions",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            )
        }

        step("And GuestTeamOwner sends the same PDF to OtherFiles as an Editor comparison") {
            testServiceHelper.contactSendsLocalPdfConversation(
                context,
                editorPdf,
                sharedPdfUri,
                "user3Name",
                "Device1",
                "OtherFiles"
            )
        }

        step("When Member1 opens SharedPermissions, the conversation explains their Editor access") {
            pages.conversationListPage.apply {
                clickGroupConversation("SharedPermissions")
            }
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("SharedPermissions")
                assertSystemMessageVisible(
                    "Shared Drive is on. You have editor access. People outside your team only have viewer access."
                )
            }
        }

        step("And Member1 opens Shared Drive from the conversation details") {
            pages.conversationViewPage.clickOnGroupConversationDetails("SharedPermissions")
            pages.groupConversationDetailsPage.apply {
                tapSharedDriveButton()
            }
        }

        step("Then Member1 sees the Shared Drive create button and both files, with Editor actions in the PDF's three-dot menu") {
            pages.wireDrivePage.apply {
                assertSharedDriveVisibleWithoutViewerBanner()
                assertFileVisibleOnce(sharedPdf)
                assertFileVisibleOnce(sharedDocument)
                tapFileMenu(sharedPdf)
                assertEditorPdfActionsVisible()
                navigateBack()
            }
        }

        step("And Member1 sees Open, Edit file and See version history in the document's three-dot menu in Shared Drive") {
            pages.wireDrivePage.apply {
                tapFileMenu(sharedDocument)
                assertEditorDocumentActionsVisible()
                navigateBack()
            }
        }

        step("When I switch to Guest's account") {
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.tapBackButton()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
            pages.conversationListPage.apply {
                clickUserProfileButton()
            }
            pages.selfUserProfilePage.apply {
                tapOtherAccountByName(guest.name.orEmpty())
            }
            clientUserManager.setSelfUser(guest)
            pages.conversationListPage.assertConversationListVisible()
        }

        step("Then SharedPermissions explains Guest’s Viewer access in the conversation") {
            pages.conversationListPage.apply {
                clickGroupConversation("SharedPermissions")
            }
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("SharedPermissions")
                assertSystemMessageVisible("Shared Drive is on. You can view, but you can’t upload, edit, or manage files.")
                clickOnGroupConversationDetails("SharedPermissions")
            }
            pages.groupConversationDetailsPage.apply {
                tapSharedDriveButton()
            }
        }

        step("And Guest sees the Viewer banner and both files in Shared Drive") {
            pages.wireDrivePage.apply {
                assertViewerAccessBannerVisible("Viewer access: you can’t upload or edit files.")
                assertFileVisibleOnce(sharedPdf)
                assertFileVisibleOnce(sharedDocument)
            }
        }

        step("When Guest taps the PDF's three-dot menu in Shared Drive") {
            pages.wireDrivePage.tapFileMenu(sharedPdf)
        }

        step(
            "Then Guest's PDF three-dot menu in Shared Drive allows Open but hides Make available offline, " +
                "Add or Remove Tags, Share via Link, Move to folder, Rename and Delete"
        ) {
            pages.wireDrivePage.assertViewerPdfMenuActionsInSharedDrive()
        }

        step("When Guest closes the PDF's three-dot menu and taps the document's three-dot menu in Shared Drive") {
            pages.wireDrivePage.apply {
                navigateBack()
                tapFileMenu(sharedDocument)
            }
        }

        step(
            "Then Guest's document three-dot menu in Shared Drive allows Open but hides Edit file, See version history, " +
                "Add or Remove Tags, Share via Link, Move to folder, Rename and Delete"
        ) {
            pages.wireDrivePage.assertViewerDocumentMenuActionsInSharedDrive()
        }

        step("And Guest closes the document's three-dot menu") {
            pages.wireDrivePage.navigateBack()
        }

        step("When I switch to GuestMember's account") {
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.tapBackButton()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
            pages.conversationListPage.apply {
                clickUserProfileButton()
            }
            pages.selfUserProfilePage.apply {
                tapOtherAccountByName(guestMember.name.orEmpty())
            }
            clientUserManager.setSelfUser(guestMember)
            pages.conversationListPage.assertConversationListVisible()
        }

        step("Then SharedPermissions explains GuestMember’s Viewer access in the conversation") {
            pages.conversationListPage.apply {
                clickGroupConversation("SharedPermissions")
            }
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("SharedPermissions")
                assertSystemMessageVisible("Shared Drive is on. You can view, but you can’t upload, edit, or manage files.")
                clickOnGroupConversationDetails("SharedPermissions")
            }
            pages.groupConversationDetailsPage.apply {
                tapSharedDriveButton()
            }
        }

        step("And GuestMember sees the Viewer banner, both files and no New button in Shared Drive") {
            pages.wireDrivePage.apply {
                assertViewerAccessBannerVisible("Viewer access: you can’t upload or edit files.")
                assertNewButtonAbsent()
                assertFileVisibleOnce(sharedPdf)
                assertFileVisibleOnce(sharedDocument)
            }
        }

        step("When GuestMember taps the PDF's three-dot menu in Shared Drive") {
            pages.wireDrivePage.tapFileMenu(sharedPdf)
        }

        step(
            "Then GuestMember's PDF three-dot menu in Shared Drive allows Open but hides Make available offline, " +
                "Add or Remove Tags, Share via Link, Move to folder, Rename and Delete"
        ) {
            pages.wireDrivePage.assertViewerPdfMenuActionsInSharedDrive()
        }

        step("When GuestMember closes the PDF's three-dot menu and taps the document's three-dot menu in Shared Drive") {
            pages.wireDrivePage.apply {
                navigateBack()
                tapFileMenu(sharedDocument)
            }
        }

        step(
            "Then GuestMember's document three-dot menu in Shared Drive allows Open but hides Edit file, See version history, " +
                "Add or Remove Tags, Share via Link, Move to folder, Rename and Delete"
        ) {
            pages.wireDrivePage.assertViewerDocumentMenuActionsInSharedDrive()
        }

        step("And GuestMember closes the document's three-dot menu") {
            pages.wireDrivePage.navigateBack()
        }

        step("When GuestMember returns to Conversations and opens global Drive") {
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.tapBackButton()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                tapDriveButtonOnMenuEntry()
            }
        }

        step("Then global Drive labels the other team's PDF with Viewer access but not GuestMember's own team's PDF") {
            pages.wireDrivePage.apply {
                assertDrivePageVisible()
                assertFileVisibleOnce(sharedPdf)
                assertFileVisibleOnce(editorPdf)
                assertViewerAccessLabelOnlyOnViewerPdf(sharedPdf, editorPdf)
            }
        }

        step("And the other team's PDF three-dot menu in global Drive shows Viewer access with link sharing and offline access disabled") {
            pages.wireDrivePage.apply {
                tapFileMenu(sharedPdf)
                assertViewerFileAccessInDrive()
                navigateBack()
            }
        }

        step(
            "And GuestMember's own team's PDF three-dot menu in global Drive has link sharing and offline access available for an Editor"
        ) {
            pages.wireDrivePage.apply {
                tapFileMenu(editorPdf)
                assertEditorFileAccessInDrive()
                navigateBack()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-12227")
    @Category("regression", "RC", "wireDrive", "wireDrivePermissions")
    @Test
    fun givenGuestHasViewerAccessToSharedDrive_whenTheyDismissBannerAndOpenSharedDriveAgain_thenTheyCannotCreateOrUploadFilesOrOpenRecycleBin() {
        step("Given TeamOwner creates team ViewerRestrictions") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "ViewerRestrictions",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And TeamOwner configures MLS for ViewerRestrictions") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "ViewerRestrictions", backendClient)
        }

        step("And Shared Drive is enabled and unlocked for ViewerRestrictions") {
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
            }
        }

        step("And Guest is a personal user with a unique username outside ViewerRestrictions") {
            clientUserManager.createPersonalUsersByAliases(listOf("user4Name"), backendClient)
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user4Name")
            }
        }
        val guest = clientUserManager.findUserByNameOrNameAlias("user4Name")

        step("And TeamOwner connects to Guest") {
            backendSetupHelper.userIsConnectedTo("user1Name", "user4Name")
        }

        step("And MLS devices are registered for TeamOwner and Guest") {
            val participantAliases = listOf("user1Name", "user4Name")
            testServiceHelper.apply {
                participantAliases.forEach { alias -> addDevice(alias, null, "Device1") }
            }
        }

        step("And I sign in as Guest and complete the login prompts") {
            clientUserManager.setSelfUser(guest)
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(guest)
        }

        step("And TeamOwner creates ViewerRestrictions Conversation with Guest and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user4Name",
                conversationName = "ViewerRestrictions",
                deviceName = "Device1",
                cellsEnabled = true
            )
        }

        val sharedPdf = "ViewerRestrictions-${UUID.randomUUID().toString().take(8)}.pdf"
        step("And TeamOwner sends a PDF to ViewerRestrictions Conversation") {
            val pdfUri = createPdfInDeviceDownloadsFolder(sharedPdf, "Shared Drive Viewer restrictions PDF")
            deviceDownloads += pdfUri
            testServiceHelper.contactSendsLocalPdfConversation(
                context,
                sharedPdf,
                pdfUri,
                "user1Name",
                "Device1",
                "ViewerRestrictions"
            )
        }

        step("When Guest opens Shared Drive from ViewerRestrictions Conversation details") {
            pages.conversationListPage.clickGroupConversation("ViewerRestrictions")
            pages.conversationViewPage.clickOnGroupConversationDetails("ViewerRestrictions")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }

        step("Then Guest sees the Viewer access banner and the PDF, with no Shared Drive options button") {
            pages.wireDrivePage.apply {
                assertViewerAccessBannerVisible("Viewer access: you can’t upload or edit files.")
                assertFileVisibleOnce(sharedPdf)
                assertSharedDriveOptionsButtonAbsent()
            }
        }

        step("When Guest dismisses the Viewer banner, the banner and New button are absent") {
            pages.wireDrivePage.apply {
                dismissViewerAccessBanner()
                assertViewerAccessBannerNoLongerVisible()
                assertNewButtonAbsent()
            }
        }

        step("Then Guest reopens Shared Drive, sees the PDF, and the banner and options button remain absent") {
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.tapSharedDriveButton()
            pages.wireDrivePage.apply {
                assertFileVisibleOnce(sharedPdf)
                assertViewerAccessBannerNoLongerVisible()
                assertSharedDriveOptionsButtonAbsent()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-12228")
    @Category("regression", "RC", "wireDrive", "wireDrivePermissions")
    @Test
    fun givenGuestMemberFromAnotherTeamHasViewerAccessToSharedDrive_whenInspectingFileAndFolderMenus_thenRestrictedActionsCannotBePerformed() {
        step("Given There is a team owner TeamOwner with team DrivePermissions") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "DrivePermissions",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And TeamOwner configures MLS for DrivePermissions") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "DrivePermissions", backendClient)
        }

        step("And There is a team owner GuestTeamOwner with team GuestDrivePermissions") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user3Name",
                "GuestDrivePermissions",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And GuestTeamOwner configures MLS for GuestDrivePermissions") {
            backendSetupHelper.userConfiguresMLSForTeam("user3Name", "GuestDrivePermissions", backendClient)
        }

        step("And GuestTeamOwner adds GuestMember to GuestDrivePermissions with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user3Name",
                "user5Name",
                "GuestDrivePermissions",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }
        val guestMember = clientUserManager.findUserByNameOrNameAlias("user5Name")

        step("And Drive is enabled and unlocked for both teams") {
            val guestTeamOwner = clientUserManager.findUserByNameOrNameAlias("user3Name")
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(guestTeamOwner))
            }
        }

        step("And TeamOwner connects to GuestMember") {
            backendSetupHelper.userIsConnectedTo("user1Name", "user5Name")
        }

        step("And MLS devices are registered for TeamOwner, GuestTeamOwner and GuestMember") {
            val participantAliases = listOf("user1Name", "user3Name", "user5Name")
            testServiceHelper.apply {
                participantAliases.forEach { alias -> addDevice(alias, null, "Device1") }
            }
        }

        step("And I sign in as GuestMember and complete the login prompts") {
            clientUserManager.setSelfUser(guestMember)
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(guestMember)
        }

        step("And I add TeamOwner's account and complete the login prompts") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.tapNewAccountButton()
            clientUserManager.setSelfUser(teamOwner)
            loginToStagingAs(teamOwner)
        }

        step("And TeamOwner creates RestrictedActions Conversation with GuestMember and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user5Name",
                conversationName = "RestrictedActions",
                deviceName = "Device1",
                cellsEnabled = true
            )
        }

        val sharedPdf = "RestrictedActions-${UUID.randomUUID().toString().take(8)}.pdf"
        step("And TeamOwner sends a PDF to RestrictedActions Conversation") {
            val pdfUri = createPdfInDeviceDownloadsFolder(sharedPdf, "Viewer restricted actions PDF")
            deviceDownloads += pdfUri
            testServiceHelper.contactSendsLocalPdfConversation(
                context,
                sharedPdf,
                pdfUri,
                "user1Name",
                "Device1",
                "RestrictedActions"
            )
        }

        step("And TeamOwner opens Shared Drive, creates ViewerFolder and sees it beside the PDF") {
            pages.conversationListPage.clickGroupConversation("RestrictedActions")
            pages.conversationViewPage.clickOnGroupConversationDetails("RestrictedActions")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
            pages.wireDrivePage.apply {
                assertSharedDriveVisibleWithoutViewerBanner()
                assertFileVisibleOnce(sharedPdf)
                createFolder("ViewerFolder")
                assertFolderVisibleOnce("ViewerFolder")
            }
        }

        step("When I switch to GuestMember's account and open RestrictedActions Shared Drive") {
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.tapBackButton()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.tapOtherAccountByName(guestMember.name.orEmpty())
            clientUserManager.setSelfUser(guestMember)
            pages.conversationListPage.clickGroupConversation("RestrictedActions")
            pages.conversationViewPage.clickOnGroupConversationDetails("RestrictedActions")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }

        step("Then GuestMember sees the Viewer banner, PDF and folder in Shared Drive") {
            pages.wireDrivePage.apply {
                assertViewerAccessBannerVisible("Viewer access: you can’t upload or edit files.")
                assertFileVisibleOnce(sharedPdf)
                assertFolderVisibleOnce("ViewerFolder")
            }
        }

        step("When GuestMember taps the PDF's three-dot menu in Shared Drive") {
            pages.wireDrivePage.tapFileMenu(sharedPdf)
        }

        step(
            "Then GuestMember's PDF three-dot menu in Shared Drive allows Open but hides Make available offline, " +
                "Add or Remove Tags, Share via Link, Move to folder, Rename and Delete"
        ) {
            pages.wireDrivePage.assertViewerPdfMenuActionsInSharedDrive()
        }

        step("When GuestMember closes the PDF's three-dot menu and taps ViewerFolder's three-dot menu in Shared Drive") {
            pages.wireDrivePage.apply {
                navigateBack()
                tapFolderMenu("ViewerFolder")
            }
        }

        step(
            "Then GuestMember's ViewerFolder three-dot menu in Shared Drive allows Open but hides Add or Remove Tags, " +
                "Share via Link, Move to folder, Rename and Delete"
        ) {
            pages.wireDrivePage.assertViewerFolderMenuActionsInSharedDrive()
        }

        step("And GuestMember opens ViewerFolder, sees that it is empty, and returns to Shared Drive") {
            pages.wireDrivePage.apply {
                tapOpenOptionOnActionsMenu()
                assertFolderHeadingVisible("ViewerFolder")
                assertEmptyFolderVisible()
                navigateBack()
            }
        }

        step("When GuestMember selects Open from the PDF's three-dot menu in Shared Drive, the correct PDF preview appears") {
            pages.wireDrivePage.apply {
                assertFileVisibleOnce(sharedPdf)
                tapFileMenu(sharedPdf)
                tapOpenOptionOnActionsMenu()
            }
            pages.pdfViewerPage.assertPdfPreviewVisible(sharedPdf)
            pages.pdfViewerPage.navigateBack()
        }

        step("When GuestMember taps the PDF's three-dot menu in Shared Drive") {
            pages.wireDrivePage.tapFileMenu(sharedPdf)
        }

        step("Then the Share action is absent from GuestMember's PDF three-dot menu in Shared Drive") {
            pages.wireDrivePage.assertViewerShareActionAbsentInSharedDrive()
        }

        step("And GuestMember closes the PDF's three-dot menu") {
            pages.wireDrivePage.navigateBack()
        }

        step("When GuestMember returns to Conversations, opens global Drive and taps the same PDF's three-dot menu") {
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.tapBackButton()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                tapDriveButtonOnMenuEntry()
            }
            pages.wireDrivePage.apply {
                assertDrivePageVisible()
                assertFileVisibleOnce(sharedPdf)
                tapFileMenu(sharedPdf)
            }
        }

        step("Then Share, Share via Link and Make available offline are disabled in the PDF's three-dot menu in global Drive") {
            pages.wireDrivePage.assertViewerShareLinkAndOfflineActionsDisabled()
        }

        step("And Open is enabled in the PDF's three-dot menu in global Drive") {
            pages.wireDrivePage.assertOpenActionEnabledOnThreeDotMenu()
        }

        val restrictedActions = listOf("Share", "Share via Link", "Make available offline")
        restrictedActions.forEach { action ->
            step("When GuestMember taps '$action' in the PDF's three-dot menu in global Drive") {
                pages.wireDrivePage.tapDisabledViewerAction(action)
            }

            step("Then the same PDF's three-dot menu remains visible and '$action' remains disabled in global Drive") {
                pages.wireDrivePage.apply {
                    assertPdfThreeDotMenuVisible(sharedPdf)
                    assertViewerFileActionDisabled(action)
                }
            }
        }

        step("When GuestMember selects Open from the PDF's three-dot menu in global Drive") {
            pages.wireDrivePage.tapOpenOptionOnActionsMenu()
        }

        step("Then GuestMember sees the correct PDF preview") {
            pages.pdfViewerPage.assertPdfPreviewVisible(sharedPdf)
        }

        step("When GuestMember returns to global Drive, searches for the PDF and taps its three-dot menu in the search results") {
            pages.pdfViewerPage.navigateBack()
            pages.wireDrivePage.apply {
                assertFileVisibleOnce(sharedPdf)
                searchInGlobalDrive(sharedPdf.substringBeforeLast('.'))
                assertFileVisibleOnce(sharedPdf, timeout = UiWaitUtils.SHORT_TIMEOUT)
                tapFileMenu(sharedPdf)
            }
        }

        step("Then Share, Share via Link and Make available offline are disabled in the PDF's three-dot menu in search results") {
            pages.wireDrivePage.assertViewerShareLinkAndOfflineActionsDisabled()
        }

        step("And Open is enabled in the PDF's three-dot menu in search results") {
            pages.wireDrivePage.assertOpenActionEnabledOnThreeDotMenu()
        }

        restrictedActions.forEach { action ->
            step("When GuestMember taps '$action' in the PDF's three-dot menu in search results") {
                pages.wireDrivePage.tapDisabledViewerAction(action)
            }

            step("Then the same PDF's three-dot menu remains visible and '$action' remains disabled in search results") {
                pages.wireDrivePage.apply {
                    assertPdfThreeDotMenuVisible(sharedPdf)
                    assertViewerFileActionDisabled(action)
                }
            }
        }

        step("When GuestMember selects Open from the PDF's three-dot menu in search results") {
            pages.wireDrivePage.tapOpenOptionOnActionsMenu()
        }

        step("Then GuestMember sees the correct PDF preview") {
            pages.pdfViewerPage.assertPdfPreviewVisible(sharedPdf)
        }

        step("When GuestMember returns from the PDF preview to the search results") {
            pages.pdfViewerPage.navigateBack()
        }

        step("Then the same PDF appears once in the search results") {
            pages.wireDrivePage.assertFileVisibleOnce(sharedPdf)
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-12229")
    @Category("regression", "RC", "wireDrive", "wireDrivePermissions")
    @Test
    fun givenGuestHasViewerAccessAndMember1HasEditorAccess_whenTheyOpenAnHtmlFile_thenOnlyMember1CanOpenItInChrome() {
        step("Given Chrome can open HTML files on the device") {
            pages.chromePage.assertHtmlFileHandlerAvailable()
        }

        step("And TeamOwner creates team ExternalOpening") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "ExternalOpening",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And TeamOwner configures MLS for ExternalOpening") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "ExternalOpening", backendClient)
        }

        step("And TeamOwner adds Member1 to ExternalOpening with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "ExternalOpening",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And Shared Drive is enabled and unlocked for ExternalOpening") {
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
            }
        }

        step("And Guest is a personal user with a unique username outside ExternalOpening") {
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

        step("And I sign in as Member1 and complete the login prompts") {
            clientUserManager.setSelfUser(member1)
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(member1)
        }

        step("And I add Guest's account and complete the login prompts") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.tapNewAccountButton()
            clientUserManager.setSelfUser(guest)
            loginToStagingAs(guest)
        }

        step("And TeamOwner creates ExternalOpening Conversation with Member1 and Guest and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name,user4Name",
                conversationName = "ExternalOpening",
                deviceName = "Device1",
                cellsEnabled = true
            )
        }

        val sharedHtml = "ExternalOpening-${UUID.randomUUID().toString().take(8)}.html"
        step("And TeamOwner sends an HTML file to ExternalOpening Conversation") {
            val htmlUri = Uri.parse("android.resource://${context.packageName}/${R.raw.drive_permissions_external_open}")
            testServiceHelper.contactSendsLocalFileConversation(
                context,
                sharedHtml,
                htmlUri,
                "user1Name",
                "Device1",
                "ExternalOpening",
                "text/html"
            )
        }

        step("When Guest opens Shared Drive from ExternalOpening Conversation details") {
            pages.conversationListPage.clickGroupConversation("ExternalOpening")
            pages.conversationViewPage.clickOnGroupConversationDetails("ExternalOpening")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }

        step("Then Guest sees the Viewer access banner and the HTML file") {
            pages.wireDrivePage.apply {
                assertViewerAccessBannerVisible("Viewer access: you can’t upload or edit files.")
                assertFileVisibleOnce(sharedHtml)
            }
        }

        step("When Guest opens the HTML file in Shared Drive, Wire shows Can't open this file. File type isn't supported.") {
            UiWaitUtils.assertToastDisplayed(
                text = "Can't open this file. File type isn't supported.",
                trigger = {
                    pages.wireDrivePage.openFile(sharedHtml)
                    pages.wireDrivePage.tapOpenOnFileReadyMessageIfShown(sharedHtml)
                },
                timeout = UiWaitUtils.VERY_LONG_TIMEOUT
            )
        }

        step("And Guest remains in Wire with the same HTML file visible in Shared Drive") {
            pages.commonAppPage.assertWireAppIsInForeground()
            pages.wireDrivePage.assertFileVisibleOnce(sharedHtml)
        }

        step("When I return to Conversations and switch from Guest to Member1's account") {
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.tapBackButton()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.tapOtherAccountByName(member1.name.orEmpty())
            clientUserManager.setSelfUser(member1)
        }

        step("And Member1 opens Shared Drive from ExternalOpening Conversation details") {
            pages.conversationListPage.clickGroupConversation("ExternalOpening")
            pages.conversationViewPage.clickOnGroupConversationDetails("ExternalOpening")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }

        step("Then Member1 sees Shared Drive's New button, no Viewer access banner and the same HTML file") {
            pages.wireDrivePage.apply {
                assertSharedDriveVisibleWithoutViewerBanner()
                assertFileVisibleOnce(sharedHtml)
            }
        }

        step("When Member1 taps the same HTML file in Shared Drive") {
            pages.wireDrivePage.openFile(sharedHtml)
        }

        step("And Member1 taps Open on the file-ready message if it appears") {
            pages.wireDrivePage.tapOpenOnFileReadyMessageIfShown(sharedHtml)
        }

        step("And Member1 selects Chrome from the Open with dialog and dismisses Chrome prompts if shown") {
            pages.chromePage.apply {
                selectChromeFromOpenWithDialog()
                dismissFirstRunIfVisible()
                dismissNotificationsPromptIfVisible()
            }
        }

        step("Then Chrome opens the same HTML file and shows Shared Drive external opening fixture") {
            pages.chromePage.assertHtmlFileOpenInChrome(sharedHtml, "Shared Drive external opening fixture")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-12230")
    @Category("regression", "RC", "wireDrive", "wireDrivePermissions")
    @Test
    fun givenMemberCanUploadToOneSharedDriveButOnlyViewAnother_whenSharingAnImageFromAndroidFiles_thenOnlyConversationAllowingUploadsCanBeSelectedAndReceiveImage() {
        step("Given TeamOwner creates team ImportEditor") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "ImportEditor",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }
        step("And TeamOwner configures MLS for ImportEditor") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "ImportEditor", backendClient)
        }
        step("And TeamOwner adds Member1 to ImportEditor with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "ImportEditor",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }
        step("And OtherTeamOwner creates team ImportViewer and configures MLS") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user3Name",
                "ImportViewer",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam("user3Name", "ImportViewer", backendClient)
        }
        step("And Shared Drive is enabled and unlocked for both teams") {
            val otherTeamOwner = clientUserManager.findUserByNameOrNameAlias("user3Name")
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(otherTeamOwner))
            }
        }
        step("And Member1 connects to OtherTeamOwner") {
            backendSetupHelper.userIsConnectedTo("user2Name", "user3Name")
        }
        step("And MLS devices are registered for TeamOwner, Member1 and OtherTeamOwner") {
            listOf("user1Name", "user2Name", "user3Name").forEach { alias ->
                testServiceHelper.addDevice(alias, null, "Device1")
            }
        }
        step("And I sign in as Member1 and complete the login prompts") {
            clientUserManager.setSelfUser(member1)
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(member1)
        }
        step("And OtherTeamOwner creates SendFilesHere Conversation with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user3Name",
                participantAliases = "user2Name",
                conversationName = "SendFilesHere",
                deviceName = "Device1",
                cellsEnabled = true
            )
        }
        step("And TeamOwner creates OtherFiles Conversation with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "OtherFiles",
                deviceName = "Device1",
                cellsEnabled = true
            )
        }
        step("And Member1 sees both Conversations") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("SendFilesHere")
                assertGroupConversationVisible("OtherFiles")
            }
        }
        step("And Member1 opens SendFilesHere Conversation and sees the Viewer access banner") {
            pages.conversationListPage.clickGroupConversation("SendFilesHere")
            pages.conversationViewPage.assertConversationBannerVisible("Viewer access: you can’t upload or edit files.")
        }
        step("And Member1 returns to Conversations before sharing from Files") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }
        val imageName = "PermissionImport-${UUID.randomUUID().toString().take(8)}"
        step("And an identifiable image is available in device Downloads") {
            createQrImageInDeviceDownloadsFolder(imageName)
        }
        step("When Member1 shares the image from Android Files into Wire") {
            pages.documentsUIPage.apply {
                openDownloadsInFilesApp()
                shareFileFromFilesApp("$imageName.png")
                selectWireFromAndroidShareSheet()
            }
        }
        step("Then SendFilesHere is disabled and unselected in the import Conversation chooser") {
            pages.importMediaPage.assertViewerConversationDisabled("SendFilesHere")
        }
        step("And Send is disabled before an Editor Conversation is selected") {
            pages.importMediaPage.assertSendButtonDisabled()
        }
        step("When Member1 taps the disabled SendFilesHere Conversation") {
            pages.importMediaPage.tapConversation("SendFilesHere")
        }
        step("Then SendFilesHere stays unselected and Send stays disabled") {
            pages.importMediaPage.apply {
                assertViewerConversationDisabled("SendFilesHere")
                assertSendButtonDisabled()
            }
        }
        step("When Member1 selects OtherFiles Conversation") {
            pages.importMediaPage.tapConversation("OtherFiles")
        }
        step("Then OtherFiles is selected and Send is enabled") {
            pages.importMediaPage.apply {
                assertEditorConversationSelected("OtherFiles")
                assertSendButtonEnabled()
            }
        }
        step("When Member1 taps Send in the import Conversation chooser") {
            pages.importMediaPage.tapSendButton()
        }
        step("Then the correct QR image is attached to OtherFiles Conversation's message composer and Send is enabled") {
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("OtherFiles")
                assertQrImageAttachedToComposer(imageName)
                assertSendButtonEnabledOnMessageComposer()
            }
        }
        step("When Member1 sends the attached image from OtherFiles Conversation's message composer") {
            pages.conversationViewPage.clickSendButton()
        }
        step("Then OtherFiles Conversation shows the correct QR image in its message history") {
            pages.conversationViewPage.assertQrImageVisibleInMessageHistory(imageName)
        }
        step("And TeamOwner's Device1 receives an image attachment in OtherFiles Conversation") {
            testServiceHelper.assertImageReceivedInGroupConversation("user1Name", "Device1", "OtherFiles")
        }
        step("And OtherFiles Shared Drive is available with Editor access") {
            pages.conversationViewPage.clickOnGroupConversationDetails("OtherFiles")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
            pages.wireDrivePage.assertSharedDriveVisibleWithoutViewerBanner()
        }
        step("When Member1 returns to Conversations and opens SendFilesHere Conversation") {
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.tapBackButton()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
            pages.conversationListPage.clickGroupConversation("SendFilesHere")
        }
        step("Then SendFilesHere Conversation has no imported QR image in its messages or composer") {
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("SendFilesHere")
                assertQrImageAbsentFromConversation(imageName)
            }
        }
        step("And SendFilesHere Shared Drive has Viewer access and contains no files") {
            pages.conversationViewPage.clickOnGroupConversationDetails("SendFilesHere")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
            pages.wireDrivePage.apply {
                assertViewerAccessBannerVisible("Viewer access: you can’t upload or edit files.")
                assertEmptyFolderVisible()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-12231")
    @Category("regression", "RC", "wireDrive", "wireDrivePermissions")
    @Test
    fun givenMemberCanEditAndGuestCanViewDocument_whenMemberSavesChanges_thenGuestReadsUpdatedPdfWithoutEditOrShareControls() {
        step("Given TeamOwner creates team DocumentPermissions") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "DocumentPermissions",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And TeamOwner configures MLS for DocumentPermissions") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "DocumentPermissions", backendClient)
        }

        step("And TeamOwner adds Member1 to DocumentPermissions with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "DocumentPermissions",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And Shared Drive is enabled and unlocked for DocumentPermissions") {
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
            }
        }

        step("And Guest is a personal user with a unique username outside DocumentPermissions") {
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

        step("And TeamOwner creates DocumentPermissions Conversation with Member1 and Guest and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name,user4Name",
                conversationName = "DocumentPermissions",
                deviceName = "Device1",
                cellsEnabled = true
            )
        }

        val suffix = UUID.randomUUID().toString().take(8)
        val sharedDocument = "DocumentPermissions-$suffix.docx"
        val referenceDocument = "ExpectedDocumentPermissions-$suffix.docx"
        val updatedText = "Drive permission update confirmed"
        step("And TeamOwner sends the original DOCX and a reference DOCX containing the expected updated text") {
            val documents = listOf(
                sharedDocument to R.raw.drive_permissions_document,
                referenceDocument to R.raw.drive_permissions_updated_document
            )
            documents.forEach { (fileName, resource) ->
                testServiceHelper.contactSendsLocalFileConversation(
                    context,
                    fileName,
                    Uri.parse("android.resource://${context.packageName}/$resource"),
                    "user1Name",
                    "Device1",
                    "DocumentPermissions",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                )
            }
        }
        step("When Member1 opens Shared Drive from DocumentPermissions Conversation details") {
            pages.conversationListPage.clickGroupConversation("DocumentPermissions")
            pages.conversationViewPage.clickOnGroupConversationDetails("DocumentPermissions")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }
        step("Then Member1 sees Editor access and both DOCX files") {
            pages.wireDrivePage.apply {
                assertSharedDriveVisibleWithoutViewerBanner()
                assertFileVisibleOnce(sharedDocument)
                assertFileVisibleOnce(referenceDocument)
            }
        }
        lateinit var expectedEditorScreenshot: Bitmap
        step("When Member1 opens the reference DOCX and captures its document area in the Online Editor") {
            pages.wireDrivePage.openFile(referenceDocument)
            expectedEditorScreenshot = pages.documentEditorPage.captureDocumentScreenshot()
            documentScreenshots += expectedEditorScreenshot
        }
        step("And Member1 closes the reference DOCX and selects Edit file from the original DOCX's three-dot menu") {
            pages.documentEditorPage.closeEditor()
            pages.wireDrivePage.apply {
                tapFileMenu(sharedDocument)
                tapEditFileOptionOnDocumentMenu()
            }
        }
        step("When Member1 replaces the document text and saves the change in the document editor") {
            pages.documentEditorPage.apply {
                replaceDocumentText(updatedText)
                saveDocument()
            }
        }
        step("And Member1 closes the document editor and reopens the same document from Shared Drive") {
            pages.documentEditorPage.closeEditor()
            pages.wireDrivePage.openFile(sharedDocument)
        }
        step("Then the saved document's screenshot matches the reference DOCX's expected updated content") {
            pages.documentEditorPage.assertDocumentMatchesScreenshot(expectedEditorScreenshot)
        }
        step("When Member1 closes the document editor and switches to Guest's account") {
            pages.documentEditorPage.closeEditor()
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.tapBackButton()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.tapOtherAccountByName(guest.name.orEmpty())
            clientUserManager.setSelfUser(guest)
        }
        step("And Guest opens Shared Drive from DocumentPermissions Conversation details") {
            pages.conversationListPage.clickGroupConversation("DocumentPermissions")
            pages.conversationViewPage.clickOnGroupConversationDetails("DocumentPermissions")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }
        step("Then Guest sees the Viewer access banner and both DOCX files") {
            pages.wireDrivePage.apply {
                assertViewerAccessBannerVisible("Viewer access: you can’t upload or edit files.")
                assertFileVisibleOnce(sharedDocument, timeout = UiWaitUtils.SHORT_TIMEOUT)
                assertFileVisibleOnce(referenceDocument)
            }
        }
        lateinit var expectedViewerScreenshot: Bitmap
        step("When Guest opens the reference DOCX's PDF rendition and captures its rendered page") {
            pages.wireDrivePage.openFile(referenceDocument)
            pages.pdfViewerPage.assertPdfPreviewVisible(referenceDocument)
            expectedViewerScreenshot = pages.pdfViewerPage.capturePdfPageScreenshot()
            documentScreenshots += expectedViewerScreenshot
        }
        step("And Guest returns from the reference PDF rendition to Shared Drive") {
            pages.pdfViewerPage.navigateBack()
        }
        step("When Guest opens the DOCX three-dot menu in Shared Drive") {
            pages.wireDrivePage.tapFileMenu(sharedDocument)
        }
        step(
            "Then Guest's document three-dot menu allows Open but hides Edit file, See version history, " +
                "tags, link, move, rename and delete"
        ) {
            pages.wireDrivePage.assertViewerDocumentMenuActionsInSharedDrive()
        }
        step("When Guest selects Open from the document three-dot menu") {
            pages.wireDrivePage.tapOpenOptionOnActionsMenu()
        }
        step("Then Guest sees the same document's PDF rendition matching the reference's expected updated content") {
            pages.pdfViewerPage.apply {
                assertPdfPreviewVisible(sharedDocument)
                assertPdfMatchesScreenshot(expectedViewerScreenshot)
            }
        }
        step("And the Viewer PDF preview has no editing or external sharing controls") {
            pages.pdfViewerPage.assertViewerPdfHasNoEditOrShareControls()
            pages.commonAppPage.assertWireAppIsInForeground()
        }
        step("When Guest returns to Shared Drive and opens the document three-dot menu") {
            pages.pdfViewerPage.navigateBack()
            pages.wireDrivePage.tapFileMenu(sharedDocument)
        }
        step("Then Share remains absent from the Viewer document three-dot menu after viewing its PDF rendition") {
            pages.wireDrivePage.assertViewerShareActionAbsentInSharedDrive()
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-12232")
    @Category("regression", "RC", "wireDrive", "wireDrivePermissions")
    @Test
    fun givenSharedDriveContainsFilesBeforeGuestJoins_whenGuestIsAddedThenRemoved_thenGuestGainsViewerAccessAndLosesOnlineFileAccess() {
        step("Given TeamOwner creates team MembershipPermissions") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "MembershipPermissions",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And TeamOwner configures MLS for MembershipPermissions") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "MembershipPermissions", backendClient)
        }

        step("And TeamOwner adds Member1 to MembershipPermissions with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "MembershipPermissions",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And Shared Drive is enabled and unlocked for MembershipPermissions") {
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
            }
        }

        step("And Guest is a personal user with a unique username outside MembershipPermissions") {
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

        step("And TeamOwner creates MembershipPermissions Conversation with Member1 and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "MembershipPermissions",
                deviceName = "Device1",
                cellsEnabled = true
            )
        }

        val suffix = UUID.randomUUID().toString().take(8)
        val existingPdf = "BeforeJoining-$suffix.pdf"
        val unopenedPdf = "UnopenedBeforeRemoval-$suffix.pdf"
        step("And TeamOwner sends two PDF files before Guest joins MembershipPermissions Conversation") {
            listOf(existingPdf, unopenedPdf).forEach { fileName ->
                val pdfUri = createPdfInDeviceDownloadsFolder(fileName, "Shared before Guest joined")
                deviceDownloads.add(pdfUri)
                testServiceHelper.contactSendsLocalPdfConversation(
                    context,
                    fileName,
                    pdfUri,
                    "user1Name",
                    "Device1",
                    "MembershipPermissions"
                )
            }
        }
        step("When TeamOwner adds Guest to MembershipPermissions Conversation through Device1") {
            testServiceHelper.userXAddedContactsToGroupChat("user1Name", "user4Name", "MembershipPermissions", "Device1")
        }
        step("And Guest opens Shared Drive from MembershipPermissions Conversation details") {
            pages.conversationListPage.clickGroupConversation("MembershipPermissions")
            pages.conversationViewPage.clickOnGroupConversationDetails("MembershipPermissions")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }
        step("Then Guest has Viewer access to both PDFs shared before joining") {
            pages.wireDrivePage.apply {
                assertViewerAccessBannerVisible("Viewer access: you can’t upload or edit files.")
                assertFileVisibleOnce(existingPdf, timeout = UiWaitUtils.VERY_LONG_TIMEOUT)
                assertFileVisibleOnce(unopenedPdf)
            }
        }
        step("When Guest opens the first PDF shared before joining") {
            pages.wireDrivePage.openFile(existingPdf)
        }
        step("Then Guest sees the correct PDF filename and a rendered page") {
            pages.pdfViewerPage.assertPdfPreviewVisible(existingPdf)
        }
        step("When Guest returns to Shared Drive and TeamOwner removes Guest through Device1") {
            pages.pdfViewerPage.navigateBack()
            testServiceHelper.userRemovesUserFromGroupConversation("user1Name", "user4Name", "MembershipPermissions", "Device1")
        }
        step("And the device can still reach the backend after Guest is removed") {
            runBlocking { backendClient.getTeam(teamOwner) }
        }
        step("When Guest tries to open the previously unopened PDF, Wire reports that the item is no longer available") {
            UiWaitUtils.assertToastDisplayed(
                text = "This item is no longer available. It may have been deleted or moved.",
                trigger = { pages.wireDrivePage.openFile(unopenedPdf) }
            )
        }
        step("Then Guest remains in Shared Drive with the Viewer banner and the unavailable PDF is absent") {
            pages.wireDrivePage.apply {
                assertViewerAccessBannerVisible("Viewer access: you can’t upload or edit files.")
                assertFileNotVisible(unopenedPdf)
            }
        }
        step("When Guest leaves Shared Drive and returns to MembershipPermissions Conversation") {
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.tapBackButton()
        }
        step("Then MembershipPermissions Conversation shows Guest was removed") {
            pages.conversationViewPage.assertSystemMessageVisible("removed you")
        }
        step("When Guest taps MembershipPermissions Conversation's header after removal") {
            pages.conversationViewPage.tapGroupConversationHeader("MembershipPermissions")
        }
        step("Then Conversation details are disabled, preventing Guest from reopening Shared Drive through them") {
            pages.conversationViewPage.assertGroupConversationDetailsDisabled("MembershipPermissions")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-12343")
    @Category("regression", "RC", "wireDrive", "wireDrivePermissions")
    @Test
    fun givenPersonalGuestAndAnotherTeamsMemberHaveViewerAccess_whenSelectingSharedDriveFilesAndFolders_thenOnlyOwningTeamMemberCanBulkSelect() {
        step("Given There is a team owner TeamOwner with team DrivePermissions") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "DrivePermissions",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And TeamOwner configures MLS for DrivePermissions") {
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "DrivePermissions", backendClient)
        }

        step("And TeamOwner adds Member1 to DrivePermissions with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "DrivePermissions",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And There is a team owner GuestTeamOwner with team GuestDrivePermissions") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user3Name",
                "GuestDrivePermissions",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And GuestTeamOwner configures MLS for GuestDrivePermissions") {
            backendSetupHelper.userConfiguresMLSForTeam("user3Name", "GuestDrivePermissions", backendClient)
        }

        step("And GuestTeamOwner adds GuestMember to GuestDrivePermissions with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user3Name",
                "user5Name",
                "GuestDrivePermissions",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }
        val guestMember = clientUserManager.findUserByNameOrNameAlias("user5Name")

        step("And Drive is enabled and unlocked for both teams") {
            val guestTeamOwner = clientUserManager.findUserByNameOrNameAlias("user3Name")
            runBlocking {
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(teamOwner))
                backendClient.enableCellsFeatureViaBackdoorTeam(backendClient.getTeam(guestTeamOwner))
            }
        }

        step("And There is a personal Guest with a unique username") {
            clientUserManager.createPersonalUsersByAliases(listOf("user4Name"), backendClient)
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user4Name")
            }
        }
        val guest = clientUserManager.findUserByNameOrNameAlias("user4Name")

        step("And TeamOwner is connected to Guest and GuestMember") {
            backendSetupHelper.userIsConnectedTo("user1Name", "user4Name")
            backendSetupHelper.userIsConnectedTo("user1Name", "user5Name")
        }

        step("And MLS devices are registered for all participants") {
            val participantAliases = listOf("user1Name", "user2Name", "user3Name", "user4Name", "user5Name")
            testServiceHelper.apply {
                participantAliases.forEach { alias -> addDevice(alias, null, "Device1") }
            }
        }

        step("And I sign in as Guest and complete the login prompts") {
            clientUserManager.setSelfUser(guest)
            pages.registrationPage.assertEmailWelcomePage()
            loginToStagingAs(guest)
        }

        step("And I add GuestMember's account and complete the login prompts") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.tapNewAccountButton()
            clientUserManager.setSelfUser(guestMember)
            loginToStagingAs(guestMember)
        }

        step("And I add Member1's account and complete the login prompts") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.tapNewAccountButton()
            clientUserManager.setSelfUser(member1)
            loginToStagingAs(member1)
        }

        step("And TeamOwner creates BulkPermissions Conversation with Member1, Guest and GuestMember and Shared Drive enabled") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name,user4Name,user5Name",
                conversationName = "BulkPermissions",
                deviceName = "Device1",
                cellsEnabled = true
            )
        }

        val suffix = UUID.randomUUID().toString().take(8)
        val sharedPdf = "BulkPermissions-$suffix.pdf"
        val childPdf = "BulkPermissionsChild-$suffix.pdf"
        step("And TeamOwner sends an identifiable PDF to BulkPermissions Conversation") {
            val pdfUri = createPdfInDeviceDownloadsFolder(sharedPdf, "Bulk selection permissions PDF")
            deviceDownloads += pdfUri
            testServiceHelper.contactSendsLocalPdfConversation(
                context,
                sharedPdf,
                pdfUri,
                "user1Name",
                "Device1",
                "BulkPermissions"
            )
        }

        step("And Member1 opens Shared Drive from BulkPermissions Conversation details and sees the PDF with Editor access") {
            pages.conversationListPage.clickGroupConversation("BulkPermissions")
            pages.conversationViewPage.clickOnGroupConversationDetails("BulkPermissions")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
            pages.wireDrivePage.apply {
                assertSharedDriveVisibleWithoutViewerBanner()
                assertFileVisibleOnce(sharedPdf)
            }
        }

        step("And Member1 creates BulkFolder in Shared Drive, opens it from the current list and verifies it is empty") {
            pages.wireDrivePage.apply {
                createFolder("BulkFolder")
                assertFolderVisibleOnce("BulkFolder")
                openFolder("BulkFolder")
                assertEmptyFolderVisible()
            }
        }

        step("And Member1 prepares a PDF and uploads it inside BulkFolder") {
            deviceDownloads += createPdfInDeviceDownloadsFolder(childPdf, "Bulk permissions folder PDF")
            pages.wireDrivePage.apply {
                tapNewButton()
                tapUploadFilesOption()
            }
            pages.documentsUIPage.selectFileInDocumentsUI(childPdf)
            pages.wireDrivePage.tapUploadButtonOnUploadConfirmationDialog()
        }

        step("And Member1 sees Upload complete, collapses the upload status panel and sees the child PDF inside BulkFolder") {
            pages.wireDrivePage.apply {
                assertUploadCompleteVisible()
                tapCollapseButtonOnUploadStatusPanel()
                assertFileVisibleOnce(childPdf)
            }
        }

        step("When Member1 returns to the Shared Drive main folder and opens the root PDF") {
            pages.wireDrivePage.apply {
                navigateBack()
                assertFileVisibleOnce(sharedPdf)
                assertFolderVisibleOnce("BulkFolder")
                openFile(sharedPdf)
            }
        }

        lateinit var expectedPdfScreenshot: Bitmap
        step("Then Member1 sees the correct PDF preview and captures its rendered page as the reference") {
            pages.pdfViewerPage.assertPdfPreviewVisible(sharedPdf)
            expectedPdfScreenshot = pages.pdfViewerPage.capturePdfPageScreenshot()
            documentScreenshots += expectedPdfScreenshot
        }

        step(
            "When Member1 returns to Shared Drive, long-presses the PDF, checks one selected file and selects BulkFolder"
        ) {
            pages.pdfViewerPage.navigateBack()
            pages.wireDrivePage.apply {
                longPressFileInSharedDrive(sharedPdf)
                assertBulkSelectionCount(1)
                tapFolderForBulkSelection("BulkFolder")
            }
        }

        step("Then Member1 sees two selected rows and enabled bulk Move and Delete actions in Shared Drive") {
            pages.wireDrivePage.apply {
                assertBulkSelectionCount(2)
                assertBulkMoveAndDeleteActionsEnabled()
            }
        }

        step("When Member1 cancels bulk selection and taps the Shared Drive options button") {
            pages.wireDrivePage.apply {
                tapCancelBulkSelectionButton()
                assertBulkSelectionControlsAbsent()
                tapSharedDriveOptionsButton()
            }
        }

        step("Then Member1 sees the Open Recycle Bin option") {
            pages.wireDrivePage.assertOpenRecycleBinOptionVisible()
        }

        step("When Member1 closes Shared Drive options and returns to the conversation list") {
            pages.wireDrivePage.apply {
                navigateBack()
                navigateBack()
            }
            pages.groupConversationDetailsPage.tapBackButton()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("And I switch from Member1 to personal Guest's account") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.tapOtherAccountByName(guest.name.orEmpty())
            clientUserManager.setSelfUser(guest)
        }

        step("And Guest opens Shared Drive from BulkPermissions Conversation details") {
            pages.conversationListPage.clickGroupConversation("BulkPermissions")
            pages.conversationViewPage.clickOnGroupConversationDetails("BulkPermissions")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }

        step("Then personal Guest sees the Viewer access banner, PDF and BulkFolder in Shared Drive") {
            pages.wireDrivePage.apply {
                assertViewerAccessBannerVisible("Viewer access: you can’t upload or edit files.")
                assertFileVisibleOnce(sharedPdf)
                assertFolderVisibleOnce("BulkFolder")
            }
        }

        step("When Guest long-presses the PDF and drags to BulkFolder in Shared Drive, bulk selection controls stay absent") {
            pages.wireDrivePage.apply {
                longPressAndDragBetweenSharedDriveRows(sharedPdf, "BulkFolder")
                assertBulkSelectionControlsAbsent()
            }
        }

        step(
            "When Guest long-presses BulkFolder and drags to the PDF in Shared Drive, " +
                "bulk selection controls and the Shared Drive options button stay absent"
        ) {
            pages.wireDrivePage.apply {
                longPressAndDragBetweenSharedDriveRows("BulkFolder", sharedPdf)
                assertBulkSelectionControlsAbsent()
                assertSharedDriveOptionsButtonAbsent()
            }
        }

        step("When Guest selects Open from the PDF's three-dot menu in Shared Drive") {
            pages.wireDrivePage.apply {
                tapFileMenu(sharedPdf)
                tapOpenOptionOnActionsMenu()
            }
        }

        step("Then Guest sees the correct PDF preview matching Member1's reference page") {
            pages.pdfViewerPage.apply {
                assertPdfPreviewVisible(sharedPdf)
                assertPdfMatchesScreenshot(expectedPdfScreenshot)
            }
        }

        step("When Guest closes the PDF preview and returns to the conversation list") {
            pages.pdfViewerPage.navigateBack()
            pages.wireDrivePage.navigateBack()
            pages.groupConversationDetailsPage.tapBackButton()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("And I switch from Guest to GuestMember's account") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.tapOtherAccountByName(guestMember.name.orEmpty())
            clientUserManager.setSelfUser(guestMember)
        }

        step("And GuestMember opens Shared Drive from BulkPermissions Conversation details") {
            pages.conversationListPage.clickGroupConversation("BulkPermissions")
            pages.conversationViewPage.clickOnGroupConversationDetails("BulkPermissions")
            pages.groupConversationDetailsPage.tapSharedDriveButton()
        }

        step("Then GuestMember from another team sees the Viewer access banner, PDF and BulkFolder in Shared Drive") {
            pages.wireDrivePage.apply {
                assertViewerAccessBannerVisible("Viewer access: you can’t upload or edit files.")
                assertFileVisibleOnce(sharedPdf)
                assertFolderVisibleOnce("BulkFolder")
            }
        }

        step(
            "When GuestMember long-presses the PDF and drags to BulkFolder in Shared Drive, bulk selection controls stay absent"
        ) {
            pages.wireDrivePage.apply {
                longPressAndDragBetweenSharedDriveRows(sharedPdf, "BulkFolder")
                assertBulkSelectionControlsAbsent()
            }
        }

        step(
            "When GuestMember long-presses BulkFolder and drags to the PDF in Shared Drive, " +
                "bulk selection controls and the Shared Drive options button stay absent"
        ) {
            pages.wireDrivePage.apply {
                longPressAndDragBetweenSharedDriveRows("BulkFolder", sharedPdf)
                assertBulkSelectionControlsAbsent()
                assertSharedDriveOptionsButtonAbsent()
            }
        }

        step("When GuestMember selects Open from the PDF's three-dot menu in Shared Drive") {
            pages.wireDrivePage.apply {
                tapFileMenu(sharedPdf)
                tapOpenOptionOnActionsMenu()
            }
        }

        step("Then GuestMember sees the correct PDF preview matching Member1's reference page") {
            pages.pdfViewerPage.apply {
                assertPdfPreviewVisible(sharedPdf)
                assertPdfMatchesScreenshot(expectedPdfScreenshot)
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
