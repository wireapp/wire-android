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
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import uiautomatorutils.UiWaitUtils
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class ConversationNotificationsTest : BaseUiTest() {

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

    @TestCaseId("TC-4313", "TC-4307", "TC-4312", "TC-4306")
    @Category("groups", "conversationNotifications", "regression", "RC")
    @Test
    fun givenGroupConversation_whenChangingNotificationStatus_thenSelectedStatusIsVisible() {
        prepareGroupConversation()
        loginCurrentUser()
        openGroupNotificationSettings()

        step("Verify default group conversation notification status") {
            pages.groupConversationDetailsPage.assertDefaultNotificationStatusIsEverything()
        }

        step("Select calls, mentions and replies notification status") {
            pages.groupConversationDetailsPage.apply {
                tapNotificationStatus(CALLS_MENTIONS_AND_REPLIES)
                assertNotificationStatusIs(CALLS_MENTIONS_AND_REPLIES)
            }
        }

        step("Select nothing notification status") {
            pages.groupConversationDetailsPage.apply {
                tapNotificationsButton()
                tapNotificationStatus(NOTHING)
                assertNotificationStatusIs(NOTHING)
            }
        }
    }

    @Ignore(
        "Blocked: 1:1 conversation notification parity needs ConnectedUserProfile notification helpers " +
                "and Android notification-shade assertions."
    )
    @TestCaseId("TC-4309", "TC-4310", "TC-4308", "TC-4311")
    @Category("conversationNotifications", "regression", "RC")
    @Test
    fun givenOneOnOneConversation_whenChangingNotificationStatus_thenSelectedStatusAndNotificationsAreVerified() {
        // Intentionally skipped until 1:1 profile notification settings and system notification center assertions are automated.
    }

    private fun prepareGroupConversation() {
        step("Prepare backend team owner, members, and group conversation") {
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
            testServiceHelper.userHasGroupConversationInTeam(
                TEAM_OWNER_ALIAS,
                GROUP_CONVERSATION_NAME,
                "$TEAM_MEMBER_ALIAS,$EXTRA_MEMBER_ALIAS",
                TEAM_NAME
            )
            currentUser = teamHelper.usersManager.findUserBy(TEAM_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        }
    }

    private fun loginCurrentUser() {
        step("Login team member via staging deep link") {
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

    private fun openGroupNotificationSettings() {
        step("Open notification settings for group conversation") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible(GROUP_CONVERSATION_NAME)
                clickGroupConversation(GROUP_CONVERSATION_NAME)
            }
            pages.conversationViewPage.apply {
                assertConversationScreenVisible()
                UiWaitUtils.waitFor(1.seconds)
                clickOnGroupConversationDetails(GROUP_CONVERSATION_NAME)
            }
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapShowMoreOptionsButton()
                tapNotificationsButton()
            }
        }
    }

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val EXTRA_MEMBER_ALIAS = "user3Name"
        const val TEAM_NAME = "Notification"
        const val GROUP_CONVERSATION_NAME = "MyTeam"
        const val CALLS_MENTIONS_AND_REPLIES = "Calls, mentions and replies"
        const val NOTHING = "Nothing"
    }
}
