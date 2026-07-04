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

import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.BackendClient
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.UiWaitUtils.waitUntilToastIsDisplayed
import user.utils.ClientUser
import java.time.Duration

@Suppress("LargeClass")
@RunWith(AndroidJUnit4::class)
class ArchiveTests : BaseUiTest() {
    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4234", "TC-4235")
    @Category("regression", "RC", "archive")
    @Test
    fun givenOneOnOneConversation_whenArchivingFromUserProfilePageAndConversationListPage_thenICanArchiveAndUnarchive() {
        step("Given There is a team owner TeamOwner with team Archive") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Archive",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Archive with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Archive",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has 1:1 conversation with Member1 in team Archive") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Archive"
            )
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User Member1 is available for 1:1 archive checks") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link login flow") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("And I login as TeamOwner") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwner.password ?: "")
                clickLoginButton()
            }
        }

        step("And I complete post-login permission and privacy prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("And I see conversation Member1 in conversation list and tap on it") {
            pages.conversationListPage.apply {
                assertConversationVisible(member1.name ?: "")
                tapConversationNameInConversationList(member1.name ?: "")
            }
        }

        step("And I open conversation details for 1:1 conversation with Member1") {
            pages.conversationViewPage.click1On1ConversationDetails(member1.name ?: "")
        }

        step("And I tap show more options button on user profile screen") {
            pages.connectedUserProfilePage.apply {
                clickShowMoreOptions()
            }
        }

        step("When I tap move to archive button and confirm archive conversation") {
            pages.connectedUserProfilePage.apply {
                tapMoveToArchiveButton()
                tapConfirmArchiveConversationButton()
            }
        }

        step("Then I see Conversation was archived toast message on conversation list") {
            pages.conversationListPage.apply {
                assertToastMessageIsDisplayedOnConversationList("Conversation was archived")
            }
        }

        step("When I close the user profile through the close button") {
            pages.connectedUserProfilePage.apply {
                tapCloseButtonOnConnectedUserProfilePage()
            }
        }

        step("And I tap back button on conversationViewPage back to conversation list page") {
            pages.conversationViewPage.apply {
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("Then I do not see conversation Member1 in conversation list") {
            pages.conversationListPage.apply {
                assertConversationNotVisible(member1.name ?: "")
            }
        }

        step("When I open the main navigation menu and tap on archive menu entry") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickArchiveButtonOnMenuEntry()
            }
        }

        step("Then I see conversation Member1 in archive list") {
            pages.archivePage.apply {
                assertConversationVisibleInArchiveList(member1.name ?: "")
            }
        }

        step("When I tap on conversation name Member1 in archive list") {
            pages.archivePage.apply {
                tapConversationNameInArchiveList(member1.name ?: "")
            }
        }

        step("And I open conversation details for 1:1 conversation with Member1") {
            pages.conversationViewPage.click1On1ConversationDetails(member1.name ?: "")
        }

        step("And I tap show more options button on user profile screen and tap move out of archive button") {
            pages.connectedUserProfilePage.apply {
                clickShowMoreOptions()
                tapMoveOutOfArchiveButton()
            }
        }

        step("Then I see toast message Conversation was unarchived in user profile screen") {
            pages.connectedUserProfilePage.apply {
                assertToastMessageIsDisplayed("Conversation was unarchived")
            }
        }

        step("When I close the user profile through the close button") {
            pages.connectedUserProfilePage.apply {
                tapCloseButtonOnConnectedUserProfilePage()
            }
        }

        step("And I tap back button on conversationViewPage back to conversation list page") {
            pages.conversationViewPage.apply {
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("And I open the main navigation menu") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
            }
        }

        step("And I tap on conversations menu entry and see conversation Member1 in conversation list") {
            pages.conversationListPage.apply {
                clickConversationsButtonOnMenuEntry()
                assertConversationVisible(member1.name ?: "")
            }
        }

