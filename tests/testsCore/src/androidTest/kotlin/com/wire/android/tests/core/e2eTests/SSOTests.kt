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
import user.utils.ClientUser
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class SSOTests : BaseUiTest() {
    private lateinit var keycloakApiClient: KeycloakApiClient
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
    @TestCaseId("TC-4547")
    @Category("regression", "RC", "login", "SSO")
    @Test
    fun givenSsoUserLogsInWithKeycloak_whenCompletingTheSsoFlow_thenUserReachesConversationList() {
        var ssoCode = ""

        step("Given There is a team owner user1Name with SSO team SSO configured for keycloak") {
            runBlocking {
                SSOServiceHelper.createKeycloakSsoTeamOwner(
                    context,
                    "user1Name",
                    "SSO",
                    keycloakApiClient
                )
            }
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

        step("Then I reach the conversation list") {
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4548")
    @Category("regression", "RC", "login", "SSO")
    @Test
    fun givenSsoUserEntersInvalidCodeAndCredentials_whenTryingToSignIn_thenTheRelevantErrorsAreShown() {
        var ssoCode = ""

        step("Given There is a team owner user1Name with SSO team SSO configured for keycloak") {
            runBlocking {
                SSOServiceHelper.createKeycloakSsoTeamOwner(
                    context,
                    "user1Name",
                    "SSO",
                    keycloakApiClient
                )
            }
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

        step("When I type an invalid SSO code on Login Tab and tap next button to login") {
            pages.loginPage.apply {
                enterSSOCodeOnSSOLoginTab("wire-74b782bd-3bb0-4247-8aaf")
                clickLoginButton()
            }
        }

        step("Then I see an error message underneath the SSO code input field") {
            pages.loginPage.assertSsoValidationErrorVisible("Please enter a valid email or SSO code")
        }

        step("And I clear the SSO Code input field and type the default SSO code on Login Tab") {
            ssoCode = SSOServiceHelper.getSsoCode()
            pages.loginPage.apply {
                clearUserIdentifierInput()
                enterSSOCodeOnSSOLoginTab(ssoCode)
            }
        }

        step("And I tap next button to login and tap use without an account button if visible") {
            pages.loginPage.clickLoginButton()
            pages.chromePage.dismissFirstRunIfVisible()
        }

        step("When I sign in with invalid credentials on Keycloak Page") {
            pages.ssoPage.apply {
                waitUntilKeycloakPageLoaded()
                enterKeycloakEmail("smoketester+invalid@wire.com")
                enterKeycloakPassword("thisIsAnInvalidPassword")
                closeKeyboardIfOpened()
                tapKeycloakSignIn()
            }
        }

        step("Then I see an error message telling me that I am unable to sign in on Keycloak Page") {
            pages.ssoPage.assertKeycloakErrorVisible("Invalid username or password")
        }
    }

}
