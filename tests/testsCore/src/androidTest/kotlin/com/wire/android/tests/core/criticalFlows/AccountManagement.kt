/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.tests.core.criticalFlows

import InbucketClient
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import backendUtils.BackendClient
import backendUtils.team.TeamHelper
import backendUtils.team.TeamRoles
import backendUtils.team.deleteTeam
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.core.pages.AllPages
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.inject
import service.TestServiceHelper
import user.UserClient
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId

@RunWith(AndroidJUnit4::class)
class AccountManagement : BaseUiTest() {
    private val pages: AllPages by inject()
    private lateinit var device: UiDevice
    private lateinit var context: Context
    private lateinit var appPackage: String
    private var teamMember: ClientUser? = null
    private lateinit var backendClient: BackendClient
    private lateinit var teamHelper: TeamHelper
    private lateinit var testServiceHelper: TestServiceHelper
    private var registeredUser: ClientUser? = null
    private lateinit var activationLink: String

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
        appPackage = UiAutomatorSetup.APP_INTERNAL
        device = UiAutomatorSetup.start(appPackage)
        backendClient = BackendClient.loadBackend("STAGING")
        teamHelper = TeamHelper()
        testServiceHelper = TestServiceHelper(teamHelper.usersManager)
    }

    @After
    fun tearDown() {
        UiAutomatorSetup.stopApp()
        // To delete team member
       // runCatching { registeredUser?.deleteTeamMember(backendClient, teamMember?.getUserId().orEmpty()) }
        // To delete team
        runCatching { registeredUser?.deleteTeam(backendClient) }
    }

        @Suppress("LongMethod", "CyclomaticComplexMethod")
        @TestCaseId("TC-8610")
        @Category("criticalFlow")
        @Test
        fun givenMember_whenEnablingLoggingAndAppLockAndChangingEmailAndResettingPassword_thenAllSettingsUpdateSuccessfully() {
            val userInfo = UserClient.generateUniqueUserInfo()

            lateinit var newEmail: ClientUser

            step("Prepare team via backend  (owner + members + conversation)") {
                teamHelper.usersManager.createTeamOwnerByAlias(
                    "user1Name",
                    "AccountManagement",
                    "en_US",
                    true,
                    backendClient,
                    context
                )

                teamHelper.userXAddsUsersToTeam(
                    "user1Name",
                    "user2Name,user3Name",
                    "AccountManagement",
                    TeamRoles.Member,
                    backendClient,
                    context,
                    true
                )

                testServiceHelper.userHasGroupConversationInTeam(
                    "user1Name",
                    "MyTeam",
                    "user2Name",
                    "AccountManagement"
                )

                registeredUser = teamHelper.usersManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
                teamMember = teamHelper.usersManager.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)
                newEmail = teamHelper.usersManager.findUserBy("user4Name", ClientUserManager.FindBy.NAME_ALIAS)
            }

            step("Login as team member in Android app") {
                pages.registrationPage.apply {
                    assertEmailWelcomePage()
                }

                pages.loginPage.apply {
                    clickStagingDeepLink()
                    clickProceedButtonOnDeeplinkOverlay()
                }

                pages.loginPage.apply {
                    enterTeamMemberLoggingEmail(teamMember?.email ?: "")
                    clickLoginButton()
                    enterTeamMemberLoggingPassword(teamMember?.password ?: "")
                    clickLoginButton()
                }

                pages.registrationPage.apply {
                    waitUntilLoginFlowIsCompleted()
                    clickAllowNotificationButton()
                    clickDeclineShareDataAlert()
                }
            }

            step("Assert conversation name and open Settings from conversation menu entry") {
                pages.conversationListPage.apply {
                    assertGroupConversationVisible("MyTeam")
                    clickConversationsMenuEntry()
                    clickSettingsButtonOnMenuEntry()
                }
            }

            step("Verify Enable Logging toggle works in Debug Settings") {
                pages.settingsPage.apply {
                    clickDebugSettingsButton()
                    tapEnableLoggingToggle()
                    assertLoggingToggleIsOff()
                    tapEnableLoggingToggle()
                    assertLoggingToggleIsOn()
                    clickBackButtonOnSettingsPage()
                }
            }

            step("Enable App Lock and set passcode") {
                pages.settingsPage.apply {
                    assertLockWithPasswordToggleIsOff()
                    turnOnLockWithPasscodeToggle()
                    assertAppLockDescriptionText()
                    enterPasscode(userInfo.password)
                    tapSetPasscodeButton()
                    assertLockWithPasswordToggleIsOn()
                }
            }

            step("Open Account Details and verify current email + domain") {
                pages.settingsPage.apply {
                    tapAccountDetailsButton()
                    verifyDisplayedEmailAddress(teamMember?.email ?: "")
                    verifyDisplayedDomain("staging.zinfra.io")
                }
            }

            step("Change email address and verify confirmation notification") {
                pages.settingsPage.apply {
                    clickDisplayedEmailAddress()
                    changeToNewEmailAddress(newEmail.email ?: "")
                    clickSaveButton()
                    assertNotificationWithNewEmail(newEmail.email ?: "")
                }
            }

            step("Fetch email verification link from test mailbox") {
                activationLink = runBlocking {
                    InbucketClient.getVerificationLink(
                        newEmail.email.orEmpty(),
                        backendClient.inbucketUrl,
                        backendClient.inbucketPassword,
                        backendClient.inbucketUsername
                    )
                }
            }

            step("Verify new email via Chrome") {
                pages.settingsPage.apply {
                    clickEmailVerificationLink(activationLink)
                    assertEmailVerifiedMessageVisibleOnChrome()
                }
            }

            step("Bring Wire app back to foreground") {
                device.executeShellCommand(
                    "monkey -p $appPackage -c android.intent.category.LAUNCHER 1"
                )
            }

            step("Verify new email is visible in Account Details") {
                pages.settingsPage.apply {
                    clickBackButtonOnSettingsPage()
                    waitUntilNewEmailIsVisible(newEmail.email ?: "")
                    assertDisplayedEmailAddressIsNewEmail(newEmail.email ?: "")
                }
            }

            step("Start reset password flow and verify the reset URL") {
                pages.settingsPage.apply {
                    assertResetPasswordButtonIsDisplayed()
                    tapResetPasswordButton()
                    assertChromeUrlIsDisplayed("wire-account-staging.zinfra.io")
                }
            }
        }
    }
