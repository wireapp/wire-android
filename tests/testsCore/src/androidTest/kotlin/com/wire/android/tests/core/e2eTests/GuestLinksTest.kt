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
class GuestLinksTest : BaseUiTest() {

    private var currentUser: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
        pages.loginPage.clickOkButtonOnRemovedDeviceAlertIfVisible()
        backendClient = BackendClient.loadBackend("STAGING")
    }

    @After
    fun tearDown() {
        runCatching { cleanupBackendClient(backendClient, currentUser) }
    }

    @TestCaseId("TC-4381", "TC-4382")
    @Category("guestLinks", "regression", "RC")
    @Test
    fun givenGuestsEnabledGroup_whenCreatingAndRevokingGuestLink_thenLinkCanBeSharedAndRemoved() {
        prepareGroupConversation(
            groupParticipantAliases = "$TEAM_MEMBER_ALIAS,$EXTRA_MEMBER_ALIAS",
            loginUserAlias = TEAM_OWNER_ALIAS
        )
        loginCurrentUser()
        openGroupConversation()

        val guestLink = createAndCopyGuestLink()
        sendGuestLinkToConversation(guestLink)
        revokeGuestLink()
    }

    @TestCaseId("TC-8122")
    @Category("guestLinks", "regression", "RC")
    @Test
    fun givenMemberIsNotParticipant_whenOpeningInviteDeepLinkAndJoining_thenConversationIsOpened() {
        prepareGroupConversation(
            groupParticipantAliases = EXTRA_MEMBER_ALIAS,
            loginUserAlias = TEAM_MEMBER_ALIAS
        )
        val inviteDeepLink = createInviteDeepLink()
        loginCurrentUser()

        step("Open invite deep link and join conversation") {
            pages.loginPage.openDeepLink(inviteDeepLink)
            pages.joinConversationPage.apply {
                assertJoinConversationAlertVisible(GROUP_CONVERSATION_NAME)
                tapJoinButton()
            }
        }

        step("Verify joined group conversation is in foreground") {
            pages.conversationViewPage.apply {
                assertChannelConversationInForeground(GROUP_CONVERSATION_NAME)
                assertMessageContainingTextIsVisibleInCurrentConversation("joined the conversation")
            }
        }
    }

    @TestCaseId("TC-4388")
    @Category("guestLinks", "regression", "RC")
    @Test
    fun givenMemberIsAlreadyParticipant_whenOpeningInviteDeepLink_thenConversationIsOpenedWithoutJoinMessage() {
        prepareGroupConversation(
            groupParticipantAliases = "$TEAM_MEMBER_ALIAS,$EXTRA_MEMBER_ALIAS",
            loginUserAlias = TEAM_MEMBER_ALIAS
        )
        val inviteDeepLink = createInviteDeepLink()
        loginCurrentUser()

        step("Open invite deep link for existing participant") {
            pages.loginPage.openDeepLink(inviteDeepLink)
        }

        step("Verify group conversation is opened without join system message") {
            pages.conversationViewPage.apply {
                assertChannelConversationInForeground(GROUP_CONVERSATION_NAME)
                assertMessageContainingTextNotVisible("joined the conversation", timeoutSeconds = 3)
            }
        }
    }

    @TestCaseId("TC-4389")
    @Category("guestLinks", "regression", "RC")
    @Test
    fun givenMemberIsNotParticipant_whenCancellingInviteDeepLinkJoin_thenConversationListIsShown() {
        prepareGroupConversation(
            groupParticipantAliases = EXTRA_MEMBER_ALIAS,
            loginUserAlias = TEAM_MEMBER_ALIAS
        )
        val inviteDeepLink = createInviteDeepLink()
        loginCurrentUser()

        step("Open invite deep link and cancel join") {
            pages.loginPage.openDeepLink(inviteDeepLink)
            pages.joinConversationPage.apply {
                assertJoinConversationAlertVisible(GROUP_CONVERSATION_NAME)
                tapCancelButton()
            }
        }

        step("Verify conversation list remains visible") {
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    @TestCaseId("TC-8124")
    @Category("guestLinks", "regression", "RC")
    @Test
    fun givenInviteLinkIsRevoked_whenOpeningInviteDeepLink_thenCannotJoinAlertIsShown() {
        prepareGroupConversation(
            groupParticipantAliases = EXTRA_MEMBER_ALIAS,
            loginUserAlias = TEAM_MEMBER_ALIAS
        )
        val inviteDeepLink = createInviteDeepLink()

        step("Revoke invite link via backend") {
            testServiceHelper.userRevokesInviteLink(TEAM_OWNER_ALIAS, GROUP_CONVERSATION_NAME)
        }

        loginCurrentUser()

        step("Open revoked invite deep link") {
            pages.loginPage.openDeepLink(inviteDeepLink)
        }

        step("Verify cannot-join alert is shown") {
            pages.joinConversationPage.apply {
                assertCannotJoinAlertVisible("Due to an error you could not be added to the conversation.")
                tapOkButton()
            }
        }
    }

    @TestCaseId("TC-8123")
    @Category("guestLinks", "regression", "RC")
    @Test
    fun givenConversationIsDeleted_whenOpeningInviteDeepLink_thenCannotJoinAlertIsShown() {
        prepareGroupConversation(
            groupParticipantAliases = EXTRA_MEMBER_ALIAS,
            loginUserAlias = TEAM_MEMBER_ALIAS
        )
        val inviteDeepLink = createInviteDeepLink()

        step("Delete conversation via backend") {
            testServiceHelper.userDeletesTeamConversation(TEAM_OWNER_ALIAS, GROUP_CONVERSATION_NAME)
        }

        loginCurrentUser()

        step("Open invite deep link for deleted conversation") {
            pages.loginPage.openDeepLink(inviteDeepLink)
        }

        step("Verify cannot-join alert is shown") {
            pages.joinConversationPage.apply {
                assertCannotJoinAlertVisible("Due to an error you could not be added to the conversation.")
                tapOkButton()
            }
        }
    }

    @TestCaseId("TC-8127")
    @Category("guestLinks", "regression", "RC")
    @Ignore(
        "Blocked: backend-generated password guest links are accepted by the API but current invite-code check does not expose the password field in the join dialog on the available emulator."
    )
    @Test
    fun givenPasswordProtectedInviteLink_whenEnteringCorrectPassword_thenConversationIsJoined() {
        prepareGroupConversation(
            groupParticipantAliases = EXTRA_MEMBER_ALIAS,
            loginUserAlias = TEAM_MEMBER_ALIAS
        )
        val inviteDeepLink = createInviteDeepLinkWithPassword(PASSWORD)
        loginCurrentUser()

        step("Open password-protected invite deep link and join conversation") {
            pages.loginPage.openDeepLink(inviteDeepLink)
            pages.joinConversationPage.apply {
                assertJoinConversationAlertVisible(GROUP_CONVERSATION_NAME)
                enterPassword(PASSWORD)
                tapJoinButton()
            }
        }

        step("Verify joined group conversation is in foreground") {
            pages.conversationViewPage.apply {
                assertChannelConversationInForeground(GROUP_CONVERSATION_NAME)
                assertMessageContainingTextIsVisibleInCurrentConversation("joined the conversation")
            }
        }
    }

    @TestCaseId("TC-8128")
    @Category("guestLinks", "regression", "RC")
    @Ignore(
        "Blocked: backend-generated password guest links are accepted by the API but current invite-code check does not expose the password field in the join dialog on the available emulator."
    )
    @Test
    fun givenPasswordProtectedInviteLink_whenEnteringWrongPassword_thenInvalidPasswordIsShown() {
        prepareGroupConversation(
            groupParticipantAliases = EXTRA_MEMBER_ALIAS,
            loginUserAlias = TEAM_MEMBER_ALIAS
        )
        val inviteDeepLink = createInviteDeepLinkWithPassword(PASSWORD)
        loginCurrentUser()

        step("Open password-protected invite deep link and enter wrong password") {
            pages.loginPage.openDeepLink(inviteDeepLink)
            pages.joinConversationPage.apply {
                assertJoinConversationAlertVisible(GROUP_CONVERSATION_NAME)
                enterPassword(WRONG_PASSWORD)
                tapJoinButton()
                assertInvalidPasswordErrorVisible()
            }
        }

        step("Verify join dialog remains visible") {
            pages.joinConversationPage.assertJoinConversationAlertVisible(GROUP_CONVERSATION_NAME)
        }
    }

    @TestCaseId("TC-8126")
    @Category("guestLinks", "regression", "RC")
    @Ignore(
        "Blocked: creating password-protected guest links through current UI still needs stable password guest-link page selectors and verification that invite-code check exposes the password field."
    )
    @Test
    fun givenGuestsEnabledGroup_whenCreatingPasswordProtectedGuestLink_thenLinkCanBeShared() = Unit

    @TestCaseId("TC-8125")
    @Category("guestLinks", "regression", "RC")
    @Ignore("Blocked: requires stable role-edit flow to promote another participant to group admin and a second-account login flow.")
    @Test
    fun givenGuestLinkExists_whenNewGroupAdminOpensGuestsPage_thenExistingLinkIsVisible() = Unit

    @TestCaseId("TC-8129")
    @Category("guestLinks", "regression", "RC")
    @Ignore("Blocked: requires password guest-link creation plus stable role-edit flow and second-account login flow.")
    @Test
    fun givenPasswordGuestLinkExists_whenNewGroupAdminOpensGuestsPage_thenExistingPasswordLinkIsVisible() = Unit

    @TestCaseId("TC-8150")
    @Category("guestLinks", "regression", "RC")
    @Ignore("Blocked: requires stable multi-account add, switch, current-account assertion, and other-account list assertions.")
    @Test
    fun givenMultipleAccounts_whenJoiningInviteLink_thenLastActivePermittedAccountIsUsed() = Unit

    private fun prepareGroupConversation(groupParticipantAliases: String, loginUserAlias: String) {
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
                "$TEAM_MEMBER_ALIAS,$EXTRA_MEMBER_ALIAS",
                TEAM_NAME,
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            testServiceHelper.userHasGroupConversationInTeam(
                TEAM_OWNER_ALIAS,
                GROUP_CONVERSATION_NAME,
                groupParticipantAliases,
                TEAM_NAME
            )
            currentUser = teamHelper.usersManager.findUserBy(loginUserAlias, ClientUserManager.FindBy.NAME_ALIAS)
        }
    }

    private fun createInviteDeepLink(): String {
        lateinit var inviteDeepLink: String
        step("Create invite deep link via backend") {
            inviteDeepLink = testServiceHelper.userCreatesInviteDeeplink(TEAM_OWNER_ALIAS, GROUP_CONVERSATION_NAME)
        }
        return inviteDeepLink
    }

    private fun createInviteDeepLinkWithPassword(password: String): String {
        lateinit var inviteDeepLink: String
        step("Create password-protected invite deep link via backend") {
            inviteDeepLink = testServiceHelper.userCreatesInviteDeeplinkWithPassword(
                TEAM_OWNER_ALIAS,
                GROUP_CONVERSATION_NAME,
                password
            )
        }
        return inviteDeepLink
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

    private fun openGroupConversation() {
        step("Open group conversation") {
            pages.conversationListPage.apply {
                assertConversationVisible(GROUP_CONVERSATION_NAME)
                tapConversationNameInConversationList(GROUP_CONVERSATION_NAME)
            }
            pages.conversationViewPage.assertChannelConversationInForeground(GROUP_CONVERSATION_NAME)
        }
    }

    private fun createAndCopyGuestLink(): String {
        lateinit var guestLink: String
        step("Create guest link without password and copy it") {
            openGroupDetails()
            pages.guestOptionsPage.apply {
                assertGuestsPageVisible()
                assertGuestsSwitchStateIs(STATE_ON)
                tapCreateLinkButton()
                tapCreateLinkWithoutPassword()
                assertGuestLinkCreated()
                guestLink = guestLinkText()
                tapCopyLinkButton()
                tapBackButton()
            }
        }
        return guestLink
    }

    private fun sendGuestLinkToConversation(guestLink: String) {
        step("Close details and send copied guest link in the conversation") {
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
            pages.conversationViewPage.apply {
                typeMessageInInputField(guestLink)
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation(guestLink)
            }
        }
    }

    private fun revokeGuestLink() {
        step("Disable guests and verify guest link is revoked") {
            openGroupDetails()
            pages.guestOptionsPage.apply {
                assertGuestsSwitchStateIs(STATE_ON)
                tapGuestsSwitch()
                tapDisableButtonOnGuestAccessDialog()
                UiWaitUtils.waitFor(1.seconds)
                assertGuestLinkNotVisible()
                assertGuestsSwitchStateIs(STATE_OFF)
            }
        }
    }

    private fun openGroupDetails() {
        val opened = UiWaitUtils.retryUntilTimeout(timeout = UiWaitUtils.LONG_TIMEOUT) {
            runCatching {
                pages.conversationViewPage.clickOnGroupConversationDetails(GROUP_CONVERSATION_NAME)
                pages.groupConversationDetailsPage.assertGroupDetailsPageVisible()
                true
            }.getOrDefault(false)
        }
        if (!opened) {
            throw AssertionError("Group details page is not visible.")
        }
        pages.groupConversationDetailsPage.tapGuestsOption()
    }

    private companion object {
        const val TEAM_OWNER_ALIAS = "user1Name"
        const val TEAM_MEMBER_ALIAS = "user2Name"
        const val EXTRA_MEMBER_ALIAS = "user3Name"
        const val TEAM_NAME = "Guests"
        const val GROUP_CONVERSATION_NAME = "GuestsHere"
        const val STATE_ON = "ON"
        const val STATE_OFF = "OFF"
        const val PASSWORD = "Password123!"
        const val WRONG_PASSWORD = "ThisIsWrong"
    }
}
