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
class MentionsTests : BaseUiTest() {
    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_BETA)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4448", "TC-4449")
    @Category("regression", "RC", "mentions")
    @Test
    fun givenTeamUsersInGroupConversation_whenISendAndReceiveMentions_thenMentionsAreVisible() {
        step("Given There is a team owner TeamOwner with team Notification") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Notification",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And User TeamOwner adds Member1 and Member2 to team Notification with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Notification",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner has group conversation MyTeam with Member1 and Member2 in team Notification") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name",
                "Notification"
            )
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(teamOwner)
        }

        step("And I see email verification Welcome Page") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("And I enter a valid email and password to sign in") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamOwnerLoggingPassword(teamOwner.password ?: "")
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in and decline share data alert") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("And I see and open group conversation MyTeam") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                tapConversationNameInConversationList("MyTeam")
            }
        }

        step("When I type the mention using Member1 first name into the text input field") {
            pages.conversationViewPage.typeMessageInInputField(
                clientUserManager.replaceAliasesOccurrences(
                    "@user2FirstName",
                    ClientUserManager.FindBy.FIRSTNAME_ALIAS
                )
            )
        }

        step("And I see and select Member1 from the mention list") {
            pages.conversationViewPage.apply {
                assertUserVisibleInMentionList(member1.name ?: "")
                tapUserInMentionList(member1.name ?: "")
            }
        }

        step("And I send the mention") {
            pages.conversationViewPage.clickSendButton()
        }

        step("Then I see the last mention is @Member1") {
            pages.conversationViewPage.assertVisibleMentionedNameIs(
                clientUserManager.replaceAliasesOccurrences(
                    "@user2Name",
                    ClientUserManager.FindBy.NAME_ALIAS
                )
            )
        }

        step("And I hide the keyboard and return to the conversation list") {
            device.pressBack()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
            pages.conversationListPage.assertConversationListVisible()
        }

        // TC-4449 - I want to be able to receive mentions from team users in a group conversation
        step("When Member2 sends a mention to TeamOwner in group conversation MyTeam") {
            testServiceHelper.userSendsMentionToConversation(
                "user3Name",
                "user1Name",
                "MyTeam"
            )
        }

        step("And I open unread group conversation MyTeam") {
            pages.conversationListPage.tapUnreadConversationNameInConversationList("MyTeam")
        }

        step("Then I see the last mention is @TeamOwner") {
            pages.conversationViewPage.assertVisibleMentionedNameIs(
                clientUserManager.replaceAliasesOccurrences(
                    "@user1Name",
                    ClientUserManager.FindBy.NAME_ALIAS
                )
            )
        }
    }
}