//        TC-4235 Verify you can archive and unarchive a 1:1 conversation from conversation list screen

        step("And I see conversation Member1 in conversation list and long tap on it") {
            pages.conversationListPage.apply {
                assertConversationVisible(member1.name ?: "")
                longPressConversation(member1.name ?: "")
            }
        }

        step("When I tap move to archive button and confirm archive conversation") {
            pages.conversationListPage.apply {
                tapMoveToArchiveButtonInConversationActions()
                tapConfirmArchiveConversationButton()
            }
        }

        step("Then I see Conversation was archived toast message and Member1 is hidden") {
            pages.conversationListPage.apply {
                assertToastMessageIsDisplayedOnConversationList("Conversation was archived")
                assertConversationNotVisible(member1.name ?: "")
            }
        }

        step("When I open the main navigation menu and tap on archive menu entry") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickArchiveButtonOnMenuEntry()
            }
        }

        step("Then I see conversation Member1 in archive list") {
            pages.archivePage.apply {
                assertConversationVisibleInArchiveList(member1.name ?: "")
            }
        }

        step("When I long tap on conversation name Member1 in archive list") {
            pages.archivePage.apply {
                longPressConversationInArchiveList(member1.name ?: "")
            }
        }

        step("And I tap move out of archive button") {
            pages.archivePage.apply {
                tapMoveOutOfArchiveButton()
            }
        }

        step("Then I see Conversation was unarchived toast message and Member1 is hidden in archive list") {
            pages.archivePage.apply {
                assertToastMessageIsDisplayedOnArchiveList("Conversation was unarchived")
                assertConversationNotVisibleInArchiveList(member1.name ?: "")
            }
        }

        step("When I open the main navigation menu and tap on conversations menu entry") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickConversationsButtonOnMenuEntry()
            }
        }

        step("Then I see conversation Member1 in conversation list") {
            pages.conversationListPage.apply {
                assertConversationVisible(member1.name ?: "")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4236", "TC-4237")
    @Category("regression", "RC", "archive")
    @Test
    fun givenGroupConversation_whenArchivingFromGroupDetailsPageAndConversationListPage_thenICanArchiveAndUnarchive() {
        step("Given There is a team owner TeamOwner with team Archive") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Archive",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds users Member1 and Member2 to team Archive with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Archive",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User TeamOwner has group conversation MyTeam with Member1 and Member2 in team Archive") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name",
                "Archive"
            )
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link login flow") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("And I login as TeamOwner") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwner.password ?: "")
                clickLoginButton()
            }
        }

        step("And I complete post-login permission and privacy prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("And I see group conversation MyTeam in conversation list and tap on it") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                clickGroupConversation("MyTeam")
            }
        }

        step("And I open group details for MyTeam") {
            pages.conversationViewPage.apply {
                clickOnGroupConversationDetails("MyTeam")
            }
        }

        step("And I tap show more options button on group details page") {
            pages.groupConversationDetailsPage.apply {
                tapShowMoreOptionsButton()
            }
        }

        step("When I tap move to archive button and confirm archive conversation") {
            pages.groupConversationDetailsPage.apply {
                tapMoveToArchiveButton()
                tapConfirmArchiveConversationButton()
            }
        }

        step("Then I see Conversation was archived toast message on conversation list") {
            pages.conversationListPage.apply {
                assertToastMessageIsDisplayedOnConversationList("Conversation was archived")
            }
        }

        step("When I close the group conversation details through X icon") {
            pages.groupConversationDetailsPage.apply {
                tapCloseButtonOnGroupConversationDetailsPage()
            }
        }

        step("And I tap back button on conversationViewPage back to conversation list page") {
            pages.conversationViewPage.apply {
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("Then I do not see conversation MyTeam in conversation list") {
            pages.conversationListPage.apply {
                assertConversationNotVisible("MyTeam")
            }
        }

        step("When I open the main navigation menu and tap on archive menu entry") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickArchiveButtonOnMenuEntry()
            }
        }

        step("Then I see conversation MyTeam in archive list") {
            pages.archivePage.apply {
                assertConversationVisibleInArchiveList("MyTeam")
            }
        }

        step("When I tap on conversation name MyTeam in archive list") {
            pages.archivePage.apply {
                tapConversationNameInArchiveList("MyTeam")
            }
        }

        step("And I open group details for MyTeam") {
            pages.conversationViewPage.apply {
                clickOnGroupConversationDetails("MyTeam")
            }
        }

        step("And I tap show more options button on group details page") {
            pages.groupConversationDetailsPage.apply {
                tapShowMoreOptionsButton()
            }
        }

        step("And I tap move out of archive button") {
            pages.groupConversationDetailsPage.apply {
                tapMoveOutOfArchiveButton()
            }
        }

        step("Then I see toast message Conversation was unarchived on group details page") {
            pages.groupConversationDetailsPage.apply {
                assertToastMessageIsDisplayed("Conversation was unarchived")
            }
        }

        step("When I close the group conversation details through X icon") {
            pages.groupConversationDetailsPage.apply {
                tapCloseButtonOnGroupConversationDetailsPage()
            }
        }

        step("And I tap back button on conversationViewPage to go back to archive list") {
            pages.conversationViewPage.apply {
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("Then I do not see conversation MyTeam in archive list") {
            pages.archivePage.apply {
                assertConversationNotVisibleInArchiveList("MyTeam")
            }
        }

        step("And I open the main navigation menu and tap on conversations menu entry") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickConversationsButtonOnMenuEntry()
            }
        }

        step("Then I see conversation MyTeam in conversation list") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
            }
        }

