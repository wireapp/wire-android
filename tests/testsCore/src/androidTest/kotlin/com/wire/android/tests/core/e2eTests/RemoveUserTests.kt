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
import backendUtils.team.deleteTeamMember
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.KeyboardUtils.closeKeyboardIfOpened
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class RemoveUserTests : BaseUiTest() {

    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser
    private lateinit var member2: ClientUser
    private lateinit var member3: ClientUser
    private lateinit var guest: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4494")
    @Category("regression", "RC", "removeUser")
    @Test
    fun givenOneOnOneConversation_whenUserIsRemovedFromTeam_thenConversationIsRemovedAfterRelogin() {
        step("Given There is a team owner TeamOwner with team RemoveUser") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "RemoveUser",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 and Member2 to team RemoveUser with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "RemoveUser",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User Member1 is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            member2 = clientUserManager.findUserByNameOrNameAlias("user3Name")
            clientUserManager.setSelfUser(member1)
        }

        step("And I see Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And User Member1 has 1:1 conversation with Member2 in team RemoveUser") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user2Name",
                "user3Name",
                "RemoveUser"
            )
        }

        step("And User Member1 has 1:1 conversation with TeamOwner in team RemoveUser") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user2Name",
                "user1Name",
                "RemoveUser"
            )
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        loginToStagingAs(member1)

        step("And I see conversations Member2 and TeamOwner in conversation list") {
            pages.conversationListPage.apply {
                assertConversationVisible(member2.name ?: "")
                assertConversationVisible(teamOwner.name ?: "")
            }
        }

        step("When I tap on conversation name Member2 in conversation list") {
            pages.conversationListPage.tapConversationNameInConversationList(member2.name ?: "")
        }

        step("And Member2 sends message Hello! and I see it in the current conversation") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user3Name",
                "Hello!",
                "Device1",
                "user2Name"
            )
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("And I send message Hello to you, too! and see it in the current conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello to you, too!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello to you, too!")
            }
        }

        step("And I hide the keyboard") {
            closeKeyboardIfOpened()
        }

        step("When User TeamOwner removes user Member2 from team RemoveUser") {
            teamOwner.deleteTeamMember(
                backendClient,
                member2.id ?: error("Member2 has no backend user ID.")
            )
        }

        step("Then I see system message This user is no longer available in conversation view") {
            pages.conversationViewPage.assertSystemMessageVisible("This user is no longer available")
        }

        step("And I close the conversation view through the back arrow") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("Then I see Member2 with deleted status and TeamOwner in conversation list") {
            pages.conversationListPage.apply {
                assertConversationVisible(member2.name ?: "")
                assertConversationDeletedStatusVisible(member2.name ?: "")
                assertConversationVisible(teamOwner.name ?: "")
            }
        }

        step("When I open User Profile and tap Log out") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                tapLogoutButton()
            }
        }

        step("And I select the clear-data option and confirm logout") {
            pages.selfUserProfilePage.apply {
                iSeeClearDataOnLogOutAlert()
                iSeeInfoTextCheckbox("Delete all your personal information and conversations on this device")
                tapInfoTextCheckbox()
                tapLogoutButton()
            }
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        loginToStagingAs(member1)

        step("Then I do not see conversation Member2 in conversation list") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertConversationNotVisible(member2.name ?: "")
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4369")
    @Category("regression", "RC", "groups", "removeUser")
    @Test
    fun givenIAmLoggedInAsGroupGuest_whenAnotherMemberIsRemovedFromTeam_thenISeeTheyLeftTheConversation() {
        step("Given There is a team owner TeamOwner with team RemoveGroup") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "RemoveGroup",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1, Member2 and Member3 to team RemoveGroup with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name,user4Name",
                "RemoveGroup",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And There is a personal user User") {
            clientUserManager.createPersonalUsersByAliases(listOf("user5Name"), backendClient)
        }

        step("And User User has a unique username") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user5Name")
            }
        }

        step("And User TeamOwner is connected to User") {
            backendSetupHelper.userIsConnectedTo("user1Name", "user5Name")
        }

        step("And User TeamOwner has group conversation MyTeam with Member1, Member2, Member3 and User") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name,user4Name,user5Name",
                "RemoveGroup"
            )
        }

        step("And User User is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member3 = clientUserManager.findUserByNameOrNameAlias("user4Name")
            guest = clientUserManager.findUserByNameOrNameAlias("user5Name")
            clientUserManager.setSelfUser(guest)
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        loginToStagingAs(guest)

        step("And I see and open group conversation MyTeam in the foreground") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                clickGroupConversation("MyTeam")
            }
            pages.conversationViewPage.assertGroupConversationInForeground("MyTeam")
        }

        step("When User TeamOwner removes user Member3 from team RemoveGroup") {
            teamOwner.deleteTeamMember(
                backendClient,
                member3.id ?: error("Member3 has no backend user ID.")
            )
        }

        step("Then I see system message Member3 left the conversation") {
            pages.conversationViewPage.assertSystemMessageVisible(
                "${member3.name ?: ""} left the conversation"
            )
        }
    }

    @TestCaseId("TC-4363")
    @Category("regression", "RC", "groups", "removeUser")
    @Test
    fun givenIAmLoggedInAsTeamOwner_whenAnotherMemberIsRemovedFromTeam_thenISeeTheyWereRemovedFromTeam() {
        step("Given There is a team owner TeamOwner with team RemoveGroup") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "RemoveGroup",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1, Member2 and Member3 to team RemoveGroup with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name,user4Name",
                "RemoveGroup",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation MyTeam with Member1, Member2 and Member3") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name,user4Name",
                "RemoveGroup"
            )
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member3 = clientUserManager.findUserByNameOrNameAlias("user4Name")
            clientUserManager.setSelfUser(teamOwner)
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        loginToStagingAs(teamOwner)

        step("And I see and open group conversation MyTeam in the foreground") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                clickGroupConversation("MyTeam")
            }
            pages.conversationViewPage.assertGroupConversationInForeground("MyTeam")
        }

        step("When User TeamOwner removes user Member3 from team RemoveGroup") {
            teamOwner.deleteTeamMember(
                backendClient,
                member3.id ?: error("Member3 has no backend user ID.")
            )
        }

        step("Then I see system message Member3 was removed from the team") {
            pages.conversationViewPage.assertSystemMessageVisible(
                "${member3.name ?: ""} was removed from the team"
            )
        }
    }

    @TestCaseId("TC-4365")
    @Category("regression", "RC", "groups", "removeUser")
    @Test
    fun givenIAmLoggedInAsTeamMember_whenAnotherMemberIsRemovedFromTeam_thenISeeTheyWereRemovedFromTeam() {
        step("Given There is a team owner TeamOwner with team RemoveGroup") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "RemoveGroup",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1, Member2 and Member3 to team RemoveGroup with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name,user4Name",
                "RemoveGroup",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation MyTeam with Member1, Member2 and Member3") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name,user4Name",
                "RemoveGroup"
            )
        }

        step("And User Member1 is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            member3 = clientUserManager.findUserByNameOrNameAlias("user4Name")
            clientUserManager.setSelfUser(member1)
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        loginToStagingAs(member1)

        step("And I see and open group conversation MyTeam in the foreground") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                clickGroupConversation("MyTeam")
            }
            pages.conversationViewPage.assertGroupConversationInForeground("MyTeam")
        }

        step("When User TeamOwner removes user Member3 from team RemoveGroup") {
            teamOwner.deleteTeamMember(
                backendClient,
                member3.id ?: error("Member3 has no backend user ID.")
            )
        }

        step("Then I see system message Member3 was removed from the team") {
            pages.conversationViewPage.assertSystemMessageVisible(
                "${member3.name ?: ""} was removed from the team"
            )
        }
    }

    // Keeps the repeated staging login flow consistent across remove-user scenarios.
    private fun loginToStagingAs(user: ClientUser) {
        step("And I open staging backend deep link") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("And I enter a valid email and password to sign in") {
            pages.loginPage.apply {
                enterUserIdentifier(user.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterUserPassword(user.password ?: "")
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
    }
}
