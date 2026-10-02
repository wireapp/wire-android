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

import androidx.test.ext.junit.runners.AndroidJUnit4
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
import uiautomatorutils.UiWaitUtils
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class NewMemberMessaging : BaseUiTest() {
    private var teamOwner: ClientUser? = null
    private var member1: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8605")
    @Category("criticalFlow")
    @Test
    fun givenUserJoinsNewTeam_whenMessagingAndMentionedInGroup_thenReceivesMessagesAndMentions() {
        step("Prepare team via backend, add members, and create group conversation") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Messaging",
                "en_US",
                true,
                backendClient,
                context
            )
            teamOwner = clientUserManager.findUserBy("user1Name", ClientUserManager.FindBy.NAME_ALIAS)

            backendSetupHelper.userConfiguresMLSForTeam(
                "user1Name",
                "Messaging",
                backendClient
            )

            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Messaging",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )

            member1 = clientUserManager.findUserBy("user2Name", ClientUserManager.FindBy.NAME_ALIAS)

            testServiceHelper.apply {
                listOf("user1Name", "user2Name", "user3Name").forEach { user ->
                    addDevice(user, null, "Device1")
                }

                userCreatesMLSGroupConversation(
                    ownerAlias = "user1Name",
                    participantAliases = "user3Name",
                    conversationName = "MyTeam",
                    deviceName = "Device1"
                )
            }
        }

        step("Login as team owner in Android app") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwner?.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwner?.password ?: "")
                clickLoginButton()
            }
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("Add the new team member to the existing MLS group through the UI") {
            pages.conversationListPage.clickGroupConversation("MyTeam")
            pages.conversationViewPage.clickOnGroupConversationDetails("MyTeam")
            pages.groupConversationDetailsPage.apply {
                tapOnParticipantsTab()
                tapAddParticipantsButton()
                assertUsernameInSuggestionsListIs(member1?.name ?: "")
                selectUserInSuggestionList(member1?.name ?: "")
                tapContinueButton()
                assertUsernameIsAddedToParticipantsList(member1?.name ?: "")
                tapCloseButtonOnGroupConversationDetailsPage()
            }
            pages.conversationViewPage.apply {
                assertSystemMessageVisible("You added ${member1?.name ?: ""} to the conversation")
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("Log out the team owner") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                tapLogoutButton()
                tapLogoutButton()
            }
            pages.registrationPage.assertEmailWelcomePage(timeout = UiWaitUtils.MEDIUM_TIMEOUT)
        }

        step("Login as the new team member in Android app") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
                enterTeamMemberLoggingEmail(member1?.email ?: "")
                clickLoginButton()
                enterTeamMemberLoggingPassword(member1?.password ?: "")
                clickLoginButton()
            }
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickDeclineShareDataAlert()
            }
        }

        step("Verify group conversation is visible and start a new conversation flow") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                tapStartNewConversationButton()
            }
        }

        step("Search for team owner and start 1:1 conversation") {
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUniqueUserNameInSearchField(clientUserManager, "user1Name")
                assertUsernameInSearchResultIs(teamOwner?.name ?: "")
                tapUsernameInSearchResult(teamOwner?.name ?: "")
            }
            pages.connectedUserProfilePage.apply {
                assertStartConversationButtonVisible()
                clickStartConversationButton()
            }
        }

        step("Send message to team owner from the new member and return to conversation list") {
            pages.conversationViewPage.apply {
                assertConversationScreenVisible()
                typeMessageInInputField("Hello Team Owner")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello Team Owner")
                tapBackButtonToCloseConversationViewPage()
            }

            pages.connectedUserProfilePage.apply {
                tapCloseButtonOnConnectedUserProfilePage()
            }

            pages.searchPage.apply {
                clickCloseButtonOnSearchInputField()
            }

            pages.conversationListPage.apply {
                UiWaitUtils.waitFor(1.seconds)
                clickCloseButtonOnNewConversationScreen()
                assertConversationListVisible()
            }
        }

        step("Send message to the new member via backend") {
            testServiceHelper.apply {
                userSendsGenericMessageToConversation(
                    "user1Name",
                    "user2Name",
                    "Device1",
                    "Hello new member"
                )
            }
        }

        step("Wait for notification popup to disappear") {
            pages.notificationsPage.apply {
                waitUntilNotificationPopUpGone()
            }
        }

        step("Verify unread message count and open unread conversation") {
            pages.conversationListPage.apply {
                assertUnreadMessagesCount("1")
                tapUnreadConversationNameInConversationList(teamOwner?.name ?: "")
            }
        }

        step("Verify received message is visible in conversation") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Hello new member")
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("Open group conversation and verify mention is visible when sent via backend") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                clickGroupConversation("MyTeam", timeout = 20.seconds)
            }

            testServiceHelper.apply {
                val mentionReplacedWithUniqueUserName = clientUserManager.replaceAliasesOccurrences(
                    "@user2Name",
                    ClientUserManager.FindBy.NAME_ALIAS
                )
                userSendsGenericMessageToConversation(
                    "user1Name",
                    "MyTeam",
                    "Device1",
                    mentionReplacedWithUniqueUserName
                )
            }

            pages.conversationViewPage.apply {
                val mentionedUser = clientUserManager.replaceAliasesOccurrences(
                    "@user2Name",
                    ClientUserManager.FindBy.NAME_ALIAS
                )
                assertVisibleMentionedNameIs(mentionedUser)
            }
        }
    }
}
