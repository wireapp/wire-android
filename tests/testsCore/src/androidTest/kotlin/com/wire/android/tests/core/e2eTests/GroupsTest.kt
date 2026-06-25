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
import com.wire.android.tests.core.BaseUiTest
import com.wire.android.tests.support.UiAutomatorSetup
import com.wire.android.tests.support.tags.Category
import com.wire.android.tests.support.tags.TestCaseId
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.UiWaitUtils.iSeeSystemMessage
import uiautomatorutils.UiWaitUtils.waitUntilToastIsDisplayed
import user.usermanager.ClientUserManager
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
class GroupsTest : BaseUiTest() {

    private var currentUser: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @After
    fun tearDown() {
        runCatching { cleanupBackendClient(backendClient, currentUser) }
    }

    @TestCaseId("TC-4348", "TC-4349", "TC-4350", "TC-4355")
    @Category("groups", "leaveGroup", "regression", "RC")
    @Test
    fun givenGroupConversation_whenLeavingFromConversationList_thenHistoryRemainsAndNewMessagesAreHidden() {
        prepareGroupConversation()
        loginCurrentUser()

        step("Open group conversation and receive a history message") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible(GROUP_CONVERSATION_NAME)
                clickGroupConversation(GROUP_CONVERSATION_NAME)
            }
            testServiceHelper.userSendMessageToConversation(
                TEAM_MEMBER_ALIAS,
                HISTORY_MESSAGE,
                MEMBER_DEVICE_NAME,
                GROUP_CONVERSATION_NAME,
                false
            )
            pages.conversationViewPage.apply {
                assertConversationScreenVisible()
                assertReceivedMessageIsVisibleInCurrentConversation(HISTORY_MESSAGE)
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("Leave group conversation from conversation list") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible(GROUP_CONVERSATION_NAME)
                longPressConversation(GROUP_CONVERSATION_NAME)
                assertLeaveConversationButtonVisibleInConversationActions()
                tapLeaveConversationButtonInConversationActions()
                assertLeaveConversationConfirmationModalVisible(GROUP_CONVERSATION_NAME)
                tapLeaveConversationButtonOnModal()
            }
            waitUntilToastIsDisplayed("You left the conversation.")
        }

        step("Verify conversation history remains visible after leaving") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible(GROUP_CONVERSATION_NAME)
                clickGroupConversation(GROUP_CONVERSATION_NAME)
            }
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation(HISTORY_MESSAGE)
            }
        }

        step("Verify new backend messages are not visible after leaving") {
            testServiceHelper.userSendMessageToConversation(
                TEAM_MEMBER_ALIAS,
                POST_LEAVE_MESSAGE,
                MEMBER_DEVICE_NAME,
                GROUP_CONVERSATION_NAME,
                false
            )
            pages.conversationViewPage.assertMessageNotVisible(POST_LEAVE_MESSAGE)
        }
    }

    @TestCaseId("TC-4345", "TC-4360", "TC-4346", "TC-4354", "TC-4347", "TC-4356", "TC-4357")
    @Category("groups", "regression", "RC")
    @Ignore("Blocked: group creation/deletion parity needs stable create-group UI helpers and external-team setup.")
    @Test
    fun groupCreationAndDeletionCasesAreMappedForLaterMigration() = Unit

    @TestCaseId("TC-4351", "TC-4362", "TC-4366", "TC-4370")
    @Category("groups", "regression", "RC")
    @Test
    fun givenGroupConversation_whenAddingParticipant_thenParticipantIsVisibleAndSystemMessageIsShown() {
        prepareGroupConversation()
        loginCurrentUser()
        val addedMember = teamHelper.usersManager.findUserBy(ADDED_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        val addedMemberName = addedMember.name.orEmpty()

        step("Open group conversation details and participants tab") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible(GROUP_CONVERSATION_NAME)
                clickGroupConversation(GROUP_CONVERSATION_NAME)
            }
            pages.conversationViewPage.clickOnGroupConversationDetails(GROUP_CONVERSATION_NAME)
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapOnParticipantsTab()
            }
        }

        step("Add a team member to the group conversation") {
            pages.groupConversationDetailsPage.apply {
                tapAddParticipantsButton()
                assertUsernameInSuggestionsListIs(addedMemberName)
                selectUserInSuggestionList(addedMemberName)
                tapContinueButton()
                assertUsernameIsAddedToParticipantsList(addedMemberName)
                tapCloseButtonOnGroupConversationDetailsPage()
            }
        }

        step("Verify system message confirms the participant was added") {
            iSeeSystemMessage("You added $addedMemberName to the conversation")
        }
    }

    @TestCaseId("TC-4352", "TC-4364", "TC-4368", "TC-4371")
    @Category("groups", "regression", "RC")
    @Test
    fun givenGroupConversation_whenRemovingParticipant_thenParticipantIsAbsentAndSystemMessageIsShown() {
        prepareGroupConversation()
        loginCurrentUser()
        val removedMember = teamHelper.usersManager.findUserBy(EXTRA_MEMBER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        val removedMemberName = removedMember.name.orEmpty()

        step("Open group conversation details and participant profile") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible(GROUP_CONVERSATION_NAME)
                clickGroupConversation(GROUP_CONVERSATION_NAME)
            }
            pages.conversationViewPage.clickOnGroupConversationDetails(GROUP_CONVERSATION_NAME)
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapOnParticipantsTab()
                tapUserInParticipantsList(removedMemberName)
            }
        }

        step("Remove participant from the group conversation") {
            pages.connectedUserProfilePage.apply {
                assertRemoveFromConversationButtonForParticipant()
                tapRemoveFromConversationButtonForParticipant()
                assetRemoveConversationButtonOnModal()
                tapRemoveConversationButtonOnModal()
            }
            waitUntilToastIsDisplayed("${removedMember.uniqueUsername} was removed from the conversation")
        }

        step("Verify participant is absent and system message confirms removal") {
            pages.connectedUserProfilePage.tapCloseButtonOnConnectedUserProfilePage()
            pages.groupConversationDetailsPage.apply {
                assertUserIsNotInParticipantsList(removedMemberName)
                tapCloseButtonOnGroupConversationDetailsPage()
            }
            iSeeSystemMessage("You removed $removedMemberName from the conversation")
        }
    }

    @TestCaseId("TC-4361", "TC-4367", "TC-4372")
    @Category("groups", "leaveGroup", "regression", "RC")
    @Ignore("Blocked: remote member leave parity needs backend leave-conversation helper and system-message assertions.")
    @Test
    fun remoteMemberLeaveCasesAreMappedForLaterMigration() = Unit

    @TestCaseId("TC-4358", "TC-4375", "TC-4378", "TC-4373", "TC-4376", "TC-4379", "TC-4374")
    @Category("groups", "regression", "RC")
    @Ignore("Blocked: guest/external banners need stable guest and external-team setup plus banner selectors.")
    @Test
    fun guestAndExternalBannerCasesAreMappedForLaterMigration() = Unit

    @TestCaseId("TC-4377", "TC-4380", "TC-4359")
    @Category("groups", "regression", "RC")
    @Ignore("Blocked/stale: service and bot banner cases need legacy service/bot setup that is not present in Kotlin support.")
    @Test
    fun serviceAndBotBannerCasesAreMappedForLaterMigration() = Unit

    private fun prepareGroupConversation() {
        step("Prepare backend team owner, members, and group conversation") {
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
                "$TEAM_MEMBER_ALIAS,$EXTRA_MEMBER_ALIAS,$ADDED_MEMBER_ALIAS",
                TEAM_NAME,
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            testServiceHelper.userHasGroupConversationInTeam(
                TEAM_OWNER_ALIAS,
                GROUP_CONVERSATION_NAME,
                "$TEAM_MEMBER_ALIAS,$EXTRA_MEMBER_ALIAS",
                TEAM_NAME
            )
            testServiceHelper.addDevice(TEAM_MEMBER_ALIAS, null, MEMBER_DEVICE_NAME)
            currentUser = teamHelper.usersManager.findUserBy(TEAM_OWNER_ALIAS, ClientUserManager.FindBy.NAME_ALIAS)
        }
    }

    private fun loginCurrentUser() {
        step("Login team owner via staging deep link") {
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

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val EXTRA_MEMBER_ALIAS = "user3Name"
        const val ADDED_MEMBER_ALIAS = "user4Name"
        const val TEAM_NAME = "LeaveGroup"
        const val GROUP_CONVERSATION_NAME = "MyTeam"
        const val MEMBER_DEVICE_NAME = "Device1"
        const val HISTORY_MESSAGE = "Hello!"
        const val POST_LEAVE_MESSAGE = "Hello again!"
    }
}
