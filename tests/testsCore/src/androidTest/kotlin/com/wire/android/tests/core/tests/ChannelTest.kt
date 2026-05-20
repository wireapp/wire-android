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
package com.wire.android.tests.core.tests

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
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.iSeeSystemMessageContainingAll
import uiautomatorutils.UiWaitUtils.waitUntilToastIsDisplayed
import user.utils.ClientUser
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class ChannelTest : BaseUiTest() {
    private val pages: AllPages by inject()
    private lateinit var device: UiDevice
    private lateinit var context: Context
    private lateinit var backendClient: BackendClient
    private lateinit var teamHelper: TeamHelper
    private lateinit var testServiceHelper: TestServiceHelper
    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser
    private lateinit var member2: ClientUser

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
         cleanupCreatedUsers(backendClient, teamHelper.usersManager)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8716") // "TC-8717"
    @Category("channels", "regression")
    @Test
    fun givenTeamMemberWithChannelFeatureEnabled_whenCreatingChannelWithTeammate_thenChannelConversationIsCreated() {
        step("There is TeamOwner with team ChannelCreation on Staging backend") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user1Name",
                "ChannelCreation",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("User TeamOwner configures MLS for team ChannelCreation") {
            teamHelper.userConfiguresMLSForTeam("user1Name", "ChannelCreation", backendClient)
        }

        step("TeamOwner enables channel feature for team ChannelCreation via backdoor") {
            teamHelper.userEnablesChannelFeatureForTeam("user1Name", "ChannelCreation", backendClient)
        }

        step("User TeamOwner adds users Member1,Member2 to team ChannelCreation with role Member") {
            teamHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "ChannelCreation",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("Register sender device and send connection request to receiver via backend") {
            testServiceHelper.apply {
                addDevice("user2Name", null, "Device1")
                addDevice("user3Name", null, "Device2")
            }
        }
        step("TeamOwner has channel conversation TestChannel in team ChannelCreation") {
            testServiceHelper.userHasChannelConversationInTeam(
                "user1Name",
                "TestChannel",
                "ChannelCreation"
            )
        }

        step("User TeamOwner is me") {
            teamOwner = teamHelper.usersManager.findUserByNameOrNameAlias("user1Name")
        }

        step("User Member1 and Member2 are available for channel participant selection") {
            member1 = teamHelper.usersManager.findUserByNameOrNameAlias("user2Name")
            member2 = teamHelper.usersManager.findUserByNameOrNameAlias("user3Name")
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

        step("And I see conversation TestChannel in conversation list") {
            pages.conversationListPage.apply {
                assertChannelConversationVisible("TestChannel")
            }
        }

        step("When I tap on conversation name TestChannel in conversation list") {
            pages.conversationListPage.apply {
                clickChannelConversation("TestChannel")
            }
        }

        step("Then I see channel conversation TestChannel is in foreground") {
            pages.conversationViewPage.apply {
                assertChannelConversationInForeground("TestChannel")
            }
        }

        step("And I tap on channel conversation title TestChannel to open group details") {
            pages.conversationViewPage.apply {
                UiWaitUtils.waitFor(1.seconds)
                clickOnChannelConversationDetails("TestChannel")
            }
        }

        step("And I open participants tab and start add participant flow") {
            pages.groupConversationDetailsPage.apply {
                tapOnParticipantsTab()
                tapAddParticipantsButton()
            }
        }

        step("And I select Member1 and Member2 from participant suggestions") {
            pages.groupConversationDetailsPage.apply {
                assertUsernameInSuggestionsListIs(member1.name ?: "")
                selectUserInSuggestionList(member1.name ?: "")
                assertUsernameInSuggestionsListIs(member2.name ?: "")
                selectUserInSuggestionList(member2.name ?: "")
                tapContinueButton()
            }
        }

        step("And I verify Member1 and Member2 are added to participants list and click close button") {
            pages.groupConversationDetailsPage.apply {
                assertUsernameIsAddedToParticipantsList(member1.name ?: "")
                assertUsernameIsAddedToParticipantsList(member2.name ?: "")
                tapCloseButtonOnGroupConversationDetailsPage()
            }
        }

        step("And I verify system message confirms Member1 and Member2 were added") {
            iSeeSystemMessageContainingAll(
                "You added",
                member1.name ?: "",
                member2.name ?: "",
                "to the conversation"
            )
        }

        step("And I see top of channel conversation message in conversation view") {
            pages.conversationViewPage.apply {
                assertTopOfConversationViewPageVisible()
            }
        }

        step("Then I see channel conversation TestChannel is in foreground") {
            pages.conversationViewPage.apply {
                assertChannelConversationInForeground("TestChannel")
            }
        }

        step("When I type the message Hello Channel Members into text input field and tap send button") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello Channel Members")
                clickSendButton()
            }
        }

        step("Then I see the message Hello Channel Members in current conversation") {
            pages.conversationViewPage.apply {
                assertSentMessageIsVisibleInCurrentConversation("Hello Channel Members")
            }
        }

        step("And User Member2 sends message Hello from Member2 to channel conversation TestChannel") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Hello from Member2",
                "Device2",
                "TestChannel",
                false
            )
        }

        step("And I see the message Hello from Member2 in current conversation") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Hello from Member2")
            }
        }

        step("And I tap back button on conversationViewPage back to conversation list page") {
            pages.conversationViewPage.apply {
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("Then I see conversation TestChannel in conversation list") {
            pages.conversationListPage.apply {
                assertChannelConversationVisible("TestChannel")
            }
        }

        step("And I long press on conversation name TestChannel in conversation list") {
            pages.conversationListPage.apply {
                longPressConversation("TestChannel")
            }
        }

        step("And I see Delete Conversation button and tap it") {
            pages.conversationListPage.apply {
                assertDeleteConversationButtonVisibleInConversationActions()
                tapDeleteConversationButtonInConversationActions()
            }
        }

        step("And I see remove conversation confirmation modal for TestChannel") {
            pages.conversationListPage.apply {
                assertRemoveConversationConfirmationModalVisible("TestChannel")
            }
        }

        step("And I tap Remove Conversation button in remove conversation confirmation modal") {
            pages.conversationListPage.apply {
                tapRemoveConversationButton()
            }
        }

        step("Then I see removed toast message for TestChannel") {
            waitUntilToastIsDisplayed("“TestChannel” removed")
        }

        step("Then I do not see conversation TestChannel in conversation list") {
            pages.conversationListPage.apply {
                assertConversationNotVisible("TestChannel")
            }
        }
    }
}
