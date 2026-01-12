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
package com.wire.android.tests.core.tests

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import backendUtils.BackendClient
import backendUtils.team.TeamHelper
import backendUtils.team.TeamRoles
import backendUtils.team.deleteTeam
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.core.pages.AllPages
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.inject
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId

@RunWith(AndroidJUnit4::class)
class ApplockTest : BaseUiTest() {
    private val pages: AllPages by inject()
    private lateinit var device: UiDevice
    private lateinit var context: Context
    private lateinit var backendClient: BackendClient
    private lateinit var teamHelper: TeamHelper
    private var registeredUser: ClientUser? = null

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_INTERNAL)
        backendClient = BackendClient.loadBackend("STAGING")
        teamHelper = TeamHelper()
    }

    @After
    fun tearDown() {
        //  UiAutomatorSetup.stopApp()
        // To delete team member
        // runCatching { registeredUser?.deleteTeamMember(backendClient, teamMember?.getUserId().orEmpty()) }
        // To delete team
        runCatching { registeredUser?.deleteTeam(backendClient) }
    }

    @TestCaseId("TC-8143")
    @Category("applock", "regression", "testTest")
    @Test
    fun givenUserEnablesAppLock_whenAppIsBackgroundedForOneMinute_thenAppRequiresUnlockOnReturn() {

        step("Prepare backend team (owner + members)") {
            teamHelper.usersManager!!.createTeamOwnerByAlias(
                "user1Name",
                "AppLock",
                "en_US",
                true,
                backendClient,
                context
            )
            registeredUser =
                teamHelper.usersManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
            teamHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name,user4Name,user5Name",
                "AppLock",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("Verify email welcome page is shown") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("Login as personal user in Android app") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterPersonalUserLoggingEmail(registeredUser?.email ?: "")
                clickLoginButton()
                enterPersonalUserLoginPassword(registeredUser?.password ?: "")
                clickLoginButton()
            }
        }

        step("Complete registration flow and reach conversation list") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickAgreeShareDataAlert()
                assertConversationPageVisible()
            }
        }
    }
}
