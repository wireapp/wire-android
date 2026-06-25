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
import org.junit.Test
import org.junit.runner.RunWith
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class SelfUserProfileTest : BaseUiTest() {

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

    @TestCaseId("TC-4533")
    @Category("selfUserProfile", "regression", "RC")
    @Test
    fun givenTeamOwner_whenChangingStatusToAvailable_thenStatusIsAvailable() {
        prepareTeamMember()
        loginCurrentUser()
        openSelfUserProfile()

        changeStatus(
            currentStatus = STATUS_NONE,
            newStatus = STATUS_AVAILABLE,
            rationale = AVAILABLE_RATIONALE
        )
    }

    @TestCaseId("TC-4534")
    @Category("selfUserProfile", "regression", "RC")
    @Test
    fun givenTeamOwner_whenChangingStatusToBusy_thenStatusIsBusy() {
        prepareTeamMember()
        loginCurrentUser()
        openSelfUserProfile()

        changeStatus(
            currentStatus = STATUS_NONE,
            newStatus = STATUS_BUSY,
            rationale = BUSY_RATIONALE
        )
    }

    @TestCaseId("TC-4535")
    @Category("selfUserProfile", "regression", "RC")
    @Test
    fun givenTeamOwner_whenChangingStatusToAway_thenStatusIsAway() {
        prepareTeamMember()
        loginCurrentUser()
        openSelfUserProfile()

        changeStatus(
            currentStatus = STATUS_NONE,
            newStatus = STATUS_AWAY,
            rationale = AWAY_RATIONALE
        )
    }

    @TestCaseId("TC-4536")
    @Category("selfUserProfile", "regression", "RC")
    @Test
    fun givenTeamOwnerWithBusyStatus_whenChangingStatusToNone_thenStatusIsNone() {
        prepareTeamMember()
        loginCurrentUser()
        openSelfUserProfile()

        changeStatus(
            currentStatus = STATUS_NONE,
            newStatus = STATUS_BUSY,
            rationale = BUSY_RATIONALE
        )
        changeStatus(
            currentStatus = STATUS_BUSY,
            newStatus = STATUS_NONE,
            rationale = NONE_RATIONALE
        )
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

    private fun changeStatus(currentStatus: String, newStatus: String, rationale: String) {
        step("Change status from '$currentStatus' to '$newStatus'") {
            pages.selfUserProfilePage.apply {
                changeStatusFromTo(currentStatus, newStatus)
                assertStatusRationaleTextIsDisplayed(rationale)
                tapOkButtonOnStatusDialog()
                assertStatusIs(newStatus)
            }
        }
    }

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val TEAM_NAME = "StatusChange"

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
        const val NONE_RATIONALE =
            "You will receive notifications for incoming calls and for messages according to the Notifications setting in each conversation."
    }
}
