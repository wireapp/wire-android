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
@file:Suppress("ArgumentListWrapping", "MaximumLineLength")

package com.wire.android.tests.core.e2eTests

import SSOServiceHelper
import SSOServiceHelper.thereIsASSOTeamOwnerForOkta
import SSOServiceHelper.userAddsOktaUser
import SSOServiceHelper.userXIsMe
import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.BackendClient
import backendUtils.user.deleteUser
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import kotlinx.coroutines.runBlocking
import okta.OktaApiClient
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.UiWaitUtils
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class LoginTest : BaseUiTest() {

    private lateinit var oktaApiClient: OktaApiClient
    private var registeredUser: ClientUser? = null
    private var ssoMember: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
        SSOServiceHelper.usersManager = teamHelper.usersManager
        oktaApiClient = OktaApiClient()
    }

    @After
    fun tearDown() {
        runCatching {
            cleanupBackendClient(backendClient, registeredUser)
            registeredUser?.deleteUser(backendClient)
        }
        runCatching { oktaApiClient.cleanUp() }
    }

    @TestCaseId("TC-8684", "TC-8685")
    @Category("login", "regression", "RC")
    @Test
    fun givenPersonalUser_whenLoggingIn_thenPasswordVisibilityWorksAndConversationListIsShown() {
        step("Create personal user via backend") {
            registeredUser = backendClient.createPersonalUserViaBackend(ClientUser())
        }

        step("Open staging backend login page") {
            pages.loginPage.clickOkButtonOnRemovedDeviceAlertIfVisible()
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
            }
        }

        step("Enter valid email and password on new login flow") {
            pages.loginPage.apply {
                enterPersonalUserLoggingEmail(registeredUser?.email.orEmpty())
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterPersonalUserLoginPassword(registeredUser?.password.orEmpty())
            }
        }

        step("Verify password visibility toggle") {
            pages.loginPage.apply {
                clickShowPasswordButton()
                assertLoginPasswordVisible(registeredUser?.password.orEmpty())
                clickHidePasswordButton()
                assertLoginPasswordHidden()
            }
        }

        step("Submit login and complete post-login prompts") {
            pages.loginPage.clickLoginButton()
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                setUserNameIfVisible(registeredUser?.uniqueUsername.orEmpty())
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("Verify conversation list is visible") {
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    @Ignore(
        "Blocked: local Okta API setup fails before app login with invalid token, so SSO login cannot be verified on the available emulator."
    )
    @TestCaseId("TC-8712", "TC-4547")
    @Category("login", "regression", "RC")
    @Test
    fun givenSSOTeamWithOkta_whenLoggingInWithSSOCodeThroughNewLoginFlow_thenConversationListIsShown() {
        step("Prepare backend SSO team in Okta and set SSO member as current user") {
            runBlocking {
                testServiceHelper.thereIsASSOTeamOwnerForOkta(
                    context,
                    TEAM_OWNER_ALIAS,
                    SSO_TEAM_NAME,
                    oktaApiClient
                )
                testServiceHelper.userAddsOktaUser(TEAM_OWNER_ALIAS, SSO_MEMBER_ALIAS, oktaApiClient)
                testServiceHelper.userXIsMe(SSO_MEMBER_ALIAS)

                ssoMember = teamHelper.usersManager.findUserBy(
                    SSO_MEMBER_ALIAS,
                    ClientUserManager.FindBy.NAME_ALIAS
                )
            }
        }

        val ssoCode = SSOServiceHelper.getSSOCode()

        step("Open staging backend login page and enter default SSO code") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterSSOCodeOnSSOLoginTab(ssoCode)
                clickLoginButton()
            }
        }

        step("Complete Okta login") {
            pages.ssoPage.apply {
                waitUntilOktaPageLoaded()
                enterOktaEmail(ssoMember?.email.orEmpty())
                enterOktaPassword(ssoMember?.password.orEmpty())
                tapOktaSignIn()
            }
            UiWaitUtils.waitFor(5.seconds)
        }

        step("Complete post-login prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                setUserNameIfVisible(ssoMember?.uniqueUsername.orEmpty())
                waitUntilLoginFlowIsCompleted()
                clickDeclineShareDataAlert()
            }
        }

        step("Verify conversation list is visible") {
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    @Ignore(
        "Blocked: depends on local Okta API setup, which currently fails before app login with invalid token; wrong-credential Okta UI cannot be reached reliably."
    )
    @TestCaseId("TC-4548")
    @Category("login", "regression", "RC", "SSO")
    @Test
    fun givenInvalidSSOCodeAndWrongOktaCredentials_whenLoggingIn_thenErrorsAreShown() = Unit

    @Ignore(
        "Stale/blocked: legacy fixed QA SSO backend scenario is documented as staging-only and currently incompatible with the fixed backend API setup."
    )
    @TestCaseId("TC-4549")
    @Category("login", "regression", "SSO", "stale")
    @Test
    fun givenFixedQaSsoBackend_whenLoggingIn_thenConversationListIsShown() = Unit

    @Ignore("Stale legacy assertion: current login flow no longer exposes the old first-login welcome/introduction message.")
    @TestCaseId("TC-4439")
    @Category("login", "regression", "RC", "stale")
    @Test
    fun givenFirstLoginCompletes_whenCheckingLegacyWelcomeMessage_thenCaseIsStale() = Unit

    @Ignore("Stale: username login is only referenced as an unfinished legacy source comment; current login accepts email or SSO code.")
    @TestCaseId("TC-8688")
    @Category("login", "regression", "RC", "stale")
    @Test
    fun givenUsernameCredentials_whenLoggingInThroughNewFlow_thenCaseIsStale() = Unit

    @Ignore("Blocked: ANTA backend deeplink login needs ANTA backend fixture/deeplink support and username setup parity.")
    @TestCaseId("TC-8691")
    @Category("login", "regression", "RC")
    @Test
    fun givenAntaBackendUser_whenLoggingInThroughDeeplink_thenConversationListIsMappedOnly() = Unit

    @Ignore("Blocked: claimed-domain SSO flow depends on Okta setup plus currently missing domain-claiming support.")
    @TestCaseId("TC-8713")
    @Category("login", "regression", "RC", "SSO")
    @Test
    fun givenClaimedDomainSsoUser_whenLoggingInThroughNewFlow_thenConversationListIsMappedOnly() = Unit

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val SSO_MEMBER_ALIAS = "user2Name"
        const val SSO_TEAM_NAME = "SSOAnta"
    }
}
