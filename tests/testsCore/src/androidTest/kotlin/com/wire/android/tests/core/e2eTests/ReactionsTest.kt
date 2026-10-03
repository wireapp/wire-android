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
import uiautomatorutils.KeyboardUtils.closeKeyboardIfOpened
import user.utils.ClientUser

@Suppress("LargeClass")
@RunWith(AndroidJUnit4::class)
class ReactionsTest : BaseUiTest() {
    private lateinit var teamOwner: ClientUser
    private var member1: ClientUser? = null
    private var member2: ClientUser? = null
    private var member3: ClientUser? = null

    @Before
    fun setUp() {
        initCommonTestHelpers()
        device = UiAutomatorSetup.start(UiAutomatorSetup.APP_ALPHA)
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4478")
    @Category("regression", "RC", "reactions")
    @Test
    fun givenOneOnOneConversation_whenMemberReactsToMyMessage_thenISeeTheReactions() {
        step("Given There is an MLS team owner with a team member") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Reactions",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam(
                "user1Name",
                "Reactions",
                backendClient
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Reactions",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And TeamOwner has a 1:1 conversation with Member1") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Reactions"
            )
        }

        step("And TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name").also {
                clientUserManager.setSelfUser(it)
            }
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I open the conversation with Member1") {
            pages.conversationListPage.apply {
                assertConversationVisible(member1?.name ?: "")
                tapConversationNameInConversationList(member1?.name ?: "")
            }
        }

        step("When I send message Hello!") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        listOf("❤️", "👍", "😁", "🙂").forEach { reaction ->
            step("Then Member1 reacts with $reaction and I see the reaction") {
                testServiceHelper.userTogglesReactionOnLatestMessage(
                    senderAlias = "user2Name",
                    convoName = "user1Name",
                    deviceName = "Device1",
                    reaction = reaction
                )
                pages.conversationViewPage.assertReactionAndUserCountVisible(reaction, 1)
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4479", "TC-4480", "TC-4481")
    @Category("regression", "RC", "reactions")
    @Test
    fun givenGroupConversation_whenMembersReactToMyMessage_thenReactionDetailsAreVisible() {
        step("Given There is an MLS team owner with three team members") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Reactions",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam(
                "user1Name",
                "Reactions",
                backendClient
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name,user4Name",
                "Reactions",
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
                addDevice("user4Name", null, "Device3")
            }
        }

        step("And TeamOwner creates the MLS group conversation ReactHere!") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name,user3Name,user4Name",
                conversationName = "ReactHere!",
                deviceName = "Device1"
            )
        }

        step("And TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name").also {
                clientUserManager.setSelfUser(it)
            }
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            member2 = clientUserManager.findUserByNameOrNameAlias("user3Name")
            member3 = clientUserManager.findUserByNameOrNameAlias("user4Name")
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I open ReactHere! and send message Hello!") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("ReactHere!")
                clickGroupConversation("ReactHere!")
            }
            pages.conversationViewPage.apply {
                typeMessageInInputField("Hello!")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("Hello!")
            }
        }

        step("When Member1 toggles reactions ❤️ and 👍 on the recent message via Device1") {
            testServiceHelper.userTogglesReactionsOnLatestMessage(
                "user2Name",
                "ReactHere!",
                "Device1",
                listOf("❤️", "👍")
            )
        }

        step("And Member2 toggles reactions ❤️, 👍, 😁 and 👎 on the recent message via Device2") {
            testServiceHelper.userTogglesReactionsOnLatestMessage(
                "user3Name",
                "ReactHere!",
                "Device2",
                listOf("❤️", "👍", "😁", "👎")
            )
        }

        step("And Member3 toggles reactions ❤️ and 🙂 on the recent message via Device3") {
            testServiceHelper.userTogglesReactionsOnLatestMessage(
                "user4Name",
                "ReactHere!",
                "Device3",
                listOf("❤️", "🙂")
            )
        }

        step("Then I see all reactions and their user counts") {
            pages.conversationViewPage.apply {
                assertReactionAndUserCountVisible("❤️", 3)
                assertReactionAndUserCountVisible("👍", 2)
                assertReactionAndUserCountVisible("😁", 1)
                assertReactionAndUserCountVisible("🙂", 1)
                assertReactionAndUserCountVisible("👎", 1)
            }
        }

        step("When I open message details for Hello!") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Hello!")
                tapMessageDetailsOption()
            }
        }

        step("Then I see 8 reactions and all reacting members in message details") {
            pages.messageDetailsPage.apply {
                assertReactionsCount(8)
                assertUserReacted(member1?.name ?: "")
                assertUserReacted(member2?.name ?: "")
                assertUserReacted(member3?.name ?: "")
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @TestCaseId("TC-4475", "TC-4483")
    @Category("regression", "RC", "reactions")
    @Test
    fun givenMessageFromMember_whenIAddAndRemoveReactions_thenReactionDetailsAreUpdated() {
        step("Given There is an MLS team owner with a team member") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Reactions",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam(
                "user1Name",
                "Reactions",
                backendClient
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Reactions",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And TeamOwner has a 1:1 conversation with Member1") {
            backendSetupHelper.userHas1on1ConversationInTeam(
                "user1Name",
                "user2Name",
                "Reactions"
            )
        }

        step("And TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name").also {
                clientUserManager.setSelfUser(it)
            }
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I open the conversation with Member1") {
            pages.conversationListPage.apply {
                assertConversationVisible(member1?.name ?: "")
                tapConversationNameInConversationList(member1?.name ?: "")
            }
        }

        step("When Member1 sends message Hello! via Device1") {
            testServiceHelper.userSendMessageToPersonalMlsConversation(
                "user2Name",
                "Hello!",
                "Device1",
                "user1Name"
            )
        }

        step("Then I see message Hello! in the conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("When I add the ❤️ reaction to Member1's message") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Hello!")
                assertTextMessageReactionOptionsVisible()
                tapReactionIcon("❤️")
                assertReactionAndUserCountVisible("❤️", 1)
            }
        }

        step("And I open message details for the ❤️ reaction") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Hello!")
                tapMessageDetailsOption()
            }
        }

        step("Then I see my ❤️ reaction in message details") {
            pages.messageDetailsPage.apply {
                assertReactionsCount(1)
                assertUserReacted(teamOwner.name ?: "")
            }
        }

        // TC-4483 - I want to be able to remove my reaction to a text message in a 1:1 conversation
        step("When I return to the conversation and remove my ❤️ reaction") {
            device.pressBack()
            pages.conversationViewPage.apply {
                assertConversationScreenVisible()
                tapReactionIcon("❤️")
                assertReactionNotVisible("❤️")
            }
        }

        step("And I add the 👍 reaction to Member1's message") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Hello!")
                assertTextMessageReactionOptionsVisible()
                tapReactionIcon("👍")
                assertReactionAndUserCountVisible("👍", 1)
            }
        }

        step("And I open message details for the 👍 reaction") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Hello!")
                tapMessageDetailsOption()
            }
        }

        step("Then I see my 👍 reaction in message details") {
            pages.messageDetailsPage.apply {
                assertReactionsCount(1)
                assertUserReacted(teamOwner.name ?: "")
            }
        }

        step("When I return to the conversation and remove my 👍 reaction") {
            device.pressBack()
            pages.conversationViewPage.apply {
                assertConversationScreenVisible()
                tapReactionIcon("👍")
                assertReactionNotVisible("👍")
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4476", "TC-4484")
    @Category("regression", "RC", "reactions")
    @Test
    fun givenGroupMessage_whenIAddAndRemoveReactions_thenReactionDetailsAreUpdated() {
        step("Given There is an MLS team owner with three team members") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Reactions",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam(
                "user1Name",
                "Reactions",
                backendClient
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name,user4Name",
                "Reactions",
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
                addDevice("user4Name", null, "Device3")
            }
        }

        step("And TeamOwner creates the MLS group conversation ReactHere!") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name,user3Name,user4Name",
                conversationName = "ReactHere!",
                deviceName = "Device1"
            )
        }

        step("And TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name").also {
                clientUserManager.setSelfUser(it)
            }
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            member2 = clientUserManager.findUserByNameOrNameAlias("user3Name")
            member3 = clientUserManager.findUserByNameOrNameAlias("user4Name")
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I open the group conversation ReactHere!") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("ReactHere!")
                clickGroupConversation("ReactHere!")
            }
        }

        step("When Member1 sends message Hello! via Device1") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Hello!",
                "Device1",
                "ReactHere!"
            )
        }

        step("Then I see message Hello! in the conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("When I add ❤️, ☹️ and 👎 reactions to Member1's message") {
            listOf("❤️", "☹️", "👎").forEach { reaction ->
                addReactionToMessage("Hello!", reaction)
            }
        }

        step("And Member1 adds the ❤️ reaction via Device1") {
            testServiceHelper.userTogglesReactionOnLatestMessage(
                "user2Name",
                "ReactHere!",
                "Device1",
                "❤️"
            )
        }

        step("And Member2 adds the ❤️ reaction via Device2") {
            testServiceHelper.userTogglesReactionOnLatestMessage(
                "user3Name",
                "ReactHere!",
                "Device2",
                "❤️"
            )
        }

        step("And Member3 adds the 👎 reaction via Device3") {
            testServiceHelper.userTogglesReactionOnLatestMessage(
                "user4Name",
                "ReactHere!",
                "Device3",
                "👎"
            )
        }

        step("When I open message details for Hello!") {
            pages.conversationViewPage.apply {
                longPressOnMessage("Hello!")
                tapMessageDetailsOption()
            }
        }

        step("Then I see 6 reactions and all expected reacting members") {
            pages.messageDetailsPage.apply {
                assertReactionsCount(6)
                assertUserReacted(teamOwner.name ?: "")
                assertUserReacted(member2?.name ?: "")
                assertUserReacted(member3?.name ?: "")
            }
        }

        // TC-4484 - I want to be able to remove my reaction to a text message in a group conversation
        step("When I return to the conversation and remove my ❤️ reaction") {
            device.pressBack()
            pages.conversationViewPage.apply {
                assertConversationScreenVisible()
                tapReactionIcon("❤️")
                assertReactionAndUserCountVisible("❤️", 2)
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4477", "TC-4485")
    @Category("regression", "RC", "reactions")
    @Test
    fun givenGroupImage_whenIAddAndRemoveReactions_thenReactionDetailsAreUpdated() {
        step("Given There is an MLS team owner with three team members") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Reactions",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam(
                "user1Name",
                "Reactions",
                backendClient
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name,user4Name",
                "Reactions",
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
                addDevice("user4Name", null, "Device3")
            }
        }

        step("And TeamOwner creates the MLS group conversation ReactHere!") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name,user3Name,user4Name",
                conversationName = "ReactHere!",
                deviceName = "Device1"
            )
        }

        step("And TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name").also {
                clientUserManager.setSelfUser(it)
            }
            member1 = clientUserManager.findUserByNameOrNameAlias("user2Name")
            member2 = clientUserManager.findUserByNameOrNameAlias("user3Name")
            member3 = clientUserManager.findUserByNameOrNameAlias("user4Name")
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I open the group conversation ReactHere!") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("ReactHere!")
                clickGroupConversation("ReactHere!")
            }
        }

        step("When Member1 sends image testing.jpg to ReactHere! via Device1") {
            testServiceHelper.contactSendsLocalImageConversation(
                context,
                "testing.jpg",
                "user2Name",
                "Device1",
                "ReactHere!"
            )
        }

        step("Then I see the image in the conversation") {
            pages.conversationViewPage.assertImageIsVisible()
        }

        step("When I add ❤️, 🙂 and ☹️ reactions to Member1's image") {
            listOf("❤️", "🙂", "☹️").forEach(::addReactionToImage)
        }

        step("And Member2 adds the ❤️ reaction via Device2") {
            testServiceHelper.userTogglesReactionOnLatestMessage(
                "user3Name",
                "ReactHere!",
                "Device2",
                "❤️"
            )
        }

        step("And Member3 adds the ❤️ reaction via Device3") {
            testServiceHelper.userTogglesReactionOnLatestMessage(
                "user4Name",
                "ReactHere!",
                "Device3",
                "❤️"
            )
        }

        step("Then I see the ❤️ reaction from 3 users") {
            pages.conversationViewPage.assertReactionAndUserCountVisible("❤️", 3)
        }

        step("When I open message details for the image") {
            pages.conversationViewPage.apply {
                longPressImageMessage()
                tapMessageDetailsOption()
            }
        }

        step("Then I see 5 reactions and all expected reacting members") {
            pages.messageDetailsPage.apply {
                assertReactionsCount(5)
                assertUserReacted(teamOwner.name ?: "")
                assertUserReacted(member2?.name ?: "")
                assertUserReacted(member3?.name ?: "")
            }
        }

        // TC-4485 - I want to be able to remove my reaction to an image in a group conversation
        step("When I return to the conversation and remove my ❤️ reaction") {
            device.pressBack()
            pages.conversationViewPage.apply {
                assertConversationScreenVisible()
                tapReactionIcon("❤️")
                assertReactionAndUserCountVisible("❤️", 2)
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4482", "TC-4486")
    @Category("regression", "RC", "reactions")
    @Test
    fun givenGroupMessageWithReactions_whenSenderEditsMessage_thenAllReactionsAreRemoved() {
        step("Given There is an MLS team owner with three team members") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Reactions",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam(
                "user1Name",
                "Reactions",
                backendClient
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name,user3Name,user4Name",
                "Reactions",
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
                addDevice("user4Name", null, "Device3")
            }
        }

        step("And TeamOwner creates the MLS group conversation ReactHere!") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name,user3Name,user4Name",
                conversationName = "ReactHere!",
                deviceName = "Device1"
            )
        }

        step("And TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I open the group conversation ReactHere!") {
            pages.conversationListPage.apply {
                assertGroupConversationVisible("ReactHere!")
                clickGroupConversation("ReactHere!")
            }
        }

        step("When Member1 sends message Hello! via Device1") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Hello!",
                "Device1",
                "ReactHere!"
            )
        }

        step("Then I see message Hello! in the conversation") {
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Hello!")
        }

        step("When I add the ❤️ reaction to Member1's message") {
            addReactionToMessage("Hello!", "❤️")
        }

        step("When Member1 edits the recent message to Good Day via Device1") {
            testServiceHelper.userEditsLatestMessageInGroupConversation(
                senderAlias = "user2Name",
                newMessage = "Good Day",
                deviceName = "Device1",
                conversationName = "ReactHere!"
            )
        }

        step("Then I see Good Day without the ❤️ reaction") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Good Day")
                assertReactionNotVisible("❤️")
            }
        }

        // TC-4486 - I want to verify that other users' reactions are deleted after a message is edited
        step("When Member2 adds the ❤️ reaction via Device2") {
            testServiceHelper.userTogglesReactionOnLatestMessage(
                "user3Name",
                "ReactHere!",
                "Device2",
                "❤️"
            )
        }

        step("Then I see the ❤️ reaction from 1 user") {
            pages.conversationViewPage.assertReactionAndUserCountVisible("❤️", 1)
        }

        step("When Member3 adds the ❤️ reaction via Device3") {
            testServiceHelper.userTogglesReactionOnLatestMessage(
                "user4Name",
                "ReactHere!",
                "Device3",
                "❤️"
            )
        }

        step("Then I see the ❤️ reaction from 2 users") {
            pages.conversationViewPage.assertReactionAndUserCountVisible("❤️", 2)
        }

        step("When Member1 edits the recent message to Good Morning via Device1") {
            testServiceHelper.userEditsLatestMessageInGroupConversation(
                senderAlias = "user2Name",
                newMessage = "Good Morning",
                deviceName = "Device1",
                conversationName = "ReactHere!"
            )
        }

        step("Then I see Good Morning without the ❤️ reaction") {
            pages.conversationViewPage.apply {
                assertReceivedMessageIsVisibleInCurrentConversation("Good Morning")
                assertReactionNotVisible("❤️")
            }
        }
    }

    @Suppress("LongMethod")
    @TestCaseId("TC-4487")
    @Category("regression", "RC", "reactions", "WPB-3525")
    @Test
    fun givenGroupConversationWithManyMessages_whenIScrollAwayAndBack_thenICanReactToMessage() {
        step("Given There is an MLS team owner with a team member") {
            backendSetupHelper.createTeamOwnerByAlias(
                "user1Name",
                "Reactions",
                "en_US",
                true,
                backendClient,
                context
            )
            backendSetupHelper.userConfiguresMLSForTeam(
                "user1Name",
                "Reactions",
                backendClient
            )
            backendSetupHelper.userXAddsUsersToTeam(
                "user1Name",
                "user2Name",
                "Reactions",
                TeamRoles.Member,
                backendClient,
                context,
                true
            )
        }

        step("And MLS devices are registered for both participants") {
            testServiceHelper.apply {
                addDevice("user1Name", null, "Device1")
                addDevice("user2Name", null, "Device1")
            }
        }

        step("And TeamOwner creates the MLS group conversation ReactHere!") {
            testServiceHelper.userCreatesMLSGroupConversation(
                ownerAlias = "user1Name",
                participantAliases = "user2Name",
                conversationName = "ReactHere!",
                deviceName = "Device1"
            )
        }

        step("And TeamOwner is me") {
            teamOwner = clientUserManager.findUserByNameOrNameAlias("user1Name").also {
                clientUserManager.setSelfUser(it)
            }
        }

        givenILoginAsTeamOwnerThroughStagingDeepLink()

        step("And I see ReactHere! in the conversation list") {
            pages.conversationListPage.assertGroupConversationVisible("ReactHere!")
        }

        step("When Member1 sends 40 messages to ReactHere! via Device1") {
            repeat(40) {
                testServiceHelper.userSendMessageToConversation(
                    "user2Name",
                    "1 message",
                    "Device1",
                    "ReactHere!"
                )
            }
        }

        step("And I wait until the notification popup disappears") {
            pages.notificationsPage.waitUntilNotificationPopUpGone()
        }

        step("And I open ReactHere! and scroll to the bottom") {
            pages.conversationListPage.clickGroupConversation("ReactHere!")
            pages.conversationViewPage.scrollToBottomOfConversationScreen()
        }

        step("And I send message That is a lot of messages") {
            pages.conversationViewPage.apply {
                typeMessageInInputField("That is a lot of messages")
                clickSendButton()
                assertSentMessageIsVisibleInCurrentConversation("That is a lot of messages")
            }
            closeKeyboardIfOpened()
        }

        step("And Member1 sends message Yes! via Device1") {
            testServiceHelper.userSendMessageToConversation(
                "user2Name",
                "Yes!",
                "Device1",
                "ReactHere!"
            )
            pages.conversationViewPage.assertReceivedMessageIsVisibleInCurrentConversation("Yes!")
        }

        step("When I scroll to the top and return to the bottom") {
            pages.conversationViewPage.apply {
                scrollToTopOfConversationScreen()
                scrollToBottomOfConversationScreen()
            }
        }

        step("And I add the ❤️ reaction to Member1's message") {
            addReactionToMessage("Yes!", "❤️")
        }

        step("Then I see the ❤️ reaction from 1 user") {
            pages.conversationViewPage.assertReactionAndUserCountVisible("❤️", 1)
        }
    }

    /** Adds a reaction to a text message and verifies that it is shown for the current user. */
    private fun addReactionToMessage(message: String, reaction: String) {
        pages.conversationViewPage.apply {
            longPressOnMessage(message)
            assertTextMessageReactionOptionsVisible()
            tapReactionIcon(reaction)
            assertReactionAndUserCountVisible(reaction, 1)
        }
    }

    /** Adds a reaction to the visible image message and verifies that it is shown for the current user. */
    private fun addReactionToImage(reaction: String) {
        pages.conversationViewPage.apply {
            longPressImageMessage()
            assertBottomSheetButtonsVisible_ReactionsDetailsReplyDownloadShareOpenDelete()
            tapReactionIcon(reaction)
            assertReactionAndUserCountVisible(reaction, 1)
        }
    }

    // Shared app login flow: opens staging, signs in as TeamOwner, and clears post-login prompts.
    private fun givenILoginAsTeamOwnerThroughStagingDeepLink() {
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

        step("And I login as TeamOwner") {
            pages.loginPage.apply {
                enterTeamOwnerLoggingEmail(teamOwner.email ?: "")
                clickLoginButton()
                enterTeamOwnerLoggingPassword(teamOwner.password ?: "")
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
