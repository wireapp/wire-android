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
package com.wire.android.tests.core.e2eTests

import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.BackendClient
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class ApplockTest : BaseUiTest() {
    private var registeredUser: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @TestCaseId("TC-8143")
    @Category("applock", "regression")
    @Test
    fun givenUserEnablesAppLock_whenAppIsBackgroundedForOneMinute_thenAppRequiresUnlockOnReturn() {

        step("Prepare backend team (owner + members)") {
            teamHelper.usersManager.createTeamOwnerByAlias(
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
