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
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class LoginOldTests : BaseUiTest() {
    override val deletePersonalUsersAfterTest = true

    private lateinit var personalUser: ClientUser
    private lateinit var teamMember: ClientUser
    private lateinit var keycloakApiClient: KeycloakApiClient

    private val introductionMessage =
        "Connect with others or create a new group to start collaborating!"
    private val incorrectCredentialsMessage =
        "These account credentials are incorrect. Please verify your details and try again."

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
    @TestCaseId("TC-4435", "TC-4438", "TC-4439")
    @Category("regression", "RC", "loginOld")
    @Test
    fun givenPersonalUser_whenLoggingInWithValidCredentialsUsingOldFlow_thenLoginIsSuccessful() {
        givenPersonalUserIsPreparedForOldLoginFlow()

        step("When I tap login button on Welcome Page and sign in using my email") {
            pages.registrationPage.clickLoginButton()
            pages.loginPage.apply {
                enterPersonalUserLoggingEmail(personalUser.email ?: "")
                enterPersonalUserLoginPassword(personalUser.password ?: "")
            }
        }

        // TC-4438 - I want to see my password in cleartext when I use the eye icon on old login flow
        step("And I tap show password icon and see my password in cleartext") {
            pages.registrationPage.clickShowPasswordEyeIcon()
            pages.loginPage.assertLoginPasswordVisible(personalUser.password ?: "")
        }

        step("And I tap hide password icon and do not see my password in cleartext") {
            pages.registrationPage.clickHidePasswordEyeIcon()
            pages.loginPage.assertLoginPasswordHidden()
        }

        step("And I tap login button on email Login Page") {
            pages.loginPage.clickLoginButton()
        }

        // TC-4439 - I want to see a welcome message when I login for the first time on old login flow
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
                assertIntroductionMessageVisible(introductionMessage)
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4442")
    @Category("regression", "RC", "loginOld")
    @Test
    fun givenPersonalUser_whenLoggingInWithInvalidEmailCredentialsUsingOldFlow_thenLoginDoesNotProceed() {
        givenPersonalUserIsPreparedForOldLoginFlow()

        step("When I tap login button on Welcome Page and sign in using an invalid email format") {
            pages.registrationPage.clickLoginButton()
            pages.loginPage.apply {
                enterUserIdentifier("smoketester+invalid@wire")
                enterPersonalUserLoginPassword(personalUser.password ?: "")
                clickLoginButton()
            }
        }

        step("Then I see an invalid email or username error on login page") {
            pages.loginPage.assertInvalidUserIdentifierErrorVisible(
                "This email or username is invalid. Please verify and try again."
            )
        }

        step("When I clear the credentials and sign in using an invalid email and valid password") {
            pages.loginPage.apply {
                clearUserIdentifierInput()
                clearLoginPasswordInput()
                enterUserIdentifier("smoketester+invalid@wire.com")
                enterPersonalUserLoginPassword(personalUser.password ?: "")
                clickLoginButton()
            }
        }

        step("Then I see incorrect credentials and conversation list is not visible") {
            pages.loginPage.apply {
                assertInvalidInformationAlertVisible()
                assertIncorrectCredentialsErrorVisible(incorrectCredentialsMessage)
            }
            pages.conversationListPage.assertConversationListNotVisible()
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4436")
    @Category("regression", "RC", "loginOld", "SF.Channel", "TSFI.UserInterface", "S0.1", "S2")
    @Test
    fun givenPersonalUser_whenLoggingInWithValidEmailAndWrongPasswordUsingOldFlow_thenLoginDoesNotProceed() {
        givenPersonalUserIsPreparedForOldLoginFlow()

        step("When I tap login button on Welcome Page and sign in using my email and an invalid password") {
            pages.registrationPage.clickLoginButton()
            pages.loginPage.apply {
                enterPersonalUserLoggingEmail(personalUser.email ?: "")
                enterPersonalUserLoginPassword("thisIsAnInvalidPassword")
                clickLoginButton()
            }
        }

        step("Then I see invalid information about the incorrect credentials") {
            pages.loginPage.apply {
                assertInvalidInformationAlertVisible()
                assertIncorrectCredentialsErrorVisible(incorrectCredentialsMessage)
            }
        }

        step("And I tap OK button on the alert and do not see conversation list") {
            pages.loginPage.clickOkButtonOnIncorrectCredentialsAlertIfVisible()
            pages.conversationListPage.assertConversationListNotVisible()
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4440")
    @Category("regression", "RC", "loginOld")
    @Test
    fun givenPersonalUser_whenLoggingInWithUsernameUsingOldFlow_thenLoginIsSuccessful() {
        givenPersonalUserIsPreparedForOldLoginFlow()

        step("When I tap login button on Welcome Page and sign in using my username") {
            pages.registrationPage.clickLoginButton()
            pages.loginPage.apply {
                enterPersonalUserLoggingUsername(personalUser.uniqueUsername ?: "")
                enterPersonalUserLoginPassword(personalUser.password ?: "")
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

        step("Then I see conversation list") {
            pages.conversationListPage.assertConversationListVisible()
        }

        step("Then I see the welcome and introduction messages") {
            pages.conversationListPage.apply {
                assertWelcomeMessageVisible()
                assertIntroductionMessageVisible(introductionMessage)
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4441")
    @Category("regression", "RC", "loginOld", "sessionExpiration")
    @Test
    fun givenTeamMemberDeviceWasRemoved_whenLoggingInAgainUsingOldFlow_thenLoginIsSuccessful() {
        givenTeamMemberIsPreparedForOldLoginFlow("SessionExpiration")

        step("When I tap login button on Welcome Page and sign in using my email") {
            pages.registrationPage.clickLoginButton()
            pages.loginPage.apply {
                enterTeamMemberLoggingEmail(teamMember.email ?: "")
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

        step("When user2Name removes all their registered OTR clients") {
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

        step("And I open staging backend deep link for old login flows and see Welcome Page") {
            pages.loginPage.apply {
                clickStagingDeepLinkForOldLoginFlow()
                clickProceedButtonOnDeeplinkOverlay()
                assertOldLoginWelcomePageVisible()
            }
        }

        step("When I tap login button on Welcome Page and sign in using the same email again") {
            pages.registrationPage.clickLoginButton()
            pages.loginPage.apply {
                enterTeamMemberLoggingEmail(teamMember.email ?: "")
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

        step("Then I see conversation list") {
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4443")
    @Category("regression", "RC", "loginOld")
    @Test
    fun givenTeamMemberOnOldLoginScreen_whenTappingForgotPasswordLink_thenAccountRecoveryWebpageIsOpened() {
        givenTeamMemberIsPreparedForOldLoginFlow("ForgotPassword")

        step("When I tap login button on Welcome Page and tap on Forgot Password Link") {
            pages.registrationPage.clickLoginButton()
            pages.loginPage.apply {
                tapForgotPasswordLink()
            }
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
    @TestCaseId("TC-8711")
    @Category("regression", "RC", "loginOld", "SSO", "TEMP")
    @Test
    fun givenKeycloakSsoUser_whenLoggingInWithSsoCodeUsingOldFlow_thenLoginIsSuccessful() {
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

        step("And I open staging backend deep link for old login flows and see Welcome Page") {
            pages.loginPage.apply {
                clickStagingDeepLinkForOldLoginFlow()
                clickProceedButtonOnDeeplinkOverlay()
                assertOldLoginWelcomePageVisible()
            }
        }

        step("When I open the SSO login tab, enter the default SSO code, and continue to Keycloak") {
            pages.registrationPage.clickLoginButton()
            pages.loginPage.apply {
                clickSsoLoginTab()
                enterSSOCodeOnOldLoginInputField(SSOServiceHelper.getSsoCode())
                clickOldSsoLoginButton()
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
                clickDeclineShareDataAlert()
            }
        }

        step("Then I see conversation list") {
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    // Shared setup for personal-user old-login tests: creates the user and opens the staging old-login Welcome Page.
    private fun givenPersonalUserIsPreparedForOldLoginFlow() {
        step("Given There is a personal user user1Name and User user1Name is me") {
            clientUserManager.createPersonalUsersByAliases(listOf("user1Name"), backendClient)
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user1Name")
            }
            personalUser = clientUserManager.findUserByNameOrNameAlias("user1Name")
            clientUserManager.setSelfUser(personalUser)
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link for old login flows and see Welcome Page") {
            pages.loginPage.apply {
                clickStagingDeepLinkForOldLoginFlow()
                clickProceedButtonOnDeeplinkOverlay()
                assertOldLoginWelcomePageVisible()
            }
        }
    }

    // Shared setup for team-member old-login tests: creates the team and member and opens the staging old-login Welcome Page.
    private fun givenTeamMemberIsPreparedForOldLoginFlow(teamName: String) {
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

        step("And I open staging backend deep link for old login flows and see Welcome Page") {
            pages.loginPage.apply {
                clickStagingDeepLinkForOldLoginFlow()
                clickProceedButtonOnDeeplinkOverlay()
                assertOldLoginWelcomePageVisible()
            }
        }
    }
}
