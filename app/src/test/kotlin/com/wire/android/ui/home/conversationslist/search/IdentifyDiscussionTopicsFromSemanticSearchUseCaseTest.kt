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
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.home.conversationslist.search

import com.wire.android.framework.TestMessage
import com.wire.android.feature.aiassistant.DiscussionTopicGenerator
import com.wire.android.feature.aiassistant.DiscussionTopicMessage
import com.wire.android.feature.aiassistant.DiscussionTopicResult
import com.wire.kalium.common.error.StorageFailure
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.GetMessagesByConversationAndDateRangeUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours

class IdentifyDiscussionTopicsFromSemanticSearchUseCaseTest {

    @Test
    fun givenMessagesInSameConversationAndDay_whenInvoked_thenCreatesSingleClusterWithExpandedRange() = runTest {
        val conversationId = conversationId("one")
        val firstHit = message("hit-1", conversationId, "2026-06-18T10:00:00Z", "Alice", "Budget update")
        val secondHit = message("hit-2", conversationId, "2026-06-18T12:00:00Z", "Bob", "Invoice timing")
        val contextMessages = listOf(
            message("context-1", conversationId, "2026-06-18T09:30:00Z", "Alice", "Budget update"),
            message("context-2", conversationId, "2026-06-18T12:30:00Z", "Bob", "Invoice timing")
        )
        val (arrangement, useCase) = Arrangement()
            .withContextMessages(conversationId, contextMessages)
            .withTopic("Budget and invoice timing")
            .arrange()

        val summaries = useCase(listOf(firstHit, secondHit))

        assertEquals(1, summaries.size)
        assertEquals("Budget and invoice timing", summaries.single().topic)
        assertEquals(listOf("Alice", "Bob"), summaries.single().participants)
        assertEquals(firstHit.date.minus(1.hours), arrangement.getMessages.requests.single().fromInclusive)
        assertEquals(secondHit.date.plus(1.hours), arrangement.getMessages.requests.single().toInclusive)
    }

    @Test
    fun givenMessagesInDifferentConversations_whenInvoked_thenCreatesSeparateClusters() = runTest {
        val firstConversation = conversationId("one")
        val secondConversation = conversationId("two")
        val firstHit = message("hit-1", firstConversation, "2026-06-18T10:00:00Z", "Alice", "Budget update")
        val secondHit = message("hit-2", secondConversation, "2026-06-18T10:30:00Z", "Bob", "Travel plan")
        val (arrangement, useCase) = Arrangement()
            .withContextMessages(firstConversation, listOf(firstHit))
            .withContextMessages(secondConversation, listOf(secondHit))
            .withTopic("Topic")
            .arrange()

        val summaries = useCase(listOf(firstHit, secondHit))

        assertEquals(2, summaries.size)
        assertEquals(1, arrangement.topicGenerator.batchRequests.size)
        assertEquals(2, arrangement.topicGenerator.batchRequests.single().size)
    }

    @Test
    fun givenMessagesInSameConversationButDifferentDays_whenInvoked_thenCreatesSeparateClusters() = runTest {
        val conversationId = conversationId("one")
        val firstHit = message("hit-1", conversationId, "2026-06-18T10:00:00Z", "Alice", "Budget update")
        val secondHit = message("hit-2", conversationId, "2026-06-19T10:00:00Z", "Bob", "Travel plan")
        val (_, useCase) = Arrangement()
            .withContextMessages(conversationId, listOf(firstHit, secondHit))
            .withTopic("Topic")
            .arrange()

        val summaries = useCase(listOf(firstHit, secondHit))

        assertEquals(2, summaries.size)
    }

    @Test
    fun givenTopicGenerationFails_whenInvoked_thenUsesFallbackTopic() = runTest {
        val conversationId = conversationId("one")
        val hit = message("hit-1", conversationId, "2026-06-18T10:00:00Z", "Alice", "Budget update")
        val (_, useCase) = Arrangement()
            .withContextMessages(conversationId, listOf(hit))
            .withTopicResult(DiscussionTopicResult.InferenceFailed("boom"))
            .arrange()

        val summary = useCase(listOf(hit)).single()

        assertEquals("Discussion topic unavailable", summary.topic)
    }

