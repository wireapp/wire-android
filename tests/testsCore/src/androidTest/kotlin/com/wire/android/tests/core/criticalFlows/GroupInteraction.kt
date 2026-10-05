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

import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.UiWaitUtils.iSeeSystemMessage
import uiautomatorutils.UiWaitUtils.waitUntilToastIsDisplayed
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class GroupInteraction : BaseUiTest() {
    private lateinit var teamOwnerA: ClientUser
    private lateinit var teamOwnerB: ClientUser
    private val pollAppId = "14969de0-76ae-486a-b3e9-7960b1c7f5b2"

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8601")
    @Category("criticalFlow")
    @Test
    fun givenTeamOwnerWithGroupConversationAndApp_whenValidatingReactionsAndInteractions_thenFlowSucceeds() {
        step("There is TeamOwnerA with team Apps") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Apps",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam("user1Name", "Apps", backendClient)
            backendSetupHelper.userEnablesAppsForTeam("user1Name", "Apps", backendClient)
            backendSetupHelper.userAddsAppAsTeamCollaborator(
                "user1Name",
                "Apps",
                pollAppId,
                backendClient
            )
        }

        step("TeamOwnerA adds Member1 and Member2 to team Apps with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Apps",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("There is TeamOwnerB with team ConnectedFriend") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user4Name",
                "ConnectedFriend",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam("user4Name", "ConnectedFriend", backendClient)
            teamOwnerB = clientUserManager.findUserBy("user4Name", ClientUserManager.FindBy.NAME_ALIAS)
        }

        step("TeamOwnerA is connected to TeamOwnerB") {
            backendSetupHelper.userIsConnectedTo("user1Name", "user4Name")
        }

        step("MLS devices are registered for all participants") {
            listOf("user1Name", "user2Name", "user3Name", "user4Name").forEach { user ->
                testServiceHelper.addDevice(user, null, "Device1")
            }
        }

        step("TeamOwnerA creates MLS group conversation AppsConversation with all participants") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name,user3Name,user4Name",
                conversationName = "AppsConversation",
                deviceName = "Device1"
            )
        }

        step("TeamOwnerA is me") {
            teamOwnerA = clientUserManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
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
                clickContinueButtonOnBackendConfigSuccess()
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

        step("Then I tap on conversation name AppsConversation in conversation list") {
            pages.conversationListPage.apply {
                clickGroupConversation("AppsConversation")
            }
        }

        step("And I see group conversation AppsConversation is in foreground") {
            pages.conversationViewPage.apply {
                assertConversationScreenVisible()
            }
        }

        step("And I open the participants list and Apps tab") {
            pages.conversationViewPage.clickOnGroupConversationDetails("AppsConversation")
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapOnParticipantsTab()
                tapAddParticipantsButton()
                tapOnAppsTab()
            }
        }

        step("And I add Poll App Staging to AppsConversation") {
            pages.groupConversationDetailsPage.apply {
                assertAppInSearchResultsVisible("Poll App (Staging)")
                tapAppInSearchResults("Poll App (Staging)")
                tapAddToConversationButton()
            }
            waitUntilToastIsDisplayed("App added to conversation")
        }

        step("And I return to AppsConversation") {
            pages.groupConversationDetailsPage.apply {
                tapBackButton()
                tapBackButton()
                tapBackButton()
            }
        }

        step("Then I see a banner informing me that Guests and apps are present in the conversation view") {
            pages.conversationViewPage.assertGuestsAndAppsBannerVisible()
        }

        step("When Member1 sends message Hello fellow members to group conversation AppsConversation") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Hello fellow members",
                "Device1",
                "AppsConversation"
            )
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

        step("When TeamOwner toggles thumbs up reaction on the recent message from AppsConversation via Device1") {
            testServiceHelper.userTogglesReactionOnLatestMessage(
                "user1Name",
                "AppsConversation",
                "Device1",
                "\uD83D\uDC4D" // 👍
            )
        }

        step("Then I see a thumbs up reaction from 1 user as reaction to TeamOwnerA message") {
            pages.conversationViewPage.apply {
                assertReactionAndUserCountVisible("\uD83D\uDC4D", 1) // 👍
            }
        }

        step("When I tap on group conversation title AppsConversation to open group details") {
            pages.conversationViewPage.apply {
                clickOnGroupConversationDetails("AppsConversation")
            }
        }

        step("And I tap on Participants tab") {
            pages.groupConversationDetailsPage.apply {
                tapOnParticipantsTab()
            }
        }

        step("Then I see Member2 in participants list") {
            val member2 = clientUserManager.findUserBy("user3Name", ClientUserManager.FindBy.NAME_ALIAS)
            pages.groupConversationDetailsPage.apply {
                assertUsernameIsAddedToParticipantsList(member2.name ?: "")
            }
        }

        step("And TeamOwnerA removes TeamOwnerB from group conversation AppsConversation") {
            pages.groupConversationDetailsPage.tapUserInParticipantsList(teamOwnerB.name ?: "")
            pages.connectedUserProfilePage.apply {
                tapRemoveFromConversationButtonForParticipant()
                tapRemoveConversationButtonOnModal()
                tapCloseButtonOnConnectedUserProfilePage()
            }
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
        }

        step("Then I see system message You removed TeamOwnerB from the conversation in conversation view") {
            iSeeSystemMessage("You removed ${teamOwnerB.name ?: ""} from the conversation")
        }

        step("When I tap on group conversation title AppsConversation to open group details") {
            pages.conversationViewPage.apply {
                clickOnGroupConversationDetails("AppsConversation")
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

        step("And I see Poll App Staging in participants list") {
            pages.groupConversationDetailsPage.apply {
                assertUsernameIsAddedToParticipantsList("Poll App (Staging)")
            }
        }

        step("And I tap on Poll App Staging in participants list") {
            pages.groupConversationDetailsPage.apply {
                tapUserInParticipantsList("Poll App (Staging)")
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

        step("Then I do not see Poll App Staging in participants list") {
            pages.groupConversationDetailsPage.apply {
                assertUserIsNotInParticipantsList("Poll App (Staging)")
            }
        }

        step("When I close the group conversation details through X icon") {
            pages.groupConversationDetailsPage.apply {
                tapCloseButtonOnGroupConversationDetailsPage()
            }
        }

        step("Then I see system message You removed Poll App Staging from the conversation in conversation view") {
            iSeeSystemMessage("You removed Poll App (Staging) from the conversation")
        }
    }
}
