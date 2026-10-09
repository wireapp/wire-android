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
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class PollMessagesTests : BaseUiTest() {
    private lateinit var teamOwner: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4472", "TC-4473")
    @Category("regression", "RC", "polls")
    @Test
    fun givenGroupConversation_whenIReceivePollAndVote_thenPollIsDisplayedAndVoteIsSelected() {
        preparePollConversationAndLogin()

        step("When Member1 sends poll What is your favorite animal? with Cat and Dog buttons to Polls") {
            testServiceHelper.userSendsPollMessageToConversation(
                senderAlias = "user2Name",
                pollMessage = "What is your favorite animal?",
                title = "Question",
                buttons = "Cat,Dog",
                deviceName = "Device1",
                conversationName = "Polls"
            )
        }

        step("Then I see poll What is your favorite animal? with Cat and Dog buttons") {
            pages.conversationViewPage.apply {
                assertPollMessageVisible("What is your favorite animal?")
                assertPollButtonVisible("Cat")
                assertPollButtonVisible("Dog")
            }
        }

        step("When I tap Cat in the poll") {
            pages.conversationViewPage.tapPollButton("Cat")
        }

        step("And Member1 confirms my Cat vote on the latest poll in Polls") {
            testServiceHelper.userSendsButtonActionConfirmationToLatestPollMessage(
                senderAlias = "user2Name",
                receiverAlias = "user1Name",
                deviceName = "Device1",
                conversationName = "Polls",
                buttonText = "Cat"
            )
        }

        step("Then I see Cat is selected in the poll") {
            pages.conversationViewPage.assertPollButtonSelected("Cat")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4474")
    @Category("regression", "RC", "polls")
    @Test
    fun givenIAlreadyVotedInGroupConversationPoll_whenISelectAnotherOption_thenMyVoteIsChanged() {
        preparePollConversationAndLogin()

        step("When Member1 sends poll What is your favorite animal? with Cat and Dog buttons to Polls") {
            testServiceHelper.userSendsPollMessageToConversation(
                senderAlias = "user2Name",
                pollMessage = "What is your favorite animal?",
                title = "Question",
                buttons = "Cat,Dog",
                deviceName = "Device1",
                conversationName = "Polls"
            )
        }

        step("Then I see poll What is your favorite animal? with Cat and Dog buttons") {
            pages.conversationViewPage.apply {
                assertPollMessageVisible("What is your favorite animal?")
                assertPollButtonVisible("Cat")
                assertPollButtonVisible("Dog")
            }
        }

        step("When I tap Dog in the poll") {
            pages.conversationViewPage.tapPollButton("Dog")
        }

        step("And Member1 confirms my Dog vote on the latest poll in Polls") {
            testServiceHelper.userSendsButtonActionConfirmationToLatestPollMessage(
                senderAlias = "user2Name",
                receiverAlias = "user1Name",
                deviceName = "Device1",
                conversationName = "Polls",
                buttonText = "Dog"
            )
        }

        step("Then I see Dog is selected in the poll") {
            pages.conversationViewPage.assertPollButtonSelected("Dog")
        }

        step("When I tap Cat in the poll") {
            pages.conversationViewPage.tapPollButton("Cat")
        }

        step("And Member1 confirms my Cat vote on the latest poll in Polls") {
            testServiceHelper.userSendsButtonActionConfirmationToLatestPollMessage(
                senderAlias = "user2Name",
                receiverAlias = "user1Name",
                deviceName = "Device1",
                conversationName = "Polls",
                buttonText = "Cat"
            )
        }

        step("Then I see Cat is selected in the poll") {
            pages.conversationViewPage.assertPollButtonSelected("Cat")
        }
    }

    // Creates the shared Polls group setup, prepares Member1's device, and logs in as TeamOwner.
    @Suppress("LongMethod")
    private fun preparePollConversationAndLogin() {
        step("Given There is a team owner TeamOwner with team Polling") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Polling",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Polling with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Polling",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation Polls with Member1 in team Polling") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "Polls",
                "user2Name",
                "Polling"
            )
        }

        step("And User Member1 adds a new device Device1 with label Device1") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            clientUserManager.setSelfUser(teamOwner)
        }

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
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamOwnerLoggingPassword(teamOwner.password ?: "")
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

        step("And I see and open group conversation Polls") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("Polls")
                tapConversationNameInConversationList("Polls")
            }
        }
    }
}
