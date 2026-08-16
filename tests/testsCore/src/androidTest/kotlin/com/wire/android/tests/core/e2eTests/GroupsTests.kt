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
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uiautomatorutils.UiWaitUtils.waitUntilToastIsDisplayed
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
@Suppress("LargeClass")
class GroupsTests : BaseUiTest() {
    private lateinit var teamOwner: ClientUser
    private lateinit var member1: ClientUser
    private lateinit var member2: ClientUser
    private lateinit var member3: ClientUser
    private lateinit var guest: ClientUser
    private lateinit var external: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4345")
    @Category("regression", "RC", "groups")
    @Test
    fun givenIAmATeamMember_whenCreatingGroupConversationWithMembersFromMyTeam_thenGroupConversationIsCreated() {
        step("Given There is a team owner user1Name with team GroupCreation and members user2Name,user3Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "GroupCreation",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "GroupCreation",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            member2 = clientUserManager.findUserByNameOrNameAlias("user3Name")
        }

        step("And User user3Name has Device1 for backend messaging") {
            testServiceHelper.addDevice("user3Name", null, "Device1")
        }

        givenILoginAsTeamMemberThroughStagingDeepLink()

        step("When I start a new conversation and tap New Group") {
            pages.conversationListPage.tapStartNewConversationButton()
            pages.searchPage.tapCreateNewGroupButton()
        }

        step("And I add user1Name and user3Name as participants") {
            pages.searchPage.apply {
                tapSearchPeopleField()
                typeUserNameInSearchField(clientUserManager, "user1Name")
                assertUsernameInSearchResultIs(teamOwner.name ?: "")
                tapUsernameInSearchResult(teamOwner.name ?: "")
                clearSearchInputField()
                typeUserNameInSearchField(clientUserManager, "user3Name")
                assertUsernameInSearchResultIs(member2.name ?: "")
                tapUsernameInSearchResult(member2.name ?: "")
                tapContinueButtonOnAddParticipantsPage()
            }
        }

        step("And I enter MyTeam as group name and continue") {
            pages.createNewGroupPage.apply {
                assertCreateNewGroupDetailsPageVisible()
                enterNewGroupName("MyTeam")
                tapContinueButtonOnGroupDetailsPage()
            }
        }

        step("And I see group settings and create the group") {
            pages.createNewGroupPage.apply {
                assertCreateNewGroupSettingsPageVisible()
                tapCreateGroupButtonOnGroupSettingsPage()
            }
        }

        step("Then I see group conversation MyTeam is in foreground") {
            pages.conversationViewPage.assertGroupConversationInForeground("MyTeam")
        }

        step("When I send message Hello! and see it in current conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        step("And User user3Name sends message Hello to you, too! to group conversation MyTeam") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Hello to you, too!",
                "Device1",
                "MyTeam"
            )
        }

        step("Then I see the message Hello to you, too! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello to you, too!")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4360")
    @Category("regression", "RC", "groups")
    @Test
    fun givenIAmAnExternalUserInATeam_whenAttemptingToCreateGroupConversation_thenGroupConversationCannotBeCreated() {
        step("Given There is a team owner user1Name with team GroupCreation and external member user2Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "GroupCreation",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "GroupCreation",
                TeamRoles.External,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        givenILoginAsTeamMemberThroughStagingDeepLink()

        step("When I start a new conversation") {
            pages.conversationListPage.tapStartNewConversationButton()
        }

        step("Then I do not see create new group button") {
            pages.searchPage.assertCreateNewGroupButtonNotVisible()
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4346")
    @Category("regression", "RC", "groups", "deleteGroup")
    @Test
    fun givenIAmAGroupCreator_whenDeletingGroupConversation_thenGroupConversationIsDeleted() {
        step("Given There is a team owner user1Name with team GroupDeletion and members user2Name,user3Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "GroupDeletion",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "GroupDeletion",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User user1Name has group conversation MyTeam with user2Name,user3Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name",
                "GroupDeletion"
            )
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I see and open group conversation MyTeam") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                clickGroupConversation("MyTeam")
            }
        }

        step("And I open MyTeam group details and show more options") {
            pages.conversationViewPage.clickOnGroupConversationDetails("MyTeam")
            pages.groupConversationDetailsPage.tapShowMoreOptionsButton()
        }

        step("When I delete the group conversation and confirm") {
            pages.groupConversationDetailsPage.apply {
                tapDeleteConversationButton()
                tapDeleteGroupButton()
            }
        }

        step("Then I see removed toast message for MyTeam") {
            waitUntilToastIsDisplayed("“MyTeam” removed")
        }

        step("And I see conversation list without conversation MyTeam") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertConversationNotVisible("MyTeam")
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4354")
    @Category("regression", "RC", "groups", "deleteGroup")
    @Test
    fun givenIAmAGroupCreator_whenDeletingGroupFromConversationList_thenGroupConversationIsDeleted() {
        step("Given There is a team owner user1Name with team GroupDeletion and members user2Name,user3Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "GroupDeletion",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "GroupDeletion",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User user1Name has group conversation MyTeam with user2Name,user3Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name",
                "GroupDeletion"
            )
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I see group conversation MyTeam in conversation list") {
            pages.conversationListPage.assertGroupConversationVisible("MyTeam")
        }

        step("When I long press MyTeam in conversation list") {
            pages.conversationListPage.longPressConversation("MyTeam")
        }

        step("And I delete the group conversation and confirm") {
            pages.conversationListPage.apply {
                assertDeleteConversationButtonVisibleInConversationActions()
                tapDeleteConversationButtonInConversationActions()
                assertDeleteConversationConfirmationModalVisible("MyTeam")
                tapDeleteConversationButtonOnModal()
            }
        }

        step("Then I see removed toast message for MyTeam") {
            waitUntilToastIsDisplayed("“MyTeam” removed")
        }

        step("And I see conversation list without conversation MyTeam") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertConversationNotVisible("MyTeam")
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4347")
    @Category("regression", "RC", "groups", "deleteGroup")
    @Test
    fun givenIAmNotAGroupCreator_whenAttemptingToDeleteGroupConversation_thenGroupConversationCannotBeDeleted() {
        step("Given There is a team owner user1Name with team GroupDeletion and members user2Name,user3Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "GroupDeletion",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "GroupDeletion",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And User user1Name has group conversation MyTeam with user2Name,user3Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name",
                "GroupDeletion"
            )
        }

        givenILoginAsTeamMemberThroughStagingDeepLink()

        step("And I see and open group conversation MyTeam") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                clickGroupConversation("MyTeam")
            }
        }