    @Test
    fun givenTextContext_whenInvoked_thenSendsTextMessagesToTopicGenerator() = runTest {
        val conversationId = conversationId("one")
        val textMessage = message("hit-1", conversationId, "2026-06-18T10:00:00Z", "Alice", "Budget update")
        val assetMessage = TestMessage.ASSET_MESSAGE.copy(
            id = "asset",
            conversationId = conversationId,
            date = Instant.parse("2026-06-18T10:05:00Z")
        )
        val (arrangement, useCase) = Arrangement()
            .withContextMessages(conversationId, listOf(textMessage, assetMessage))
            .withTopic("Budget")
            .arrange()

        useCase(listOf(textMessage))

        assertEquals(listOf(DiscussionTopicMessage("Alice", "Budget update")), arrangement.topicGenerator.requests.single())
    }

    private class Arrangement {
        @MockK
        lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

        val getMessages = FakeGetMessagesByConversationAndDateRangeUseCase()
        val topicGenerator = FakeDiscussionTopicGenerator()

        init {
            MockKAnnotations.init(this)
            coEvery { observeConversationDetails(any()) } returns flowOf(
                ObserveConversationDetailsUseCase.Result.Failure(StorageFailure.DataNotFound)
            )
        }

        fun withContextMessages(conversationId: ConversationId, messages: List<Message.Standalone>) = apply {
            getMessages.messagesByConversation[conversationId] = messages
        }

        fun withTopic(topic: String) = withTopicResult(DiscussionTopicResult.Success(topic))

        fun withTopicResult(result: DiscussionTopicResult) = apply {
            topicGenerator.result = result
        }

        fun arrange() = this to IdentifyDiscussionTopicsFromSemanticSearchUseCase(
            getMessagesByConversationAndDateRange = getMessages,
            observeConversationDetails = observeConversationDetails,
            discussionTopicGenerator = topicGenerator
        )
    }

    private class FakeGetMessagesByConversationAndDateRangeUseCase : GetMessagesByConversationAndDateRangeUseCase {
        val messagesByConversation = mutableMapOf<ConversationId, List<Message.Standalone>>()
        val requests = mutableListOf<Request>()

        override suspend fun invoke(
            conversationId: ConversationId,
            fromInclusive: Instant,
            toInclusive: Instant
        ): GetMessagesByConversationAndDateRangeUseCase.Result {
            requests += Request(conversationId, fromInclusive, toInclusive)
            return GetMessagesByConversationAndDateRangeUseCase.Result.Success(messagesByConversation[conversationId].orEmpty())
        }

        data class Request(
            val conversationId: ConversationId,
            val fromInclusive: Instant,
            val toInclusive: Instant
        )
    }

    private class FakeDiscussionTopicGenerator : DiscussionTopicGenerator {
        var result: DiscussionTopicResult = DiscussionTopicResult.Success("Topic")
        val requests = mutableListOf<List<DiscussionTopicMessage>>()
        val batchRequests = mutableListOf<List<List<DiscussionTopicMessage>>>()

        override suspend fun generateTopic(messages: List<DiscussionTopicMessage>): DiscussionTopicResult {
            requests += messages
            return result
        }

        override suspend fun generateTopics(messageClusters: List<List<DiscussionTopicMessage>>): List<DiscussionTopicResult> {
            batchRequests += messageClusters
            requests += messageClusters
            return messageClusters.map { result }
        }
    }

    private fun conversationId(value: String) = ConversationId(value, "example.com")

    private fun message(
        id: String,
        conversationId: ConversationId,
        date: String,
        senderName: String,
        text: String
    ) = TestMessage.TEXT_MESSAGE.copy(
        id = id,
        conversationId = conversationId,
        date = Instant.parse(date),
        senderUserId = UserId(senderName.lowercase(), "example.com"),
        senderUserName = senderName,
        content = MessageContent.Text(text)
    )
}
