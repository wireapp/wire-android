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


import InbucketClient
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import backendUtils.BackendClient
import backendUtils.team.TeamHelper
import backendUtils.team.TeamRoles
import backendUtils.team.deleteTeam
import com.wire.android.testSupport.uiautomatorutils.UiAutomatorSetup
import com.wire.android.tests.core.di.testModule
import com.wire.android.tests.core.pages.AllPages
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import service.TestServiceHelper
import uiautomatorutils.UiWaitUtils
import user.UserClient
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class GroupMessaging : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.Companion.create {
        modules(testModule)
    }
    private val pages: AllPages by inject()
    private lateinit var device: UiDevice
    lateinit var context: Context
    var registeredUser: ClientUser? = null
    var backendClient: BackendClient? = null
    var teamHelper: TeamHelper? = null
    val testServiceHelper by lazy {
        TestServiceHelper()
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
        // device = UiAutomatorSetup.start(UiAutomatorSetup.APP_DEV)
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_STAGING)
        backendClient = BackendClient.loadBackend("STAGING")
        teamHelper = TeamHelper()
    }

    @After
    fun tearDown() {
        // To delete team
        //  registeredUser?.deleteTeam(backendClient!!)
    }

    @Suppress("LongMethod")
    @Test
    fun groupMessagingFeature() {
        teamHelper?.usersManager!!.createTeamOwnerByAlias(
            "user1Name",
            "GroupMessaging",
            "en_US",
            true,
            backendClient!!,
            context
        )
        registeredUser = teamHelper?.usersManager!!.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
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
            enterPersonalUserLoggingEmail(registeredUser?.email ?: "")
            clickLoginButton()
            enterPersonalUserLoginPassword(registeredUser?.password ?: "")
            clickLoginButton()

        }
        pages.registrationPage.apply {
            waitUntilLoginFlowIsComplete()
            clickAllowNotificationButton()
            clickDeclineShareDataAlert()
        }
        pages.conversationPage.apply {
            assertGroupConversationVisible("MyTeam")
        }

        pages.conversationListPage.apply {
            tapSearchConversationField()
            typeFirstNCharsInSearchField("MyTestGroup", 3) // types "MyT"
        }
        pages.conversationPage.apply {
            assertGroupConversationVisible("MyTeam")
        }
        pages.conversationListPage.apply {
            clickGroupConversation("MyTeam")
        }
        pages.conversationViewPage.apply {
            typeMessageInInputField("Hello Team Members")
            clickSendButton()

            assertMessageSentIsVisible("Hello Team Members")

            tapBackButtonOnConversationViewPage()
        }
        testServiceHelper.apply {
            addDevice("user2Name", null, "Device1")
            userSendMessageToConversation("user2Name", "Hello Friends", "Device1", "MyTeam", false)
        }
        // Thread.sleep(1000)
        pages.notificationsPage.apply {

            waitUntilNotificationPopUpGone()
        }
        pages.conversationListPage.apply {
            assertUnreadMessagesCount("1")
        }
        pages.conversationListPage.apply {
            clickGroupConversation("MyTeam")
        }
        pages.conversationViewPage.apply {
            assertMessageReceivedIsVisible("Hello Friends")
            tapMessageInInputField()
            tapSelfDestructTimerButton()
            assertSelfDestructOptionVisible("OFF")
            assertSelfDestructOptionVisible("10 seconds")
            assertSelfDestructOptionVisible("1 minute")
            assertSelfDestructOptionVisible("5 minutes")
            assertSelfDestructOptionVisible("1 hour")
            assertSelfDestructOptionVisible("1 day")
            assertSelfDestructOptionVisible("7 days")
            assertSelfDestructOptionVisible("4 weeks")
            tapSelfDestructOption("10 seconds")
            assertSelfDeletingMessageLabelVisible()
            typeMessageInInputField("This is a Self deleting Message")
            clickSendButton()
            assertMessageSentIsVisible("This is a Self deleting Message")
            UiWaitUtils.WaitUtils.waitFor(14)  // Simple wait
            assertMessageNotVisible("This is a Self deleting Message")
            assertMessageSentIsVisible("After one participant has seen your message and the timer has expired on their side, this note disappears.")
            pages.conversationListPage.apply {
                clickGroupConversation("MyTeam")
            }
            pages.groupConversationDetailsPage.apply {
                tapShowMoreOptionsButton()
                tapDeleteConversationButton()
                tapRemoveGroupButton()
                pages.conversationListPage.apply {
                    assertConversationNotVisible("MyTeam")
                }
            }
        }
    }
}

