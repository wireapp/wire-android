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
import backendUtils.BackendClient
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.iSeeSystemMessage
import uiautomatorutils.UiWaitUtils.waitUntilToastIsDisplayed
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class GroupDetailsTest : BaseUiTest() {

    private var currentUser: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @After
    fun tearDown() {
        runCatching { cleanupBackendClient(backendClient, currentUser) }
    }

    @TestCaseId("TC-4353")
    @Category("groups", "groupDetails", "regression", "RC")
    @Test
    fun givenTeamOwnerWithGroupConversation_whenChangingGroupName_thenGroupIsRenamed() {
        prepareGroupConversation(GROUP_CONVERSATION_RENAME)
        loginCurrentUser()
        openGroupDetails(GROUP_CONVERSATION_RENAME)

        step("Rename group conversation") {
            pages.groupConversationDetailsPage.apply {
                assertGroupNameVisible(GROUP_CONVERSATION_RENAME)
                tapOnGroupName(GROUP_CONVERSATION_RENAME)
                changeGroupName(RENAMED_GROUP_CONVERSATION)
                assertGroupNameVisible(RENAMED_GROUP_CONVERSATION)
            }
            waitUntilToastIsDisplayed("Conversation renamed")
        }

        step("Close details and verify rename system message") {
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
            iSeeSystemMessage("You renamed the conversation")
        }
    }

    @TestCaseId("TC-4344")
    @Category("groups", "groupDetails", "regression", "RC")
    @Test
    fun givenTeamOwnerWithGroupConversation_whenDisablingGuests_thenGuestAccessIsOff() {
        prepareGroupConversation(GROUP_CONVERSATION_GUESTS)
        loginCurrentUser()
        openGroupDetails(GROUP_CONVERSATION_GUESTS)

        step("Open guest access details and disable guests") {
            pages.groupConversationDetailsPage.apply {
                assertGuestsOptionStateIs(STATE_ON)
                tapGuestsOption()
                assertGuestsSwitchStateIs(STATE_ON)
                tapGuestsSwitch()
                tapDisableButtonOnGuestAccessDialog()
                assertGuestsSwitchStateIs(STATE_OFF)
                tapBackButton()
                assertGuestsOptionStateIs(STATE_OFF)
            }
        }
    }

    private fun prepareGroupConversation(groupName: String) {
        prepareTeamWithMembers()
        step("Team owner has group conversation '$groupName' with members") {
            testServiceHelper.userHasGroupConversationInTeam(
                TEAM_OWNER_ALIAS,
                groupName,
                "$TEAM_MEMBER_ALIAS,$EXTRA_MEMBER_ALIAS",
                TEAM_NAME
            )
        }
    }

    private fun prepareTeamWithMembers() {
        step("Prepare backend team owner and members") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                TEAM_OWNER_ALIAS,
                TEAM_NAME,
                "en_US",
                true,
                backendClient,
                context
            )
            teamHelper.userXAddsUsersToTeam(
                TEAM_OWNER_ALIAS,
                "$TEAM_MEMBER_ALIAS,$EXTRA_MEMBER_ALIAS",
                TEAM_NAME,
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            currentUser = teamHelper.usersManager.findUserBy(TEAM_OWNER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        }
    }

    private fun loginCurrentUser() {
        step("Login current user via staging deep link") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterTeamOwnerLoggingEmail(currentUser?.email.orEmpty())
                clickLoginButton()
                enterTeamOwnerLoggingPassword(currentUser?.password.orEmpty())
                clickLoginButton()
            }
        }

        step("Complete login flow") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    private fun openGroupDetails(groupName: String) {
        step("Open group details for '$groupName'") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible(groupName)
                clickGroupConversation(groupName)
            }
            pages.conversationViewPage.apply {
                assertConversationScreenVisible()
                UiWaitUtils.waitFor(1.seconds)
                clickOnGroupConversationDetails(groupName)
            }
            pages.groupConversationDetailsPage.assertGroupDetailsPageVisible()
        }
    }

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val EXTRA_MEMBER_ALIAS = "user3Name"
        const val TEAM_NAME = "GroupDetails"

        const val GROUP_CONVERSATION_RENAME = "GroupName"
        const val RENAMED_GROUP_CONVERSATION = "NewGroupName"
        const val GROUP_CONVERSATION_GUESTS = "MyTeam"

        const val STATE_ON = "ON"
        const val STATE_OFF = "OFF"
    }
}
