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
@file:Suppress("ArgumentListWrapping")

package com.wire.android.tests.core.e2eTests

import androidx.test.ext.junit.runners.AndroidJUnit4
import backendUtils.BackendClient
import backendUtils.team.TeamRoles
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
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
class AudioMessagesTest : BaseUiTest() {

    private var teamOwner: ClientUser? = null
    private var currentUser: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @After
    fun tearDown() {
        runCatching { cleanupBackendClient(backendClient, teamOwner) }
    }

    @TestCaseId("TC-4081")
    @Category("audioMessages", "regression", "RC")
    @Test
    fun givenTeamMemberInGroupConversation_whenOwnerSendsAudioFile_thenAudioCanBePlayed() {
        prepareGroupConversationWithOwnerDevice()
        loginCurrentUser()
        openGroupConversation()

        step("Owner sends audio file through backend device") {
            testServiceHelper.contactSendsLocalAudioConversation(
                context,
                AUDIO_FILE_NAME,
                TEAM_OWNER_ALIAS,
                DEVICE_NAME,
                GROUP_CONVERSATION_NAME
            )
        }

        step("Verify received audio can be played") {
            pages.conversationViewPage.apply {
                assertAudioMessageIsVisible()
                assertAudioTimeStartsAtZero()
                clickPlayButtonOnAudioMessage()
                UiWaitUtils.waitFor(10.seconds)
                clickPauseButtonOnAudioMessage()
                assertAudioTimeIsNotZeroAnymore()
            }
        }
    }

    @TestCaseId("TC-4082")
    @Category("audioMessages", "regression", "RC")
    @Ignore(
        "Blocked: recording and sending audio messages needs stable microphone permission, recorder controls, and recorded-audio preview helpers."
    )
    @Test
    fun givenGroupConversation_whenRecordingAudioMessage_thenRecordedAudioCanBeSent() = Unit

    @TestCaseId("TC-4083", "TC-4084")
    @Category("audioMessages", "regression", "RC")
    @Ignore(
        "Blocked: long recorded-audio preview and audio filter parity need stable recorder controls, preview play/pause " +
            "assertions, and audio-filter checkbox selectors."
    )
    @Test
    fun givenRecordedLongAudioMessage_whenPreviewingAndApplyingFilter_thenAudioCanBeSent() = Unit

    private fun prepareGroupConversationWithOwnerDevice() {
        step("Prepare team owner, member, owner backend device, and group conversation") {
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
            testServiceHelper.addDevice(TEAM_OWNER_ALIAS, null, DEVICE_NAME)
            testServiceHelper.userHasGroupConversationInTeam(
                TEAM_OWNER_ALIAS,
                GROUP_CONVERSATION_NAME,
                TEAM_MEMBER_ALIAS,
                TEAM_NAME
            )
            teamOwner = teamHelper.usersManager.findUserBy(TEAM_OWNER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
            currentUser = teamHelper.usersManager.findUserBy(TEAM_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        }
    }

    private fun loginCurrentUser() {
        step("Login team member via staging deep link") {
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

    private fun openGroupConversation() {
        step("Open group conversation") {
            pages.conversationListPage.apply {
                assertConversationVisible(GROUP_CONVERSATION_NAME)
                tapConversationNameInConversationList(GROUP_CONVERSATION_NAME)
            }
            pages.conversationViewPage.assertChannelConversationInForeground(GROUP_CONVERSATION_NAME)
        }
    }

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val TEAM_NAME = "Audio"
        const val GROUP_CONVERSATION_NAME = "SendYourAudioHere"
        const val DEVICE_NAME = "Device1"
        const val AUDIO_FILE_NAME = "test.m4a"
    }
}