//        TC-4237 Verify you can archive and unarchive a group conversation from conversation list screen

        step("And I see conversation MyTeam in conversation list and long tap on it") {
            pages.conversationListPage.apply {
                longPressConversation("MyTeam")
            }
        }

        step("When I tap move to archive button and confirm archive conversation") {
            pages.conversationListPage.apply {
                tapMoveToArchiveButtonInConversationActions()
                tapConfirmArchiveConversationButton()
            }
        }

        step("Then I see Conversation was archived toast message and MyTeam is hidden") {
            pages.conversationListPage.apply {
                assertToastMessageIsDisplayedOnConversationList("Conversation was archived")
                assertConversationNotVisible("MyTeam")
            }
        }

        step("When I open the main navigation menu and tap on archive menu entry") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickArchiveButtonOnMenuEntry()
            }
        }

        step("Then I see conversation MyTeam in archive list") {
            pages.archivePage.apply {
                assertConversationVisibleInArchiveList("MyTeam")
            }
        }

        step("When I long tap on conversation name MyTeam in archive list") {
            pages.archivePage.apply {
                longPressConversationInArchiveList("MyTeam")
            }
        }

        step("And I tap move out of archive button") {
            pages.archivePage.apply {
                tapMoveOutOfArchiveButton()
            }
        }

        step("Then I see Conversation was unarchived toast message and MyTeam is hidden in archive list") {
            pages.archivePage.apply {
                assertToastMessageIsDisplayedOnArchiveList("Conversation was unarchived")
                assertConversationNotVisibleInArchiveList("MyTeam")
            }
        }

        step("When I open the main navigation menu and tap on conversations menu entry") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickConversationsButtonOnMenuEntry()
            }
        }

        step("Then I see conversation MyTeam in conversation list") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4238")
    @Category("regression", "RC", "archive")
    @Test
    fun givenArchivedGroupConversation_whenSendingAndReceivingMessages_thenCanICanSendAndReceiveMessage() {
        step("Given There is a team owner TeamOwner with team Archive") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Archive",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds users Member1 and Member2 to team Archive with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Archive",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User Member2 has Device1 available for backend messaging") {
            testServiceHelper.addDevice("user3Name", null, "Device1")
        }

        step("And User TeamOwner has group conversation MyTeam with Member1 and Member2 in team Archive") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name",
                "Archive"
            )
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link login flow") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("And I login as TeamOwner") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwner.password ?: "")
                clickLoginButton()
            }
        }

        step("And I complete post-login permission and privacy prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("And I see conversation MyTeam in conversation list and long tap on it") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                longPressConversation("MyTeam")
            }
        }

        step("And I tap move to archive button and confirm archive conversation") {
            pages.conversationListPage.apply {
                tapMoveToArchiveButtonInConversationActions()
                tapConfirmArchiveConversationButton()
            }
        }

        step("And I open the main navigation menu and tap on archive menu entry") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickArchiveButtonOnMenuEntry()
            }
        }

        step("And I see conversation MyTeam in archive list") {
            pages.archivePage.apply {
                assertConversationVisibleInArchiveList("MyTeam")
            }
        }

        step("And I tap on conversation name MyTeam in archive list") {
            pages.archivePage.apply {
                tapConversationNameInArchiveList("MyTeam")
            }
        }

        step("When I type the message Hello! into text input field") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
            }
        }

        step("And I tap send button and see the message Hello! in current conversation") {
            pages.conversationViewPage.apply {
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        step("When User Member2 sends message Hello to you, too! to group conversation MyTeam") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Hello to you, too!",
                "Device1",
                "MyTeam"
            )
        }

        step("Then I see the message Hello to you, too! in current conversation") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Hello to you, too!")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4239")
    @Category("regression", "RC", "archive")
    @Test
    fun givenILeftGroupConversation_whenArchivingFromConversationListPage_thenICanArchive() {
        step("Given There is a team owner TeamOwner with team Archive") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Archive",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds users Member1 and Member2 to team Archive with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Archive",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User TeamOwner has group conversation MyTeam with Member1 and Member2 in team Archive") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name",
                "Archive"
            )
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link login flow") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("And I login as TeamOwner") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwner.password ?: "")
                clickLoginButton()
            }
        }

        step("And I complete post-login permission and privacy prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("And I see conversation MyTeam in conversation list and long tap on it") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                longPressConversation("MyTeam")
            }
        }

        step("And I tap leave conversation button and confirm leave conversation") {
            pages.conversationListPage.apply {
                assertLeaveConversationButtonVisibleInConversationActions()
                tapLeaveConversationButtonInConversationActions()
                assertLeaveConversationConfirmationModalVisible("MyTeam")
                tapLeaveConversationButtonOnModal()
            }
        }

        step("And I see you left conversation toast message") {
            waitUntilToastIsDisplayed("You left the conversation.")
        }

        step("And I see conversation MyTeam in conversation list and long tap on it") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                longPressConversation("MyTeam")
            }
        }

        step("When I tap move to archive button and confirm archive conversation") {
            pages.conversationListPage.apply {
                tapMoveToArchiveButtonInConversationActions()
                tapConfirmArchiveConversationButton()
            }
        }

        step("Then I see Conversation was archived toast message and MyTeam is hidden") {
            pages.conversationListPage.apply {
                assertToastMessageIsDisplayedOnConversationList("Conversation was archived")
                assertConversationNotVisible("MyTeam")
            }
        }

        step("When I open the main navigation menu and tap on archive menu entry") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickArchiveButtonOnMenuEntry()
            }
        }

        step("Then I see conversation MyTeam in archive list") {
            pages.archivePage.apply {
                assertConversationVisibleInArchiveList("MyTeam")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4240")
    @Category("regression", "archive")
    @Test
    fun givenArchivedConversations_whenReceivingMessages_thenNoNotifications() {
        step("Given There is a team owner TeamOwner with team Archive") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Archive",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Archive with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Archive",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User Member1 adds 1 device") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }

        step("And User TeamOwner has 1:1 conversation with Member1 in team Archive") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Archive"
            )
        }

        step("And User TeamOwner has group conversation ArchivedConvo with Member1 in team Archive") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "ArchivedConvo",
                "user2Name",
                "Archive"
            )
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User Member1 is available for archive checks") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link login flow") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("And I login as TeamOwner") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwner.password ?: "")
                clickLoginButton()
            }
        }

        step("And I complete post-login permission and privacy prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("And I see conversation Member1 in conversation list and long tap on it") {
            pages.conversationListPage.apply {
                assertConversationVisible(member1.name ?: "")
                longPressConversation(member1.name ?: "")
            }
        }

        step("When I tap move to archive button and confirm archive conversation") {
            pages.conversationListPage.apply {
                tapMoveToArchiveButtonInConversationActions()
                tapConfirmArchiveConversationButton()
            }
        }

        step("Then I see Conversation was archived toast message and Member1 is hidden") {
            pages.conversationListPage.apply {
                assertToastMessageIsDisplayedOnConversationList("Conversation was archived")
                assertConversationNotVisible(member1.name ?: "")
            }
        }

        step("And I see conversation ArchivedConvo in conversation list and long tap on it") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("ArchivedConvo")
                longPressConversation("ArchivedConvo")
            }
        }

        step("When I tap move to archive button and confirm archive conversation") {
            pages.conversationListPage.apply {
                tapMoveToArchiveButtonInConversationActions()
                tapConfirmArchiveConversationButton()
            }
        }

        step("Then I see Conversation was archived toast message and ArchivedConvo is hidden") {
            pages.conversationListPage.apply {
                assertToastMessageIsDisplayedOnConversationList("Conversation was archived")
                assertConversationNotVisible("ArchivedConvo")
            }
        }

        step("When I minimise Wire") {
            device.pressHome()
        }

        step("And User Member1 sends message Hello from 1:1 to group conversation ArchivedConvo") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Hello from 1:1",
                "Device1",
                "ArchivedConvo"
            )
        }

        step("And User Member1 sends message Hello from group to User Myself") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user2Name",
                "Hello from group",
                "Device1",
                "user1Name"
            )
        }

        step("And I open the notification center") {
            pages.notificationsPage.apply {
                openNotificationCenter()
            }
        }

        step("Then I do not see messages from archived conversations in the notification center") {
            pages.notificationsPage.apply {
                assertMessageNotVisibleInNotificationCenter("Hello from 1:1")
                assertMessageNotVisibleInNotificationCenter("Hello from group")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4241")
    @Category("regression", "archive")
    @Test
    fun givenArchivedConversations_whenReceivingSelfDeletingMessages_thenNoNotifications() {
        step("Given There is a team owner TeamOwner with team Archive") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Archive",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Archive with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Archive",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User Member1 adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }

        step("And User TeamOwner has 1:1 conversation with Member1 in team Archive") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Archive"
            )
        }

        step("And User TeamOwner has group conversation ArchivedConvo with Member1 in team Archive") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "ArchivedConvo",
                "user2Name",
                "Archive"
            )
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User Member1 is available for self-deleting archive notification checks") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link login flow") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("And I login as TeamOwner") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwner.password ?: "")
                clickLoginButton()
            }
        }

        step("And I complete post-login permission and privacy prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("And I see conversation Member1 in conversation list and long tap on it") {
            pages.conversationListPage.apply {
                assertConversationVisible(member1.name ?: "")
                longPressConversation(member1.name ?: "")
            }
        }

        step("When I tap move to archive button and confirm archive conversation") {
            pages.conversationListPage.apply {
                tapMoveToArchiveButtonInConversationActions()
                tapConfirmArchiveConversationButton()
            }
        }

        step("Then I see Conversation was archived toast message and Member1 is hidden") {
            pages.conversationListPage.apply {
                assertToastMessageIsDisplayedOnConversationList("Conversation was archived")
                assertConversationNotVisible(member1.name ?: "")
            }
        }

        step("And I see conversation ArchivedConvo in conversation list and long tap on it") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("ArchivedConvo")
                longPressConversation("ArchivedConvo")
            }
        }

        step("When I tap move to archive button and confirm archive conversation") {
            pages.conversationListPage.apply {
                tapMoveToArchiveButtonInConversationActions()
                tapConfirmArchiveConversationButton()
            }
        }

        step("Then I see Conversation was archived toast message and ArchivedConvo is hidden") {
            pages.conversationListPage.apply {
                assertToastMessageIsDisplayedOnConversationList("Conversation was archived")
                assertConversationNotVisible("ArchivedConvo")
            }
        }

        step("When I minimise Wire") {
            device.pressHome()
        }

        step("And User Member1 sends ephemeral messages to archived group and 1:1 conversations") {
            testServiceHelper.userSendEphemeralMessageToConversation(
                "user2Name",
                "Hello from 1:1",
                "Device1",
                "ArchivedConvo",
                Duration.ofSeconds(10)
            )
            testServiceHelper.userSendEphemeralMessageToConversation(
                "user2Name",
                "Hello from group",
                "Device1",
                "user1Name",
                Duration.ofSeconds(10)
            )
        }

        step("And I open the notification center") {
            pages.notificationsPage.apply {
                openNotificationCenter()
            }
        }

        step("Then I do not see self-deleting message notifications from archived conversations") {
            pages.notificationsPage.apply {
                assertMessageNotVisibleInNotificationCenter("Sent a self-deleting message")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4242")
    @Category("regression", "RC", "archive")
    @Test
    fun givenConversationsArchivedBeforeLogin_whenAppIsInForegroundAndReceivingMessages_thenNoNotifications() {
        step("Given There is a team owner TeamOwner with team Archive") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Archive",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Archive with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Archive",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has 1:1 conversation with Member1 in team Archive") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Archive"
            )
        }

        step("And User TeamOwner has group conversation ArchivedConvo with Member1 in team Archive") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "ArchivedConvo",
                "user2Name",
                "Archive"
            )
        }
        step("And User Member1 has Device1 available for pre-login archive notification checks") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User Member1 is available for archived conversation visibility checks") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("When User TeamOwner archives conversation Member1") {
            backendSetupHelper.userArchivesConversation(
                "user1Name",
                "user2Name"

            )
        }

        step("And User TeamOwner archives conversation ArchivedConvo") {
            backendSetupHelper.userArchivesConversation(
                "user1Name",
                "ArchivedConvo"
            )
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link login flow") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("And I login as TeamOwner") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwner.password ?: "")
                clickLoginButton()
            }
        }

        step("And I complete post-login permission and privacy prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("Then I do not see archived conversations in conversation list") {
            pages.conversationListPage.apply {
                assertConversationNotVisible(member1.name ?: "")
                assertConversationNotVisible("ArchivedConvo")
            }
        }

        step("When User Member1 sends messages to archived group and 1:1 conversations") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Hello from 1:1",
                null,
                "ArchivedConvo"
            )
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user2Name",
                "Hello from group",
                null,
                "user1Name"
            )
        }

        step("And I open the notification center") {
            pages.notificationsPage.apply {
                openNotificationCenter()
            }
        }

        step("Then I do not see messages from archived conversations in the notification center") {
            pages.notificationsPage.apply {
                assertMessageNotVisibleInNotificationCenter("Hello from 1:1")
                assertMessageNotVisibleInNotificationCenter("Hello from group")
            }
        }
    }
}
