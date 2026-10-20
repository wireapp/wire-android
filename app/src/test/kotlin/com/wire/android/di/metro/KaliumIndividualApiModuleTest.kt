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
package com.wire.android.di.metro

import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.accountScoped.ChannelsModule
import com.wire.android.di.accountScoped.ClientModule
import com.wire.android.di.accountScoped.ConversationModule
import com.wire.android.di.accountScoped.DebugModule
import com.wire.android.di.accountScoped.MeetingModule
import com.wire.android.di.accountScoped.MessageModule
import com.wire.android.di.accountScoped.SearchModule
import com.wire.android.di.accountScoped.UserModule
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.di.KaliumChannelsApi
import com.wire.kalium.logic.di.KaliumClientApi
import com.wire.kalium.logic.di.KaliumConsumer
import com.wire.kalium.logic.di.KaliumConversationApi
import com.wire.kalium.logic.di.KaliumDebugApi
import com.wire.kalium.logic.di.KaliumMeetingApi
import com.wire.kalium.logic.di.KaliumMessageApi
import com.wire.kalium.logic.di.KaliumSearchApi
import com.wire.kalium.logic.di.KaliumUserApi
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.channels.ObserveChannelsCreationPermissionUseCase
import com.wire.kalium.logic.feature.client.ObserveCurrentClientIdUseCase
import com.wire.kalium.logic.feature.conversation.ObserveArchivedUnreadConversationsCountUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.debug.RepairFaultyRemovalKeysUseCase
import com.wire.kalium.logic.feature.meeting.GetPaginatedMeetingOccurrencesUseCase
import com.wire.kalium.logic.feature.message.GetMessageByIdUseCase
import com.wire.kalium.logic.feature.message.ObserveMessageByIdUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import com.wire.kalium.logic.feature.search.FederatedSearchParser
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.util.DebugKaliumApi
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/**
 * Generated graph uses the eight production account modules and the production consumer module.
 * SDK resources are mocked here; real captures/identities are covered by KaliumSessionApiIdentityTest.
 */
@OptIn(DebugKaliumApi::class)
class KaliumIndividualApiModuleTest {
    @Test
    fun givenAccountModules_whenResolvingEightFamilies_thenOnlyPublicBindingApisAreRead() {
        val ports = SessionApiPorts()
        val graph = ports.graph()
        assertSame(ports.conversationObserver, graph.conversationObserver)
        assertSame(ports.sendText, graph.sendText)
        assertSame(ports.clientObserver, graph.clientObserver)
        assertSame(ports.selfUser, graph.selfUser)
        assertSame(ports.searchParser, graph.searchParser)
        assertSame(ports.channelsPermission, graph.channelsPermission)
        assertSame(ports.meetingOccurrences, graph.meetingOccurrences)
        assertSame(ports.debugRepair, graph.debugRepair)
        ports.verifyNoFacadeReads()
    }

    @Test
    fun givenSiblingChildren_whenResolvingUseCases_thenConsumerReadsStayChildOwnedAndParentStateStaysShared() {
        val ports = SessionApiPorts()
        val parent = ports.graph()
        val firstConsumer = mockk<KaliumConsumer>()
        val secondConsumer = mockk<KaliumConsumer>()
        val firstMessage = mockk<ObserveMessageByIdUseCase>()
        val secondMessage = mockk<ObserveMessageByIdUseCase>()
        val firstConversation = mockk<ObserveConversationDetailsUseCase>()
        val secondConversation = mockk<ObserveConversationDetailsUseCase>()
        val firstLookup = mockk<GetMessageByIdUseCase>()
        val secondLookup = mockk<GetMessageByIdUseCase>()
        every { firstConsumer.observeMessageById } returns firstMessage
        every { secondConsumer.observeMessageById } returns secondMessage
        every { firstConsumer.observeConversationDetails } returns firstConversation
        every { secondConsumer.observeConversationDetails } returns secondConversation
        every { firstConsumer.getMessageById } returns firstLookup
        every { secondConsumer.getMessageById } returns secondLookup
        val first = parent.childFactory.create(firstConsumer)
        val second = parent.childFactory.create(secondConsumer)
        verify(exactly = 0) { firstConsumer.getMessageById }
        verify(exactly = 0) { secondConsumer.getMessageById }
        assertSame(firstMessage, first.observeMessageById)
        assertSame(secondMessage, second.observeMessageById)
        assertNotSame(first.observeMessageById, second.observeMessageById)
        assertSame(firstConversation, first.observeConversationDetails)
        assertSame(secondConversation, second.observeConversationDetails)
        assertSame(firstLookup, first.getMessageById)
        assertSame(secondLookup, second.getMessageById)
        assertSame(parent.sendText, first.sendText)
        assertSame(first.sendText, second.sendText)
        assertSame(parent.debugRepair, first.debugRepair)
        assertSame(first.debugRepair, second.debugRepair)
        ports.verifyNoFacadeReads()
    }
}

