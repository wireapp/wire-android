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
import backendUtils.client.getBackendClientIds
import backendUtils.client.removeBackendClient
import backendUtils.team.TeamRoles
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

@RunWith(AndroidJUnit4::class)
class LoginTests : BaseUiTest() {
    override val deletePersonalUsersAfterTest = true

    private lateinit var personalUser: ClientUser
    private lateinit var teamMember: ClientUser
    private lateinit var keycloakApiClient: KeycloakApiClient

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

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8684", "TC-8685", "TC-4439")
    @Category("regression", "RC", "login")
    @Test
    fun givenPersonalUser_whenLoggingInWithValidCredentials_thenLoginIsSuccessful() {
        givenPersonalUserIsPrepared()

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("When I enter a valid email and password to sign in") {
            pages.loginPage.apply {
                enterPersonalUserLoggingEmail(personalUser.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterPersonalUserLoginPassword(personalUser.password ?: "")
            }
        }

        // TC-8685 - I want to see my password in cleartext when I use the eye icon in new login flow
        step("And I tap show password icon and see my password in cleartext") {
            pages.registrationPage.clickShowPasswordEyeIcon()
            pages.loginPage.assertLoginPasswordVisible(personalUser.password ?: "")
        }

        step("And I tap hide password icon and do not see my password in cleartext") {
            pages.registrationPage.clickHidePasswordEyeIcon()
            pages.loginPage.assertLoginPasswordHidden()
        }

        step("And I tap next button to login") {
            pages.loginPage.clickLoginButton()
        }

        // TC-4439 - I want to see a welcome message when I login for the first time
        step("And I wait until I am fully logged in and decline share data alert") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("Then I see the welcome and introduction messages") {
            pages.conversationListPage.apply {
                assertWelcomeMessageVisible()
                assertIntroductionMessageVisible(
                    "Connect with others or create a new group to start collaborating!"
                )
            }
        }
    }

    @TestCaseId("TC-8686")
    @Category("regression", "RC", "login")
    @Test
    fun givenPersonalUser_whenSubmittingInvalidEmail_thenLoginDoesNotProceed() {
        givenPersonalUserIsPrepared()

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("When I sign in using my invalid email and tap next button") {
            pages.loginPage.apply {
                enterUserIdentifier("smoketester+invalid@wire")
                clickLoginButton()
            }
        }

        step("Then I see an error informing me that \"Please enter a valid email or SSO code\" on login page") {
            pages.loginPage.assertSsoValidationErrorVisible(
                "Please enter a valid email or SSO code"
            )
        }
    }

    @TestCaseId("TC-8687")
    @Category("regression", "RC", "login")
    @Test
    fun givenPersonalUser_whenLoggingInWithValidEmailAndWrongPassword_thenLoginDoesNotProceed() {
        givenPersonalUserIsPrepared()

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("When I enter a valid email and invalid password to sign in") {
            pages.loginPage.apply {
                enterPersonalUserLoggingEmail(personalUser.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterPersonalUserLoginPassword("thisIsAnInvalidPassword")
                clickLoginButton()
            }
        }

        step("Then I see incorrect credentials information") {
            pages.loginPage.assertIncorrectCredentialsErrorVisible(
                "These account credentials are incorrect. Please verify your details and try again."
            )
        }

        step("And I dismiss the alert if shown and do not see conversation list") {
            pages.loginPage.clickOkButtonOnIncorrectCredentialsAlertIfVisible()
            pages.conversationListPage.assertConversationListNotVisible()
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8689")
    @Category("regression", "RC", "login", "sessionExpiration")
    @Test
    fun givenTeamMemberDeviceWasRemoved_whenLoggingInAgain_thenLoginIsSuccessful() {
        givenTeamMemberIsPreparedForLoginFlow("SessionExpiration")

        step("When I enter a valid email and password to sign in") {
            pages.loginPage.apply {
                enterTeamMemberLoggingEmail(teamMember.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamMemberLoggingPassword(teamMember.password ?: "")
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

        step("And user2Name removes all their registered OTR clients") {
            val registeredClientIds = backendClient.getBackendClientIds(teamMember)
            if (registeredClientIds.isEmpty()) {
                throw AssertionError("No registered OTR clients were found for user2Name.")
            }
            registeredClientIds.forEach { clientId ->
                backendClient.removeBackendClient(teamMember, clientId)
            }
        }

        step("Then I see the removed device alert and its subtext") {
            pages.commonAppPage.apply {
                assertRemovedDeviceDialogVisible()
                assertRemovedDeviceDialogSubtextVisible(
                    "You were logged out because your device was removed."
                )
            }
        }

        step("When I tap OK button on the alert and see email verification Welcome Page") {
            pages.commonAppPage.confirmRemovedDeviceDialog()
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("When I enter the same valid email and password to sign in again") {
            pages.loginPage.apply {
                enterTeamMemberLoggingEmail(teamMember.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamMemberLoggingPassword(teamMember.password ?: "")
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

        step("Then I see the welcome and introduction messages") {
            pages.conversationListPage.apply {
                assertWelcomeMessageVisible()
                assertIntroductionMessageVisible(
                    "Connect with others or create a new group to start collaborating!"
                )
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-8690")
    @Category("regression", "RC", "login")
    @Test
    fun givenTeamMemberOnPasswordScreen_whenTappingForgotPasswordLink_thenAccountRecoveryWebpageIsOpened() {
        givenTeamMemberIsPreparedForLoginFlow("ForgotPassword")

        step("When I enter a valid email and tap next button to see the user login screen") {
            pages.loginPage.apply {
                enterTeamMemberLoggingEmail(teamMember.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
            }
        }

        step("When I tap on Forgot Password Link") {
            pages.loginPage.tapForgotPasswordLink()
        }

        step("Then I see the Wire app is not in foreground and the account recovery webpage is open") {
            pages.commonAppPage.assertWireAppIsNotInForeground()
            pages.chromePage.apply {
                dismissFirstRunIfVisible()
                dismissNotificationsPromptIfVisible()
            }
            pages.settingsPage.assertChromeUrlIsDisplayed("wire-account-staging.zinfra.io")
        }

        step("And I close the page through the X icon") {
            pages.commonAppPage.closeWebPage()
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-8691")
    @Category("regression", "RC", "login")
    @Test
    fun givenPersonalUserOnFederatedBackend_whenLoggingInUsingDeeplink_thenLoginIsSuccessful() {
        step("Given There is a personal user user1Name on anta backend and User user1Name is me") {
            initCommonTestHelpers("anta")
            clientUserManager.createPersonalUsersByAliases(listOf("user1Name"), backendClient)
            personalUser = clientUserManager.findUserByNameOrNameAlias("user1Name")
            clientUserManager.setSelfUser(personalUser)
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open anta backend deep link") {
            pages.loginPage.apply {
                clickStagingDeepLink("anta")
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("When I enter a valid email and password to sign in") {
            pages.loginPage.apply {
                enterPersonalUserLoggingEmail(personalUser.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterPersonalUserLoginPassword(personalUser.password ?: "")
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in, submit my Username user1UniqueUsername and confirm") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                assertEnterYourUserNameInfoText()
                setUserName(personalUser.uniqueUsername ?: "")
                clickConfirmButton()
            }
        }

        step("Then I see the welcome and introduction messages") {
            pages.conversationListPage.apply {
                assertWelcomeMessageVisible()
                assertIntroductionMessageVisible(
                    "Connect with others or create a new group to start collaborating!"
                )
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-8713", "TC-8712")
    @Category("regression", "RC", "login", "SSO", "TEMP")
    @Test
    fun givenKeycloakSsoUser_whenLoggingInWithSsoCode_thenLoginIsSuccessful() {
        step("Given There is a team owner \"user1Name\" with SSO team \"SSO\" configured for keycloak") {
            runBlocking {
                SSOServiceHelper.createKeycloakSsoTeamOwner(
                    context,
                    "user1Name",
                    "SSO",
                    keycloakApiClient
                )
                SSOServiceHelper.addKeycloakSsoUsers(
                    "user1Name",
                    "user2Name",
                    keycloakApiClient
                )
            }
            SSOServiceHelper.setCurrentSsoUser("user2Name")
            teamMember = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("When I type the default SSO code and continue to Keycloak") {
            pages.loginPage.apply {
                enterSSOCodeOnSSOLoginTab(SSOServiceHelper.getSsoCode())
                clickLoginButton()
            }
            pages.chromePage.dismissFirstRunIfVisible()
        }

        step("And I sign in with my credentials on Keycloak Page") {
            pages.ssoPage.apply {
                waitUntilKeycloakPageLoaded()
                enterKeycloakEmail(teamMember.email ?: "")
                closeKeyboardIfOpened()
                enterKeycloakPassword(teamMember.password ?: "")
                closeKeyboardIfOpened()
                tapKeycloakSignIn()
            }
        }

        step("And I submit my Username user2UniqueUsername and tap confirm button") {
            pages.registrationPage.apply {
                assertEnterYourUserNameInfoText()
                setUserName(teamMember.uniqueUsername ?: "")
                clickConfirmButton()
            }
        }

        step("And I wait until I am fully logged in and decline share data alert") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert(timeout = UiWaitUtils.SHORT_WAIT)
            }
        }

        step("Then I see conversation list") {
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    // Shared setup for team-member login tests: creates the team and member and opens the staging login flow.
    private fun givenTeamMemberIsPreparedForLoginFlow(teamName: String) {
        step("Given There is a team owner user1Name with team $teamName and member user2Name is me") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                teamName,
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                teamName,
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            teamMember = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(teamMember)
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }
    }

    // Shared backend setup for personal-user login tests: creates the user, assigns a username, and sets the user as self.
    private fun givenPersonalUserIsPrepared() {
        step("Given There is a personal user user1Name and User user1Name is me") {
            clientUserManager.createPersonalUsersByAliases(listOf("user1Name"), backendClient)
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user1Name")
            }
            personalUser = clientUserManager.findUserByNameOrNameAlias("user1Name")
            clientUserManager.setSelfUser(personalUser)
        }
    }
}
