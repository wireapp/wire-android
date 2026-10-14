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

@Suppress("LargeClass")
@RunWith(AndroidJUnit4::class)
class GuestLinksTests : BaseUiTest() {
    private lateinit var teamOwner: ClientUser
    private lateinit var teamMember: ClientUser
    private lateinit var secondTeamMember: ClientUser
    private lateinit var teamOwnerB: ClientUser

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    // ######################
    // Without Password
    // ######################

    @Suppress("LongMethod")
    @TestCaseId("TC-4381", "TC-4382")
    @Category("regression", "RC", "guestLinks")
    @Test
    fun givenGuestsAreEnabled_whenAdminCreatesAndDisablesGuestLink_thenLinkIsCreatedAndRevoked() {
        step("Given There is an MLS team owner with two team members") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Guests",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam(
                "user1Name",
                "Guests",
                backendClient
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Guests",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And MLS devices are registered for all participants") {
            testServiceHelper.apply {
                addDevice("user1Name", null, "Device1")
                addDevice("user2Name", null, "Device1")
                addDevice("user3Name", null, "Device2")
            }
        }

        step("And TeamOwner creates the MLS group conversation GuestsHere") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name,user3Name",
                conversationName = "GuestsHere",
                deviceName = "Device1"
            )
        }

        step("And TeamOwner enables guest access for GuestsHere") {
            backendSetupHelper.userEnablesGuestAccessForConversation("user1Name", "GuestsHere")
        }

        step("And TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        givenILoginThroughStagingDeepLink(teamOwner, "TeamOwner")

        step("And I open group conversation GuestsHere") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("GuestsHere")
                clickGroupConversation("GuestsHere")
            }
        }

        step("When I open Guests options in conversation details") {
            pages.conversationViewPage.clickOnGroupConversationDetails("GuestsHere")
            pages.groupConversationDetailsPage.apply {
                assertGroupDetailsPageVisible()
                tapGuestOptions()
            }
        }

        step("And I create a guest link without a password") {
            pages.groupAccessOptionsPage.apply {
                assertGuestSwitchState("ON")
                tapCreateLinkButton()
                tapCreateLinkWithoutPassword()
            }
        }

        step("Then I see the guest link was created") {
            pages.groupAccessOptionsPage.assertGuestLinkCreated()
        }

        step("When I copy and send the guest link in GuestsHere") {
            pages.groupAccessOptionsPage.apply {
                tapCopyGuestLinkButton()
                tapBackButton()
            }
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
            pages.conversationViewPage.apply {
                pasteClipboardIntoMessageInputField()
                clickSendButton()
            }
        }

        step("Then I see the guest link in the conversation") {
            pages.conversationViewPage.assertGuestLinkVisibleInCurrentConversation()
        }

        // TC-4382 - I want to revoke an invite link by disabling guests in the group conversation
        step("When I return to Guests options and disable guests") {
            pages.conversationViewPage.clickOnGroupConversationDetails("GuestsHere")
            pages.groupConversationDetailsPage.tapGuestOptions()
            pages.groupAccessOptionsPage.apply {
                assertGuestSwitchState("ON")
                tapGuestSwitch()
                tapDisableButton()
            }
        }

        step("Then the guest link is revoked and the Guests switch is OFF") {
            pages.groupAccessOptionsPage.apply {
                assertGuestLinkNotVisible()
                assertGuestSwitchState("OFF")
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-8122")
    @Category("regression", "RC", "guestLinks")
    @Test
    fun givenTeamMemberReceivesGuestLink_whenJoiningConversation_thenConversationIsOpened() {
        step("Given There is an MLS team owner with two team members") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Guests",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam(
                "user1Name",
                "Guests",
                backendClient
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Guests",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And MLS devices are registered for the conversation participants") {
            testServiceHelper.apply {
                addDevice("user1Name", null, "Device1")
                addDevice("user3Name", null, "Device2")
            }
        }

        step("And TeamOwner creates the MLS group conversation GuestsHere with Member2") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user3Name",
                conversationName = "GuestsHere",
                deviceName = "Device1"
            )
        }

        step("And TeamOwner enables guest access for GuestsHere") {
            backendSetupHelper.userEnablesGuestAccessForConversation("user1Name", "GuestsHere")
        }

        step("And Member1 is me") {
            teamMember = clientUserManager.findUserByNameOrNameAlias("user2Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        givenILoginThroughStagingDeepLink(teamMember, "Member1")

        step("And TeamOwner creates an invite link for GuestsHere") {
            backendSetupHelper.userCreatesInviteLink("user1Name", "GuestsHere")
        }

        step("When I open the invite deep link for GuestsHere") {
            pages.commonAppPage.openDeepLink(
                backendSetupHelper.getClientDeepLinkForPublicConversation(
                    "user1Name",
                    "GuestsHere"
                )
            )
        }

        step("And I see the join conversation alert") {
            pages.commonAppPage.assertJoinConversationAlertVisible()
        }

        step("And I see the invitation text and GuestsHere in the alert") {
            pages.commonAppPage.apply {
                assertJoinConversationAlertTextVisible("You have been invited to a conversation.")
                assertJoinConversationAlertTextVisible("GuestsHere")
            }
        }

        step("And I join the conversation") {
            pages.commonAppPage.tapJoinConversationButton()
        }

        step("Then GuestsHere is open and I see that I joined the conversation") {
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("GuestsHere")
                assertSystemMessageVisible("You joined the conversation")
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4388")
    @Category("regression", "RC", "guestLinks")
    @Test
    fun givenMemberIsAlreadyInConversation_whenOpeningGuestLink_thenConversationOpensWithoutJoiningAgain() {
        step("Given There is an MLS team owner with two team members") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Guests",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam(
                "user1Name",
                "Guests",
                backendClient
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Guests",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And MLS devices are registered for TeamOwner and Member2") {
            testServiceHelper.apply {
                addDevice("user1Name", null, "Device1")
                addDevice("user3Name", null, "Device2")
            }
        }

        step("And Member1 is me") {
            teamMember = clientUserManager.findUserByNameOrNameAlias("user2Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        givenILoginThroughStagingDeepLink(teamMember, "Member1")

        step("And TeamOwner creates the MLS group conversation GuestsHere with Member1 and Member2") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name,user3Name",
                conversationName = "GuestsHere",
                deviceName = "Device1"
            )
        }

        step("And TeamOwner enables guest access for GuestsHere") {
            backendSetupHelper.userEnablesGuestAccessForConversation("user1Name", "GuestsHere")
        }

        step("And TeamOwner creates an invite link for GuestsHere") {
            backendSetupHelper.userCreatesInviteLink("user1Name", "GuestsHere")
        }

        step("When I open the invite deep link for GuestsHere") {
            pages.commonAppPage.openDeepLink(
                backendSetupHelper.getClientDeepLinkForPublicConversation(
                    "user1Name",
                    "GuestsHere"
                )
            )
        }

        step("Then I see group conversation GuestsHere is in foreground") {
            pages.conversationViewPage.assertGroupConversationInForeground("GuestsHere")
        }

        step("And I do not see system message You joined the conversation in conversation view") {
            pages.conversationViewPage.assertSystemMessageNotVisible("You joined the conversation")
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4389")
    @Category("regression", "RC", "guestLinks")
    @Test
    fun givenIAmATeamMember_whenICancelJoiningViaGuestLink_thenGroupConversationIsNotInForeground() {
        step("Given There is an MLS team owner with two team members") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Guests",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam(
                "user1Name",
                "Guests",
                backendClient
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Guests",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And MLS devices are registered for TeamOwner and Member2") {
            testServiceHelper.apply {
                addDevice("user1Name", null, "Device1")
                addDevice("user3Name", null, "Device2")
            }
        }

        step("And TeamOwner creates the MLS group conversation GuestsHere with Member2") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user3Name",
                conversationName = "GuestsHere",
                deviceName = "Device1"
            )
        }

        step("And TeamOwner enables guest access for GuestsHere") {
            backendSetupHelper.userEnablesGuestAccessForConversation("user1Name", "GuestsHere")
        }

        step("And Member1 is me") {
            teamMember = clientUserManager.findUserByNameOrNameAlias("user2Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        givenILoginThroughStagingDeepLink(teamMember, "Member1")

        step("And TeamOwner creates an invite link for GuestsHere") {
            backendSetupHelper.userCreatesInviteLink("user1Name", "GuestsHere")
        }

        step("When I open the invite deep link for GuestsHere") {
            pages.commonAppPage.openDeepLink(
                backendSetupHelper.getClientDeepLinkForPublicConversation(
                    "user1Name",
                    "GuestsHere"
                )
            )
        }

        step("And I see the join conversation alert") {
            pages.commonAppPage.assertJoinConversationAlertVisible()
        }

        step("And I see the invitation text and GuestsHere in the alert") {
            pages.commonAppPage.apply {
                assertJoinConversationAlertTextVisible("You have been invited to a conversation.")
                assertJoinConversationAlertTextVisible("GuestsHere")
            }
        }

        step("And I cancel joining the conversation") {
            pages.commonAppPage.tapCancelButtonOnAlert()
        }

        step("Then I do not see group conversation GuestsHere in foreground") {
            pages.conversationViewPage.assertConversationScreenNotVisible()
        }

        step("And I see conversation list") {
            pages.conversationListPage.assertConversationListVisible()
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-8123")
    @Category("regression", "RC", "guestLinks")
    @Test
    fun givenGroupConversationHasBeenDeleted_whenIOpenItsInviteDeepLink_thenUnableToJoinAlertIsShown() {
        lateinit var rememberedGuestLink: String

        step("Given There is an MLS team owner with two team members") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Guests",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam(
                "user1Name",
                "Guests",
                backendClient
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Guests",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And MLS devices are registered for TeamOwner and Member2") {
            testServiceHelper.apply {
                addDevice("user1Name", null, "Device1")
                addDevice("user3Name", null, "Device2")
            }
        }

        step("And TeamOwner creates the MLS group conversation GuestsHere with Member2") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user3Name",
                conversationName = "GuestsHere",
                deviceName = "Device1"
            )
        }

        step("And TeamOwner enables guest access for GuestsHere") {
            backendSetupHelper.userEnablesGuestAccessForConversation("user1Name", "GuestsHere")
        }

        step("And Member1 is me") {
            teamMember = clientUserManager.findUserByNameOrNameAlias("user2Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        givenILoginThroughStagingDeepLink(teamMember, "Member1")

        step("And TeamOwner creates an invite link for GuestsHere") {
            backendSetupHelper.userCreatesInviteLink("user1Name", "GuestsHere")
        }

        step("And I remember the invite deep link for GuestsHere") {
            rememberedGuestLink = backendSetupHelper.getClientDeepLinkForPublicConversation(
                "user1Name",
                "GuestsHere"
            )
        }

        step("And TeamOwner deletes GuestsHere") {
            backendSetupHelper.userDeletesGroupConversation("user1Name", "GuestsHere")
        }

        step("When I open the remembered invite deep link") {
            pages.commonAppPage.openDeepLink(rememberedGuestLink)
        }

        step("Then I see the unable to join conversation alert") {
            pages.commonAppPage.assertUnableToJoinConversationAlertVisible()
        }

        step("And I see why I could not be added to the conversation") {
            pages.commonAppPage.assertUnableToJoinConversationAlertTextVisible(
                "Due to an error you could not be added to the conversation."
            )
        }

        step("And I dismiss the alert") {
            pages.commonAppPage.tapOkButtonOnAlert()
        }

        step("And I do not see group conversation GuestsHere in foreground") {
            pages.conversationViewPage.assertConversationScreenNotVisible()
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-8124")
    @Category("regression", "RC", "guestLinks")
    @Test
    fun givenGuestLinkHasBeenRevoked_whenIOpenIt_thenUnableToJoinAlertIsShown() {
        lateinit var rememberedGuestLink: String

        step("Given There is an MLS team owner with two team members") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Guests",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam(
                "user1Name",
                "Guests",
                backendClient
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Guests",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And MLS devices are registered for TeamOwner and Member2") {
            testServiceHelper.apply {
                addDevice("user1Name", null, "Device1")
                addDevice("user3Name", null, "Device2")
            }
        }

        step("And TeamOwner creates the MLS group conversation GuestsHere with Member2") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user3Name",
                conversationName = "GuestsHere",
                deviceName = "Device1"
            )
        }

        step("And TeamOwner enables guest access for GuestsHere") {
            backendSetupHelper.userEnablesGuestAccessForConversation("user1Name", "GuestsHere")
        }

        step("And Member1 is me") {
            teamMember = clientUserManager.findUserByNameOrNameAlias("user2Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        givenILoginThroughStagingDeepLink(teamMember, "Member1")

        step("And TeamOwner creates an invite link for GuestsHere") {
            backendSetupHelper.userCreatesInviteLink("user1Name", "GuestsHere")
        }

        step("And I remember the invite deep link for GuestsHere") {
            rememberedGuestLink = backendSetupHelper.getClientDeepLinkForPublicConversation(
                "user1Name",
                "GuestsHere"
            )
        }

        step("And TeamOwner revokes the invite link for GuestsHere") {
            backendSetupHelper.userRevokesInviteLink("user1Name", "GuestsHere")
        }

        step("When I open the remembered invite deep link") {
            pages.commonAppPage.openDeepLink(rememberedGuestLink)
        }

        step("Then I see the unable to join conversation alert") {
            pages.commonAppPage.assertUnableToJoinConversationAlertVisible()
        }

        step("And I see why I could not be added to the conversation") {
            pages.commonAppPage.assertUnableToJoinConversationAlertTextVisible(
                "Due to an error you could not be added to the conversation."
            )
        }

        step("And I dismiss the alert") {
            pages.commonAppPage.tapOkButtonOnAlert()
        }

        step("And I do not see group conversation GuestsHere in foreground") {
            pages.conversationViewPage.assertConversationScreenNotVisible()
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-8125")
    @Category("regression", "RC", "guestLinks")
    @Test
    fun givenGuestLinkExists_whenAnotherMemberBecomesAdmin_thenGuestLinkRemainsVisible() {
        step("Given There is an MLS team owner with two team members") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Guests",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam(
                "user1Name",
                "Guests",
                backendClient
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Guests",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            secondTeamMember = clientUserManager.findUserByNameOrNameAlias("user3Name")
        }

        step("And MLS devices are registered for all conversation participants") {
            testServiceHelper.apply {
                addDevice("user1Name", null, "Device1")
                addDevice("user2Name", null, "Device1")
                addDevice("user3Name", null, "Device2")
            }
        }

        step("And TeamOwner creates the MLS group conversation GuestsHere with Member1 and Member2") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name,user3Name",
                conversationName = "GuestsHere",
                deviceName = "Device1"
            )
        }

        step("And TeamOwner enables guest access for GuestsHere") {
            backendSetupHelper.userEnablesGuestAccessForConversation("user1Name", "GuestsHere")
        }

        step("And TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        givenILoginThroughStagingDeepLink(teamOwner, "TeamOwner")

        step("And I open Guests options for GuestsHere") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("GuestsHere")
                clickGroupConversation("GuestsHere")
            }
            pages.conversationViewPage.clickOnGroupConversationDetails("GuestsHere")
            pages.groupConversationDetailsPage.tapGuestOptions()
        }

        step("And I create a guest link without a password") {
            pages.groupAccessOptionsPage.apply {
                assertGuestSwitchState("ON")
                tapCreateLinkButton()
                tapCreateLinkWithoutPassword()
                assertGuestLinkCreated()
                tapBackButton()
            }
        }

        step("And I open Member2 from the participants list") {
            pages.groupConversationDetailsPage.apply {
                tapOnParticipantsTab()
                assertUsernameIsAddedToParticipantsList(secondTeamMember.name ?: "")
                tapUserInParticipantsList(secondTeamMember.name ?: "")
            }
            pages.connectedUserProfilePage.assertConnectedUserProfileVisible(secondTeamMember.name ?: "")
        }

        step("When I change Member2's conversation role to Admin") {
            pages.connectedUserProfilePage.apply {
                tapEditRoleButton()
                tapAdminRoleButton()
                assertAdminRoleVisible()
            }
        }

        step("And I return to the conversation list") {
            pages.connectedUserProfilePage.tapBackToConversationDetailsButton()
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
            pages.conversationViewPage.apply {
                assertConversationScreenVisible()
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("And I add Member2 as another account") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.tapNewTeamOrAddAccountButton()
            clientUserManager.setSelfUser(secondTeamMember)
        }

        givenILoginThroughStagingDeepLink(secondTeamMember, "Member2")

        step("And I see GuestsHere in the conversation list") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertGroupConversationVisible("GuestsHere")
                clickGroupConversation("GuestsHere")
            }
        }

        step("And I open Guests options as the new group admin") {
            pages.conversationViewPage.clickOnGroupConversationDetails("GuestsHere")
            pages.groupConversationDetailsPage.tapGuestOptions()
            pages.groupAccessOptionsPage.assertGuestSwitchState("ON")
        }

        step("Then I see the previously created guest link") {
            pages.groupAccessOptionsPage.assertGuestLinkCreated()
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-8150")
    @Category("regression", "RC", "guestLinks")
    @Test
    fun givenMultipleAccountsAreLoggedIn_whenIJoinViaGuestLink_thenLastActiveAccountJoinsConversation() {
        step("Given there are two MLS team owners") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Guests",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam(
                "user1Name",
                "Guests",
                backendClient
            )
            backendSetupHelper.createTeamOwnerByAlias(
                "user4Name",
                "NoGuests",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam(
                "user4Name",
                "NoGuests",
                backendClient
            )
        }

        step("And TeamOwnerA adds Member1 and Member2 to the Guests team") {
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Guests",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And MLS devices are registered for the conversation participants") {
            testServiceHelper.apply {
                addDevice("user1Name", null, "Device1")
                addDevice("user3Name", null, "Device2")
            }
        }

        step("And TeamOwnerA creates the MLS group conversation GuestsHere with Member2") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user3Name",
                conversationName = "GuestsHere",
                deviceName = "Device1"
            )
        }

        step("And TeamOwnerA enables guest access for GuestsHere") {
            backendSetupHelper.userEnablesGuestAccessForConversation("user1Name", "GuestsHere")
        }

        step("And Member1 is me") {
            teamMember = clientUserManager.findUserByNameOrNameAlias("user2Name").also {
                clientUserManager.setSelfUser(it)
            }
            teamOwnerB = clientUserManager.findUserByNameOrNameAlias("user4Name")
        }

        givenILoginThroughStagingDeepLink(teamMember, "Member1")

        step("And I add TeamOwnerB as another account") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                tapNewTeamOrAddAccountButton()
            }
            clientUserManager.setSelfUser(teamOwnerB)
        }

        givenILoginThroughStagingDeepLink(teamOwnerB, "TeamOwnerB")

        step("When I switch to Member1 account") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.apply {
                iSeeUserProfilePage()
                tapOtherAccountByName(teamMember.name ?: "")
            }
            clientUserManager.setSelfUser(teamMember)
        }

        step("And I see the conversation list") {
            pages.conversationListPage.assertConversationListVisible()
        }

        step("And TeamOwnerA creates an invite link for GuestsHere") {
            backendSetupHelper.userCreatesInviteLink("user1Name", "GuestsHere")
        }

        step("And I open the invite deep link for GuestsHere") {
            pages.commonAppPage.openDeepLink(
                backendSetupHelper.getClientDeepLinkForPublicConversation(
                    "user1Name",
                    "GuestsHere"
                )
            )
        }

        step("And I see the join conversation alert") {
            pages.commonAppPage.assertJoinConversationAlertVisible()
        }

        step("And I see the invitation text and GuestsHere in the alert") {
            pages.commonAppPage.apply {
                assertJoinConversationAlertTextVisible("You have been invited to a conversation.")
                assertJoinConversationAlertTextVisible("GuestsHere")
            }
        }

        step("And I join the conversation") {
            pages.commonAppPage.tapJoinConversationButton()
        }

        step("Then GuestsHere is open and I see that I joined the conversation") {
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("GuestsHere")
                assertSystemMessageVisible("You joined the conversation")
            }
        }

        step("When I return to the conversation list and open my profile") {
            pages.conversationViewPage.tapBackButtonToCloseConversationViewPage()
            pages.conversationListPage.clickUserProfileButton()
        }

        step("Then Member1 is active and TeamOwnerB is listed as another account") {
            pages.selfUserProfilePage.apply {
                assertCurrentAccountActive(teamMember.name ?: "")
                assertOtherLoggedInAccountVisible(teamOwnerB.name ?: "")
            }
        }
    }

    // ######################
    // With Password
    // ######################

    @Suppress("LongMethod")
    @TestCaseId("TC-8126")
    @Category("regression", "RC", "guestLinks")
    @Test
    fun givenGuestsAreEnabled_whenAdminCreatesPasswordSecuredGuestLink_thenLinkCanBeShared() {
        step("Given There is an MLS team owner with two team members") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Guests",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam(
                "user1Name",
                "Guests",
                backendClient
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Guests",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And MLS devices are registered for all conversation participants") {
            testServiceHelper.apply {
                addDevice("user1Name", null, "Device1")
                addDevice("user2Name", null, "Device1")
                addDevice("user3Name", null, "Device2")
            }
        }

        step("And TeamOwner creates the MLS group conversation GuestsHere") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name,user3Name",
                conversationName = "GuestsHere",
                deviceName = "Device1"
            )
        }

        step("And TeamOwner enables guest access for GuestsHere") {
            backendSetupHelper.userEnablesGuestAccessForConversation("user1Name", "GuestsHere")
        }

        step("And TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        givenILoginThroughStagingDeepLink(teamOwner, "TeamOwner")

        step("When I open Guests options for GuestsHere") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("GuestsHere")
                clickGroupConversation("GuestsHere")
            }
            pages.conversationViewPage.clickOnGroupConversationDetails("GuestsHere")
            pages.groupConversationDetailsPage.tapGuestOptions()
            pages.groupAccessOptionsPage.apply {
                assertGuestSwitchState("ON")
                tapCreateLinkButton()
            }
        }

        step("And I select create link with password") {
            pages.groupAccessOptionsPage.tapCreateLinkWithPassword()
        }

        step("And I see the create password secured link page") {
            pages.groupAccessOptionsPage.assertCreatePasswordSecuredLinkPageVisible()
        }

        step("And I enter and confirm the guest link password") {
            pages.groupAccessOptionsPage.apply {
                enterGuestLinkPassword(teamOwner.password)
                enterGuestLinkConfirmPassword(teamOwner.password)
            }
        }

        step("And I create the password secured guest link") {
            pages.groupAccessOptionsPage.tapCreateLinkButton()
        }

        step("Then I see the password secured guest link was created") {
            pages.groupAccessOptionsPage.apply {
                assertGuestLinkCreated()
                assertGuestLinkIsPasswordSecured()
            }
        }

        step("When I copy the guest link and return to GuestsHere") {
            pages.groupAccessOptionsPage.apply {
                tapCopyGuestLinkButton()
                tapBackButton()
            }
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
        }

        step("And I send the copied guest link") {
            pages.conversationViewPage.apply {
                pasteClipboardIntoMessageInputField()
                clickSendButton()
            }
        }

        step("Then I see the guest link in GuestsHere") {
            pages.conversationViewPage.assertGuestLinkVisibleInCurrentConversation()
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-8127")
    @Category("regression", "RC", "guestLinks")
    @Test
    fun givenPasswordSecuredGuestLink_whenTeamMemberEntersCorrectPassword_thenConversationIsJoined() {
        step("Given There is an MLS team owner with two team members") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Guests",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam(
                "user1Name",
                "Guests",
                backendClient
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Guests",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And MLS devices are registered for the conversation participants") {
            testServiceHelper.apply {
                addDevice("user1Name", null, "Device1")
                addDevice("user3Name", null, "Device2")
            }
        }

        step("And TeamOwner creates the MLS group conversation GuestsHere with Member2") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user3Name",
                conversationName = "GuestsHere",
                deviceName = "Device1"
            )
        }

        step("And TeamOwner enables guest access for GuestsHere") {
            backendSetupHelper.userEnablesGuestAccessForConversation("user1Name", "GuestsHere")
        }

        step("And Member1 is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            teamMember = clientUserManager.findUserByNameOrNameAlias("user2Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        givenILoginThroughStagingDeepLink(teamMember, "Member1")

        step("And TeamOwner creates a password secured invite link for GuestsHere") {
            backendSetupHelper.userCreatesInviteLink(
                "user1Name",
                "GuestsHere",
                teamOwner.password
            )
        }

        step("When I open the invite deep link for GuestsHere") {
            pages.commonAppPage.openDeepLink(
                backendSetupHelper.getClientDeepLinkForPublicConversation(
                    "user1Name",
                    "GuestsHere"
                )
            )
        }

        step("And I see the join conversation alert") {
            pages.commonAppPage.assertJoinConversationAlertVisible()
        }

        step("And I see the invitation text and GuestsHere in the alert") {
            pages.commonAppPage.apply {
                assertJoinConversationAlertTextVisible("You have been invited to a conversation.")
                assertJoinConversationAlertTextVisible("GuestsHere")
            }
        }

        step("And I enter the guest link password") {
            pages.commonAppPage.enterJoinConversationPassword(teamOwner.password)
        }

        step("And I join the conversation") {
            pages.commonAppPage.tapJoinConversationButton()
        }

        step("Then GuestsHere is open and I see that I joined the conversation") {
            pages.conversationViewPage.apply {
                assertGroupConversationInForeground("GuestsHere")
                assertSystemMessageVisible("You joined the conversation")
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-8128")
    @Category("regression", "RC", "guestLinks")
    @Test
    fun givenPasswordSecuredGuestLink_whenTeamMemberEntersWrongPassword_thenConversationIsNotJoined() {
        step("Given There is an MLS team owner with two team members") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Guests",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam(
                "user1Name",
                "Guests",
                backendClient
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Guests",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And MLS devices are registered for the conversation participants") {
            testServiceHelper.apply {
                addDevice("user1Name", null, "Device1")
                addDevice("user3Name", null, "Device2")
            }
        }

        step("And TeamOwner creates the MLS group conversation GuestsHere with Member2") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user3Name",
                conversationName = "GuestsHere",
                deviceName = "Device1"
            )
        }

        step("And TeamOwner enables guest access for GuestsHere") {
            backendSetupHelper.userEnablesGuestAccessForConversation("user1Name", "GuestsHere")
        }

        step("And Member1 is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name")
            teamMember = clientUserManager.findUserByNameOrNameAlias("user2Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        givenILoginThroughStagingDeepLink(teamMember, "Member1")

        step("And TeamOwner creates a password secured invite link for GuestsHere") {
            backendSetupHelper.userCreatesInviteLink(
                "user1Name",
                "GuestsHere",
                teamOwner.password
            )
        }

        step("When I open the invite deep link for GuestsHere") {
            pages.commonAppPage.openDeepLink(
                backendSetupHelper.getClientDeepLinkForPublicConversation(
                    "user1Name",
                    "GuestsHere"
                )
            )
        }

        step("And I see the join conversation alert") {
            pages.commonAppPage.assertJoinConversationAlertVisible()
        }

        step("And I see the invitation text and GuestsHere in the alert") {
            pages.commonAppPage.apply {
                assertJoinConversationAlertTextVisible("You have been invited to a conversation.")
                assertJoinConversationAlertTextVisible("GuestsHere")
            }
        }

        step("And I enter an incorrect guest link password") {
            pages.commonAppPage.enterJoinConversationPassword("${teamOwner.password}Wrong")
        }

        step("When I try to join the conversation, I see the invalid password error") {
            pages.commonAppPage.apply {
                tapJoinConversationButton()
                assertInvalidPasswordErrorVisible()
            }
        }

        step("And GuestsHere is not opened") {
            pages.conversationViewPage.assertConversationScreenNotVisible()
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-8129")
    @Category("regression", "RC", "guestLinks")
    @Test
    fun givenPasswordSecuredGuestLinkExists_whenAnotherMemberBecomesAdmin_thenLinkRemainsVisible() {
        step("Given There is an MLS team owner with two team members") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Guests",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam(
                "user1Name",
                "Guests",
                backendClient
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name",
                "Guests",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
            secondTeamMember = clientUserManager.findUserByNameOrNameAlias("user3Name")
        }

        step("And MLS devices are registered for all conversation participants") {
            testServiceHelper.apply {
                addDevice("user1Name", null, "Device1")
                addDevice("user2Name", null, "Device1")
                addDevice("user3Name", null, "Device2")
            }
        }

        step("And TeamOwner creates the MLS group conversation GuestsHere with Member1 and Member2") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name,user3Name",
                conversationName = "GuestsHere",
                deviceName = "Device1"
            )
        }

        step("And TeamOwner enables guest access for GuestsHere") {
            backendSetupHelper.userEnablesGuestAccessForConversation("user1Name", "GuestsHere")
        }

        step("And TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        givenILoginThroughStagingDeepLink(teamOwner, "TeamOwner")

        step("And I open Guests options for GuestsHere") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("GuestsHere")
                clickGroupConversation("GuestsHere")
            }
            pages.conversationViewPage.clickOnGroupConversationDetails("GuestsHere")
            pages.groupConversationDetailsPage.tapGuestOptions()
            pages.groupAccessOptionsPage.apply {
                assertGuestSwitchState("ON")
                tapCreateLinkButton()
            }
        }

        step("When I create a password secured guest link") {
            pages.groupAccessOptionsPage.apply {
                tapCreateLinkWithPassword()
                assertCreatePasswordSecuredLinkPageVisible()
                enterGuestLinkPassword(teamOwner.password)
                enterGuestLinkConfirmPassword(teamOwner.password)
                tapCreateLinkButton()
            }
        }

        step("Then I see the password secured guest link was created") {
            pages.groupAccessOptionsPage.apply {
                assertGuestLinkCreated()
                assertGuestLinkIsPasswordSecured()
                tapBackButton()
            }
        }

        step("And I open Member2 from the participants list") {
            pages.groupConversationDetailsPage.apply {
                tapOnParticipantsTab()
                assertUsernameIsAddedToParticipantsList(secondTeamMember.name ?: "")
                tapUserInParticipantsList(secondTeamMember.name ?: "")
            }
            pages.connectedUserProfilePage.assertConnectedUserProfileVisible(secondTeamMember.name ?: "")
        }

        step("When I change Member2's conversation role to Admin") {
            pages.connectedUserProfilePage.apply {
                tapEditRoleButton()
                tapAdminRoleButton()
                assertAdminRoleVisible()
            }
        }

        step("And I return to the conversation list") {
            pages.connectedUserProfilePage.tapBackToConversationDetailsButton()
            pages.groupConversationDetailsPage.tapCloseButtonOnGroupConversationDetailsPage()
            pages.conversationViewPage.apply {
                assertConversationScreenVisible()
                tapBackButtonToCloseConversationViewPage()
            }
        }

        step("And I add Member2 as another account") {
            pages.conversationListPage.clickUserProfileButton()
            pages.selfUserProfilePage.tapNewTeamOrAddAccountButton()
            clientUserManager.setSelfUser(secondTeamMember)
        }

        givenILoginThroughStagingDeepLink(secondTeamMember, "Member2")

        step("And I open Guests options as the new group admin") {
            pages.conversationListPage.apply {
                assertConversationListVisible()
                assertGroupConversationVisible("GuestsHere")
                clickGroupConversation("GuestsHere")
            }
            pages.conversationViewPage.clickOnGroupConversationDetails("GuestsHere")
            pages.groupConversationDetailsPage.tapGuestOptions()
            pages.groupAccessOptionsPage.assertGuestSwitchState("ON")
        }

        step("Then I see the previously created password secured guest link") {
            pages.groupAccessOptionsPage.apply {
                assertGuestLinkCreated()
                assertGuestLinkIsPasswordSecured()
            }
        }
    }

    // Shared app login flow: opens staging, signs in as the selected team user, and clears post-login prompts.
    private fun givenILoginThroughStagingDeepLink(user: ClientUser, userLabel: String) {
        step("And I see welcome screen before login") {
            pages.registrationPage.apply {
                assertEmailWelcomePage()
            }
        }

        step("And I open staging deep link login flow") {
            pages.loginPage.apply {
                clickStagingDeepLink()
                clickProceedButtonOnDeeplinkOverlay()
                clickContinueButtonOnBackendConfigSuccess()
            }
        }

        step("And I login as $userLabel") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(user.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(user.password ?: "")
                clickLoginButton()
            }
        }

        step("And I complete post-login permission and privacy prompts") {
            pages.registrationPage.apply {
                waitUntilLoginFlowIsCompleted()
                clickAllowNotificationButton()
                clickDeclineShareDataAlert()
            }
        }
    }
}
