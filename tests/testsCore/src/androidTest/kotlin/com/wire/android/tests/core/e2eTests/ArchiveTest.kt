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
@file:Suppress("ArgumentListWrapping", "MaximumLineLength")

package com.wire.android.tests.core.e2eTests

import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.BackendClient
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.UiWaitUtils.waitUntilToastIsDisplayed
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class ArchiveTest : BaseUiTest() {

    private var currentUser: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @After
    fun tearDown() {
        runCatching { cleanupBackendClient(backendClient, currentUser) }
    }

    @TestCaseId("TC-4235")
    @Category("archive", "regression", "RC")
    @Test
    fun givenOneOnOneConversation_whenArchivingFromConversationList_thenConversationCanBeUnarchived() {
        prepareOneOnOneConversation()
        loginCurrentUser()

        archiveConversationFromList(memberName())
        openArchiveList()
        unarchiveConversationFromArchiveList(memberName())
        openConversationList()

        step("Verify 1:1 conversation is visible in conversation list again") {
            pages.conversationListPage.assertConversationVisible(memberName())
        }
    }

    @TestCaseId("TC-4234")
    @Category("archive", "regression", "RC")
    @Test
    fun givenOneOnOneConversation_whenArchivingFromUserProfile_thenConversationCanBeUnarchived() {
        prepareOneOnOneConversation()
        loginCurrentUser()

        openOneOnOneConversation()
        archiveOneOnOneConversationFromProfile()
        pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()

        openArchiveList()
        pages.conversationListPage.tapConversationNameInConversationList(memberName())
        pages.conversationViewPage.click1On1ConversationDetails(memberName())
        unarchiveOneOnOneConversationFromProfile()
        pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        openConversationList()

        step("Verify 1:1 conversation is visible in conversation list again") {
            pages.conversationListPage.assertConversationVisible(memberName())
        }
    }

    @TestCaseId("TC-4237")
    @Category("archive", "regression", "RC")
    @Test
    fun givenGroupConversation_whenArchivingFromConversationList_thenConversationCanBeUnarchived() {
        prepareGroupConversation()
        loginCurrentUser()

        archiveConversationFromList(GROUP_CONVERSATION_NAME)
        openArchiveList()
        unarchiveConversationFromArchiveList(GROUP_CONVERSATION_NAME)
        openConversationList()

        step("Verify group conversation is visible in conversation list again") {
            pages.conversationListPage.assertConversationVisible(GROUP_CONVERSATION_NAME)
        }
    }

    @TestCaseId("TC-4236")
    @Category("archive", "regression", "RC")
    @Test
    fun givenGroupConversation_whenArchivingFromGroupDetails_thenConversationCanBeUnarchived() {
        prepareGroupConversation()
        loginCurrentUser()

        openGroupConversation(GROUP_CONVERSATION_NAME)
        archiveGroupConversationFromDetails()
        pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()

        openArchiveList()
        pages.conversationListPage.tapConversationNameInConversationList(GROUP_CONVERSATION_NAME)
        pages.conversationViewPage.clickOnGroupConversationDetails(GROUP_CONVERSATION_NAME)
        unarchiveGroupConversationFromDetails()
        pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        openConversationList()

        step("Verify group conversation is visible in conversation list again") {
            pages.conversationListPage.assertConversationVisible(GROUP_CONVERSATION_NAME)
        }
    }

    @TestCaseId("TC-4238")
    @Category("archive", "regression", "RC")
    @Test
    fun givenArchivedGroupConversation_whenOpenedFromArchive_thenMessagesCanStillBeSentAndReceived() {
        prepareGroupConversation()
        loginCurrentUser()

        archiveConversationFromList(GROUP_CONVERSATION_NAME)
        openArchiveList()
        pages.conversationListPage.tapConversationNameInConversationList(GROUP_CONVERSATION_NAME)

        sendLocalMessage(LOCAL_MESSAGE)
        sendRemoteGroupMessage(REMOTE_MESSAGE)

        step("Verify local and remote messages are visible in archived conversation") {
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation(LOCAL_MESSAGE)
                assertReceivedMessageIsVisibleInCurrentConversation(REMOTE_MESSAGE)
            }
        }
    }

    @Ignore(
        "Blocked: source scenario still has left-conversation archive assertions disabled by WPB-19133/WPB-19135; activate only after expected post-leave archive behavior and assertions are restored."
    )
    @TestCaseId("TC-4239")
    @Category("archive", "regression", "RC")
    @Test
    fun givenLeftGroupConversation_whenArchivingFromConversationList_thenConversationMovesToArchive() = Unit

    @Ignore("Blocked: notification-center parity requires push notification delivery assertions while Wire is backgrounded.")
    @TestCaseId("TC-4240")
    @Category("archive", "regression")
    @Test
    fun givenArchivedConversations_whenRemoteMessagesArriveInBackground_thenNoNotificationsAreShown() = Unit

    @Ignore(
        "Blocked: notification-center parity for self-deleting messages requires backend ephemeral-message sending plus push notification assertions while Wire is backgrounded."
    )
    @TestCaseId("TC-4241")
    @Category("archive", "regression")
    @Test
    fun givenArchivedConversations_whenRemoteEphemeralMessagesArriveInBackground_thenNoNotificationsAreShown() = Unit

    @Ignore(
        "Blocked: requires pre-login backend archiving for current user plus foreground notification-center assertions for archived conversations."
    )
    @TestCaseId("TC-4242")
    @Category("archive", "regression", "RC")
    @Test
    fun givenConversationsArchivedBeforeLogin_whenRemoteMessagesArrive_thenNoNotificationsAreShown() = Unit

    private fun prepareOneOnOneConversation() {
        prepareTeamWithMembers()
        step("Team owner has 1:1 conversation with member") {
            testServiceHelper.userHas1on1ConversationInTeam(TEAM_OWNER_ALIAS, TEAM_MEMBER_ALIAS, TEAM_NAME)
        }
    }

    private fun prepareGroupConversation() {
        prepareTeamWithMembers()
        step("Team owner has group conversation with members") {
            testServiceHelper.userHasGroupConversationInTeam(
                TEAM_OWNER_ALIAS,
                GROUP_CONVERSATION_NAME,
                "$TEAM_MEMBER_ALIAS,$EXTRA_MEMBER_ALIAS",
                TEAM_NAME
            )
        }
    }

    private fun prepareTeamWithMembers() {
        step("Prepare backend team owner and members") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                TEAM_OWNER_ALIAS,
                TEAM_NAME,
                "en_US",
                true,
                backendClient,
                context
            )
            teamHelper.userXAddsUsersToTeam(
                TEAM_OWNER_ALIAS,
                "$TEAM_MEMBER_ALIAS,$EXTRA_MEMBER_ALIAS",
                TEAM_NAME,
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            currentUser = teamHelper.usersManager.findUserBy(TEAM_OWNER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        }
    }

    private fun loginCurrentUser() {
        step("Login current user via staging deep link") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterTeamOwnerLoggingEmail(currentUser?.email.orEmpty())
                clickLoginButton()
                enterTeamOwnerLoggingPassword(currentUser?.password.orEmpty())
                clickLoginButton()
            }
        }

        step("Complete login flow") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    private fun openOneOnOneConversation() {
        step("Open 1:1 conversation with team member") {
            pages.conversationListPage.apply {
                assertConversationVisible(memberName())
                tapConversationNameInConversationList(memberName())
            }
            pages.conversationViewPage.assertConversationIsVisibleWithTeamMember(memberName())
        }
    }

    private fun openGroupConversation(conversationName: String) {
        step("Open group conversation '$conversationName'") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible(conversationName)
                clickGroupConversation(conversationName)
            }
            pages.conversationViewPage.assertConversationScreenVisible()
        }
    }

    private fun archiveConversationFromList(conversationName: String) {
        step("Archive conversation '$conversationName' from conversation list") {
            pages.conversationListPage.apply {
                assertConversationVisible(conversationName)
                longPressConversation(conversationName)
                tapMoveToArchiveButtonInConversationActions()
                tapArchiveConversationConfirmationButton()
            }
            waitUntilToastIsDisplayed(ARCHIVED_TOAST)
            pages.conversationListPage.assertConversationNotVisible(conversationName)
        }
    }

    private fun archiveOneOnOneConversationFromProfile() {
        step("Archive 1:1 conversation from user profile") {
            pages.conversationViewPage.click1On1ConversationDetails(memberName())
            pages.connectedUserProfilePage.apply {
                clickShowMoreOptions()
                clickMoveToArchiveOption()
                clickArchiveButtonAlert()
            }
            waitUntilToastIsDisplayed(ARCHIVED_TOAST)
            pages.connectedUserProfilePage.tapCloseButtonOnConnectedUserProfilePage()
        }
    }

    private fun unarchiveOneOnOneConversationFromProfile() {
        step("Unarchive 1:1 conversation from user profile") {
            pages.connectedUserProfilePage.apply {
                clickShowMoreOptions()
                clickUnarchiveOption()
            }
            waitUntilToastIsDisplayed(UNARCHIVED_TOAST)
            pages.connectedUserProfilePage.tapCloseButtonOnConnectedUserProfilePage()
        }
    }

    private fun archiveGroupConversationFromDetails() {
        step("Archive group conversation from group details") {
            pages.conversationViewPage.clickOnGroupConversationDetails(GROUP_CONVERSATION_NAME)
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapShowMoreOptionsButton()
                tapMoveToArchiveOption()
                tapArchiveButtonAlert()
            }
            waitUntilToastIsDisplayed(ARCHIVED_TOAST)
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
        }
    }

    private fun unarchiveGroupConversationFromDetails() {
        step("Unarchive group conversation from group details") {
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapShowMoreOptionsButton()
                tapUnarchiveOption()
            }
            waitUntilToastIsDisplayed(UNARCHIVED_TOAST)
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
        }
    }

    private fun openArchiveList() {
        step("Open archive list from main navigation") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickArchiveButtonOnMenuEntry()
            }
        }
    }

    private fun unarchiveConversationFromArchiveList(conversationName: String) {
        step("Unarchive conversation '$conversationName' from archive list") {
            pages.conversationListPage.apply {
                assertConversationVisible(conversationName)
                longPressConversation(conversationName)
                tapUnarchiveButtonInConversationActions()
            }
            waitUntilToastIsDisplayed(UNARCHIVED_TOAST)
            pages.conversationListPage.assertConversationNotVisible(conversationName)
        }
    }

    private fun openConversationList() {
        step("Open conversation list from main navigation") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickConversationsButtonOnMenuEntry()
            }
        }
    }

    private fun sendLocalMessage(message: String) {
        step("Send local message '$message'") {
            pages.conversationViewPage.apply {
                typeMessageInInputField(message)
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation(message)
            }
        }
    }

    private fun sendRemoteGroupMessage(message: String) {
        step("Member sends message '$message' to archived group conversation") {
            testServiceHelper.apply {
                addDevice(EXTRA_MEMBER_ALIAS, null, DEVICE_NAME)
                userSendMessageToConversation(
                    EXTRA_MEMBER_ALIAS,
                    message,
                    DEVICE_NAME,
                    GROUP_CONVERSATION_NAME,
                    false
                )
            }
        }
    }

    private fun memberName(): String =
        teamHelper.usersManager.findUserBy(TEAM_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS).name.orEmpty()

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val EXTRA_MEMBER_ALIAS = "user3Name"
        const val TEAM_NAME = "Archive"
        const val GROUP_CONVERSATION_NAME = "MyTeam"
        const val ARCHIVED_TOAST = "Conversation was archived"
        const val UNARCHIVED_TOAST = "Conversation was unarchived"
        const val LOCAL_MESSAGE = "Hello!"
        const val REMOTE_MESSAGE = "Hello to you, too!"
        const val DEVICE_NAME = "Device1"
    }
}
