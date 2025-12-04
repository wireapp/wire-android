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

    lateinit var context: Context
    var teamOwner: ClientUser? = null
    var backendClient: BackendClient? = null
    var teamHelper: TeamHelper? = null
    val testServiceHelper by lazy {
        TestServiceHelper()
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
        // device = UiAutomatorSetup.start(UiAutomatorSetup.APP_DEV)
        // device = UiAutomatorSetup.start(UiAutomatorSetup.APP_STAGING)
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_INTERNAL)
        backendClient = BackendClient.loadBackend("STAGING")
        teamHelper = TeamHelper()
    }

    @After
    fun tearDown() {
        // To delete team
        teamOwner?.deleteTeam(backendClient!!)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8606")
    @Category("criticalFlow")
    @Test
    fun givenGroupConversation_whenMessagesAreExchangedAndSelfDeletingMessageIsSent_thenMessageIsVisibleAndExpires() {
        teamHelper?.usersManager!!.createTeamOwnerByAlias(
            "user1Name",
            "GroupMessaging",
            "en_US",
            true,
            backendClient!!,
            context
        )
        teamOwner = teamHelper?.usersManager!!.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
        teamHelper?.userXAddsUsersToTeam(
            "user1Name",
            "user2Name,user3Name,user4Name,user5Name,user6Name",
            "GroupMessaging",
            TeamRoles.Member,
            backendClient!!,
            context,
            true
        )

        testServiceHelper.userHasGroupConversationInTeam(
            "user1Name",
            "MyTeam",
            "user2Name,user3Name,user4Name,user5Name,user6Name",
            "GroupMessaging"
        )

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
        pages.conversationListPage.apply {
            assertGroupConversationVisible("MyTeam")
        }
        pages.conversationListPage.apply {
            tapSearchConversationField()
            typeFirstNCharsInSearchField("MyTestGroup", 3) // types "MyT"
        }
        pages.conversationListPage.apply {
            assertGroupConversationVisible("MyTeam")
        }
        pages.conversationListPage.apply {
            clickGroupConversation("MyTeam")
        }
        pages.conversationViewPage.apply {
            typeMessageInInputField("Hello Team Members")
            clickSendButton()
            assertSentMessageIsVisibleInCurrentConversation("Hello Team Members")
            tapBackButtonToCloseConversationViewPage()
        }
        testServiceHelper.apply {
            addDevice("user2Name", null, "Device1")
            userSendMessageToConversation("user2Name", "Hello Friends", "Device1", "MyTeam", false)
        }
        pages.notificationsPage.apply {
            waitUntilNotificationPopUpGone()
        }
        pages.conversationListPage.apply {
            assertUnreadMessagesCount("1")
            clickGroupConversation("MyTeam")
        }
        pages.conversationViewPage.apply {
            assertReceivedMessageIsVisibleInCurrentConversation("Hello Friends")
            tapMessageInInputField()
            tapSelfDeleteTimerButton()
            assertSelfDeleteOptionVisible("OFF")
            assertSelfDeleteOptionVisible("10 seconds")
            assertSelfDeleteOptionVisible("5 minutes")
            assertSelfDeleteOptionVisible("1 hour")
            assertSelfDeleteOptionVisible("1 day")
            assertSelfDeleteOptionVisible("7 days")
            assertSelfDeleteOptionVisible("4 weeks")
            tapSelfDeleteOption("10 seconds")
            assertSelfDeletingMessageLabelVisible()
            typeMessageInInputField("This is a Self deleting Message")
            clickSendButton()
            assertSentMessageIsVisibleInCurrentConversation("This is a Self deleting Message")
            waitFor(14) // Simple wait
            assertMessageNotVisible("This is a Self deleting Message")
            assertSentMessageIsVisibleInCurrentConversation(
                "After one participant has seen your message and the timer has expired on their side, this note disappears."
            )
        }
        pages.conversationListPage.apply {
            clickGroupConversation("MyTeam")
        }
        pages.groupConversationDetailsPage.apply {
            tapShowMoreOptionsButton()
            tapDeleteConversationButton()
            tapRemoveGroupButton()
        }
        pages.conversationListPage.apply {
            assertConversationNotVisible("MyTeam")
        }
    }
}
