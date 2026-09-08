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
import uiautomatorutils.UiWaitUtils.waitUntilToastIsDisplayed
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class GroupDetailsTests : BaseUiTest() {
    private lateinit var teamOwner: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4344")
    @Category("regression", "RC", "groups", "groupDetails")
    @Test
    fun givenIAmATeamOwner_whenChangingGuestAndServicesStates_thenGuestAndServicesStatesAreUpdated() {
        step("Given There is a team owner user1Name with team GroupDeletion and members user2Name,user3Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "GroupDeletion",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "GroupDeletion",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User user1Name has group conversation MyTeam with user2Name,user3Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name",
                "GroupDeletion"
            )
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

        step("And I see and open group conversation MyTeam") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                clickGroupConversation("MyTeam")
            }
        }

        step("When I open MyTeam group details") {
            pages.conversationViewPage.clickOnGroupConversationDetails("MyTeam")
            pages.groupConversationDetailsPage.assertGroupDetailsPageVisible()
        }

        step("Then I see Guests and Apps options are ON") {
            pages.groupConversationDetailsPage.apply {
                assertGuestOptionsState("ON")
                assertAppsOptionsState("ON")
            }
        }

        step("When I open Guests options and disable guest access") {
            pages.groupConversationDetailsPage.tapGuestOptions()
            pages.groupAccessOptionsPage.apply {
                assertGuestSwitchState("ON")
                tapGuestSwitch()
                tapDisableButton()
            }
        }

        step("Then I see the Guests switch is OFF") {
            pages.groupAccessOptionsPage.assertGuestSwitchState("OFF")
        }

        step("And I return to conversation details") {
            pages.groupAccessOptionsPage.tapBackButton()
            pages.groupConversationDetailsPage.assertGroupDetailsPageVisible()
        }

        step("When I open Apps options and disable app access") {
            pages.groupConversationDetailsPage.apply {
                assertAppsOptionsState("ON")
                tapAppsOptions()
            }
            pages.groupAccessOptionsPage.apply {
                assertAppsSwitchState("ON")
                tapAppsSwitch()
                tapDisableButton()
            }
        }

        step("Then I see the Apps switch is OFF") {
            pages.groupAccessOptionsPage.assertAppsSwitchState("OFF")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4353")
    @Category("regression", "RC", "groups", "groupDetails")
    @Test
    fun givenIAmATeamOwner_whenChangingGroupName_thenGroupNameIsUpdated() {
        step("Given There is a team owner user1Name with team GroupDeletion and members user2Name,user3Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "GroupDeletion",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "GroupDeletion",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User user1Name has group conversation GroupName with user2Name,user3Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "GroupName",
                "user2Name,user3Name",
                "GroupDeletion"
            )
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

        step("And I see and open group conversation GroupName") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("GroupName")
                clickGroupConversation("GroupName")
            }
        }

        step("And I open GroupName group details") {
            pages.conversationViewPage.clickOnGroupConversationDetails("GroupName")
            pages.groupConversationDetailsPage.assertGroupDetailsPageVisible()
        }

        step("And I see and tap GroupName as group name") {
            pages.groupConversationDetailsPage.apply {
                assertGroupNameVisible("GroupName")
                tapOnGroupName("GroupName")
            }
        }

        step("When I change the group name to NewGroupName") {
            pages.groupConversationDetailsPage.changeGroupName("NewGroupName")
        }

        step("Then I see NewGroupName and Conversation renamed toast message") {
            pages.groupConversationDetailsPage.assertGroupNameVisible("NewGroupName")
            waitUntilToastIsDisplayed("Conversation renamed")
        }

        step("And I close group details and see rename system message") {
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
            pages.conversationViewPage.assertSystemMessageVisible("You renamed the conversation")
        }
    }
}
