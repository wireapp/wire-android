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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.UiWaitUtils.waitUntilToastIsDisplayed
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class SettingsTest : BaseUiTest() {

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

    @TestCaseId("TC-4545")
    @Category("settings", "regression", "RC")
    @Test
    fun givenTeamMemberWithUsername_whenOpeningAccountDetails_thenUserDetailsAreShown() {
        prepareTeamMemberWithUsername()
        loginCurrentUser()
        openSettings()

        step("Open Account Details and verify user details") {
            pages.settingsPage.apply {
                tapAccountDetailsButton()
                verifyDisplayedProfileName(currentUser?.name.orEmpty())
                verifyDisplayedUserName(currentUser?.uniqueUsername.orEmpty())
                verifyDisplayedEmailAddress(currentUser?.email.orEmpty())
                verifyDisplayedTeamName(TEAM_NAME)
                verifyDisplayedDomain(DOMAIN)
            }
        }
    }

    @TestCaseId("TC-4546")
    @Category("settings", "regression", "RC")
    @Test
    fun givenTeamMember_whenChangingProfileNameInAccountDetails_thenNewNameIsShown() {
        prepareTeamMemberWithUsername()
        loginCurrentUser()
        openSettings()

        step("Open Account Details and edit profile name") {
            pages.settingsPage.apply {
                tapAccountDetailsButton()
                verifyDisplayedProfileName(currentUser?.name.orEmpty())
                tapDisplayedProfileName(currentUser?.name.orEmpty())
                assertEditProfileNamePageVisible()
                editProfileName(NEW_PROFILE_NAME)
                clickSaveButton()
            }
            waitUntilToastIsDisplayed(PROFILE_NAME_CHANGED_TOAST)
        }

        step("Verify updated profile name is displayed") {
            pages.settingsPage.verifyDisplayedProfileName(NEW_PROFILE_NAME)
        }
    }

    @TestCaseId("TC-4543")
    @Category("settings", "regression", "RC")
    @Test
    fun givenTeamMember_whenTappingResetPassword_thenResetPasswordUrlIsOpened() {
        prepareTeamMemberWithUsername()
        loginCurrentUser()
        openSettings()

        verifyResetPasswordUrl()
    }

    @TestCaseId("TC-4544")
    @Category("settings", "regression", "RC")
    @Test
    fun givenPersonalUser_whenTappingResetPassword_thenResetPasswordUrlIsOpened() {
        preparePersonalUser()
        loginCurrentPersonalUser()
        openSettings()

        verifyResetPasswordUrl()
    }

    @TestCaseId("TC-4540")
    @Category("settings", "regression", "RC")
    @Ignore("Blocked: Report a Bug flow needs stable Settings entry selector and Android share-sheet/app-drawer assertions.")
    @Test
    fun givenTeamMember_whenOpeningReportBug_thenShareSheetIsShown() = Unit

    @TestCaseId("TC-4541", "TC-4542")
    @Category("settings", "regression", "RC")
    @Ignore(
        "Blocked: Network Settings websocket toggle flow needs network-settings page helpers, stable app background/restart " +
            "handling, and Android notification-shade assertions."
    )
    @Test
    fun givenTeamMember_whenTogglingWebsocketNetworkSetting_thenSwitchStateAndNotificationAreMappedOnly() = Unit

    private fun prepareTeamMemberWithUsername() {
        step("Prepare backend team owner and member with unique username") {
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
            runBlocking {
                testServiceHelper.usersSetUniqueUsername(TEAM_MEMBER_ALIAS)
            }
            currentUser = teamHelper.usersManager.findUserBy(TEAM_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        }
    }

    private fun preparePersonalUser() {
        step("Create personal user via backend") {
            currentUser = backendClient.createPersonalUserViaBackend(ClientUser())
        }
    }

    private fun loginCurrentUser() {
        step("Login current team member via staging deep link") {
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

    private fun loginCurrentPersonalUser() {
        step("Login current personal user via staging deep link") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterPersonalUserLoggingEmail(currentUser?.email.orEmpty())
                clickLoginButton()
                enterPersonalUserLoginPassword(currentUser?.password.orEmpty())
                clickLoginButton()
            }
        }

        step("Complete login flow") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                setUserNameIfVisible(currentUser?.uniqueUsername.orEmpty())
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    private fun openSettings() {
        step("Open Settings from conversation list") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickSettingsButtonOnMenuEntry()
            }
        }
    }

    private fun verifyResetPasswordUrl() {
        step("Open Account Details and verify reset password URL") {
            pages.settingsPage.apply {
                tapAccountDetailsButton()
                assertResetPasswordButtonIsDisplayed()
                tapResetPasswordButton()
                assertChromeUrlIsDisplayed(RESET_PASSWORD_URL)
            }
        }
    }

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val TEAM_NAME = "MyAmazingTeam"
        const val DOMAIN = "staging.zinfra.io"
        const val RESET_PASSWORD_URL = "wire-account-staging.zinfra.io"
        const val NEW_PROFILE_NAME = "ThisIsMyNewName"
        const val PROFILE_NAME_CHANGED_TOAST = "Your profile name changed"
    }
}
