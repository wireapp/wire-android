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

import SSOServiceHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import keycloak.KeycloakApiClient
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.KeyboardUtils.closeKeyboardIfOpened
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.STABLE_TIMEOUT
import user.utils.ClientUser
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class SSOProvisioningTests : BaseUiTest() {
    private lateinit var keycloakApiClient: KeycloakApiClient
    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        SSOServiceHelper.initialize(clientUserManager)
        keycloakApiClient = KeycloakApiClient(backendClient)
    }

    @After
    fun tearDown() {
        runCatching { keycloakApiClient.cleanUp() }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4550")
    @Category("regression", "RC", "settings", "SSO")
    @Test
    fun givenSsoUserLogsInWithKeycloak_whenOpeningAccountDetails_thenResetPasswordButtonIsNotVisible() {
        var ssoCode = ""

        step("Given There is a team owner user1Name with SSO team ResetPassword configured for keycloak") {
            runBlocking {
                SSOServiceHelper.createKeycloakSsoTeamOwner(
                    context,
                    "user1Name",
                    "ResetPassword",
                    keycloakApiClient
                )
            }
        }

        step("And User TeamOwner is available") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User user1Name adds user user2Name to keycloak") {
            runBlocking {
                SSOServiceHelper.addKeycloakSsoUsers(
                    "user1Name",
                    "user2Name",
                    keycloakApiClient
                )
            }
        }

        step("And SSO user user2Name is me") {
            SSOServiceHelper.setCurrentSsoUser("user2Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link and tap proceed button on custom backend alert") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("When I type the default SSO code on Login Tab") {
            ssoCode = SSOServiceHelper.getSsoCode()
            pages.loginPage.enterSSOCodeOnSSOLoginTab(ssoCode)
        }

        step("And I tap next button to login and tap use without an account button if visible") {
            pages.loginPage.clickLoginButton()
            pages.chromePage.dismissFirstRunIfVisible()
        }

        step("And I sign in with my credentials on Keycloak Page") {
            pages.ssoPage.apply {
                waitUntilKeycloakPageLoaded()
                enterKeycloakEmail(member1.email.orEmpty())
                enterKeycloakPassword(member1.password.orEmpty())
                closeKeyboardIfOpened()
            }
        }

        step("And I tap login button on Keycloak Page and wait until username setup page is visible") {
            pages.ssoPage.tapKeycloakSignIn()
            pages.registrationPage.assertEnterYourUserNameInfoText()
        }

        step("And I submit my Username user2UniqueUsername on registration page") {
            pages.registrationPage.apply {
                setUserName(member1.uniqueUsername.orEmpty())
                clickConfirmButton()
            }
        }

        step("And I wait until I am fully logged in") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
            }
        }

        step("And I decline share data alert") {
            UiWaitUtils.waitFor(1.seconds)
            pages.registrationPage.clickDeclineShareDataAlert()
        }

        step("When I open Account Details from conversation list menu") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
            pages.settingsPage.tapAccountDetailsButton()
        }

        step("Then I do not see reset password button") {
            pages.settingsPage.apply {
                assertResetPasswordButtonIsNotDisplayed()
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4551")
    @Category("regression", "RC", "settings", "SSO")
    @Test
    fun givenScimManagedSsoUserLogsInWithKeycloak_whenOpeningAccountDetails_thenProfileNameCannotBeChanged() {
        var ssoCode = ""

        step("Given There is a team owner user1Name with SSO team ChangeUserName configured for keycloak") {
            runBlocking {
                SSOServiceHelper.createKeycloakSsoTeamOwner(
                    context,
                    "user1Name",
                    "ChangeUserName",
                    keycloakApiClient
                )
            }
        }

        step("And User TeamOwner is available") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User user1Name adds user user2Name to keycloak and SCIM") {
            runBlocking {
                SSOServiceHelper.addKeycloakSsoUsersWithScim(
                    "user1Name",
                    "user2Name",
                    keycloakApiClient
                )
            }
        }

        step("And SSO user user2Name is me") {
            SSOServiceHelper.setCurrentSsoUser("user2Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link and tap proceed button on custom backend alert") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("When I type the default SSO code on Login Tab") {
            ssoCode = SSOServiceHelper.getSsoCode()
            pages.loginPage.enterSSOCodeOnSSOLoginTab(ssoCode)
        }

        step("And I tap next button to login and tap use without an account button if visible") {
            pages.loginPage.clickLoginButton()
            pages.chromePage.dismissFirstRunIfVisible()
        }

        step("And I sign in with my credentials on Keycloak Page") {
            pages.ssoPage.apply {
                waitUntilKeycloakPageLoaded()
                enterKeycloakEmail(member1.email.orEmpty())
                enterKeycloakPassword(member1.password.orEmpty())
                closeKeyboardIfOpened()
            }
        }

        step("And I tap login button on Keycloak Page and wait until I am logged in from keycloak page") {
            pages.ssoPage.tapKeycloakSignIn()
            pages.registrationPage.waitUntilLoginFlowIsCompleted()
        }

        step("And I wait until I am fully logged in") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
            }
        }

        step("And I decline share data alert") {
            UiWaitUtils.waitFor(1.seconds)
            pages.registrationPage.clickDeclineShareDataAlert()
        }

        UiWaitUtils.waitFor(STABLE_TIMEOUT) // wait for websocket notification to disappear

        step("And I open Account Details from conversation list menu") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
            pages.settingsPage.tapAccountDetailsButton()
        }

        step("When I see my profile name user2Name is displayed and tap on it in Account Details") {
            pages.settingsPage.apply {
                verifyDisplayedProfileName(member1.name.orEmpty())
                tapDisplayedProfileName(member1.name.orEmpty())
            }
        }

        step("Then I do not see edit profile name page") {
            pages.settingsPage.assertEditProfileNamePageIsNotDisplayed()
        }
    }
}
