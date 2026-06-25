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
import com.wire.android.tests.core.BaseCallUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class CallingCbrTest : BaseCallUiTest() {

    private var currentUser: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
        initCallTestHelpers()
    }

    @After
    fun tearDown() {
        runCatching { cleanupBackendClient(backendClient, currentUser) }
    }

    @Ignore("Blocked: incoming call parity needs stable Android incoming-call selectors and accept-call page helpers.")
    @TestCaseId("TC-4672")
    @Category("callingCBR", "regression", "security")
    @Test
    fun givenRemoteUserStartsOneOnOneCall_whenAndroidAccepts_thenCallUsesCbr() = Unit

    @TestCaseId("TC-4673")
    @Category("callingCBR", "regression", "security")
    @Test
    fun givenAndroidStartsOneOnOneCall_whenRemoteAccepts_thenCallUsesCbr() {
        prepareOneOnOneCall()
        startCallingInstances(TEAM_MEMBER_ALIAS)
        loginCurrentUser()
        openConversation(TEAM_MEMBER_ALIAS)

        step("Prepare remote member to accept the next incoming call") {
            runBlocking { callHelper.userXAcceptsNextIncomingCallAutomatically(TEAM_MEMBER_ALIAS) }
        }

        step("Start 1:1 audio call and verify remote member is active") {
            pages.conversationViewPage.iTapStartCallButton()
            runBlocking {
                callHelper.userVerifiesCallStatusToUserY(TEAM_MEMBER_ALIAS, "active", ONE_ON_ONE_CALL_TIMEOUT_SECONDS)
            }
            pages.callingPage.iSeeOngoingGroupCall()
            callHelper.iSeeParticipantsInGroupCall(TEAM_MEMBER_ALIAS)
        }

        step("Verify audio flow and CBR packet lengths") {
            runBlocking {
                callHelper.userVerifiesAudio(TEAM_MEMBER_ALIAS)
                callHelper.usersVerifyCbrConnection(TEAM_MEMBER_ALIAS)
            }
        }

        step("Hang up and verify call UI is dismissed") {
            pages.callingPage.iTapOnHangUpButton()
            pages.callingPage.iDoNotSeeOngoingGroupCall()
        }
    }

    @Ignore("Blocked: incoming group call parity needs stable Android incoming group-call selectors and accept-call page helpers.")
    @TestCaseId("TC-4674")
    @Category("callingCBR", "regression", "security")
    @Test
    fun givenRemoteUserStartsGroupCall_whenAndroidAccepts_thenCallUsesCbr() = Unit

    @TestCaseId("TC-4675")
    @Category("callingCBR", "regression", "security")
    @Test
    fun givenAndroidStartsGroupCall_whenRemoteParticipantsAccept_thenCallUsesCbr() {
        prepareGroupCall()
        enableConferenceCallingAndStartCallingInstances()
        loginCurrentUser()
        openConversation(GROUP_CONVERSATION_NAME)

        step("Prepare remote members to accept the next incoming group call") {
            runBlocking { callHelper.userXAcceptsNextIncomingCallAutomatically(GROUP_MEMBERS) }
        }

        step("Start group audio call and verify remote members are active") {
            pages.conversationViewPage.iTapStartCallButton()
            runBlocking {
                callHelper.userVerifiesCallStatusToUserY(GROUP_MEMBERS, "active", GROUP_CALL_TIMEOUT_SECONDS)
            }
            pages.callingPage.iSeeOngoingGroupCall()
            callHelper.iSeeParticipantsInGroupCall(GROUP_MEMBERS)
        }

        step("Verify audio flow and CBR packet lengths") {
            runBlocking {
                callHelper.userVerifiesAudio(GROUP_MEMBERS)
                callHelper.usersVerifyCbrConnection(GROUP_MEMBERS)
            }
        }

        step("Hang up and verify call UI is dismissed") {
            pages.callingPage.iTapOnHangUpButton()
            pages.callingPage.iDoNotSeeOngoingGroupCall()
        }
    }

    private fun prepareOneOnOneCall() {
        step("Prepare team owner, member, and 1:1 conversation") {
            createTeamOwner()
            addTeamMembers(TEAM_MEMBER_ALIAS)
            testServiceHelper.userHas1on1ConversationInTeam(TEAM_OWNER_ALIAS, TEAM_MEMBER_ALIAS, TEAM_NAME)
            currentUser = teamHelper.usersManager.findUserBy(TEAM_OWNER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        }
    }

    private fun prepareGroupCall() {
        step("Prepare team owner, members, and group conversation") {
            createTeamOwner()
            addTeamMembers(GROUP_MEMBERS)
            testServiceHelper.userHasGroupConversationInTeam(
                TEAM_OWNER_ALIAS,
                GROUP_CONVERSATION_NAME,
                GROUP_MEMBERS,
                TEAM_NAME
            )
            currentUser = teamHelper.usersManager.findUserBy(TEAM_OWNER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        }
    }

    private fun createTeamOwner() {
        teamHelper.usersManager.createTeamOwnerByAlias(
            TEAM_OWNER_ALIAS,
            TEAM_NAME,
            "en_US",
            true,
            backendClient,
            context
        )
    }

    private fun addTeamMembers(memberAliases: String) {
        teamHelper.userXAddsUsersToTeam(
            TEAM_OWNER_ALIAS,
            memberAliases,
            TEAM_NAME,
            TeamRoles.Member,
            backendClient,
            context,
            true
        )
    }

    private fun enableConferenceCallingAndStartCallingInstances() {
        step("Enable conference calling and start remote calling instances") {
            runBlocking {
                callHelper.enableConferenceCallingFeatureViaBackdoorTeam(TEAM_OWNER_ALIAS, TEAM_NAME)
                callHelper.userXStartsInstance(GROUP_MEMBERS, CALLING_SERVICE_BACKEND)
            }
        }
    }

    private fun startCallingInstances(memberAliases: String) {
        step("Start remote calling instances") {
            runBlocking { callHelper.userXStartsInstance(memberAliases, CALLING_SERVICE_BACKEND) }
        }
    }

    private fun loginCurrentUser() {
        step("Login current team owner") {
            pages.registrationPage.assertEmailWelcomePage()
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                enterTeamOwnerLoggingEmail(currentUser?.email.orEmpty())
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamOwnerLoggingPassword(currentUser?.password.orEmpty())
                clickLoginButton()
            }
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    private fun openConversation(aliasOrName: String) {
        val conversationName = if (aliasOrName in USER_ALIASES) {
            teamHelper.usersManager.findUserBy(aliasOrName, ClientUserManager.FindBy.NAME_ALIAS).name ?: aliasOrName
        } else {
            aliasOrName
        }

        step("Open conversation $conversationName") {
            pages.conversationListPage.apply {
                assertConversationVisible(conversationName)
                clickGroupConversation(conversationName)
            }
            pages.conversationViewPage.assertConversationScreenVisible()
        }
    }

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val EXTRA_MEMBER_ALIAS = "user3Name"
        const val GROUP_MEMBERS = "$TEAM_MEMBER_ALIAS,$EXTRA_MEMBER_ALIAS"
        val USER_ALIASES = setOf(TEAM_OWNER_ALIAS, TEAM_MEMBER_ALIAS, EXTRA_MEMBER_ALIAS)
        const val TEAM_NAME = "WeLikeCalls"
        const val GROUP_CONVERSATION_NAME = "GroupCall"
        const val CALLING_SERVICE_BACKEND = "Chrome"
        const val ONE_ON_ONE_CALL_TIMEOUT_SECONDS = 30
        const val GROUP_CALL_TIMEOUT_SECONDS = 90
    }
}