        step("And I open MyTeam group details") {
            pages.conversationViewPage.clickOnGroupConversationDetails("MyTeam")
        }

        step("When I tap show more options button") {
            pages.groupConversationDetailsPage.tapShowMoreOptionsButton()
        }

        step("Then I do not see delete conversation button") {
            pages.groupConversationDetailsPage.assertDeleteConversationButtonNotVisible()
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4356")
    @Category("regression", "RC", "groups", "deleteGroup")
    @Test
    fun givenIDeletedAGroupConversation_whenSendingAndReceivingMessages_thenMessagesAreSentAndReceivedSuccessfully() {
        step("Given There is a team owner user1Name with team GroupDeletion and members user2Name,user3Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "GroupDeletion",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "GroupDeletion",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User user2Name has Device1 for backend messaging") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }

        step("And User user1Name has group conversations DeleteTeam and StaysTeam with user2Name,user3Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "DeleteTeam",
                "user2Name,user3Name",
                "GroupDeletion"
            )
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "StaysTeam",
                "user2Name,user3Name",
                "GroupDeletion"
            )
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I see and open group conversation DeleteTeam") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("DeleteTeam")
                clickGroupConversation("DeleteTeam")
            }
        }

        step("When I send message Hello! and see it in current conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        step("And User user2Name sends message Hello 2 to group conversation DeleteTeam") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Hello 2",
                "Device1",
                "DeleteTeam"
            )
        }

        step("Then I see the message Hello 2 in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello 2")
        }

        step("And I open DeleteTeam group details and show more options") {
            pages.conversationViewPage.clickOnGroupConversationDetails("DeleteTeam")
            pages.groupConversationDetailsPage.tapShowMoreOptionsButton()
        }

        step("When I delete the group conversation and confirm") {
            pages.groupConversationDetailsPage.apply {
                tapDeleteConversationButton()
                tapDeleteGroupButton()
            }
        }

        step("Then I see removed toast message for DeleteTeam") {
            waitUntilToastIsDisplayed("“DeleteTeam” removed")
        }

        step("And I see StaysTeam but do not see DeleteTeam in conversation list") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertConversationNotVisible("DeleteTeam")
                assertGroupConversationVisible("StaysTeam")
            }
        }

        step("And I open group conversation StaysTeam") {
            pages.conversationListPage.clickGroupConversation("StaysTeam")
        }

        step("When I send message Hello 3 and see it in current conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello 3")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello 3")
            }
        }

        step("And User user2Name sends message Hello 3 to group conversation StaysTeam") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Hello 3",
                "Device1",
                "StaysTeam"
            )
        }

        step("Then I see the message Hello 3 in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello 3")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4357")
    @Category("regression", "RC", "groups", "deleteGroup")
    @Test
    fun givenAnotherUserDeletedAGroupConversation_whenSendingAndReceivingMessages_thenMessagesAreSentAndReceivedSuccessfully() {
        step("Given There is a team owner user1Name with team GroupDeletion and members user2Name,user3Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "GroupDeletion",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "GroupDeletion",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name has group conversations DeleteMe and Stay with user2Name,user3Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "DeleteMe",
                "user2Name,user3Name",
                "GroupDeletion"
            )
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "Stay",
                "user2Name,user3Name",
                "GroupDeletion"
            )
        }

        step("And Users user1Name and user3Name have devices for backend messaging") {
            testServiceHelper.apply {
                addDevice("user1Name", null, "Device1")
                addDevice("user3Name", null, "Device2")
            }
        }

        step("And User user2Name is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        givenILoginAsTeamMemberThroughStagingDeepLink()

        step("And I see group conversations DeleteMe and Stay in conversation list") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("DeleteMe")
                assertGroupConversationVisible("Stay")
            }
        }

        step("And I open group conversation DeleteMe") {
            pages.conversationListPage.clickGroupConversation("DeleteMe")
        }

        step("And I send message Hello! and see it in current conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        step("And User user3Name sends message Hello to you, too! to group conversation DeleteMe") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "Hello to you, too!",
                "Device2",
                "DeleteMe"
            )
        }

        step("And I see the message Hello to you, too! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello to you, too!")
        }

        step("And User user1Name reads the recent message from group conversation DeleteMe via Device1") {
            testServiceHelper.userReadsRecentMessageFromGroupConversation("user1Name", "DeleteMe", "Device1")
        }

        step("When Group admin user user1Name deletes conversation DeleteMe") {
            backendSetupHelper.userDeletesGroupConversation("user1Name", "DeleteMe")
        }

        step("Then I see Stay but do not see DeleteMe in conversation list") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertGroupConversationVisible("Stay")
                assertConversationNotVisible("DeleteMe")
            }
        }

        step("When User user3Name sends message This group should still be there to group conversation Stay") {
            testServiceHelper.userSendMessageToConversation(
                "user3Name",
                "This group should still be there",
                "Device2",
                "Stay"
            )
        }

        step("And I wait until the notification popup disappears") {
            pages.notificationsPage.waitUntilNotificationPopUpGone()
        }

        step("Then I see group conversation Stay has 1 unread message") {
            pages.conversationListPage.assertConversationHasUnreadMessagesCount("Stay", "1")
        }

        step("And I open unread group conversation Stay") {
            pages.conversationListPage.tapUnreadConversationNameInConversationList("Stay")
        }

        step("And I see the message This group should still be there in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation(
                "This group should still be there"
            )
        }

        step("And User user1Name reads the recent message from group conversation Stay via Device1") {
            testServiceHelper.userReadsRecentMessageFromGroupConversation("user1Name", "Stay", "Device1")
        }

        step("And I send message This worked. and see it in current conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("This worked.")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("This worked.")
            }
        }

        step("And User user1Name reads the recent message from group conversation Stay via Device1") {
            testServiceHelper.userReadsRecentMessageFromGroupConversation("user1Name", "Stay", "Device1")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4348", "TC-4349", "TC-4350")
    @Category("regression", "RC", "groups", "leaveGroup")
    @Test
    fun givenIAmAGroupCreator_whenLeavingGroupConversation_thenGroupHistoryRemainsVisibleAndNewMessagesAreNotVisible() {
        step("Given There is a team owner user1Name with team LeaveGroup and members user2Name,user3Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "LeaveGroup",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "LeaveGroup",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User user1Name has group conversation MyTeam with user2Name,user3Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name",
                "LeaveGroup"
            )
        }

        step("And User user2Name has Device1 for backend messaging") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I see and open group conversation MyTeam") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                clickGroupConversation("MyTeam")
            }
        }

        step("And User user2Name sends message Hello! to group conversation MyTeam") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Hello!",
                "Device1",
                "MyTeam"
            )
        }

        step("And I see the message Hello! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("And I open MyTeam group details and show more options") {
            pages.conversationViewPage.clickOnGroupConversationDetails("MyTeam")
            pages.groupConversationDetailsPage.tapShowMoreOptionsButton()
        }

        step("When I leave the group conversation and confirm") {
            pages.groupConversationDetailsPage.apply {
                tapLeaveConversationButton()
                tapLeaveConversationConfirmButton()
            }
        }

        step("Then I see you left conversation toast message") {
            waitUntilToastIsDisplayed("You left the conversation.")
        }

        step("And I see conversation list with group conversation MyTeam") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertGroupConversationVisible("MyTeam")
            }
        }

        // TC-4349 - I want to be able to see conversation history after I left the conversation
        step("When I open group conversation MyTeam") {
            pages.conversationListPage.clickGroupConversation("MyTeam")
        }

        step("Then I see system message You left the conversation") {
            pages.conversationViewPage.assertSystemMessageVisible("You left the conversation")
        }

        step("And I see the message Hello! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        // TC-4350 - I should not be able to see new messages after I left the conversation
        step("When User user2Name sends message Hello again! to group conversation MyTeam") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Hello again!",
                "Device1",
                "MyTeam"
            )
        }

        step("Then I do not see the message Hello again! in current conversation") {
            pages.conversationViewPage.assertMessageNotVisible("Hello again!")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4355")
    @Category("regression", "RC", "groups", "leaveGroup")
    @Test
    fun givenIAmAGroupCreator_whenLeavingGroupConversationFromConversationList_thenGroupConversationIsLeft() {
        step("Given There is a team owner user1Name with team LeaveGroup and members user2Name,user3Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "LeaveGroup",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "LeaveGroup",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User user1Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User user1Name has group conversation MyTeam with user2Name,user3Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name",
                "LeaveGroup"
            )
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I see group conversation MyTeam in conversation list") {
            pages.conversationListPage.assertGroupConversationVisible("MyTeam")
        }

        step("When I long press MyTeam in conversation list") {
            pages.conversationListPage.longPressConversation("MyTeam")
        }

        step("And I leave the group conversation and confirm") {
            pages.conversationListPage.apply {
                assertLeaveConversationButtonVisibleInConversationActions()
                tapLeaveConversationButtonInConversationActions()
                assertLeaveConversationConfirmationModalVisible("MyTeam")
                tapLeaveConversationButtonOnModal()
            }
        }

        step("Then I see you left conversation toast message") {
            waitUntilToastIsDisplayed("You left the conversation.")
        }

        step("And I see conversation list with group conversation MyTeam") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertGroupConversationVisible("MyTeam")
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4361")
    @Category("regression", "RC", "groups", "leaveGroup")
    @Test
    fun givenIAmAGroupAdmin_whenAnotherMemberLeavesGroupConversation_thenSystemMessageIsVisible() {
        step("Given There is a team owner user1Name with team LeaveGroup and members user2Name,user3Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "LeaveGroup",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "LeaveGroup",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member2 = clientUserManager.findUserByNameOrNameAlias("user3Name")
        }

        step("And User user1Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User user1Name has group conversation MyTeam with user2Name,user3Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name",
                "LeaveGroup"
            )
        }

        step("And User user2Name has Device1 for backend messaging") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I see and open group conversation MyTeam") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                clickGroupConversation("MyTeam")
            }
        }

        step("And User user2Name sends message Hello! to group conversation MyTeam") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Hello!",
                "Device1",
                "MyTeam"
            )
        }

        step("And I see the message Hello! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("When user3Name leaves group conversation MyTeam") {
            backendSetupHelper.userRemovesUserFromGroupConversation(
                "user3Name",
                "user3Name",
                "MyTeam"
            )
        }

        step("Then I see system message user3Name left the conversation") {
            pages.conversationViewPage.assertSystemMessageVisible("${member2.name ?: ""} left the conversation")
        }

        step("When I send message Hello! and see it in current conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello!")
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4367")
    @Category("regression", "RC", "groups", "leaveGroup")
    @Test
    fun givenIAmAGroupMember_whenAnotherMemberLeavesGroupConversation_thenSystemMessageIsVisible() {
        step("Given There is a team owner user1Name with team LeaveGroup and members user2Name,user3Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "LeaveGroup",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "LeaveGroup",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member2 = clientUserManager.findUserByNameOrNameAlias("user3Name")
        }

        step("And User user2Name is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        step("And User user1Name has group conversation MyTeam with user2Name,user3Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name",
                "LeaveGroup"
            )
        }

        step("And User user1Name has Device1 for backend messaging") {
            testServiceHelper.addDevice("user1Name", null, "Device1")
        }

        givenILoginAsTeamMemberThroughStagingDeepLink()

        step("And I see and open group conversation MyTeam") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                clickGroupConversation("MyTeam")
            }
        }

        step("And User user1Name sends message Hello! to group conversation MyTeam") {
            testServiceHelper.userSendMessageToConversation(
                "user1Name",
                "Hello!",
                "Device1",
                "MyTeam"
            )
        }

        step("And I see the message Hello! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("When user3Name leaves group conversation MyTeam") {
            backendSetupHelper.userRemovesUserFromGroupConversation(
                "user3Name",
                "user3Name",
                "MyTeam"
            )
        }

        step("Then I see system message user3Name left the conversation") {
            pages.conversationViewPage.assertSystemMessageVisible("${member2.name ?: ""} left the conversation")
        }

        step("When I send message Hello! and see it in current conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello!")
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4372")
    @Category("regression", "RC", "groups", "leaveGroup")
    @Test
    fun givenIAmAGroupGuest_whenAnotherMemberLeavesGroupConversation_thenSystemMessageIsVisible() {
        step("Given There is a team owner user1Name with team LeaveGroup and members user2Name,user3Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "LeaveGroup",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "LeaveGroup",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member2 = clientUserManager.findUserByNameOrNameAlias("user3Name")
        }

        step("And There is a personal user user4Name") {
            clientUserManager.createPersonalUsersByAliases(listOf("user4Name"), backendClient)
        }

        step("And User user4Name has a unique username") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user4Name")
            }
        }

        step("And User user1Name is connected to user4Name") {
            backendSetupHelper.userIsConnectedTo("user1Name", "user4Name")
        }

        step("And User user1Name has group conversation MyTeam with user2Name,user3Name,user4Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name,user4Name",
                "LeaveGroup"
            )
        }

        step("And User user4Name is me") {
            guest = clientUserManager.findUserByNameOrNameAlias("user4Name")
        }

        step("And User user2Name has Device1 for backend messaging") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }

        givenILoginAsGuestThroughStagingDeepLink()

        step("And I see and open group conversation MyTeam") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                clickGroupConversation("MyTeam")
            }
        }

        step("And User user2Name sends message Hello! to group conversation MyTeam") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Hello!",
                "Device1",
                "MyTeam"
            )
        }

        step("And I see the message Hello! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("When user3Name leaves group conversation MyTeam") {
            backendSetupHelper.userRemovesUserFromGroupConversation(
                "user3Name",
                "user3Name",
                "MyTeam"
            )
        }

        step("Then I see system message user3Name left the conversation") {
            pages.conversationViewPage.assertSystemMessageVisible("${member2.name ?: ""} left the conversation")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4351", "TC-4362")
    @Category("regression", "RC", "groups", "removeGroup")
    @Test
    fun givenIAmAGroupAdmin_whenRemovingParticipantFromGroupConversation_thenParticipantIsRemovedAndSystemMessageIsVisible() {
        step("Given There is a team owner user1Name with team RemoveGroup and members user2Name,user3Name,user4Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "RemoveGroup",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name,user4Name",
                "RemoveGroup",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member3 = clientUserManager.findUserByNameOrNameAlias("user4Name")
        }

        step("And User user1Name has group conversation MyTeam with user2Name,user3Name,user4Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name,user4Name",
                "RemoveGroup"
            )
        }

        step("And User user1Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User user2Name has Device1 for backend messaging") {
            testServiceHelper.addDevice("user2Name", null, "Device1")
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I open group conversation MyTeam and see it in foreground") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                clickGroupConversation("MyTeam")
            }
            pages.conversationViewPage.assertGroupConversationInForeground("MyTeam")
        }

        step("And I open group details and see user4Name in participants list") {
            pages.conversationViewPage.clickOnGroupConversationDetails("MyTeam")
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapOnParticipantsTab()
                assertUsernameIsAddedToParticipantsList(member3.name ?: "")
            }
        }

        step("When I tap user4Name in participants list") {
            pages.groupConversationDetailsPage.tapUserInParticipantsList(member3.name ?: "")
        }

        step("Then I see connected user user4Name profile with remove from conversation button") {
            pages.connectedUserProfilePage.apply {
                assertConnectedUserProfileVisible(member3.name ?: "")
                assertRemoveFromConversationButtonForParticipant()
            }
        }

        step("When I tap remove from conversation button and see the confirmation alert") {
            pages.connectedUserProfilePage.apply {
                tapRemoveFromConversationButtonForParticipant()
                assetRemoveConversationButtonOnModal()
            }
        }

        step("And I confirm removing user4Name") {
            pages.connectedUserProfilePage.tapRemoveConversationButtonOnModal()
        }

        step("Then I see toast message that ${member3.uniqueUsername} was removed from the conversation") {
            waitUntilToastIsDisplayed("${member3.uniqueUsername} was removed from the conversation")
        }

        step("And I do not see remove from conversation button") {
            pages.connectedUserProfilePage.assertRemoveFromConversationButtonForParticipantNotVisible()
        }

        // TC-4362 - I want to see a system message when another member was removed from the conversation as an admin
        step("When I close the user profile and group conversation details") {
            pages.connectedUserProfilePage.tapCloseButtonOnConnectedUserProfilePage()
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
        }

        step("Then I see system message You removed user4Name from the conversation") {
            pages.conversationViewPage.assertSystemMessageVisible(
                "You removed ${member3.name ?: ""} from the conversation"
            )
        }

        step("When I send message Hello! and see it in current conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        step("When User user2Name sends message Hello to you, too! to group conversation MyTeam") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Hello to you, too!",
                "Device1",
                "MyTeam"
            )
        }

        step("Then I see the message Hello to you, too! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello to you, too!")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4366")
    @Category("regression", "RC", "groups", "removeGroup")
    @Test
    fun givenIAmAGroupMember_whenAnotherMemberIsRemovedFromGroupConversation_thenSystemMessageIsVisible() {
        step("Given There is a team owner user1Name with team RemoveGroup and members user2Name,user3Name,user4Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "RemoveGroup",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name,user4Name",
                "RemoveGroup",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member3 = clientUserManager.findUserByNameOrNameAlias("user4Name")
        }

        step("And User user1Name has group conversation MyTeam with user2Name,user3Name,user4Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name,user4Name",
                "RemoveGroup"
            )
        }

        step("And User user2Name is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        givenILoginAsTeamMemberThroughStagingDeepLink()

        step("And I open group conversation MyTeam and see it in foreground") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                clickGroupConversation("MyTeam")
            }
            pages.conversationViewPage.assertGroupConversationInForeground("MyTeam")
        }

        step("When User user1Name removes user user4Name from group conversation MyTeam") {
            backendSetupHelper.userRemovesUserFromGroupConversation(
                "user1Name",
                "user4Name",
                "MyTeam"
            )
        }

        step("Then I see system message user1Name removed user4Name from the conversation") {
            pages.conversationViewPage.assertSystemMessageVisible(
                "${teamOwner.name ?: ""} removed ${member3.name ?: ""} from the conversation"
            )
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4370")
    @Category("regression", "RC", "groups", "removeGroup")
    @Test
    fun givenIAmAGroupGuest_whenAnotherMemberIsRemovedFromGroupConversation_thenSystemMessageIsVisible() {
        step("Given There is a team owner user1Name with team RemoveGroup and members user2Name,user3Name,user4Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "RemoveGroup",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name,user4Name",
                "RemoveGroup",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member3 = clientUserManager.findUserByNameOrNameAlias("user4Name")
        }

        step("And There is a personal user user5Name") {
            clientUserManager.createPersonalUsersByAliases(listOf("user5Name"), backendClient)
        }

        step("And User user5Name has a unique username") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user5Name")
            }
        }

        step("And User user1Name is connected to user5Name") {
            backendSetupHelper.userIsConnectedTo("user1Name", "user5Name")
        }

        step("And User user1Name has group conversation MyTeam with user2Name,user3Name,user4Name,user5Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name,user4Name,user5Name",
                "RemoveGroup"
            )
        }

        step("And User user5Name is me") {
            guest = clientUserManager.findUserByNameOrNameAlias("user5Name")
        }

        givenILoginAsGuestThroughStagingDeepLink()

        step("And I open group conversation MyTeam and see it in foreground") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                clickGroupConversation("MyTeam")
            }
            pages.conversationViewPage.assertGroupConversationInForeground("MyTeam")
        }

        step("When User user1Name removes user user4Name from group conversation MyTeam") {
            backendSetupHelper.userRemovesUserFromGroupConversation(
                "user1Name",
                "user4Name",
                "MyTeam"
            )
        }

        step("Then I see system message user1Name removed user4Name from the conversation") {
            pages.conversationViewPage.assertSystemMessageVisible(
                "${teamOwner.name ?: ""} removed ${member3.name ?: ""} from the conversation"
            )
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4352", "TC-4364")
    @Category("regression", "RC", "groups", "addGroup")
    @Test
    fun givenIAmAGroupAdmin_whenAddingParticipantToExistingGroupConversation_thenParticipantIsAddedAndSystemMessageIsVisible() {
        step("Given There is a team owner user1Name with team AddGroup and members user2Name,user3Name,user4Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "AddGroup",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name,user4Name",
                "AddGroup",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            member3 = clientUserManager.findUserByNameOrNameAlias("user4Name")
        }

        step("And User user1Name has group conversation MyTeam with user2Name,user3Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name",
                "AddGroup"
            )
        }

        step("And User user1Name is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        step("And User user4Name has Device1 for backend messaging") {
            testServiceHelper.addDevice("user4Name", null, "Device1")
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I open group conversation MyTeam and see it in foreground") {
            pages.conversationListPage.clickGroupConversation("MyTeam")
            pages.conversationViewPage.assertGroupConversationInForeground("MyTeam")
        }

        step("And I open group details and the participants tab") {
            pages.conversationViewPage.clickOnGroupConversationDetails("MyTeam")
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapOnParticipantsTab()
            }
        }

        step("When I tap Add Participants button") {
            pages.groupConversationDetailsPage.tapAddParticipantsButton()
        }

        step("Then I see user4Name in search suggestions list") {
            pages.groupConversationDetailsPage.assertUsernameInSuggestionsListIs(member3.name ?: "")
        }

        step("When I select user4Name and continue") {
            pages.groupConversationDetailsPage.apply {
                selectUserInSuggestionList(member3.name ?: "")
                tapContinueButton()
            }
        }

        step("Then I see user4Name in participants list") {
            pages.groupConversationDetailsPage.assertUsernameIsAddedToParticipantsList(member3.name ?: "")
        }

        step("When I close the group conversation details") {
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
        }

        // TC-4364 - I want to see a system message when another member gets added to a group conversation as an admin
        step("Then I see system message You added user4Name to the conversation") {
            pages.conversationViewPage.assertSystemMessageVisible(
                "You added ${member3.name ?: ""} to the conversation"
            )
        }

        step("When I send message Hello! and see it in current conversation") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        step("When User user4Name sends message Hello to you, too! to group conversation MyTeam") {
            testServiceHelper.userSendMessageToConversation(
                "user4Name",
                "Hello to you, too!",
                "Device1",
                "MyTeam"
            )
        }

        step("Then I see the message Hello to you, too! in current conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello to you, too!")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4368")
    @Category("regression", "RC", "groups", "addGroup")
    @Test
    fun givenIAmAGroupMember_whenTeamOwnerAddsAnotherMemberToGroupConversation_thenISeeTheSystemMessage() {
        step("Given There is a team owner user1Name with team AddGroup and members user2Name,user3Name,user4Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "AddGroup",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name,user4Name",
                "AddGroup",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member3 = clientUserManager.findUserByNameOrNameAlias("user4Name")
        }

        step("And User user1Name has group conversation MyTeam with user2Name,user3Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name",
                "AddGroup"
            )
        }

        step("And User user2Name is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        givenILoginAsTeamMemberThroughStagingDeepLink()

        step("And I open group conversation MyTeam and see it in foreground") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                clickGroupConversation("MyTeam")
            }
            pages.conversationViewPage.assertGroupConversationInForeground("MyTeam")
        }

        step("When User user1Name adds user user4Name to group conversation MyTeam") {
            backendSetupHelper.userXAddedContactsToGroupChat(
                "user1Name",
                "user4Name",
                "MyTeam"
            )
        }

        step("Then I see system message user1Name added user4Name to the conversation") {
            pages.conversationViewPage.assertSystemMessageVisible(
                "${teamOwner.name ?: ""} added ${member3.name ?: ""} to the conversation"
            )
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4371")
    @Category("regression", "RC", "groups", "addGroup")
    @Test
    fun givenIAmAGroupGuest_whenTeamOwnerAddsAnotherMemberToGroupConversation_thenISeeTheSystemMessage() {
        step("Given There is a team owner user1Name with team AddGroup and members user2Name,user3Name,user4Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "AddGroup",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name,user4Name",
                "AddGroup",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member3 = clientUserManager.findUserByNameOrNameAlias("user4Name")
        }

        step("And There is a personal user user5Name") {
            clientUserManager.createPersonalUsersByAliases(listOf("user5Name"), backendClient)
        }

        step("And User user5Name has a unique username") {
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user5Name")
            }
        }

        step("And User user1Name is connected to user5Name") {
            backendSetupHelper.userIsConnectedTo("user1Name", "user5Name")
        }

        step("And User user1Name has group conversation MyTeam with user2Name,user3Name,user5Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name,user5Name",
                "AddGroup"
            )
        }

        step("And User user5Name is me") {
            guest = clientUserManager.findUserByNameOrNameAlias("user5Name")
        }

        givenILoginAsGuestThroughStagingDeepLink()

        step("And I open group conversation MyTeam and see it in foreground") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                clickGroupConversation("MyTeam")
            }
            pages.conversationViewPage.assertGroupConversationInForeground("MyTeam")
        }

        step("When User user1Name adds user user4Name to group conversation MyTeam") {
            backendSetupHelper.userXAddedContactsToGroupChat(
                "user1Name",
                "user4Name",
                "MyTeam"
            )
        }

        step("Then I see system message user1Name added user4Name to the conversation") {
            pages.conversationViewPage.assertSystemMessageVisible(
                "${teamOwner.name ?: ""} added ${member3.name ?: ""} to the conversation"
            )
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4358")
    @Category("regression", "RC", "groups", "addGroup", "WPB-3047")
    @Test
    fun givenIAmAGroupMember_whenTeamOwnerRemovesAndAddsMembers_thenISeeUpdatedParticipantList() {
        step("Given There is a team owner user1Name with team AddGroup and members user2Name,user3Name,user4Name") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "AddGroup",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name,user4Name",
                "AddGroup",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            member2 = clientUserManager.findUserByNameOrNameAlias("user3Name")
            member3 = clientUserManager.findUserByNameOrNameAlias("user4Name")
        }

        step("And User user1Name has group conversation MyTeam with user2Name") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name",
                "AddGroup"
            )
        }

        step("And User user2Name is me") {
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        givenILoginAsTeamMemberThroughStagingDeepLink()

        step("And I open group conversation MyTeam and see TeamOwner in participants list") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("MyTeam")
                clickGroupConversation("MyTeam")
            }
            pages.conversationViewPage.clickOnGroupConversationDetails("MyTeam")
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapOnParticipantsTab()
                assertUsernameIsAddedToParticipantsList(teamOwner.name ?: "")
            }
        }

        step("When I close group details and User TeamOwner adds Member2 to group conversation MyTeam") {
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
            backendSetupHelper.userXAddedContactsToGroupChat(
                "user1Name",
                "user3Name",
                "MyTeam"
            )
        }

        step("Then I see system message TeamOwner added Member2 to the conversation") {
            pages.conversationViewPage.assertSystemMessageVisible(
                "${teamOwner.name ?: ""} added ${member2.name ?: ""} to the conversation"
            )
        }

        step("When I open the participants list") {
            pages.conversationViewPage.clickOnGroupConversationDetails("MyTeam")
            pages.groupConversationDetailsPage.tapOnParticipantsTab()
        }

        step("Then I see Member2 in participants list") {
            pages.groupConversationDetailsPage.assertUsernameIsAddedToParticipantsList(member2.name ?: "")
        }

        step("When I close group details and User TeamOwner removes Member2 from group conversation MyTeam") {
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
            backendSetupHelper.userRemovesUserFromGroupConversation(
                "user1Name",
                "user3Name",
                "MyTeam"
            )
        }

        step("Then I see system message TeamOwner removed Member2 from the conversation") {
            pages.conversationViewPage.assertSystemMessageVisible(
                "${teamOwner.name ?: ""} removed ${member2.name ?: ""} from the conversation"
            )
        }

        step("When User TeamOwner adds Member3 to group conversation MyTeam") {
            backendSetupHelper.userXAddedContactsToGroupChat(
                "user1Name",
                "user4Name",
                "MyTeam"
            )
        }

        step("Then I see system message TeamOwner added Member3 to the conversation") {
            pages.conversationViewPage.assertSystemMessageVisible(
                "${teamOwner.name ?: ""} added ${member3.name ?: ""} to the conversation"
            )
        }

        step("When I open the participants list") {
            pages.conversationViewPage.clickOnGroupConversationDetails("MyTeam")
            pages.groupConversationDetailsPage.tapOnParticipantsTab()
        }

        step("Then I see Member3 and do not see Member2 in participants list") {
            pages.groupConversationDetailsPage.apply {
                assertUsernameIsAddedToParticipantsList(member3.name ?: "")
                assertUserIsNotInParticipantsList(member2.name ?: "")
            }
        }

        step("When I close group details and User TeamOwner adds Member2 to group conversation MyTeam") {
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
            backendSetupHelper.userXAddedContactsToGroupChat(
                "user1Name",
                "user3Name",
                "MyTeam"
            )
        }

        step("And I open the participants list") {
            pages.conversationViewPage.clickOnGroupConversationDetails("MyTeam")
            pages.groupConversationDetailsPage.tapOnParticipantsTab()
        }

        step("Then I see Member2 and Member3 in participants list") {
            pages.groupConversationDetailsPage.apply {
                assertUsernameIsAddedToParticipantsList(member2.name ?: "")
                assertUsernameIsAddedToParticipantsList(member3.name ?: "")
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4375", "TC-4378", "TC-4373")
    @Category("regression", "RC", "groups", "membershipIdentifiers")
    @Test
    fun givenGuestsArePresentInGroupConversation_whenIOpenConversation_thenISeeGuestsBanner() {
        step("Given There is a team owner TeamOwner with team Guests and member Member1") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Guests",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Guests",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And There is a personal user User with a unique username") {
            clientUserManager.createPersonalUsersByAliases(listOf("user3Name"), backendClient)
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user3Name")
            }
            guest = clientUserManager.findUserByNameOrNameAlias("user3Name")
        }

        step("And User TeamOwner is connected to User") {
            backendSetupHelper.userIsConnectedTo("user1Name", "user3Name")
        }

        step("And User TeamOwner has group conversation GuestConversation with Member1 and User") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "GuestConversation",
                "user2Name,user3Name",
                "Guests"
            )
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("When I open group conversation GuestConversation and see it in foreground") {
            pages.conversationListPage.clickGroupConversation("GuestConversation")
            pages.conversationViewPage.assertGroupConversationInForeground("GuestConversation")
        }

        step("Then I see Guests are present banner in conversation view") {
            pages.conversationViewPage.assertConversationBannerVisible("Guests are present")
        }

        step("And I close the conversation view") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        // TC-4373 - I want to see a guest identifier on conversation list for 1:1 conversations with a guest
        step("When I see conversation User in conversation list") {
            pages.conversationListPage.assertConversationVisible(guest.name ?: "")
        }

        step("Then I see User has Guest identifier next to their name") {
            pages.conversationListPage.assertMembershipIdentifierVisible(
                guest.name ?: "",
                "Guest"
            )
        }

        // TC-4378 - I should not see a guests banner if guests are present in a 1:1 conversation
        step("When I open conversation User") {
            pages.conversationListPage.tapConversationNameInConversationList(guest.name ?: "")
        }

        step("Then I do not see Guests are present banner in conversation view") {
            pages.conversationViewPage.assertConversationBannerNotVisible("Guests are present")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4376", "TC-4379", "TC-4374")
    @Category("regression", "RC", "groups", "membershipIdentifiers")
    @Test
    fun givenExternalsArePresentInGroupConversation_whenIOpenConversation_thenISeeExternalBanner() {
        step("Given There is a team owner TeamOwner with team External and member Member1") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "External",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "External",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner adds user External to team External with role External") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user3Name",
                "External",
                TeamRoles.External,
                backendClient,
                context,
                true
            )
            external = clientUserManager.findUserByNameOrNameAlias("user3Name")
        }

        step("And User TeamOwner has a 1:1 conversation with External in team External") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user3Name",
                "External"
            )
        }

        step("And User TeamOwner has group conversation ExternalConversation with Member1 and External") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "ExternalConversation",
                "user2Name,user3Name",
                "External"
            )
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("When I open group conversation ExternalConversation and see it in foreground") {
            pages.conversationListPage.clickGroupConversation("ExternalConversation")
            pages.conversationViewPage.assertGroupConversationInForeground("ExternalConversation")
        }

        step("Then I see Externals are present banner in conversation view") {
            pages.conversationViewPage.assertConversationBannerVisible("Externals are present")
        }

        step("And I close the conversation view") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        // TC-4374 - I want to see an external identifier on conversation list for 1:1 conversations with an external
        step("When I see conversation External in conversation list") {
            pages.conversationListPage.assertConversationVisible(external.name ?: "")
        }

        step("Then I see External has External identifier next to their name") {
            pages.conversationListPage.assertMembershipIdentifierVisible(
                external.name ?: "",
                "External"
            )
        }

        // TC-4379 - I should not see an external banner if externals are present in a 1:1 conversation
        step("When I open conversation External") {
            pages.conversationListPage.tapConversationNameInConversationList(external.name ?: "")
        }

        step("Then I do not see Externals are present banner in conversation view") {
            pages.conversationViewPage.assertConversationBannerNotVisible("Externals are present")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4377")
    @Category("groups", "membershipIdentifiers", "services")
    @Test
    fun givenServiceIsPresentInGroupConversation_whenIOpenConversation_thenISeeServicesBanner() {
        step("Given There is a team owner TeamOwner with team Bots and member Member1") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Bots",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Bots",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner enables Poll Bot service for team Bots") {
            backendSetupHelper.userEnablesServiceForTeam("user1Name", "Poll Bot", "Bots")
        }

        step("And User TeamOwner has group conversation BotsConversation with Member1") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "BotsConversation",
                "user2Name",
                "Bots"
            )
        }

        step("And User TeamOwner adds Poll Bot to conversation BotsConversation") {
            backendSetupHelper.userAddsBotToConversation("user1Name", "Poll Bot", "BotsConversation")
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("When I open group conversation BotsConversation and see it in foreground") {
            pages.conversationListPage.clickGroupConversation("BotsConversation")
            pages.conversationViewPage.assertGroupConversationInForeground("BotsConversation")
        }

        step("Then I see Apps are active banner in conversation view") {
            pages.conversationViewPage.assertConversationBannerVisible("Apps are active")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4380")
    @Category("groups", "membershipIdentifiers", "services")
    @Test
    fun givenExternalsGuestsAndServicesArePresentInGroupConversation_whenIOpenConversation_thenISeeCombinedBanner() {
        step("Given There is a team owner TeamOwner with team AllAtOnce and member Member1") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "AllAtOnce",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "AllAtOnce",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner adds user External to team AllAtOnce with role External") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user4Name",
                "AllAtOnce",
                TeamRoles.External,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner enables Poll Bot service for team AllAtOnce") {
            backendSetupHelper.userEnablesServiceForTeam("user1Name", "Poll Bot", "AllAtOnce")
        }

        step("And There is a personal user User with a unique username") {
            clientUserManager.createPersonalUsersByAliases(listOf("user3Name"), backendClient)
            runBlocking {
                backendSetupHelper.usersSetUniqueUsername("user3Name")
            }
        }

        step("And User TeamOwner is connected to User") {
            backendSetupHelper.userIsConnectedTo("user1Name", "user3Name")
        }

        step("And User TeamOwner has group conversation EverythingAtOnce with User and External") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "EverythingAtOnce",
                "user3Name,user4Name",
                "AllAtOnce"
            )
        }

        step("And User TeamOwner adds Poll Bot to conversation EverythingAtOnce") {
            backendSetupHelper.userAddsBotToConversation("user1Name", "Poll Bot", "EverythingAtOnce")
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("When I open group conversation EverythingAtOnce and see it in foreground") {
            pages.conversationListPage.clickGroupConversation("EverythingAtOnce")
            pages.conversationViewPage.assertGroupConversationInForeground("EverythingAtOnce")
        }

        step("Then I see Externals, guests and apps are present banner in conversation view") {
            pages.conversationViewPage.assertConversationBannerVisible(
                "Externals, guests and apps are present"
            )
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4359")
    @Category("groups", "addGroup", "removeGroup", "services")
    @Test
    fun givenIHaveAnExistingGroupConversation_whenIAddAndRemoveService_thenServiceIsAddedAndRemoved() {
        step("Given There is a team owner TeamOwner with team AddGroup and members Member1,Member2,Member3") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "AddGroup",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name,user4Name",
                "AddGroup",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And User TeamOwner enables Poll Bot service for team AddGroup") {
            backendSetupHelper.userEnablesServiceForTeam("user1Name", "Poll Bot", "AddGroup")
        }

        step("And User TeamOwner has group conversation MyTeam with Member1 and Member2") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "MyTeam",
                "user2Name,user3Name",
                "AddGroup"
            )
        }

        step("And User TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I open group conversation MyTeam and its participants list") {
            pages.conversationListPage.clickGroupConversation("MyTeam")
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("MyTeam")
                clickOnGroupConversationDetails("MyTeam")
            }
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapOnParticipantsTab()
            }
        }

        step("When I tap Add participants and open the Apps tab") {
            pages.groupConversationDetailsPage.apply {
                tapAddParticipantsButton()
                tapOnAppsTab()
            }
        }

        step("And I see and open Poll Bot in Apps search results") {
            pages.groupConversationDetailsPage.apply {
                assertAppInSearchResultsVisible("Poll Bot")
                tapAppInSearchResults("Poll Bot")
            }
        }

        step("When I add Poll Bot and see App added to conversation toast message") {
            pages.groupConversationDetailsPage.tapAddToConversationButton()
            waitUntilToastIsDisplayed("App added to conversation")
        }

        step("Then I see Remove From Conversation button for Poll Bot") {
            pages.groupConversationDetailsPage.assertRemoveFromConversationButtonForAppVisible()
        }

        step("When I wait until the notification popup disappears") {
            pages.notificationsPage.waitUntilNotificationPopUpGone()
        }

        step("And I go back twice to the participants list") {
            pages.groupConversationDetailsPage.apply {
                tapBackButton()
                tapBackButton()
            }
        }

        step("Then I see Poll Bot in participants list") {
            pages.groupConversationDetailsPage.assertUsernameIsAddedToParticipantsList("Poll Bot")
        }

        step("When I close the group conversation details") {
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
        }

        // TC-4364 - I want to see a system message when a service gets added to a group conversation
        step("Then I see system message You added Poll Bot to the conversation") {
            pages.conversationViewPage.assertSystemMessageVisible("You added Poll Bot to the conversation")
        }

        step("When I open Poll Bot from the participants list") {
            pages.conversationViewPage.clickOnGroupConversationDetails("MyTeam")
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapOnParticipantsTab()
                assertUsernameIsAddedToParticipantsList("Poll Bot")
                tapUserInParticipantsList("Poll Bot")
            }
        }

        step("When I remove Poll Bot and see App removed from conversation toast message") {
            pages.groupConversationDetailsPage.apply {
                assertRemoveFromConversationButtonForAppVisible()
                tapRemoveFromConversationButton()
            }
            waitUntilToastIsDisplayed("App removed from conversation")
            pages.groupConversationDetailsPage.assertRemoveFromConversationButtonNotVisible()
        }

        step("Then I return to the participants list and do not see Poll Bot") {
            pages.groupConversationDetailsPage.apply {
                tapBackButton()
                assertUserIsNotInParticipantsList("Poll Bot")
            }
        }

        step("And I close group details and see Poll Bot removal system message") {
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
            pages.conversationViewPage.assertSystemMessageVisible("You removed Poll Bot from the conversation")
        }
    }

    // Shared app login flow for team-owner group tests: opens the staging deep link, signs in,
    // and clears post-login prompts.
    private fun givenILoginAsTeamOwnerThroughStagingDeepLink() {
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
    }

    // Shared app login flow for team-member group tests: opens the staging deep link, signs in,
    // and clears post-login prompts.
    private fun givenILoginAsTeamMemberThroughStagingDeepLink() {
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
                enterTeamMemberLoggingEmail(member1.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamMemberLoggingPassword(member1.password ?: "")
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
    }

    // Shared app login flow for personal guests in group tests: opens the staging deep link, signs in,
    // and clears post-login prompts.
    private fun givenILoginAsGuestThroughStagingDeepLink() {
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
                enterPersonalUserLoggingEmail(guest.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterPersonalUserLoginPassword(guest.password ?: "")
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
    }
}
