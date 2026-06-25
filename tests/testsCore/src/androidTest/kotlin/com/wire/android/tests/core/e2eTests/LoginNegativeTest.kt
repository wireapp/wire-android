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
import backendUtils.user.removeBackendClients
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
class LoginNegativeTest : BaseUiTest() {
    private var registeredUser: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @After
    fun tearDown() {
        runCatching {
            cleanupBackendClient(backendClient, registeredUser)
            registeredUser?.deleteUser(backendClient)
        }
    }

    @TestCaseId("TC-8686")
    @Category("login", "negative", "regression")
    @Test
    fun givenInvalidEmailCredentials_whenUserTriesToLogin_thenErrorIsShownAndConversationListIsNotVisible() {
        step("Open staging backend login page") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("Enter malformed email and verify validation error") {
            pages.loginPage.apply {
                enterPersonalUserLoggingEmail(INVALID_EMAIL_FORMAT)
                clickLoginButton()
                assertInvalidEmailErrorVisible()
                clearEmailInputField()
            }
        }

        step("Enter valid-format unknown email and verify credentials error") {
            pages.loginPage.apply {
                enterPersonalUserLoggingEmail(UNKNOWN_EMAIL)
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterPersonalUserLoginPassword(VALID_PASSWORD)
                clickLoginButton()
                assertIncorrectCredentialsErrorVisible()
                clickOkButtonOnIncorrectCredentialsAlertIfVisible()
            }
        }

        step("Verify user is not logged in") {
            pages.conversationListPage.assertConversationListNotVisible()
        }
    }

    @TestCaseId("TC-8687")
    @Category("login", "negative", "regression")
    @Test
    fun givenWrongPassword_whenUserTriesToLogin_thenErrorIsShownAndConversationListIsNotVisible() {
        step("Create personal user via backend") {
            registeredUser = backendClient.createPersonalUserViaBackend(ClientUser())
        }

        step("Open staging backend login page") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("Enter valid email with wrong password and verify credentials error") {
            pages.loginPage.apply {
                enterPersonalUserLoggingEmail(registeredUser?.email.orEmpty())
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterPersonalUserLoginPassword(INVALID_PASSWORD)
                clickLoginButton()
                assertIncorrectCredentialsErrorVisible()
                clickOkButtonOnIncorrectCredentialsAlertIfVisible()
            }
        }

        step("Verify user is not logged in") {
            pages.conversationListPage.assertConversationListNotVisible()
        }
    }

    @TestCaseId("TC-8689")
    @Category("login", "negative", "regression", "sessionExpiration")
    @Test
    fun givenLoggedInUser_whenAllRegisteredOtrClientsAreRemoved_thenRemovedDeviceAlertIsShownAndUserCanLoginAgain() {
        step("Create team owner and member via backend") {
            teamHelper.usersManager.createTeamOwnerByAlias(
                TEAM_OWNER_ALIAS,
                SESSION_EXPIRATION_TEAM,
                "en_US",
                true,
                backendClient,
                context
            )
            teamHelper.userXAddsUsersToTeam(
                TEAM_OWNER_ALIAS,
                TEAM_MEMBER_ALIAS,
                SESSION_EXPIRATION_TEAM,
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            registeredUser = teamHelper.usersManager.findUserBy(TEAM_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        }

        step("Login as team member") {
            openStagingLoginPage()
            loginWithRegisteredUser()
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
            pages.conversationListPage.assertConversationListVisible()
        }

        step("Remove all registered OTR clients for the logged-in user") {
            registeredUser?.removeBackendClients(backendClient)
        }

        step("Verify removed device alert and acknowledge it") {
            pages.loginPage.apply {
                assertRemovedDeviceAlertVisible()
                clickOkButtonOnRemovedDeviceAlert()
            }
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("Login again successfully") {
            openStagingLoginPage()
            loginWithRegisteredUser()
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    @TestCaseId("TC-8690")
    @Category("login", "negative", "regression")
    @Test
    fun givenPasswordScreen_whenUserTapsForgotPassword_thenAccountRecoveryPageOpens() {
        step("Create personal user via backend") {
            registeredUser = backendClient.createPersonalUserViaBackend(ClientUser())
        }

        step("Open staging backend login page") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("Navigate to password screen") {
            pages.loginPage.apply {
                enterPersonalUserLoggingEmail(registeredUser?.email.orEmpty())
                clickLoginButton()
                assertUserLoginScreenVisible()
            }
        }

        step("Tap forgot password and verify account recovery page opens") {
            pages.loginPage.clickForgotPasswordButton()
            pages.chromePage.assertUrlContains("wire-account-staging.zinfra.io")
        }
    }

    private fun openStagingLoginPage() {
        pages.registrationPage.assertEmailWelcomePage()
        pages.loginPage.apply {
            clickStagingDeepLink()
            clickProceedButtonOnDeeplinkOverlay()
        }
    }

    private fun loginWithRegisteredUser() {
        pages.loginPage.apply {
            enterPersonalUserLoggingEmail(registeredUser?.email.orEmpty())
            clickLoginButton()
            assertUserLoginScreenVisible()
            enterPersonalUserLoginPassword(registeredUser?.password.orEmpty())
            clickLoginButton()
        }
    }

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val SESSION_EXPIRATION_TEAM = "SessionExpiration"
        const val INVALID_EMAIL_FORMAT = "smoketester+invalid@wire"
        const val UNKNOWN_EMAIL = "smoketester+invalid@wire.com"
        const val VALID_PASSWORD = "ValidPassword1!"
        const val INVALID_PASSWORD = "thisIsAnInvalidPassword"
    }
}
