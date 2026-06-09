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
package com.wire.android.tests.core.criticalFlows

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import backendUtils.BackendClient
import backendUtils.team.TeamHelper
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.core.pages.AllPages
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.inject
import service.TestServiceHelper
import uiautomatorutils.UiWaitUtils.iSeeSystemMessage
import uiautomatorutils.UiWaitUtils.waitUntilToastIsDisplayed
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import kotlin.getValue

@RunWith(AndroidJUnit4::class)
class GroupInteraction : BaseUiTest() {
    private val pages: AllPages by inject()
    private lateinit var device: UiDevice
    private lateinit var context: Context
    private lateinit var backendClient: BackendClient
    private lateinit var teamHelper: TeamHelper
    private lateinit var testServiceHelper: TestServiceHelper
    private lateinit var teamOwnerA: ClientUser

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
        teamHelper = TeamHelper()
        testServiceHelper = TestServiceHelper(teamHelper.usersManager)
    }

    @After
    fun tearDown() {
        cleanupCreatedUsers(backendClient, teamHelper.usersManager)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8601")
    @Category("criticalFlow")
    @Test
    fun givenTeamOwnerWithGroupConversationAndBot_whenValidatingReactionsAndInteractions_thenFlowSucceeds() {
        step("There is TeamOwnerA with team Bots") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user1Name",
                "Bots",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("TeamOwnerA adds Member1 and Member2 to team Bots with role Member") {
            teamHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Bots",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("There is TeamOwnerB with team ConnectedFriend") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user4Name",
                "ConnectedFriend",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("TeamOwnerA is connected to TeamOwnerB") {
            testServiceHelper.userIsConnectedTo("user1Name", "user4Name")
        }

        step("TeamOwnerA adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }

        step("TeamOwnerA enables Poll Bot service for team Bots") {
            testServiceHelper.userEnablesServiceForTeam("user1Name", "Poll Bot", "Bots")
        }

        step("TeamOwnerA has group conversation BotsConversation with Member1, Member2, and TeamOwnerB in team Bots") {
            testServiceHelper.userHasGroupConversationInTeam(
                "user1Name",
                "BotsConversation",
                "user2Name,user3Name,user4Name",
                "Bots"
            )
        }

        step("TeamOwnerA adds Poll Bot to conversation BotsConversation") {
            testServiceHelper.userAddsBotToConversation("user1Name", "Poll Bot", "BotsConversation")
        }

        step("TeamOwnerA is me") {
            teamOwnerA = teamHelper.usersManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
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

        step("And I login as TeamOwnerA") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwnerA.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwnerA.password ?: "")
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

        step("Then I tap on conversation name BotsConversation in conversation list") {
            pages.conversationListPage.apply {
                clickGroupConversation("BotsConversation")
            }
        }

        step("And I see group conversation BotsConversation is in foreground") {
            pages.conversationViewPage.apply {
                assertConversationScreenVisible()
            }
        }

        step("Then I see a banner informing me that Guests and apps are present in the conversation view") {
            pages.conversationViewPage.apply {
                assertGuestsAndAppsBannerVisible()
            }
        }

        step("When Member1 sends message Hello fellow members to group conversation BotsConversation") {
            testServiceHelper.apply {
                addDevice("user2Name", null, "Device2")
                userSendMessageToConversation(
                    "user2Name",
                    "Hello fellow members",
                    "Device2",
                    "BotsConversation",
                    false
                )
            }
        }

        step("Then I see the message Hello fellow members in current conversation") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Hello fellow members")
            }
        }

        step("When I long tap on the message Hello fellow members in current conversation") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Hello fellow members")
            }
        }

        step("And I see reactions options") {
            pages.conversationViewPage.apply {
                assertTextMessageReactionOptionsVisible()
            }
        }

        step("And I tap on heart reaction icon") {
            pages.conversationViewPage.apply {
                tapReactionIcon("\u2764\uFE0F") // ❤️
            }
        }

        step("Then I see a heart reaction from 1 user as reaction to Member1 message") {
            pages.conversationViewPage.apply {
                assertReactionAndUserCountVisible("\u2764\uFE0F", 1) // ❤️
            }
        }

        step("When I type the message Hello Team Members into text input field and send it") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello Team Members")
                clickSendButton()
            }
        }

        step("Then I see the message Hello Team Members in current conversation") {
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("Hello Team Members")
            }
        }

        step("When TeamOwner toggles thumbs up reaction on the recent message from BotsConversation via Device1") {
            testServiceHelper.userTogglesReactionOnLatestMessage(
                "user1Name",
                "BotsConversation",
                "Device1",
                "\uD83D\uDC4D" // 👍
            )
        }

        step("Then I see a thumbs up reaction from 1 user as reaction to TeamOwnerA message") {
            pages.conversationViewPage.apply {
                assertReactionAndUserCountVisible("\uD83D\uDC4D", 1) // 👍
            }
        }

        step("When I tap on group conversation title BotsConversation to open group details") {
            pages.conversationViewPage.apply {
                clickOnGroupConversationDetails("BotsConversation")
            }
        }

        step("And I tap on Participants tab") {
            pages.groupConversationDetailsPage.apply {
                tapOnParticipantsTab()
            }
        }

        step("Then I see Member2 in participants list") {
            val member2 = teamHelper.usersManager.findUserBy("user3Name", ClientUserManager.FindBy.NAME_ALIAS)
            pages.groupConversationDetailsPage.apply {
                assertUsernameIsAddedToParticipantsList(member2.name ?: "")
            }
        }

        step("When I close the group conversation details through X icon") {
            pages.groupConversationDetailsPage.apply {
                tapCloseButtonOnGroupConversationDetailsPage()
            }
        }

        step("And TeamOwnerA removes TeamOwnerB from group conversation BotsConversation") {
            testServiceHelper.userRemovesUserFromGroupConversation(
                "user1Name",
                "user4Name",
                "BotsConversation"
            )
        }

        step("Then I see system message You removed TeamOwnerB from the conversation in conversation view") {
            val teamOwnerB = teamHelper.usersManager.findUserBy("user4Name", ClientUserManager.FindBy.NAME_ALIAS)
            iSeeSystemMessage("You removed ${teamOwnerB.name ?: ""} from the conversation")
        }

        step("When I tap on group conversation title BotsConversation to open group details") {
            pages.conversationViewPage.apply {
                clickOnGroupConversationDetails("BotsConversation")
            }
        }

        step("And I see group details page") {
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
            }
        }

        step("And I tap on Participants tab") {
            pages.groupConversationDetailsPage.apply {
                tapOnParticipantsTab()
            }
        }

        step("And I see user Poll Bot in participants list") {
            pages.groupConversationDetailsPage.apply {
                assertUsernameIsAddedToParticipantsList("Poll Bot")
            }
        }

        step("And I tap on user Poll Bot in participants list") {
            pages.groupConversationDetailsPage.apply {
                tapUserInParticipantsList("Poll Bot")
            }
        }

        step("Then I see Remove From Conversation button for App") {
            pages.groupConversationDetailsPage.apply {
                assertRemoveFromConversationButtonForAppVisible()
            }
        }

        step("When I tap Remove From Conversation button and see toast message App removed from Conversation") {
            pages.groupConversationDetailsPage.apply {
                tapRemoveFromConversationButton()
                waitUntilToastIsDisplayed("App removed from conversation")
            }
        }

        step("Then I do not see Remove From Conversation button again") {
            pages.groupConversationDetailsPage.apply {
                assertRemoveFromConversationButtonNotVisible()
            }
        }

        step("And I now see Add to Conversation button") {
            pages.groupConversationDetailsPage.apply {
                assertAddToConversationButtonVisible()
            }
        }

        step("When I tap back button") {
            pages.groupConversationDetailsPage.apply {
                tapBackButton()
            }
        }

        step("Then I do not see user Poll Bot in participants list") {
            pages.groupConversationDetailsPage.apply {
                assertUserIsNotInParticipantsList("Poll Bot")
            }
        }

        step("When I close the group conversation details through X icon") {
            pages.groupConversationDetailsPage.apply {
                tapCloseButtonOnGroupConversationDetailsPage()
            }
        }

        step("Then I see system message You removed Poll Bot from the conversation in conversation view") {
            iSeeSystemMessage("You removed Poll Bot from the conversation")
        }
    }
}
