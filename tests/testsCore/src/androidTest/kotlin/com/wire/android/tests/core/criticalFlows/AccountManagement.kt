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
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.core.di.testModule
import com.wire.android.tests.core.pages.AllPages
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import service.TestServiceHelper
import user.UserClient
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class AccountManagement : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.Companion.create {
        modules(testModule)
    }
    private val pages: AllPages by inject()
    private lateinit var device: UiDevice
    lateinit var context: Context
    var registeredUser: ClientUser? = null
    var backendClient: BackendClient? = null
    var teamHelper: TeamHelper? = null

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
        // device = UiAutomatorSetup.start(UiAutomatorSetup.APP_DEV)
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_STAGING)
        backendClient = BackendClient.loadBackend("STAGING")
        teamHelper = TeamHelper()
    }

    @After
    fun tearDown() {
        //  UiAutomatorSetup.stopApp()
        // To delete team member
        // registeredUser?.deleteTeamMember(backendClient!!, teamMember?.getUserId().orEmpty())
        // To delete team
        registeredUser?.deleteTeam(backendClient!!)
    }

    @Suppress("LongMethod")
    @Test
    fun accountManagementFeature() {
        val userInfo = UserClient.generateUniqueUserInfo()
        teamHelper?.usersManager!!.createTeamOwnerByAlias("user1Name", "AccountManagement", "en_US", true, backendClient!!, context)
        registeredUser = teamHelper?.usersManager!!.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
        teamHelper?.userXAddsUsersToTeam(
            "user1Name",
            "user2Name,user3Name",
            "AccountManagement",
            TeamRoles.Member,
            backendClient!!,
            context,
            true
        )
        val teamMember = teamHelper?.usersManager!!.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)
        val newEmail = teamHelper?.usersManager!!.findUserBy("user4Name", ClientUserManager.FindBy.NAME_ALIAS)

        TestServiceHelper().userHasGroupConversationInTeam("user1Name", "MyTeam", "user2Name", "AccountManagement")

        pages.registrationPage.apply {
            assertEmailWelcomePage()
        }
        pages.loginPage.apply {
            enterPersonalUserLoggingEmail(teamMember.email ?: "")
            clickLoginButton()
            enterPersonalUserLoginPassword(teamMember.password ?: "")
            clickLoginButton()
        }
        pages.registrationPage.apply {
            waitUntilLoginFlowIsComplete()
            clickAllowNotificationButton()
            clickDeclineShareDataAlert()
        }
        pages.conversationPage.apply {
            assertGroupConversationVisible("MyTeam")
            clickMainMenuButtonOnConversationPage()
            clickSettingsButtonOnMenuEntry()
            pages.settingsPage.apply {
                clickDebugSettingsButton()
                tapEnableLoggingToggle()
                assertLoggingToggleIsOff()
                tapEnableLoggingToggle()
                assertLoggingToggleIsOn()
                clickBackButtonOnSettingsPage()
                assertLockWithPasswordToggleIsOff()
                turnOnLockWithPasscodeToggle()
                assertAppLockDescriptionText()
                enterPasscode(userInfo.password)
                tapSetPasscodeButton()
                assertLockWithPasswordToggleIsOn()
                tapAccountDetailsButton()
                verifyDisplayedEmailAddress(teamMember.email ?: "")
                verifyDisplayedDomain("staging.zinfra.io")
                clickDisplayedEmailAddress()
                changeToNewEmailAddress(newEmail.email ?: "")
                clickSaveButton()
                assertNotificationWithNewEmail(newEmail.email ?: "")
                val activationLink = runBlocking {
                    InbucketClient.getVerificationLink(
                        newEmail.email.orEmpty(),
                        backendClient!!.inbucketUrl,
                        backendClient!!.inbucketPassword,
                        backendClient!!.inbucketUsername
                    )
                }
                clickEmailVerificationLink(activationLink)
                assertEmailVerifiedMessageVisibleOnChrome()
                // Brings the Wire staging app to the foreground using the monkey tool
                device.executeShellCommand("monkey -p ${UiAutomatorSetup.APP_STAGING} -c android.intent.category.LAUNCHER 1")
                clickBackButtonOnSettingsPage()
                waitUntilNewEmailIsVisible(newEmail.email ?: "")
                assertDisplayedEmailAddressIsNewEmail(newEmail.email ?: "")
                assertResetPasswordButtonIsDisplayed()
                tapResetPasswordButton()
                assertChromeUrlIsDisplayed("wire-account-staging.zinfra.io")
            }
        }
    }
}
