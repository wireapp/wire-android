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
import backendUtils.user.deleteUser
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
class GdprTest : BaseUiTest() {
    private var currentUser: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @After
    fun tearDown() {
        runCatching {
            cleanupBackendClient(backendClient, currentUser)
            if (currentUser?.teamId.isNullOrBlank()) {
                currentUser?.deleteUser(backendClient)
            }
        }
    }

    @TestCaseId("TC-8704", "TC-8132")
    @Category("gdpr", "regression", "RC")
    @Test
    fun givenTeamUserAcceptsAnonymousDataSharing_thenAnalyticsIsEnabledAndIdentifierIsVisible() {
        prepareTeamUser()
        loginAndAnswerShareDataConsent(accept = true)
        verifyAnonymousUsageDataToggle(isOn = true)
        verifyAnalyticsDebugState(initialized = true)
    }

    @TestCaseId("TC-8705", "TC-8134")
    @Category("gdpr", "regression", "RC")
    @Test
    fun givenPersonalUserAcceptsAnonymousDataSharing_thenAnalyticsIsEnabledAndIdentifierIsVisible() {
        preparePersonalUser()
        loginAndAnswerShareDataConsent(accept = true)
        verifyAnonymousUsageDataToggle(isOn = true)
        verifyAnalyticsDebugState(initialized = true)
    }

    @TestCaseId("TC-8706", "TC-8133")
    @Category("gdpr", "regression", "RC")
    @Test
    fun givenTeamUserDeclinesAnonymousDataSharing_thenAnalyticsIsDisabledAndIdentifierIsVisible() {
        prepareTeamUser()
        loginAndAnswerShareDataConsent(accept = false)
        verifyAnonymousUsageDataToggle(isOn = false)
        verifyAnalyticsDebugState(initialized = false)
    }

    @TestCaseId("TC-8707", "TC-8135")
    @Category("gdpr", "regression", "RC")
    @Test
    fun givenPersonalUserDeclinesAnonymousDataSharing_thenAnalyticsIsDisabledAndIdentifierIsVisible() {
        preparePersonalUser()
        loginAndAnswerShareDataConsent(accept = false)
        verifyAnonymousUsageDataToggle(isOn = false)
        verifyAnalyticsDebugState(initialized = false)
    }

    private fun prepareTeamUser() {
        step("Prepare backend team user") {
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

    private fun preparePersonalUser() {
        step("Create personal user via backend") {
            currentUser = backendClient.createPersonalUserViaBackend(ClientUser())
        }
    }

    private fun loginAndAnswerShareDataConsent(accept: Boolean) {
        step("Verify welcome page and login via staging deep link") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterPersonalUserLoggingEmail(currentUser?.email.orEmpty())
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterPersonalUserLoginPassword(currentUser?.password.orEmpty())
                clickLoginButton()
            }
        }

        step("Complete login flow and username prompt when present") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                setUserNameIfVisible(currentUser?.uniqueUsername.orEmpty())
                clickAllowNotificationButton()
            }
        }

        step("Answer share data alert and verify conversation list") {
            pages.registrationPage.apply {
                if (accept) {
                    clickAgreeShareDataAlert()
                } else {
                    clickDeclineShareDataAlert()
                }
            }
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    private fun verifyAnonymousUsageDataToggle(isOn: Boolean) {
        step("Verify anonymous usage data toggle state in privacy settings") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
            pages.settingsPage.apply {
                clickPrivacySettingsButtonOnSettingsPage()
                if (isOn) {
                    assertSendAnonymousUsageDataToggleIsOn()
                } else {
                    assertSendAnonymousUsageDataToggleIsOff()
                }
                clickBackButtonOnPrivacySettingsPage()
            }
        }
    }

    private fun verifyAnalyticsDebugState(initialized: Boolean) {
        step("Open debug settings and verify analytics state and tracking identifier") {
            pages.settingsPage.apply {
                clickDebugSettingsButton()
                if (initialized) {
                    assertAnalyticsInitializedIsSetToTrue()
                } else {
                    assertAnalyticsInitializedIsSetToFalse()
                }
                assertAnalyticsTrackingIdentifierIsDispayed()
            }
        }
    }

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val TEAM_NAME = "GDPR"
    }
}
