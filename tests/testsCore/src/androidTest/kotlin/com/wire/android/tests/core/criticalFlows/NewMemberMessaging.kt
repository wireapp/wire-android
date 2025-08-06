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

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import backendUtils.BackendClient
import backendUtils.team.TeamHelper
import backendUtils.team.TeamRoles
import backendUtils.team.deleteTeam
import com.wire.android.tests.core.di.testModule
import com.wire.android.tests.core.pages.AllPages
import com.wire.android.tests.support.UiAutomatorSetup
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import service.TestServiceHelper
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import kotlin.getValue

@RunWith(AndroidJUnit4::class)
class NewMemberMessaging : KoinTest {

    @get:Rule
    val koinTestRule = KoinTestRule.Companion.create {
        modules(testModule)
    }
    private val pages: AllPages by inject()
    private lateinit var device: UiDevice

    lateinit var context: Context
    var teamOwner: ClientUser? = null

    var member1: ClientUser? = null

    var backendClient: BackendClient? = null
    var teamHelper: TeamHelper? = null
    val testServiceHelper by lazy {
        TestServiceHelper()
    }

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
        // To delete team
        //  teamOwner?.deleteTeam(backendClient!!)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @Test
    fun givenUserJoinsNewTeam_whenMessagingAndMentionedInGroup_thenReceivesMessagesAndMentions() {
        teamHelper?.usersManager!!.createTeamOwnerByAlias(
            "user1Name",
            "Messaging",
            "en_US",
            true,
            backendClient!!,
            context
        )
        teamHelper?.userXAddsUsersToTeam(
            "user1Name",
            "user2Name,user3Name",
            "Messaging",
            TeamRoles.Member,
            backendClient!!,
            context,
            true
        )

        teamOwner = teamHelper?.usersManager!!.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)
        member1 = teamHelper?.usersManager!!.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)


        testServiceHelper.apply {
            userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user3Name",
                "Messaging"
            )
            addDevice("user1Name", null, "Device1")
        }

        pages.registrationPage.apply {
            assertEmailWelcomePage()
        }
        pages.loginPage.apply {
            enterTeamOwnerLoggingEmail(member1?.email ?: "")
            clickLoginButton()
            enterTeamOwnerLoggingPassword(member1?.password ?: "")
            clickLoginButton()
        }
        pages.registrationPage.apply {
            waitUntilLoginFlowIsComplete()
            clickAllowNotificationButton()
            clickDeclineShareDataAlert()
        }
        pages.conversationPage.apply {
            assertGroupConversationVisible("MyTeam")
        }
    }
}
