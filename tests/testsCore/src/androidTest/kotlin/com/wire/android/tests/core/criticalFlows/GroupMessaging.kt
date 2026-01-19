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
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.core.pages.AllPages
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.inject
import service.TestServiceHelper
import uiautomatorutils.UiWaitUtils.WaitUtils.waitFor
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId

@RunWith(AndroidJUnit4::class)
class GroupMessaging : BaseUiTest() {
    private val pages: AllPages by inject()
    private lateinit var device: UiDevice
    private lateinit var context: Context
    private var teamOwner: ClientUser? = null
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
    @TestCaseId("TC-8606")
    @Category("criticalFlow")
    @Test
    fun givenGroupConversation_whenMessagesAreExchangedAndSelfDeletingMessageIsSent_thenMessageIsVisibleAndExpires() {
        step("Prepare team via backend and group conversation with members") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user1Name",
                "GroupMessaging",
                "en_US",
                true,
                backendClient,
                context
            )

            teamOwner = teamHelper.usersManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)

            teamHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name,user4Name,user5Name,user6Name",
                "GroupMessaging",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )

            testServiceHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name,user4Name,user5Name,user6Name",
                "GroupMessaging"
            )
        }

        step("Login as team owner in Android app") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwner?.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwner?.password ?: "")
                clickLoginButton()
            }
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("Verify group conversation is visible in conversation list") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
            }
        }

        step("Search for group conversation and verify it remains visible in results") {
            pages.conversationListPage.apply {
                tapSearchConversationField()
                typeFirstNCharsInSearchField("MyTestGroup", 3) // types "MyT"
                assertGroupConversationVisible("MyTeam")
            }
        }

        step("Open group conversation and send a message as team owner") {
            pages.conversationListPage.apply {
                clickGroupConversation("MyTeam")
            }
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello Team Members")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello Team Members")
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("Send a message to the group conversation as another member via backend") {
            testServiceHelper.apply {
                addDevice("user2Name", null, "Device1")
                userSendMessageToConversation("user2Name", "Hello Friends", "Device1", "MyTeam", false)
            }
        }

        step("Wait for notification popup to disappear") {
            pages.notificationsPage.apply {
                waitUntilNotificationPopUpGone()
            }
        }

        step("Verify unread count and open conversation to view received message") {
            pages.conversationListPage.apply {
                assertUnreadMessagesCount("1")
                clickGroupConversation("MyTeam")
            }
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Hello Friends")
            }
        }

        step("Open self-delete timer menu and verify available timer options") {
            pages.conversationViewPage.apply {
                tapMessageInInputField()
                tapSelfDeleteTimerButton()

                assertSelfDeleteOptionVisible("OFF")
                assertSelfDeleteOptionVisible("10 seconds")
                assertSelfDeleteOptionVisible("5 minutes")
                assertSelfDeleteOptionVisible("1 hour")
                assertSelfDeleteOptionVisible("1 day")
                assertSelfDeleteOptionVisible("7 days")
                assertSelfDeleteOptionVisible("4 weeks")
            }
        }

        step("Set self-delete timer to 10 seconds and send a self-deleting message") {
            pages.conversationViewPage.apply {
                tapSelfDeleteOption("10 seconds")
                assertSelfDeletingMessageLabelVisible()

                typeMessageInInputField("This is a Self deleting Message")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("This is a Self deleting Message")
            }
        }

        step("Verify self-deleting message expires and expiration note is shown") {
            pages.conversationViewPage.apply {
                waitFor(14) // Simple wait
                assertMessageNotVisible("This is a Self deleting Message")
                assertSentMessageIsVisibleInCurrentConversation(
                    "After one participant has seen your message and the timer has expired on their side, this note disappears."
                )
            }
        }

        step("Delete the group conversation from conversation details") {
            pages.conversationListPage.apply {
                clickGroupConversation("MyTeam")
            }
            pages.groupConversationDetailsPage.apply {
                tapShowMoreOptionsButton()
                tapDeleteConversationButton()
                tapRemoveGroupButton()
            }
        }

        step("Verify group conversation is no longer visible in conversation list") {
            pages.conversationListPage.apply {
                assertConversationNotVisible("MyTeam")
            }
        }
    }
}
