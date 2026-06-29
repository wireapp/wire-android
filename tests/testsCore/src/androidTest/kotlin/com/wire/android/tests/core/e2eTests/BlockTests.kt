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
package com.wire.android.tests.core.e2eTests

import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.BackendClient
import backendUtils.team.TeamRoles
import backendUtils.team.updateUserProfileImage
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.UiWaitUtils
import user.utils.ClientUser
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@Suppress("LargeClass")
class BlockTests : BaseUiTest() {
    override val deletePersonalUsersAfterTest = true

    private lateinit var teamOwner: ClientUser
    private lateinit var teamOwnerB: ClientUser
    private lateinit var member1: ClientUser
    private lateinit var member: ClientUser
    private lateinit var contact1: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4246")
    @Category("regression", "RC", "blockUser")
    @Test
    fun givenTeamOwnerWhenViewingTeamMemberFromGroupDetails_thenBlockOptionIsNotVisible() {
        step("Given there is TeamOwner with team Blocking on Staging backend") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user1Name",
                "Blocking",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Blocking with role Member") {
            teamHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Blocking",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has 1:1 conversation with Member1 in team Blocking") {
            testServiceHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Blocking"
            )
        }

        step("And User TeamOwner is me") {
            teamOwner = teamHelper.usersManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User Member1 is available for 1:1 conversation checks") {
            member1 = teamHelper.usersManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link and login as TeamOwner") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
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

        step("Then I see conversation Member1 in conversation list and tap on it") {
            pages.conversationListPage.apply {
                assertConversationVisible(member1.name ?: "")
                tapConversationNameInConversationList(member1.name ?: "")
            }
        }

        step("And I open conversation details for 1:1 conversation with Member1") {
            UiWaitUtils.waitFor(1.seconds)
            pages.conversationViewPage.click1On1ConversationDetails(member1.name ?: "")
        }

        step("When I tap show more options button on user profile screen") {
            pages.connectedUserProfilePage.clickShowMoreOptions()
        }

        step("Then I do not see Block option") {
            pages.connectedUserProfilePage.assertBlockOptionNotVisible()
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4247", "TC-4248", "TC-4252", "TC-4253")
    @Category("regression", "RC", "blockUser", "unblockUser")
    @Test
    fun givenConnectedGuestUser_whenBlockingAndUnblockingFromUserProfile_thenUserIsBlockedAndUnblockedSuccessfully() {
        step("Given there are 2 personal users on Staging backend") {
            teamHelper.usersManager.createXPersonalUsers(2, backendClient)
            member = teamHelper.usersManager.findUserByNameOrNameAlias("user1Name")
            contact1 = teamHelper.usersManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And User Member is me") {
            teamHelper.usersManager.setSelfUser(member)
        }

        step("And Personal user Member sets profile image and unique username") {
            backendClient.updateUserProfileImage(member, context)
            runBlocking {
                testServiceHelper.usersSetUniqueUsername("user1Name")
            }
        }

        step("And User Contact1 is connected to Member") {
            testServiceHelper.userIsConnectedTo("user2Name", "user1Name")
        }

        step("And Personal user Contact1 sets profile image and unique username") {
            backendClient.updateUserProfileImage(contact1, context)
            runBlocking {
                testServiceHelper.usersSetUniqueUsername("user2Name")
            }
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link and login as Member") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterTeamMemberLoggingEmail(member.email ?: "")
                clickLoginButton()
                enterTeamMemberLoggingPassword(member.password ?: "")
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

        step("And I see conversation Contact1 in conversation list") {
            pages.conversationListPage.apply {
                assertConversationVisible(contact1.name ?: "")
            }
        }

        step("And I tap on conversation name Contact1 in conversation list") {
            pages.conversationListPage.apply {
                tapConversationNameInConversationList(contact1.name ?: "")
            }
        }

        step("Then I see conversation view with Contact1 is in foreground") {
            pages.conversationViewPage.apply {
                assertConversationScreenVisible()
                assertConversationIsVisibleWithTeamOwner(contact1.name ?: "")
            }
        }

        step("When I open conversation details for 1:1 conversation with Contact1 and tap show more options button") {
            pages.conversationViewPage.apply {
                click1On1ConversationDetails(contact1.name ?: "")
            }
            pages.connectedUserProfilePage.apply {
                clickShowMoreOptions()
            }
        }

        step("And I tap on Block option and Block button on alert") {
            pages.connectedUserProfilePage.apply {
                clickBlockOption()
                clickBlockButtonAlert()
            }
        }

        step("Then I see toast message Contact1 blocked in user profile screen") {
            pages.connectedUserProfilePage.apply {
                assertToastMessageIsDisplayed("${contact1.name ?: ""} blocked")
            }
        }

        step("And I see Blocked label") {
            pages.connectedUserProfilePage.apply {
                assertBlockedLabelVisible()
            }
        }

        step("And I see Unblock User button") {
            pages.connectedUserProfilePage.apply {
                assertUnblockUserButtonVisible()
            }
        }

        // TC-4248 I want to be able to unblock a guest user from group details through the unblock button
        step("When I tap Unblock User button and Unblock button alert") {
            pages.connectedUserProfilePage.apply {
                clickUnblockUserButton()
                clickUnblockButtonAlert()
            }
        }

        step("Then I do not see Blocked label and Unblock User button") {
            pages.connectedUserProfilePage.apply {
                assertBlockedLabelNotVisible()
                assertUnblockUserButtonNotVisible()
            }
        }

        step("When I tap show more options button on user profile screen and block Contact1 again") {
            pages.connectedUserProfilePage.apply {
                clickShowMoreOptions()
                clickBlockOption()
                clickBlockButtonAlert()
            }
        }

        // TC-4252 - I want to see a blocked label on conversation list for a blocked user
        step("Then I see toast message Contact1 blocked in user profile screen and Blocked label") {
            pages.connectedUserProfilePage.apply {
                assertToastMessageIsDisplayed("${contact1.name ?: ""} blocked")
                assertBlockedLabelVisible()
            }
        }

        // TC-4253 I want to be able to unblock a guest user from group details through the unblock option in the context menu
        step("When I tap show more options button on user profile screen and unblock Contact1 from context menu") {
            pages.connectedUserProfilePage.apply {
                clickShowMoreOptions()
                clickUnblockOption()
                clickUnblockButtonAlert()
            }
        }

        step("Then I do not see Blocked label and Unblock User button after unblocking from context menu") {
            pages.connectedUserProfilePage.apply {
                assertBlockedLabelNotVisible()
                assertUnblockUserButtonNotVisible()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4249")
    @Category("regression", "RC", "blockUser")
    @Test
    fun givenTeamOwner_whenViewingTeamMemberFromConversationList_thenBlockOptionIsNotVisible() {
        step("Given there is TeamOwner with team Blocking on Staging backend") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user1Name",
                "Blocking",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds user Member1 to team Blocking with role Member") {
            teamHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Blocking",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has 1:1 conversation with Member1 in team Blocking") {
            testServiceHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Blocking"
            )
        }

        step("And User TeamOwner is me") {
            teamOwner = teamHelper.usersManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User Member1 is available for conversation list checks") {
            member1 = teamHelper.usersManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link and login as TeamOwner") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
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

        step("And I see conversation Member1 in conversation list") {
            pages.conversationListPage.apply {
                assertConversationVisible(member1.name ?: "")
            }
        }

        step("When I long tap on conversation name Member1 in conversation list") {
            pages.conversationListPage.apply {
                longPressConversation(member1.name ?: "")
            }
        }

        step("Then I do not see Block option on conversation list") {
            pages.conversationListPage.apply {
                assertBlockOptionNotVisibleInConversationActions()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4250", "TC-4251")
    @Category("regression", "RC", "blockUser", "unblockUser")
    @Test
    fun givenConnectedGuestUser_whenBlockingAndUnblockingFromConversationList_thenUserIsBlockedAndUnblockedSuccessfully() {
        step("Given there are 2 personal users on Staging backend") {
            teamHelper.usersManager.createXPersonalUsers(2, backendClient)
            member = teamHelper.usersManager.findUserByNameOrNameAlias("user1Name")
            contact1 = teamHelper.usersManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And User Member is me") {
            teamHelper.usersManager.setSelfUser(member)
        }

        step("And Personal user Member sets profile image and unique username") {
            backendClient.updateUserProfileImage(member, context)
            runBlocking {
                testServiceHelper.usersSetUniqueUsername("user1Name")
            }
        }

        step("And User Contact1 is connected to Member") {
            testServiceHelper.userIsConnectedTo("user2Name", "user1Name")
        }

        step("And Personal user Contact1 sets profile image and unique username") {
            backendClient.updateUserProfileImage(contact1, context)
            runBlocking {
                testServiceHelper.usersSetUniqueUsername("user2Name")
            }
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link and login as Member") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterTeamMemberLoggingEmail(member.email ?: "")
                clickLoginButton()
                enterTeamMemberLoggingPassword(member.password ?: "")
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

        step("Then I see conversation Contact1 in conversation list") {
            pages.conversationListPage.apply {
                assertConversationVisible(contact1.name ?: "")
            }
        }

        step("When I long tap on conversation name Contact1 in conversation list") {
            pages.conversationListPage.apply {
                longPressConversation(contact1.name ?: "")
            }
        }

        step("And I tap Block option and Block confirm button on conversation list") {
            pages.conversationListPage.apply {
                tapBlockOptionOnConversationList()
                tapBlockConfirmButtonOnConversationList()
            }
        }

        step("Then I see Contact1 blocked toast message on conversation list") {
            pages.conversationListPage.apply {
                assertToastMessageIsDisplayedOnConversationList("${contact1.name ?: ""} blocked")
            }
        }

        step("And I see user Contact1 is having the Blocked label on conversation list") {
            pages.conversationListPage.apply {
                assertConversationVisible(contact1.name ?: "")
                assertBlockedLabelVisibleInConversationList()
            }
        }

        // TC-4251 I want to be able to unblock a guest user from conversation list
        step("When I long tap on conversation name Contact1 in conversation list") {
            pages.conversationListPage.apply {
                longPressConversation(contact1.name ?: "")
            }
        }

        step("And I tap Unblock option and Unblock confirm button on conversation list") {
            pages.conversationListPage.apply {
                tapUnblockOptionOnConversationList()
                tapUnblockConfirmButtonOnConversationList()
            }
        }

        step("Then I do not see user Contact1 is having the Blocked label on conversation list") {
            pages.conversationListPage.apply {
                assertConversationVisible(contact1.name ?: "")
                assertBlockedLabelNotVisibleInConversationList()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4254", "TC-4255")
    @Category("regression", "RC", "blockUser", "unblockUser")
    @Test
    fun givenConnectedTeamUserFromAnotherTeam_whenBlockingAndUnblockingFromUserProfile_thenUserIsBlockedAndUnblockedSuccessfully() {
        step("Given there is TeamOwnerA with team Blocking on Staging backend") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user1Name",
                "Blocking",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And there is TeamOwnerB with team ToBeBlocked on Staging backend") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user2Name",
                "ToBeBlocked",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwnerA is connected to TeamOwnerB") {
            testServiceHelper.userIsConnectedTo("user1Name", "user2Name")
        }

        step("And User TeamOwnerA has 1:1 conversation with TeamOwnerB in team Blocking") {
            testServiceHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Blocking"
            )
        }

        step("And User TeamOwnerA is me") {
            teamOwner = teamHelper.usersManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User TeamOwnerB is available for 1:1 conversation checks") {
            teamOwnerB = teamHelper.usersManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link and login as TeamOwnerA") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
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

        step("Then I see conversation TeamOwnerB in conversation list and tap on it") {
            pages.conversationListPage.apply {
                assertConversationVisible(teamOwnerB.name ?: "")
                tapConversationNameInConversationList(teamOwnerB.name ?: "")
            }
        }

        step("And I open conversation details for 1:1 conversation with TeamOwnerB") {
            UiWaitUtils.waitFor(1.seconds)
            pages.conversationViewPage.click1On1ConversationDetails(teamOwnerB.name ?: "")
        }

        step("When I tap show more options button on user profile screen") {
            pages.connectedUserProfilePage.clickShowMoreOptions()
        }

        step("And I tap on Block option and Block button on alert") {
            pages.connectedUserProfilePage.apply {
                clickBlockOption()
                clickBlockButtonAlert()
            }
        }

        step("Then I see toast message TeamOwnerB blocked in user profile screen") {
            pages.connectedUserProfilePage.apply {
                assertToastMessageIsDisplayed("${teamOwnerB.name ?: ""} blocked")
            }
        }

        step("And I see Blocked label") {
            pages.connectedUserProfilePage.apply {
                assertBlockedLabelVisible()
            }
        }

        step("And I see Unblock User button") {
            pages.connectedUserProfilePage.apply {
                assertUnblockUserButtonVisible()
            }
        }

        // TC-4255 I want to be able to unblock a guest user from group details through the unblock button
        step("When I tap Unblock User button and Unblock button alert") {
            pages.connectedUserProfilePage.apply {
                clickUnblockUserButton()
                clickUnblockButtonAlert()
            }
        }

        step("Then I do not see Blocked label and Unblock User button") {
            pages.connectedUserProfilePage.apply {
                assertBlockedLabelNotVisible()
                assertUnblockUserButtonNotVisible()
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4256", "TC-4257")
    @Category("regression", "RC", "blockUser", "unblockUser")
    @Test
    fun givenConnectedTeamUserFromAnotherTeam_whenBlockingAndUnblockingFromConversationList_thenUserIsBlockedAndUnblockedSuccessfully() {
        step("Given there is TeamOwnerA with team Blocking on Staging backend") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user1Name",
                "Blocking",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And there is TeamOwnerB with team ToBeBlocked on Staging backend") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                "user2Name",
                "ToBeBlocked",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwnerA is connected to TeamOwnerB") {
            testServiceHelper.userIsConnectedTo("user1Name", "user2Name")
        }

        step("And User TeamOwnerA has 1:1 conversation with TeamOwnerB in team Blocking") {
            testServiceHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Blocking"
            )
        }

        step("And User TeamOwnerA is me") {
            teamOwner = teamHelper.usersManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User TeamOwnerB is available for conversation list checks") {
            teamOwnerB = teamHelper.usersManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link and login as TeamOwnerA") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
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

        step("Then I see conversation TeamOwnerB in conversation list") {
            pages.conversationListPage.apply {
                assertConversationVisible(teamOwnerB.name ?: "")
            }
        }

        step("When I long tap on conversation name TeamOwnerB in conversation list") {
            pages.conversationListPage.apply {
                longPressConversation(teamOwnerB.name ?: "")
            }
        }

        step("And I tap Block option and Block confirm button on conversation list") {
            pages.conversationListPage.apply {
                tapBlockOptionOnConversationList()
                tapBlockConfirmButtonOnConversationList()
            }
        }

        step("Then I see TeamOwnerB blocked toast message on conversation list") {
            pages.conversationListPage.apply {
                assertToastMessageIsDisplayedOnConversationList("${teamOwnerB.name ?: ""} blocked")
            }
        }

        step("And I see user TeamOwnerB is having the Blocked label on conversation list") {
            pages.conversationListPage.apply {
                assertConversationVisible(teamOwnerB.name ?: "")
                assertBlockedLabelVisibleInConversationList()
            }
        }

        // TC-4257 I want to be able to unblock a guest user from conversation list
        step("When I long tap on conversation name TeamOwnerB in conversation list") {
            pages.conversationListPage.apply {
                longPressConversation(teamOwnerB.name ?: "")
            }
        }

        step("And I tap Unblock option and Unblock confirm button on conversation list") {
            pages.conversationListPage.apply {
                tapUnblockOptionOnConversationList()
                tapUnblockConfirmButtonOnConversationList()
            }
        }

        step("Then I do not see user TeamOwnerB is having the Blocked label on conversation list") {
            pages.conversationListPage.apply {
                assertConversationVisible(teamOwnerB.name ?: "")
                assertBlockedLabelNotVisibleInConversationList()
            }
        }
    }
}
