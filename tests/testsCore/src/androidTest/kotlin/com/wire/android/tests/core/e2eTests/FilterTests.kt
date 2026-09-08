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
import user.utils.ClientUser

@RunWith(AndroidJUnit4::class)
@Suppress("LargeClass")
class FilterTests : BaseUiTest() {
    private lateinit var teamOwner: ClientUser
    private lateinit var teamOwnerB: ClientUser
    private lateinit var teamOwnerC: ClientUser
    private lateinit var member1: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8642", "TC-8652", "TC-8645")
    @Category("regression", "filter", "RC")
    @Test
    fun givenIHaveCreatedFavorite_whenILogOutWithClearDataAndLogInAgain_thenFavoriteIsVisibleAndCanBeRemoved() {
        step("Given There are team owners TeamOwner, TeamOwnerB and TeamOwnerC with their teams") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Filters",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.createTeamOwnerByAlias(
                "user5Name",
                "ConnectedFriend",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.createTeamOwnerByAlias(
                "user2Name",
                "ConnectedFriend2",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And TeamOwner adds Member1 to team Filters with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user3Name",
                "Filters",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And TeamOwner is connected to TeamOwnerB and TeamOwnerC") {
            backendSetupHelper.userIsConnectedTo("user1Name", "user5Name")
            backendSetupHelper.userIsConnectedTo("user1Name", "user2Name")
        }

        step("And TeamOwner has group conversation Filter Group with Member1") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "Filter Group",
                "user3Name",
                "Filters"
            )
        }

        step("And TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            teamOwnerB = clientUserManager.findUserByNameOrNameAlias("user5Name")
            teamOwnerC = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(teamOwner)
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I see Filter Group, TeamOwnerB and TeamOwnerC conversations") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("Filter Group")
                assertConversationVisible(teamOwnerB.name ?: "")
                assertConversationVisible(teamOwnerC.name ?: "")
            }
        }

        step("When I long tap on conversation Filter Group") {
            pages.conversationListPage.longPressConversation("Filter Group")
        }

        step("Then I see Filter Group and Add to Favorites button on filter bottom sheet") {
            pages.conversationListPage.apply {
                assertConversationVisible("Filter Group")
                assertAddToFavoritesButtonVisible()
            }
        }

        step("When I tap Add to Favorites button and see Filter Group was added to Favorites toast message") {
            pages.conversationListPage.apply {
                tapAddToFavoritesButton()
                assertToastMessageIsDisplayedOnConversationList("“Filter Group” was added to Favorites")
            }
        }

        step("When I open Filter Conversations bottom sheet and select Favorites") {
            pages.conversationListPage.apply {
                tapFilterConversationButton()
                assertFilterConversationsBottomSheetVisible()
                assertFavoritesButtonVisibleOnFilterBottomSheet()
                tapFavoritesButtonOnFilterBottomSheet()
            }
        }

        step("Then I see Favorites page with Filter Group") {
            pages.conversationListPage.apply {
                assertFavoritesPageVisible()
                assertGroupConversationVisible("Filter Group")
            }
        }

        // TC-8652 - I want to see existing favorites that I created before I logged out with clear data when I login again
        step("When I open User Profile Page and log out with clear data selected") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                tapLogoutButton()
                iSeeClearDataOnLogOutAlert()
                iSeeInfoTextCheckbox("Delete all your personal information and conversations on this device")
                tapInfoTextCheckbox()
                tapLogoutButton()
            }
        }

        step("And I see email verification Welcome Page after logout") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link and sign in again") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamOwnerLoggingPassword(teamOwner.password ?: "")
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in again and decline share data alert") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("Then I see Filter Group, TeamOwnerB and TeamOwnerC conversations") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("Filter Group")
                assertConversationVisible(teamOwnerB.name ?: "")
                assertConversationVisible(teamOwnerC.name ?: "")
            }
        }

        step("When I open Filter Conversations bottom sheet and select Favorites") {
            pages.conversationListPage.apply {
                tapFilterConversationButton()
                assertFilterConversationsBottomSheetVisible()
                assertFavoritesButtonVisibleOnFilterBottomSheet()
                tapFavoritesButtonOnFilterBottomSheet()
            }
        }

        step("Then I see Favorites page with Filter Group") {
            pages.conversationListPage.apply {
                assertFavoritesPageVisible()
                assertGroupConversationVisible("Filter Group")
            }
        }

        step("When I long tap on conversation Filter Group in Favorites") {
            pages.conversationListPage.longPressConversation("Filter Group")
        }

        step("Then I see Filter Group and Remove from Favorites button on filter bottom sheet") {
            pages.conversationListPage.apply {
                assertConversationVisible("Filter Group")
                assertRemoveFromFavoritesButtonVisible()
            }
        }

        // TC-8645 - I want to remove a conversation from favorites
        step("When I tap Remove from Favorites button and see Filter Group was removed from Favorites toast message") {
            pages.conversationListPage.apply {
                tapRemoveFromFavoritesButton()
                assertToastMessageIsDisplayedOnConversationList("“Filter Group” was removed from Favorites")
            }
        }

        step("Then I do not see Filter Group in Favorites") {
            pages.conversationListPage.assertConversationNotVisible("Filter Group")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8643", "TC-8644", "TC-8646", "TC-8666")
    @Category("regression", "filter", "RC")
    @Test
    fun givenIHaveCreatedConversationFolder_whenILogOutWithClearDataAndLogInAgain_thenFolderIsVisibleAndConversationsCanBeManaged() {
        givenTeamOwnerMemberGroupAndConnectedTeamOwnerArePrepared()

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I see Folder Group and TeamOwnerB conversations") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("Folder Group")
                assertConversationVisible(teamOwnerB.name ?: "")
            }
        }

        step("When I long tap on conversation Folder Group") {
            pages.conversationListPage.longPressConversation("Folder Group")
        }

        step("Then I see Folder Group and Move to Folder... button on filter bottom sheet") {
            pages.conversationListPage.apply {
                assertConversationVisible("Folder Group")
                assertMoveToFolderButtonVisible()
            }
        }

        step("When I tap Move to Folder... button on filter bottom sheet") {
            pages.conversationListPage.tapMoveToFolderButton()
        }

        step("Then I see Move to Folder... page with New Folder button and Done button") {
            pages.conversationListPage.apply {
                assertMoveToFolderPageVisible()
                assertNewFolderButtonVisible()
                assertDoneButtonVisible()
            }
        }

        step("When I tap New Folder button and see New Folder page") {
            pages.conversationListPage.apply {
                tapNewFolderButton()
                assertNewFolderPageVisible()
            }
        }

        step("And I enter Test Folder as folder name and tap Create Folder button") {
            pages.conversationListPage.apply {
                enterFolderName("Test Folder")
                tapCreateFolderButton()
            }
        }

        step("Then I see Folder Group was moved to Test Folder toast message") {
            pages.conversationListPage.assertToastMessageIsDisplayedOnConversationList(
                "“Folder Group” was moved to “Test Folder”"
            )
        }

        step("When I open Filter Conversations bottom sheet and select Folders") {
            pages.conversationListPage.apply {
                tapFilterConversationButton()
                assertFilterConversationsBottomSheetVisible()
                assertFoldersButtonVisibleOnFilterBottomSheet()
                tapFoldersButtonOnFilterBottomSheet()
            }
        }

        step("And I see Folders bottom sheet and open Test Folder") {
            pages.conversationListPage.apply {
                assertFoldersBottomSheetVisible()
                assertFolderVisible("Test Folder")
                tapFolder("Test Folder")
            }
        }

        step("Then I see Test Folder page with Folder Group") {
            pages.conversationListPage.apply {
                assertFolderPageVisible("Test Folder")
                assertGroupConversationVisible("Folder Group")
            }
        }

        // TC-8666 - I want to see existing folders that I created before I logged out with clear data when I login again
        step("When I open User Profile Page and log out with clear data selected") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                tapLogoutButton()
                iSeeClearDataOnLogOutAlert()
                iSeeInfoTextCheckbox("Delete all your personal information and conversations on this device")
                tapInfoTextCheckbox()
                tapLogoutButton()
            }
        }

        step("And I see email verification Welcome Page after logout") {
            pages.registrationPage.assertEmailWelcomePage()
        }

        step("And I open staging backend deep link and sign in again") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
                assertUserLoginScreenVisible()
                enterTeamOwnerLoggingPassword(teamOwner.password ?: "")
                clickLoginButton()
            }
        }

        step("And I wait until I am fully logged in again and decline share data alert") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }

        step("Then I see Folder Group and TeamOwnerB conversations") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("Folder Group")
                assertConversationVisible(teamOwnerB.name ?: "")
            }
        }

        // TC-8644 - I want to move a conversation to an existing folder
        step("When I long tap TeamOwnerB conversation and tap Move to Folder... button") {
            pages.conversationListPage.apply {
                longPressConversation(teamOwnerB.name ?: "")
                assertConversationVisible(teamOwnerB.name ?: "")
                assertMoveToFolderButtonVisible()
                tapMoveToFolderButton()
            }
        }

        step("Then I see Move to Folder... page with Test Folder, New Folder button and Done button") {
            pages.conversationListPage.apply {
                assertMoveToFolderPageVisible()
                assertFolderVisible("Test Folder")
                assertNewFolderButtonVisible()
                assertDoneButtonVisible()
            }
        }

        step("When I select Test Folder and tap Done button") {
            pages.conversationListPage.apply {
                tapFolder("Test Folder")
                tapDoneButtonOnMoveToFolderPage()
            }
        }

        step("Then I see TeamOwnerB was moved to Test Folder toast message") {
            pages.conversationListPage.assertToastMessageIsDisplayedOnConversationList(
                "“${teamOwnerB.name ?: ""}” was moved to “Test Folder”"
            )
        }

        step("When I open Filter Conversations bottom sheet and select Folders") {
            pages.conversationListPage.apply {
                tapFilterConversationButton()
                assertFilterConversationsBottomSheetVisible()
                assertFoldersButtonVisibleOnFilterBottomSheet()
                tapFoldersButtonOnFilterBottomSheet()
            }
        }

        step("And I see Folders bottom sheet and open Test Folder") {
            pages.conversationListPage.apply {
                assertFoldersBottomSheetVisible()
                assertFolderVisible("Test Folder")
                tapFolder("Test Folder")
            }
        }

        step("Then I see Test Folder page with Folder Group and TeamOwnerB") {
            pages.conversationListPage.apply {
                assertFolderPageVisible("Test Folder")
                assertGroupConversationVisible("Folder Group")
                assertConversationVisible(teamOwnerB.name ?: "")
            }
        }

        // TC-8646 - I want to remove a conversation from a folder
        step("When I long tap Folder Group in Test Folder") {
            pages.conversationListPage.longPressConversation("Folder Group")
        }

        step("Then I see Folder Group and Remove from Folder “Test Folder” button on filter bottom sheet") {
            pages.conversationListPage.apply {
                assertConversationVisible("Folder Group")
                assertRemoveFromFolderButtonVisible("Test Folder")
            }
        }

        step("When I tap Remove from Folder “Test Folder” button") {
            pages.conversationListPage.tapRemoveFromFolderButton("Test Folder")
        }

        step("Then I see Folder Group was removed from Test Folder toast message") {
            pages.conversationListPage.assertToastMessageIsDisplayedOnConversationList(
                "“Folder Group” was removed from “Test Folder”"
            )
        }

        step("And I do not see Folder Group in Test Folder") {
            pages.conversationListPage.assertConversationNotVisible("Folder Group")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8651", "TC-8650")
    @Category("regression", "filter", "RC")
    @Test
    fun givenOneToOneConversation_whenIMoveItToFolderFromUserProfile_thenConversationIsVisibleInFavoritesAndFolder() {
        step("Given There are team owners TeamOwner and TeamOwnerB with their teams") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "folderFilter",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.createTeamOwnerByAlias(
                "user2Name",
                "ConnectedFriend",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And TeamOwner adds Member1 to team folderFilter with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user3Name",
                "folderFilter",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And TeamOwner is connected to TeamOwnerB") {
            backendSetupHelper.userIsConnectedTo("user1Name", "user2Name")
        }

        step("And TeamOwner has 1:1 conversation with Member1 in team folderFilter") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user3Name",
                "folderFilter"
            )
        }

        step("And TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            teamOwnerB = clientUserManager.findUserByNameOrNameAlias("user2Name")
            member1 = clientUserManager.findUserByNameOrNameAlias("user3Name")
            clientUserManager.setSelfUser(teamOwner)
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I see Member1 and TeamOwnerB conversations") {
            pages.conversationListPage.apply {
                assertConversationVisible(member1.name ?: "")
                assertConversationVisible(teamOwnerB.name ?: "")
            }
        }

        step("When I long tap Member1 conversation") {
            pages.conversationListPage.longPressConversation(member1.name ?: "")
        }

        step("Then I see Member1 and Add to Favorites button on filter bottom sheet") {
            pages.conversationListPage.apply {
                assertConversationVisible(member1.name ?: "")
                assertAddToFavoritesButtonVisible()
            }
        }

        step("When I tap Add to Favorites button") {
            pages.conversationListPage.tapAddToFavoritesButton()
        }

        step("Then I see Member1 was added to Favorites toast message") {
            pages.conversationListPage.assertToastMessageIsDisplayedOnConversationList(
                "“${member1.name ?: ""}” was added to Favorites"
            )
        }

        step("When I open Member1 conversation") {
            pages.conversationListPage.tapConversationNameInConversationList(member1.name ?: "")
        }

        step("And I open 1:1 conversation details for Member1") {
            pages.conversationViewPage.click1On1ConversationDetails(member1.name ?: "")
        }

        step("When I tap show more options button on user profile screen") {
            pages.connectedUserProfilePage.clickShowMoreOptions()
        }

        step("Then I see Member1 and Move to Folder... button on filter bottom sheet") {
            pages.conversationListPage.apply {
                assertConversationVisible(member1.name ?: "")
                assertMoveToFolderButtonVisible()
            }
        }

        step("When I tap Move to Folder... button on filter bottom sheet") {
            pages.conversationListPage.tapMoveToFolderButton()
        }

        step("Then I see Move to Folder... page with New Folder button and Done button") {
            pages.conversationListPage.apply {
                assertMoveToFolderPageVisible()
                assertNewFolderButtonVisible()
                assertDoneButtonVisible()
            }
        }

        step("When I tap New Folder button and see New Folder page") {
            pages.conversationListPage.apply {
                tapNewFolderButton()
                assertNewFolderPageVisible()
            }
        }

        step("And I enter Filter and Folder as folder name and tap Create Folder button") {
            pages.conversationListPage.apply {
                enterFolderName("Filter and Folder")
                tapCreateFolderButton()
            }
        }

        step("Then I see Member1 was moved to Filter and Folder toast message") {
            pages.connectedUserProfilePage.assertToastMessageIsDisplayed(
                "“${member1.name ?: ""}” was moved to “Filter and Folder”"
            )
        }

        step("When I close the user profile and return to conversation list") {
            pages.connectedUserProfilePage.tapCloseButtonOnConnectedUserProfilePage()
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
        }

        step("Then I see Member1 conversation in conversation list") {
            pages.conversationListPage.assertConversationVisible(member1.name ?: "")
        }

        // TC-8650 - I want to see a conversation in favorites and folder if I marked it as favorite and moved it to a folder
        step("When I open Filter Conversations bottom sheet and select Favorites") {
            pages.conversationListPage.apply {
                tapFilterConversationButton()
                assertFilterConversationsBottomSheetVisible()
                assertFavoritesButtonVisibleOnFilterBottomSheet()
                tapFavoritesButtonOnFilterBottomSheet()
            }
        }

        step("Then I see Favorites page with Member1 conversation") {
            pages.conversationListPage.apply {
                assertFavoritesPageVisible()
                assertConversationVisible(member1.name ?: "")
            }
        }

        step("When I open Filter Conversations bottom sheet and select Folders") {
            pages.conversationListPage.apply {
                tapFilterConversationButton()
                assertFilterConversationsBottomSheetVisible()
                assertFoldersButtonVisibleOnFilterBottomSheet()
                tapFoldersButtonOnFilterBottomSheet()
            }
        }

        step("And I see Folders bottom sheet and open Filter and Folder") {
            pages.conversationListPage.apply {
                assertFoldersBottomSheetVisible()
                assertFolderVisible("Filter and Folder")
                tapFolder("Filter and Folder")
            }
        }

        step("Then I see Filter and Folder page with Member1 conversation") {
            pages.conversationListPage.apply {
                assertFolderPageVisible("Filter and Folder")
                assertConversationVisible(member1.name ?: "")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8654", "TC-8655")
    @Category("regression", "filter", "RC")
    @Test
    fun givenGroupAndOneToOneConversations_whenIFilterByType_thenOnlyMatchingConversationsAreVisible() {
        givenTeamOwnerMemberGroupAndConnectedTeamOwnerArePrepared()

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I see Folder Group and TeamOwnerB conversations") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("Folder Group")
                assertConversationVisible(teamOwnerB.name ?: "")
            }
        }

        step("When I open Filter Conversations bottom sheet and select Groups") {
            pages.conversationListPage.apply {
                tapFilterConversationButton()
                assertFilterConversationsBottomSheetVisible()
                assertGroupsButtonVisibleOnFilterBottomSheet()
                tapGroupsButtonOnFilterBottomSheet()
            }
        }

        step("Then I see Groups page with Folder Group conversation") {
            pages.conversationListPage.apply {
                assertGroupsPageVisible()
                assertGroupConversationVisible("Folder Group")
            }
        }

        step("And I do not see TeamOwnerB conversation on Groups page") {
            pages.conversationListPage.assertConversationNotVisible(teamOwnerB.name ?: "")
        }

        // TC-8655 - I want to see only 1on1 conversations when I filter by 1on1 conversations
        step("When I open Filter Conversations bottom sheet and select 1:1 Conversations") {
            pages.conversationListPage.apply {
                tapFilterConversationButton()
                assertFilterConversationsBottomSheetVisible()
                assertOneToOneConversationsButtonVisibleOnFilterBottomSheet()
                tapOneToOneConversationsButtonOnFilterBottomSheet()
            }
        }

        step("Then I see 1:1 Conversations page with TeamOwnerB conversation") {
            pages.conversationListPage.apply {
                assertOneToOneConversationsPageVisible()
                assertConversationVisible(teamOwnerB.name ?: "")
            }
        }

        step("And I do not see Folder Group conversation on 1:1 Conversations page") {
            pages.conversationListPage.assertConversationNotVisible("Folder Group")
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8647")
    @Category("regression", "filter", "RC")
    @Test
    fun givenArchivedOneToOneConversation_whenIOpenUserProfileOptions_thenMoveToFolderButtonIsNotVisible() {
        givenTeamOwnerMemberGroupAndConnectedTeamOwnerArePrepared()

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I see Folder Group and TeamOwnerB conversations") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("Folder Group")
                assertConversationVisible(teamOwnerB.name ?: "")
            }
        }

        step("When I long tap TeamOwnerB conversation") {
            pages.conversationListPage.longPressConversation(teamOwnerB.name ?: "")
        }

        step("Then I see TeamOwnerB and Move to Archive button on filter bottom sheet") {
            pages.conversationListPage.apply {
                assertConversationVisible(teamOwnerB.name ?: "")
                assertMoveToArchiveButtonVisibleInConversationActions()
            }
        }

        step("When I tap Move to Archive button and confirm archiving") {
            pages.conversationListPage.apply {
                tapMoveToArchiveButtonInConversationActions()
                tapConfirmArchiveConversationButton()
            }
        }

        step("Then I see Conversation was archived toast message") {
            pages.conversationListPage.assertToastMessageIsDisplayedOnConversationList("Conversation was archived")
        }

        step("And I do not see TeamOwnerB conversation in conversation list") {
            pages.conversationListPage.assertConversationNotVisible(teamOwnerB.name ?: "")
        }

        step("When I open the main navigation menu and tap Archive") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickArchiveButtonOnMenuEntry()
            }
        }

        step("Then I see TeamOwnerB conversation in archive list") {
            pages.archivePage.assertConversationVisibleInArchiveList(teamOwnerB.name ?: "")
        }

        step("When I open TeamOwnerB conversation from archive list") {
            pages.archivePage.tapConversationNameInArchiveList(teamOwnerB.name ?: "")
        }

        step("And I open 1:1 conversation details for TeamOwnerB") {
            pages.conversationViewPage.click1On1ConversationDetails(teamOwnerB.name ?: "")
        }

        step("And I tap show more options button on user profile screen") {
            pages.connectedUserProfilePage.clickShowMoreOptions()
        }

        step("Then I see TeamOwnerB and do not see Move to Folder... button on filter bottom sheet") {
            pages.conversationListPage.assertConversationVisible(teamOwnerB.name ?: "")
            pages.connectedUserProfilePage.assertMoveToFolderButtonNotVisible()
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8648")
    @Category("regression", "filter", "RC")
    @Test
    fun givenConversationInFolder_whenIArchiveAndUnarchiveIt_thenConversationReturnsToSameFolder() {
        givenTeamOwnerMemberGroupAndConnectedTeamOwnerArePrepared()

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I see Folder Group and TeamOwnerB conversations") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("Folder Group")
                assertConversationVisible(teamOwnerB.name ?: "")
            }
        }

        step("When I long tap TeamOwnerB conversation") {
            pages.conversationListPage.longPressConversation(teamOwnerB.name ?: "")
        }

        step("Then I see TeamOwnerB and Move to Folder... button on filter bottom sheet") {
            pages.conversationListPage.apply {
                assertConversationVisible(teamOwnerB.name ?: "")
                assertMoveToFolderButtonVisible()
            }
        }

        step("When I tap Move to Folder... button on filter bottom sheet") {
            pages.conversationListPage.tapMoveToFolderButton()
        }

        step("Then I see Move to Folder... page with New Folder button and Done button") {
            pages.conversationListPage.apply {
                assertMoveToFolderPageVisible()
                assertNewFolderButtonVisible()
                assertDoneButtonVisible()
            }
        }

        step("When I tap New Folder button and see New Folder page") {
            pages.conversationListPage.apply {
                tapNewFolderButton()
                assertNewFolderPageVisible()
            }
        }

        step("And I enter Test Folder as folder name and tap Create Folder button") {
            pages.conversationListPage.apply {
                enterFolderName("Test Folder")
                tapCreateFolderButton()
            }
        }

        step("Then I see TeamOwnerB was moved to Test Folder toast message") {
            pages.conversationListPage.assertToastMessageIsDisplayedOnConversationList(
                "“${teamOwnerB.name ?: ""}” was moved to “Test Folder”"
            )
        }

        step("When I long tap TeamOwnerB conversation") {
            pages.conversationListPage.longPressConversation(teamOwnerB.name ?: "")
        }

        step("Then I see TeamOwnerB and Move to Archive button on filter bottom sheet") {
            pages.conversationListPage.apply {
                assertConversationVisible(teamOwnerB.name ?: "")
                assertMoveToArchiveButtonVisibleInConversationActions()
            }
        }

        step("When I tap Move to Archive button and confirm archiving") {
            pages.conversationListPage.apply {
                tapMoveToArchiveButtonInConversationActions()
                tapConfirmArchiveConversationButton()
            }
        }

        step("Then I see Conversation was archived toast message") {
            pages.conversationListPage.assertToastMessageIsDisplayedOnConversationList("Conversation was archived")
        }

        step("And I do not see TeamOwnerB conversation in conversation list") {
            pages.conversationListPage.assertConversationNotVisible(teamOwnerB.name ?: "")
        }

        step("When I open the main navigation menu and tap Archive") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickArchiveButtonOnMenuEntry()
            }
        }

        step("Then I see TeamOwnerB conversation in archive list") {
            pages.archivePage.assertConversationVisibleInArchiveList(teamOwnerB.name ?: "")
        }

        step("When I long tap TeamOwnerB conversation in archive list") {
            pages.archivePage.longPressConversationInArchiveList(teamOwnerB.name ?: "")
        }

        step("And I tap Unarchive button") {
            pages.archivePage.tapMoveOutOfArchiveButton()
        }

        step("Then I see Conversation was unarchived toast message") {
            pages.archivePage.assertToastMessageIsDisplayedOnArchiveList("Conversation was unarchived")
        }

        step("And I do not see TeamOwnerB conversation in archive list") {
            pages.archivePage.assertConversationNotVisibleInArchiveList(teamOwnerB.name ?: "")
        }

        step("When I open the main navigation menu and tap Conversations") {
            pages.conversationListPage.apply {
                clickConversationsMenuEntry()
                clickConversationsButtonOnMenuEntry()
            }
        }

        step("Then I see Folder Group and TeamOwnerB conversations") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("Folder Group")
                assertConversationVisible(teamOwnerB.name ?: "")
            }
        }

        step("When I open Filter Conversations bottom sheet and select Folders") {
            pages.conversationListPage.apply {
                tapFilterConversationButton()
                assertFilterConversationsBottomSheetVisible()
                assertFoldersButtonVisibleOnFilterBottomSheet()
                tapFoldersButtonOnFilterBottomSheet()
            }
        }

        step("Then I see Folders bottom sheet with Test Folder") {
            pages.conversationListPage.apply {
                assertFoldersBottomSheetVisible()
                assertFolderVisible("Test Folder")
            }
        }

        step("When I open Test Folder") {
            pages.conversationListPage.tapFolder("Test Folder")
        }

        step("Then I see Test Folder page with TeamOwnerB conversation") {
            pages.conversationListPage.apply {
                assertFolderPageVisible("Test Folder")
                assertConversationVisible(teamOwnerB.name ?: "")
            }
        }
    }

    // Shared backend setup for filter tests: creates TeamOwner, Member1, connected TeamOwnerB,
    // and Folder Group, then sets TeamOwner as self.
    private fun givenTeamOwnerMemberGroupAndConnectedTeamOwnerArePrepared() {
        step("Given There are team owners TeamOwner and TeamOwnerB with their teams") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Folder",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.createTeamOwnerByAlias(
                "user2Name",
                "ConnectedFriend",
                "en_US",
                true,
                backendClient,
                context
            )
        }

        step("And TeamOwner adds Member1 to team Folder with role Member") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user3Name",
                "Folder",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And TeamOwner is connected to TeamOwnerB") {
            backendSetupHelper.userIsConnectedTo("user1Name", "user2Name")
        }

        step("And TeamOwner has group conversation Folder Group with Member1") {
            backendSetupHelper.userHasGroupConversationInTeam(
                "user1Name",
                "Folder Group",
                "user3Name",
                "Folder"
            )
        }

        step("And TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            teamOwnerB = clientUserManager.findUserByNameOrNameAlias("user2Name")
            clientUserManager.setSelfUser(teamOwner)
        }
    }

    // Shared app login flow: opens the staging deep link, signs in as TeamOwner, and clears post-login prompts.
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
}
