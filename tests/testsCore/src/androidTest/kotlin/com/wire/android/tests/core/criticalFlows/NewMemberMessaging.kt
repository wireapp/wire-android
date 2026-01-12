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
package com.wire.android.tests.core.criticalFlows

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import backendUtils.BackendClient
import backendUtils.team.TeamHelper
import backendUtils.team.TeamRoles
import backendUtils.team.deleteTeam
import com.wire.android.tests.core.pages.AllPages
import com.wire.android.tests.support.UiAutomatorSetup
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.inject
import service.TestServiceHelper
import service.userSendsGenericMessageToConversation
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import kotlin.getValue
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import uiautomatorutils.UiWaitUtils.WaitUtils.waitFor

@RunWith(AndroidJUnit4::class)
class NewMemberMessaging : BaseUiTest() {
    private val pages: AllPages by inject()
    private lateinit var device: UiDevice
    private lateinit var context: Context
    private var teamOwner: ClientUser? = null
    private var member1: ClientUser? = null
    private lateinit var backendClient: BackendClient
    private lateinit var teamHelper: TeamHelper
    private lateinit var testServiceHelper: TestServiceHelper

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_INTERNAL)
        backendClient = BackendClient.loadBackend("STAGING")
        teamHelper = TeamHelper()
        testServiceHelper = TestServiceHelper(teamHelper.usersManager)
    }

    @After
    fun tearDown() {
        runCatching { teamOwner?.deleteTeam(backendClient) }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8605")
    @Category("criticalFlow", "testTest")
    @Test
    fun givenUserJoinsNewTeam_whenMessagingAndMentionedInGroup_thenReceivesMessagesAndMentions() {
        step("Prepare team via backend, add members, and create group conversation") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user1Name",
                "Messaging",
                "en_US",
                true,
                backendClient,
                context
            )

            teamHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Messaging",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )

            teamOwner = teamHelper.usersManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
            member1 = teamHelper.usersManager.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)

            testServiceHelper.apply {
                userHasGroupConversationInTeam(
                    "user1Name",
                    "MyTeam",
                    "user3Name",
                    "Messaging"
                )
                addDevice("user1Name", null, "Device1")
                userXAddedContactsToGroupChat("user1Name", "user2Name", "MyTeam")
            }
        }

        step("Login as new team member in Android app") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
            pages.loginPage.apply {
                enterTeamMemberLoggingEmail(member1?.email ?: "")
                clickLoginButton()
                enterTeamMemberLoggingPassword(member1?.password ?: "")
                clickLoginButton()
            }
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("Verify group conversation is visible and start a new conversation flow") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                tapStartNewConversationButton()
            }
        }

        step("Search for team owner and start 1:1 conversation") {
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUniqueUserNameInSearchField(teamHelper, "user1Name")
                assertUsernameInSearchResultIs(teamOwner?.name ?: "")
                tapUsernameInSearchResult(teamOwner?.name ?: "")
            }
            pages.connectedUserProfilePage.apply {
                assertStartConversationButtonVisible()
                clickStartConversationButton()
            }
        }

        step("Send message to team owner from the new member and return to conversation list") {
            pages.conversationViewPage.apply {
                assertConversationScreenVisible()
                typeMessageInInputField("Hello Team Owner")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello Team Owner")
                tapBackButtonToCloseConversationViewPage()
            }

            pages.connectedUserProfilePage.apply {
                tapCloseButtonOnConnectedUserProfilePage()
            }

            pages.conversationListPage.apply {
                waitFor(1)
                clickCloseButtonOnNewConversationScreen()
                assertConversationListVisible()
            }
        }

        step("Send message to the new member via backend") {
            testServiceHelper.apply {
                userSendsGenericMessageToConversation(
                    "user1Name",
                    "user2Name",
                    "Device1",
                    "Hello new member"
                )
            }
        }

        step("Wait for notification popup to disappear") {
            pages.notificationsPage.apply {
                waitUntilNotificationPopUpGone()
            }
        }

        step("Verify unread message count and open unread conversation") {
            pages.conversationListPage.apply {
                assertUnreadMessagesCount("1")
                tapUnreadConversationNameInConversationList(teamOwner?.name ?: "")
            }
        }

        step("Verify received message is visible in conversation") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Hello new member")
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("Open group conversation and verify mention is visible when sent via backend") {
            pages.conversationListPage.apply {
                clickGroupConversation("MyTeam")
            }

            testServiceHelper.apply {
                val mentionReplacedWithUniqueUserName = teamHelper.usersManager.replaceAliasesOccurrences(
                    "@user2Name",
                    ClientUserManager.FindBy.NAME_ALIAS
                )
                userSendsGenericMessageToConversation(
                    "user1Name",
                    "MyTeam",
                    "Device1",
                    mentionReplacedWithUniqueUserName
                )
            }

            pages.conversationViewPage.apply {
                val mentionedUser = teamHelper.usersManager.replaceAliasesOccurrences(
                    "@user2Name",
                    ClientUserManager.FindBy.NAME_ALIAS
                )
                assertVisibleMentionedNameIs(mentionedUser)
            }
        }
    }
}
