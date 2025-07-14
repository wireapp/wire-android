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
import backendconnections.team.TeamHelper
import com.wire.android.testSupport.backendConnections.BackendClient
import com.wire.android.testSupport.backendConnections.team.TeamRoles
import com.wire.android.testSupport.backendConnections.team.deleteTeam
import com.wire.android.testSupport.uiautomatorutils.UiAutomatorSetup
import com.wire.android.tests.core.di.testModule
import com.wire.android.tests.core.pages.AllPages
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class ApplockTest : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.create {
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

    @Test
    fun setAppLockForAppAndVerifyAppIsLockedAfter1MinuteInTheBackground() {

        teamHelper?.usersManager!!.createTeamOwnerByAlias("user1Name", "AppLock", "en_US", true, backendClient!!, context)
        registeredUser = teamHelper?.usersManager!!.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
        teamHelper?.userXAddsUsersToTeam(
            "user1Name",
            "user2Name,user3Name,user4Name,user5Name",
            "AppLock",
            TeamRoles.Member,
            backendClient!!,
            context,
            true
        )

        pages.registrationPage.apply {
            assertEmailWelcomePage()
        }
        pages.loginPage.apply {
            enterPersonalUserLoggingEmail(registeredUser?.email ?: "")
            clickLoginButton()
            enterPersonalUserLoginPassword(registeredUser?.password ?: "")
            clickLoginButton()
        }
        pages.registrationPage.apply {
            waitUntilLoginFlowIsComplete()
            clickAllowNotificationButton()
            clickAgreeShareDataAlert()
            assertConversationPageVisible()
        }
    }
}
