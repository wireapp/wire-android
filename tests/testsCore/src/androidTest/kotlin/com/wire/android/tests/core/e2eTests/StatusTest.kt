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
@file:Suppress("MaximumLineLength", "MaxLineLength")

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
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class StatusTest : BaseUiTest() {

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

    @TestCaseId("TC-8413")
    @Category("status", "regression", "RC")
    @Test
    fun givenTeamMember_whenChangingStatusToBusy_thenProfileAndConversationListShowStatus() {
        prepareTeamMember()
        loginCurrentUser()
        openSelfUserProfile()

        changeStatusAndAssertConversationListIndicator(
            currentStatus = STATUS_NONE,
            newStatus = STATUS_BUSY,
            rationale = BUSY_RATIONALE
        )
    }

    @TestCaseId("TC-8412")
    @Category("status", "regression", "RC")
    @Test
    fun givenTeamMember_whenChangingStatusToAway_thenProfileAndConversationListShowStatus() {
        prepareTeamMember()
        loginCurrentUser()
        openSelfUserProfile()

        changeStatusAndAssertConversationListIndicator(
            currentStatus = STATUS_NONE,
            newStatus = STATUS_AWAY,
            rationale = AWAY_RATIONALE
        )
    }

    @TestCaseId("TC-8414")
    @Category("status", "regression", "RC")
    @Test
    fun givenTeamMember_whenChangingStatusToAvailable_thenProfileAndConversationListShowStatus() {
        prepareTeamMember()
        loginCurrentUser()
        openSelfUserProfile()

        changeStatusAndAssertConversationListIndicator(
            currentStatus = STATUS_NONE,
            newStatus = STATUS_AVAILABLE,
            rationale = AVAILABLE_RATIONALE
        )
    }

    @Ignore(
        "Blocked: remote availability scenarios need a high-level TestServiceHelper wrapper around " +
                "TestService.setAvailability plus stable status-indicator assertions in conversation list, " +
                "conversation title, group messages, and participants list."
    )
    @TestCaseId("TC-8415", "TC-8416", "TC-8417", "TC-8426")
    @Category("status", "regression", "RC")
    @Test
    fun givenOtherUserChangedAvailability_whenViewingConversations_thenTheirStatusIndicatorIsDisplayed() {
        // Mapping-only test for the legacy multi-surface remote availability scenario.
    }

    private fun prepareTeamMember() {
        step("Prepare backend team owner and member") {
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
                TEAM_MEMBER_ALIAS,
                TEAM_NAME,
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            currentUser = teamHelper.usersManager.findUserBy(TEAM_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
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

    private fun openSelfUserProfile() {
        step("Open self user profile") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                assertChangeStatusOptionsVisible()
            }
        }
    }

    private fun changeStatusAndAssertConversationListIndicator(
        currentStatus: String,
        newStatus: String,
        rationale: String
    ) {
        step("Change status from '$currentStatus' to '$newStatus'") {
            pages.selfUserProfilePage.apply {
                changeStatusFromTo(currentStatus, newStatus)
                assertStatusRationaleTextIsDisplayed(rationale)
                tapOkButtonOnStatusDialog()
                assertStatusIs(newStatus)
                tapCloseButton()
            }
        }

        step("Assert conversation list shows self status indicator") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertSelfStatusIconDisplayed(newStatus)
            }
        }
    }

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val TEAM_NAME = "Status"

        const val STATUS_AVAILABLE = "Available"
        const val STATUS_BUSY = "Busy"
        const val STATUS_AWAY = "Away"
        const val STATUS_NONE = "None"

        const val AVAILABLE_RATIONALE =
            "You will appear as Available to other people. You will receive notifications for incoming calls and for messages " +
                    "according to the Notifications setting in each conversation."
        const val BUSY_RATIONALE =
            "You will appear as Busy to other people. You will only receive notifications for mentions, replies, and calls in conversations " +
                    "that are not muted."
        const val AWAY_RATIONALE =
            "You will appear as Away to other people. You will not receive notifications about any incoming calls or messages."
    }
}