@OptIn(DebugKaliumApi::class)
private class SessionApiPorts {
    private val account = UserId("individual-api", "wire.example")
    private val core = mockk<CoreLogic>()
    private val session = mockk<UserSessionScope>()
    private val conversations = mockk<KaliumConversationApi>()
    private val messages = mockk<KaliumMessageApi>()
    private val clients = mockk<KaliumClientApi>()
    private val users = mockk<KaliumUserApi>()
    private val search = mockk<KaliumSearchApi>()
    private val channels = mockk<KaliumChannelsApi>()
    private val meetings = mockk<KaliumMeetingApi>()
    private val debug = mockk<KaliumDebugApi>()
    val conversationObserver = mockk<ObserveArchivedUnreadConversationsCountUseCase>()
    val sendText = mockk<SendTextMessageUseCase>()
    val clientObserver = mockk<ObserveCurrentClientIdUseCase>()
    val selfUser = mockk<GetSelfUserUseCase>()
    val searchParser = mockk<FederatedSearchParser>()
    val channelsPermission = mockk<ObserveChannelsCreationPermissionUseCase>()
    val meetingOccurrences = mockk<GetPaginatedMeetingOccurrencesUseCase>()
    val debugRepair = mockk<RepairFaultyRemovalKeysUseCase>()

    init {
        every { core.getSessionScope(account) } returns session
        every { session.conversationApi } returns conversations
        every { session.messageApi } returns messages
        every { session.clientApi } returns clients
        every { session.userApi } returns users
        every { session.searchApi } returns search
        every { session.channelsApi } returns channels
        every { session.meetingApi } returns meetings
        every { session.debugApi } returns debug
        every { conversations.conversationObserveArchivedUnreadConversationsCount } returns conversationObserver
        every { messages.messageSendTextMessage } returns sendText
        every { clients.clientObserveCurrentClientId } returns clientObserver
        every { users.userGetSelfUser } returns selfUser
        every { search.searchFederatedSearchParser } returns searchParser
        every { channels.channelsObserveChannelsCreationPermissionUseCase } returns channelsPermission
        every { meetings.meetingGetPaginatedMeetingOccurrenceDetails } returns meetingOccurrences
        every { debug.debugRepairFaultyRemovalKeysUseCase } returns debugRepair
    }

    fun graph(): IndividualSessionModuleTestGraph =
        createGraphFactory<IndividualSessionModuleTestGraph.Factory>().create(core, account)

    fun verifyNoFacadeReads() {
        verify(exactly = 0) {
            session.conversations
            session.messages
            session.client
            session.users
            session.search
            session.channels
            session.meetings
            session.debug
        }
    }
}

@OptIn(DebugKaliumApi::class)
@DependencyGraph(
    bindingContainers = [
        ConversationModule::class,
        MessageModule::class,
        ClientModule::class,
        UserModule::class,
        SearchModule::class,
        ChannelsModule::class,
        MeetingModule::class,
        DebugModule::class,
    ],
)
internal interface IndividualSessionModuleTestGraph {
    val conversationObserver: ObserveArchivedUnreadConversationsCountUseCase
    val sendText: SendTextMessageUseCase
    val clientObserver: ObserveCurrentClientIdUseCase
    val selfUser: GetSelfUserUseCase
    val searchParser: FederatedSearchParser
    val channelsPermission: ObserveChannelsCreationPermissionUseCase
    val meetingOccurrences: GetPaginatedMeetingOccurrencesUseCase
    val debugRepair: RepairFaultyRemovalKeysUseCase
    val childFactory: IndividualConsumerModuleTestGraph.Factory

    @DependencyGraph.Factory
    interface Factory {
        fun create(
            @Provides @KaliumCoreLogic core: CoreLogic,
            @Provides @CurrentAccount account: UserId,
        ): IndividualSessionModuleTestGraph
    }
}

@GraphExtension(bindingContainers = [KaliumConsumerModule::class])
internal interface IndividualConsumerModuleTestGraph {
    val observeMessageById: ObserveMessageByIdUseCase
    val observeConversationDetails: ObserveConversationDetailsUseCase
    val getMessageById: GetMessageByIdUseCase
    val sendText: SendTextMessageUseCase
    val debugRepair: RepairFaultyRemovalKeysUseCase

    @GraphExtension.Factory
    interface Factory {
        fun create(@Provides consumer: KaliumConsumer): IndividualConsumerModuleTestGraph
    }
}
