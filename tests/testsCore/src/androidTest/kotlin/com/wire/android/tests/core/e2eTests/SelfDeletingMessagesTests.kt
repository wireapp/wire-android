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
import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import createOneKbFileInDeviceDownloadsFolder
import deleteDownloadedFilesContaining
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.KeyboardUtils.closeKeyboardIfOpened
import uiautomatorutils.UiWaitUtils
import user.utils.ClientUser
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@Suppress("LargeClass")
class SelfDeletingMessagesTests : BaseUiTest() {
    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @After
    fun tearDown() {
        deleteDownloadedFilesContaining("File")
        deleteDownloadedFilesContaining("Image")
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4517", "TC-4523")
    @Category("regression", "RC", "selfDeletingMessages", "smoke")
    @Test
    fun givenGroupConversation_whenISendTenSecondSelfDeletingMessage_thenMessageExpires() {
        step("Given There is a team owner TeamOwner with team Deleting") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Deleting",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Deleting with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Deleting",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation SelfDeleting with Member1 in team Deleting") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "SelfDeleting",
                "user2Name",
                "Deleting"
            )
        }

        step("And User TeamOwner adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I see and open group conversation SelfDeleting") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("SelfDeleting")
                clickGroupConversation("SelfDeleting")
            }
        }

        step("And I send message Let us test self deleting messages! and see it in the conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Let us test self deleting messages!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Let us test self deleting messages!")
            }
        }

        step("When I open the self-deleting message timer options") {
            pages.conversationViewPage.apply {
                tapMessageInInputField()
                tapSelfDeleteTimerButton()
            }
        }

        // TC-4523 - I want to see all available timer options for self deleting messages.
        step("Then I see OFF selected and all available self-deleting message timer options") {
            pages.conversationViewPage.apply {
                assertSelfDeleteOptionSelected("OFF")
                assertSelfDeleteOptionVisible("10 seconds")
                assertSelfDeleteOptionVisible("5 minutes")
                assertSelfDeleteOptionVisible("1 hour")
                assertSelfDeleteOptionVisible("1 day")
                assertSelfDeleteOptionVisible("7 days")
                assertSelfDeleteOptionVisible("4 weeks")
            }
        }

        step("When I select the 10 seconds timer") {
            pages.conversationViewPage.tapSelfDeleteOption("10 seconds")
        }

        step("Then I see the self-deleting message label in the text input field") {
            pages.conversationViewPage.assertSelfDeletingMessageLabelVisible()
        }

        step("And I send self-deleting message This will delete after 10 seconds. and see it in the conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("This will delete after 10 seconds.")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("This will delete after 10 seconds.")
            }
        }

        step("And I wait for 10 seconds") {
            UiWaitUtils.waitFor(10.seconds)
        }

        step("Then I do not see message This will delete after 10 seconds. in the conversation") {
            pages.conversationViewPage.assertMessageNotVisible("This will delete after 10 seconds.")
        }

        val selfDeletingMessageHint =
            "After one participant has seen your message and the timer has expired on their side, this note disappears."

        step("And I see the self-deleting message hint in the conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation(selfDeletingMessageHint)
        }

        step("When User TeamOwner reads the recent message from group conversation SelfDeleting via Device1") {
            testServiceHelper.userReadsRecentMessageFromGroupConversation("user1Name", "SelfDeleting", "Device1")
        }

        step("Then I do not see the self-deleting message hint in the conversation") {
            pages.conversationViewPage.assertMessageNotVisible(selfDeletingMessageHint)
        }

        step("When User TeamOwner sends message I do not see the message anymore. via Device1 to SelfDeleting") {
            testServiceHelper.userSendMessageToConversation(
                "user1Name",
                "I do not see the message anymore.",
                "Device1",
                "SelfDeleting"
            )
        }

        step("Then I see message I do not see the message anymore. in the conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation(
                "I do not see the message anymore."
            )
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4524")
    @Category("regression", "selfDeletingMessages")
    @Test
    fun givenGroupConversation_whenISendOneMinuteSelfDeletingMessage_thenMessageExpires() {
        step("Given There is a team owner TeamOwner with team Deleting") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Deleting",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Deleting with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Deleting",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation SelfDeleting with Member1 in team Deleting") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "SelfDeleting",
                "user2Name",
                "Deleting"
            )
        }

        step("And User TeamOwner adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I see and open group conversation SelfDeleting") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("SelfDeleting")
                clickGroupConversation("SelfDeleting")
            }
        }

        step("And I send message Let us test self deleting messages! and see it in the conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Let us test self deleting messages!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Let us test self deleting messages!")
            }
        }

        step("When I open the self-deleting message timer options") {
            pages.conversationViewPage.apply {
                tapMessageInInputField()
                tapSelfDeleteTimerButton()
            }
        }

        step("Then I see OFF selected and the 1 minute timer option") {
            pages.conversationViewPage.apply {
                assertSelfDeleteOptionSelected("OFF")
                assertSelfDeleteOptionVisible("1 minute")
            }
        }

        step("When I select the 1 minute timer") {
            pages.conversationViewPage.tapSelfDeleteOption("1 minute")
        }

        step("And I send self-deleting message This will delete after 60 seconds. and see it in the conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("This will delete after 60 seconds.")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("This will delete after 60 seconds.")
            }
        }

        step("And I wait for 60 seconds") {
            UiWaitUtils.waitFor(60.seconds)
        }

        step("Then I do not see message This will delete after 60 seconds. in the conversation") {
            pages.conversationViewPage.assertMessageNotVisible("This will delete after 60 seconds.")
        }

        val selfDeletingMessageHint =
            "After one participant has seen your message and the timer has expired on their side, this note disappears."

        step("When User TeamOwner reads the recent message from group conversation SelfDeleting via Device1") {
            testServiceHelper.userReadsRecentMessageFromGroupConversation("user1Name", "SelfDeleting", "Device1")
        }

        step("Then I do not see the self-deleting message hint in the conversation") {
            pages.conversationViewPage.assertMessageNotVisible(selfDeletingMessageHint)
        }

        step("When User TeamOwner sends message I cannot see the message anymore. via Device1 to SelfDeleting") {
            testServiceHelper.userSendMessageToConversation(
                "user1Name",
                "I cannot see the message anymore.",
                "Device1",
                "SelfDeleting"
            )
        }

        step("Then I see message I cannot see the message anymore. in the conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation(
                "I cannot see the message anymore."
            )
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4518")
    @Category("regression", "RC", "selfDeletingMessages")
    @Test
    fun givenGroupConversation_whenIReceiveSelfDeletingMessage_thenMessageExpires() {
        step("Given There is a team owner TeamOwner with team Deleting") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Deleting",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Deleting with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Deleting",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation SelfDeleting with Member1 in team Deleting") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "SelfDeleting",
                "user2Name",
                "Deleting"
            )
        }

        step("And User TeamOwner adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I see and open group conversation SelfDeleting") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("SelfDeleting")
                clickGroupConversation("SelfDeleting")
            }
        }

        step("And I send message Let us test self deleting messages! and see it in the conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Let us test self deleting messages!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Let us test self deleting messages!")
            }
        }

        step("When User TeamOwner sends a 10-second self-deleting message via Device1 to SelfDeleting") {
            testServiceHelper.userSendEphemeralMessageToConversation(
                "user1Name",
                "This will delete after 10 seconds.",
                "Device1",
                "SelfDeleting",
                Duration.ofSeconds(10)
            )
        }

        step("Then I see message This will delete after 10 seconds. in the conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation(
                "This will delete after 10 seconds."
            )
        }

        step("When I wait for 10 seconds") {
            UiWaitUtils.waitFor(10.seconds)
        }

        step("Then I do not see message This will delete after 10 seconds. in the conversation") {
            pages.conversationViewPage.assertMessageNotVisible("This will delete after 10 seconds.")
        }

        step("When I send message I do not see the message anymore.") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("I do not see the message anymore.")
                clickSendButton()
            }
        }

        step("Then I see message I do not see the message anymore. in the conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation(
                "I do not see the message anymore."
            )
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4519")
    @Category("regression", "RC", "selfDeletingMessages")
    @Test
    fun givenOneOnOneConversation_whenISendTenSecondSelfDeletingMessage_thenMessageExpires() {
        step("Given There is a team owner TeamOwner with team Deleting") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Deleting",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Deleting with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Deleting",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has 1:1 conversation with Member1 in team Deleting") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Deleting"
            )
        }

        step("And User Member1 adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(teamOwner)
        }

        loginToStagingAs(teamOwner)

        step("And I see and open conversation Member1") {
            pages.conversationListPage.apply {
                assertConversationVisible(member1.name ?: "")
                tapConversationNameInConversationList(member1.name ?: "")
            }
        }

        step("And I send message Let us test self deleting messages! and see it in the conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Let us test self deleting messages!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Let us test self deleting messages!")
            }
        }

        step("When I open the self-deleting message timer options and select 10 seconds") {
            pages.conversationViewPage.apply {
                tapMessageInInputField()
                tapSelfDeleteTimerButton()
                tapSelfDeleteOption("10 seconds")
            }
        }

        step("And I send self-deleting message This will delete after 10 seconds.") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("This will delete after 10 seconds.")
                clickSendButton()
            }
        }

        step("And I hide the keyboard") {
            closeKeyboardIfOpened()
        }

        step("And I see message This will delete after 10 seconds. in the conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation(
                "This will delete after 10 seconds."
            )
        }

        step("And I wait for 10 seconds") {
            UiWaitUtils.waitFor(10.seconds)
        }

        step("Then I do not see message This will delete after 10 seconds. in the conversation") {
            pages.conversationViewPage.assertMessageNotVisible("This will delete after 10 seconds.")
        }

        val selfDeletingMessageHint =
            "After ${member1.name} has seen your message and the timer has expired on their side, this note disappears."

        step("And I see the self-deleting message hint for Member1 in the conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation(selfDeletingMessageHint)
        }

        step("When User Member1 reads the recent message from TeamOwner via Device1") {
            testServiceHelper.userReadsRecentMessageFromPersonalConversation("user2Name", "user1Name", "Device1")
        }

        step("Then I do not see the self-deleting message hint for Member1 in the conversation") {
            pages.conversationViewPage.assertMessageNotVisible(selfDeletingMessageHint)
        }

        step("When User Member1 sends message I do not see the message anymore. via Device1 to TeamOwner") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user2Name",
                "I do not see the message anymore.",
                "Device1",
                "user1Name"
            )
        }

        step("Then I see message I do not see the message anymore. in the conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation(
                "I do not see the message anymore."
            )
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4520")
    @Category("regression", "RC", "selfDeletingMessages")
    @Test
    fun givenOneOnOneConversation_whenIReceiveSelfDeletingMessage_thenMessageExpires() {
        step("Given There is a team owner TeamOwner with team Deleting") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Deleting",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Deleting with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Deleting",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has 1:1 conversation with Member1 in team Deleting") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Deleting"
            )
        }

        step("And User Member1 has device Device1 for sending the self-deleting message") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(teamOwner)
        }

        loginToStagingAs(teamOwner)

        step("And I see and open conversation Member1") {
            pages.conversationListPage.apply {
                assertConversationVisible(member1.name ?: "")
                tapConversationNameInConversationList(member1.name ?: "")
            }
        }

        step("And I send message Let us test self deleting messages! and see it in the conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Let us test self deleting messages!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Let us test self deleting messages!")
            }
        }

        step("When User Member1 sends a 10-second self-deleting message via Device1 to TeamOwner") {
            testServiceHelper.userSendEphemeralMessageToConversation(
                "user2Name",
                "This will delete after 10 seconds.",
                "Device1",
                "user1Name",
                Duration.ofSeconds(10)
            )
        }

        step("Then I see message This will delete after 10 seconds. in the conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation(
                "This will delete after 10 seconds."
            )
        }

        step("When I wait for 10 seconds") {
            UiWaitUtils.waitFor(10.seconds)
        }

        step("Then I do not see message This will delete after 10 seconds. in the conversation") {
            pages.conversationViewPage.assertMessageNotVisible("This will delete after 10 seconds.")
        }

        step("When I send message I do not see the message anymore.") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("I do not see the message anymore.")
                clickSendButton()
            }
        }

        step("Then I see message I do not see the message anymore. in the conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation(
                "I do not see the message anymore."
            )
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4525")
    @Category("regression", "RC", "selfDeletingMessages", "fileSharing")
    @Test
    fun givenGroupConversation_whenISendTenSecondSelfDeletingImage_thenImageExpires() {
        step("Given There is a team owner TeamOwner with team Deleting") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Deleting",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Deleting with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Deleting",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation SelfDeleting with Member1 in team Deleting") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "SelfDeleting",
                "user2Name",
                "Deleting"
            )
        }

        step("And User TeamOwner adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I see and open group conversation SelfDeleting") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("SelfDeleting")
                clickGroupConversation("SelfDeleting")
            }
        }

        step("And I send message Let us test self deleting assets! and see it in the conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Let us test self deleting assets!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Let us test self deleting assets!")
            }
        }

        step("When I open the self-deleting message timer options") {
            pages.conversationViewPage.apply {
                tapMessageInInputField()
                tapSelfDeleteTimerButton()
            }
        }

        step("Then I see OFF selected") {
            pages.conversationViewPage.assertSelfDeleteOptionSelected("OFF")
        }

        step("When I select the 10 seconds timer and open file sharing") {
            pages.conversationViewPage.apply {
                tapSelfDeleteOption("10 seconds")
                iTapFileSharingButton()
            }
        }

        step("Then I see File, Gallery, Camera, Video and Audio sharing options") {
            pages.conversationViewPage.apply {
                assertSharingOptionVisible("File")
                assertSharingOptionVisible("Gallery")
                assertSharingOptionVisible("Camera")
                assertSharingOptionVisible("Video")
                assertSharingOptionVisible("Audio")
            }
        }

        step("When I create an image containing an Image QR code and open Gallery") {
            createQrImageInDeviceDownloadsFolder("Image")
            pages.conversationViewPage.tapSharingOption("Gallery")
        }

        step("And I select the Image QR code in the photo picker") {
            pages.documentsUIPage.apply {
                selectMostRecentImageInPhotoPicker()
                tapAddOrDoneButtonIfVisible()
            }
        }

        step("And I see the image preview page and send the image") {
            pages.documentsUIPage.apply {
                assertImagePreviewPageVisible()
                iTapSendButtonOnPreviewImage()
            }
        }

        step("Then I see the Image QR code in the conversation view") {
            pages.conversationViewPage.iSeeSentQrCodeImageInCurrentConversation()
        }

        step("When I wait for 10 seconds") {
            UiWaitUtils.waitFor(10.seconds)
        }

        step("Then I do not see an image in the conversation view") {
            pages.conversationViewPage.assertImageNotVisible()
        }

        val selfDeletingMessageHint =
            "After one participant has seen your message and the timer has expired on their side, this note disappears."

        step("And I see the self-deleting message hint in the conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation(selfDeletingMessageHint)
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4527")
    @Category("regression", "RC", "selfDeletingMessages", "fileSharing")
    @Test
    fun givenGroupConversation_whenISendTenSecondSelfDeletingFile_thenFileExpires() {
        step("Given There is a team owner TeamOwner with team Deleting") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Deleting",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Deleting with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Deleting",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation SelfDeleting with Member1 in team Deleting") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "SelfDeleting",
                "user2Name",
                "Deleting"
            )
        }

        step("And User TeamOwner adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I see and open group conversation SelfDeleting") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("SelfDeleting")
                clickGroupConversation("SelfDeleting")
            }
        }

        step("And I send message Let us test self deleting assets! and see it in the conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Let us test self deleting assets!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Let us test self deleting assets!")
            }
        }

        step("When I open the self-deleting message timer options") {
            pages.conversationViewPage.apply {
                tapMessageInInputField()
                tapSelfDeleteTimerButton()
            }
        }

        step("Then I see OFF selected") {
            pages.conversationViewPage.assertSelfDeleteOptionSelected("OFF")
        }

        step("When I select the 10 seconds timer and open file sharing") {
            pages.conversationViewPage.apply {
                tapSelfDeleteOption("10 seconds")
                iTapFileSharingButton()
            }
        }

        step("And I create textfile.txt and open the File picker") {
            createOneKbFileInDeviceDownloadsFolder("textfile.txt")
            pages.conversationViewPage.tapSharingOption("File")
        }

        step("And I select textfile.txt in DocumentsUI") {
            pages.documentsUIPage.selectFileInDocumentsUI("textfile.txt")
        }

        step("And I see textfile.txt on the preview page and send it") {
            pages.documentsUIPage.apply {
                assertFilePreviewPageVisible("textfile.txt")
                iTapSendButtonOnPreviewImage()
            }
        }

        step("Then I see file textfile.txt in the conversation view") {
            pages.conversationViewPage.assertFileWithNameIsVisible("textfile.txt")
        }

        step("When I wait for 10 seconds") {
            UiWaitUtils.waitFor(10.seconds)
        }

        step("Then I do not see file textfile.txt in the conversation view") {
            pages.conversationViewPage.assertFileWithNameNotVisible("textfile.txt")
        }

        val selfDeletingMessageHint =
            "After one participant has seen your message and the timer has expired on their side, this note disappears."

        step("And I see the self-deleting message hint in the conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation(selfDeletingMessageHint)
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4526", "TC-4522")
    @Category("regression", "RC", "selfDeletingMessages", "fileSharing")
    @Test
    fun givenGroupConversation_whenIReceiveTenSecondSelfDeletingImage_thenImageExpires() {
        step("Given There is a team owner TeamOwner with team Deleting") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Deleting",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Deleting with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Deleting",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation SelfDeleting with Member1 in team Deleting") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "SelfDeleting",
                "user2Name",
                "Deleting"
            )
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I see and open group conversation SelfDeleting") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("SelfDeleting")
                clickGroupConversation("SelfDeleting")
            }
        }

        step("And I send message Let us test self deleting assets! and see it in the conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Let us test self deleting assets!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Let us test self deleting assets!")
            }
        }

        step("And I hide the keyboard") {
            closeKeyboardIfOpened()
        }

        // TC-4522 - Receive a self-deleting message when the group conversation has self-deleting messages enforced.
        step("When TeamOwner sends image testing.jpg with a 10-second timer to SelfDeleting") {
            testServiceHelper.contactSendsLocalImageConversation(
                context,
                "testing.jpg",
                "user1Name",
                null,
                "SelfDeleting",
                Duration.ofSeconds(10)
            )
        }

        step("Then I see an image in the conversation view") {
            pages.conversationViewPage.assertImageIsVisible()
        }

        step("When I wait for 10 seconds") {
            UiWaitUtils.waitFor(10.seconds)
        }

        step("Then I do not see an image in the conversation view") {
            pages.conversationViewPage.assertImageNotVisible()
        }

        step("And I still see message Let us test self deleting assets! in the conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation(
                "Let us test self deleting assets!"
            )
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4531")
    @Category("regression", "selfDeletingMessages", "fileSharing", "WPB-439")
    @Test
    fun givenSelfDeletingImage_whenIOpenItsContextMenu_thenOnlyAllowedOptionsAreDisplayed() {
        step("Given There is a team owner TeamOwner with team Deleting") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Deleting",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Deleting with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Deleting",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation SelfDeleting with Member1 in team Deleting") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "SelfDeleting",
                "user2Name",
                "Deleting"
            )
        }

        step("And User TeamOwner adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }

        step("And User Member1 is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(member1)
        }

        loginToStagingAs(member1)

        step("And I see and open group conversation SelfDeleting") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("SelfDeleting")
                clickGroupConversation("SelfDeleting")
            }
        }

        step("And I send message Let us test self deleting assets! and see it in the conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Let us test self deleting assets!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Let us test self deleting assets!")
            }
        }

        step("When I open the self-deleting message timer options") {
            pages.conversationViewPage.apply {
                tapMessageInInputField()
                tapSelfDeleteTimerButton()
            }
        }

        step("Then I see OFF selected") {
            pages.conversationViewPage.assertSelfDeleteOptionSelected("OFF")
        }

        step("When I select the 1 minute timer and open file sharing") {
            pages.conversationViewPage.apply {
                tapSelfDeleteOption("1 minute")
                iTapFileSharingButton()
            }
        }

        step("Then I see File, Gallery, Camera, Video and Audio sharing options") {
            pages.conversationViewPage.apply {
                assertSharingOptionVisible("File")
                assertSharingOptionVisible("Gallery")
                assertSharingOptionVisible("Camera")
                assertSharingOptionVisible("Video")
                assertSharingOptionVisible("Audio")
            }
        }

        step("When I create an image containing an Image QR code and open Gallery") {
            createQrImageInDeviceDownloadsFolder("Image")
            pages.conversationViewPage.tapSharingOption("Gallery")
        }

        step("And I select the Image QR code in the photo picker") {
            pages.documentsUIPage.apply {
                selectMostRecentImageInPhotoPicker()
                tapAddOrDoneButtonIfVisible()
            }
        }

        step("And I see the image preview page and send the image") {
            pages.documentsUIPage.apply {
                assertImagePreviewPageVisible()
                iTapSendButtonOnPreviewImage()
            }
        }

        step("Then I see the Image QR code in the conversation view") {
            pages.conversationViewPage.iSeeSentQrCodeImageInCurrentConversation()
        }

        step("When I tap the image, see its context menu button and open the menu") {
            pages.conversationViewPage.apply {
                tapImageMessage()
                assertImageContextMenuButtonVisible()
                tapImageContextMenuButton()
            }
        }

        step("Then I see only the allowed context menu options for a self-deleting image") {
            pages.conversationViewPage.assertSelfDeletingImageContextMenuOptionsVisible()
        }

        step("And I tap back twice") {
            device.pressBack()
            device.pressBack()
        }

        step("When I long tap the image") {
            pages.conversationViewPage.longPressImageMessage()
        }

        step("Then I see only the allowed context menu options for a self-deleting image") {
            pages.conversationViewPage.assertSelfDeletingImageContextMenuOptionsVisible()
        }

        step("And I tap back") {
            device.pressBack()
        }

        step("When I wait for 30 seconds") {
            UiWaitUtils.waitFor(30.seconds)
        }

        step("Then I do not see an image in the conversation view") {
            pages.conversationViewPage.assertImageNotVisible(timeoutSeconds = 35)
        }

        val selfDeletingMessageHint =
            "After one participant has seen your message and the timer has expired on their side, this note disappears."

        step("And I see the self-deleting message hint in the conversation") {
            pages.conversationViewPage.assertSentMessageIsVisibleInCurrentConversation(selfDeletingMessageHint)
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4529", "TC-4521")
    @Category("regression", "RC", "selfDeletingMessages")
    @Test
    fun givenGroupConversation_whenISetSelfDeletingTimer_thenExchangedMessagesExpire() {
        step("Given There is a team owner TeamOwner with team Deleting") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Deleting",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Deleting with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Deleting",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation SelfDeleting with Member1 in team Deleting") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "SelfDeleting",
                "user2Name",
                "Deleting"
            )
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            clientUserManager.setSelfUser(teamOwner)
        }

        loginToStagingAs(teamOwner)

        step("And I see and open group conversation SelfDeleting") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("SelfDeleting")
                clickGroupConversation("SelfDeleting")
            }
        }

        step("And I open group conversation details for SelfDeleting") {
            pages.conversationViewPage.clickOnGroupConversationDetails("SelfDeleting")
        }

        step("And I see Self-deleting messages is OFF") {
            pages.groupConversationDetailsPage.assertSelfDeletingMessagesState("OFF")
        }

        // TC-4521 - Send a self-deleting message when the group conversation has self-deleting messages enforced.
        step("When I enforce a 10-second self-deleting message timer for the group conversation") {
            pages.groupConversationDetailsPage.apply {
                tapSelfDeletingMessagesOption()
                tapSelfDeletingMessagesToggle()
                tapSelfDeletingMessagesTimer("10 seconds")
                tapApplyButton()
            }
        }

        step("And I close the group conversation details through the X icon") {
            UiWaitUtils.waitFor(UiWaitUtils.SHORT_WAIT)
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
        }

        step("Then I see system message You set self-deleting messages to 10 seconds for everyone") {
            pages.conversationViewPage.assertSystemMessageVisible(
                "set self-deleting messages to 10 seconds for everyone"
            )
        }

        step("When I send message This will delete after 10 seconds. and see it in the conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("This will delete after 10 seconds.")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("This will delete after 10 seconds.")
            }
        }

        step("And I wait for 10 seconds") {
            UiWaitUtils.waitFor(10.seconds)
        }

        step("Then I do not see message This will delete after 10 seconds. in the conversation") {
            pages.conversationViewPage.assertMessageNotVisible("This will delete after 10 seconds.")
        }

        step("When Member1 sends message This will delete after 10 seconds as well. to SelfDeleting") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "This will delete after 10 seconds as well.",
                null,
                "SelfDeleting"
            )
        }

        step("And I wait for 10 seconds") {
            UiWaitUtils.waitFor(10.seconds)
        }

        step("Then I do not see message This will delete after 10 seconds as well. in the conversation") {
            pages.conversationViewPage.assertMessageNotVisible("This will delete after 10 seconds as well.")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4530", "TC-4528")
    @Category("regression", "RC", "selfDeletingMessages", "WPB-286", "WPB-10854")
    @Test
    fun givenGroupConversation_whenISetSelfDeletingTimer_thenSystemMessagesAreDisplayedOnce() {
        step("Given There is a team owner TeamOwner with team Deleting") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Deleting",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Deleting with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Deleting",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation SelfDeleting with Member1 in team Deleting") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "SelfDeleting",
                "user2Name",
                "Deleting"
            )
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            clientUserManager.setSelfUser(teamOwner)
        }

        loginToStagingAs(teamOwner)

        step("And I see and open group conversation SelfDeleting") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("SelfDeleting")
                clickGroupConversation("SelfDeleting")
            }
        }

        step("And I open group conversation details for SelfDeleting") {
            pages.conversationViewPage.clickOnGroupConversationDetails("SelfDeleting")
        }

        step("And I see Self-deleting messages is OFF") {
            pages.groupConversationDetailsPage.assertSelfDeletingMessagesState("OFF")
        }

        step("When I enforce a 10-second self-deleting message timer for the group conversation") {
            pages.groupConversationDetailsPage.apply {
                tapSelfDeletingMessagesOption()
                tapSelfDeletingMessagesToggle()
                tapSelfDeletingMessagesTimer("10 seconds")
                tapApplyButton()
            }
        }

        step("And I close the group conversation details through the X icon") {
            UiWaitUtils.waitFor(UiWaitUtils.SHORT_WAIT)
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
        }

        step("Then I see system message You set self-deleting messages to 10 seconds for everyone") {
            pages.conversationViewPage.assertSystemMessageVisible(
                "set self-deleting messages to 10 seconds for everyone"
            )
        }

        // TC-4528 - Do not duplicate timer-change system messages after an asset is sent.
        step("When I open group conversation details for SelfDeleting") {
            pages.conversationViewPage.clickOnGroupConversationDetails("SelfDeleting")
        }

        step("And I see Self-deleting messages is ON") {
            pages.groupConversationDetailsPage.assertSelfDeletingMessagesState("ON")
        }

        step("And I turn off the self-deleting message timer for the group conversation") {
            pages.groupConversationDetailsPage.apply {
                tapSelfDeletingMessagesOption()
                tapSelfDeletingMessagesToggle()
                tapApplyButton()
            }
        }

        step("And I close the group conversation details through the X icon") {
            UiWaitUtils.waitFor(UiWaitUtils.SHORT_WAIT)
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
        }

        step("Then I see system message You turned off the timer for self-deleting messages for everyone") {
            pages.conversationViewPage.assertSystemMessageVisible(
                "turned off the timer for self-deleting messages for everyone"
            )
        }

        step("When I open file sharing, create an Image QR code and open Gallery") {
            pages.conversationViewPage.iTapFileSharingButton()
            createQrImageInDeviceDownloadsFolder("Image")
            pages.conversationViewPage.tapSharingOption("Gallery")
        }

        step("And I select the Image QR code in the photo picker") {
            pages.documentsUIPage.apply {
                selectMostRecentImageInPhotoPicker()
                tapAddOrDoneButtonIfVisible()
            }
        }

        step("And I see the image preview page and send the image") {
            pages.documentsUIPage.apply {
                assertImagePreviewPageVisible()
                iTapSendButtonOnPreviewImage()
            }
        }

        step("Then I see the Image QR code in the conversation view") {
            pages.conversationViewPage.iSeeSentQrCodeImageInCurrentConversation()
        }

        step("And I see the timer-enabled system message only once in the conversation") {
            pages.conversationViewPage.assertSystemMessageVisibleOnlyOnce(
                "set self-deleting messages to 10 seconds for everyone"
            )
        }

        step("And I see the timer-disabled system message only once in the conversation") {
            pages.conversationViewPage.assertSystemMessageVisibleOnlyOnce(
                "turned off the timer for self-deleting messages for everyone"
            )
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4532")
    @Category("regression", "RC", "selfDeletingMessages")
    @Test
    fun givenEnforcedSelfDeletingTimer_whenITryToChangeIt_thenTimerOptionsAreNotDisplayed() {
        step("Given There is a team owner TeamOwner with team Deleting") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Deleting",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Deleting with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Deleting",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation SelfDeleting with Member1 in team Deleting") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "SelfDeleting",
                "user2Name",
                "Deleting"
            )
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            clientUserManager.setSelfUser(teamOwner)
        }

        loginToStagingAs(teamOwner)

        step("And I see and open group conversation SelfDeleting") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("SelfDeleting")
                clickGroupConversation("SelfDeleting")
            }
        }

        step("And I open group conversation details for SelfDeleting") {
            pages.conversationViewPage.clickOnGroupConversationDetails("SelfDeleting")
        }

        step("And I see Self-deleting messages is OFF") {
            pages.groupConversationDetailsPage.assertSelfDeletingMessagesState("OFF")
        }

        step("When I enforce a 10-second self-deleting message timer for the group conversation") {
            pages.groupConversationDetailsPage.apply {
                tapSelfDeletingMessagesOption()
                tapSelfDeletingMessagesToggle()
                tapSelfDeletingMessagesTimer("10 seconds")
                tapApplyButton()
            }
        }

        step("And I close the group conversation details through the X icon") {
            UiWaitUtils.waitFor(UiWaitUtils.SHORT_WAIT)
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
        }

        step("Then I see system message You set self-deleting messages to 10 seconds for everyone") {
            pages.conversationViewPage.assertSystemMessageVisible(
                "set self-deleting messages to 10 seconds for everyone"
            )
        }

        step("When I tap the text input field") {
            pages.conversationViewPage.tapMessageInInputField()
        }

        step("Then I see the self-deleting message button in the conversation") {
            pages.conversationViewPage.assertSelfDeleteTimerButtonVisible()
        }

        step("When I tap the self-deleting messages button") {
            pages.conversationViewPage.tapSelfDeleteTimerButton()
        }

        step("Then I do not see self-deleting timer options") {
            pages.conversationViewPage.assertSelfDeleteTimerOptionsNotVisible()
        }

        step("When I send message Let us test self deleting messages! and see it in the conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Let us test self deleting messages!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Let us test self deleting messages!")
            }
        }

        step("And I wait for 10 seconds") {
            UiWaitUtils.waitFor(10.seconds)
        }

        step("Then I do not see message Let us test self deleting messages! in the conversation") {
            pages.conversationViewPage.assertMessageNotVisible("Let us test self deleting messages!")
        }
    }

    // Keeps the repeated staging login flow consistent across self-deleting-message scenarios.
    private fun loginToStagingAs(user: ClientUser) {
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

        step("And I enter a valid email and password to sign in") {
            pages.loginPage.apply {
                enterUserIdentifier(user.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterUserPassword(user.password ?: "")
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in and decline share data alert") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }
    }
}
