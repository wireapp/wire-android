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

import com.wire.android.AppJsonStyledLogger
import com.wire.android.feature.aiassistant.DiscussionTopicGenerator
import com.wire.android.feature.aiassistant.DiscussionTopicMessage
import com.wire.android.feature.aiassistant.DiscussionTopicResult
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.GetMessagesByConversationAndDateRangeUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours

class IdentifyDiscussionTopicsFromSemanticSearchUseCase @Inject constructor(
    private val getMessagesByConversationAndDateRange: GetMessagesByConversationAndDateRangeUseCase,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val discussionTopicGenerator: DiscussionTopicGenerator
) {
    suspend operator fun invoke(searchResults: List<Message.Standalone>): List<DiscussionClusterSummary> {
        if (searchResults.isEmpty()) return emptyList()

        val contexts = searchResults
            .groupBy { it.conversationId }
            .flatMap { (conversationId, messages) -> messages.groupByLocalDay(conversationId) }
            .mapNotNull { cluster -> cluster.toContext() }

        if (contexts.isEmpty()) return emptyList()

        val topicResults = discussionTopicGenerator.generateTopics(contexts.map { it.topicMessages })
        return contexts.mapIndexed { index, context ->
            val summary = context.toSummary(topic = topicResults.getOrNull(index).toTopic())
            summary.log()
            summary
        }
    }

    private suspend fun SearchHitCluster.toContext(): DiscussionClusterContext? {
        val hitRangeStart = messages.minOf { it.date }
        val hitRangeEnd = messages.maxOf { it.date }
        val contextRangeStart = hitRangeStart.minus(CONTEXT_WINDOW)
        val contextRangeEnd = hitRangeEnd.plus(CONTEXT_WINDOW)

        val contextMessages = when (
            val result = getMessagesByConversationAndDateRange(
                conversationId = conversationId,
                fromInclusive = contextRangeStart,
                toInclusive = contextRangeEnd
            )
        ) {
            is GetMessagesByConversationAndDateRangeUseCase.Result.Success -> result.messages
            is GetMessagesByConversationAndDateRangeUseCase.Result.Failure -> messages
        }.sortedBy { it.date }

        if (contextMessages.isEmpty()) return null

        val topicMessages = contextMessages.mapNotNull { message ->
            message.textForTopic()?.let { text ->
                DiscussionTopicMessage(
                    senderName = message.participantName(),
                    text = text
                )
            }
        }

        return DiscussionClusterContext(
            conversationId = conversationId,
            contextMessages = contextMessages,
            topicMessages = topicMessages
        )
    }

    private suspend fun DiscussionClusterContext.toSummary(topic: String): DiscussionClusterSummary =
        DiscussionClusterSummary(
            topic = topic,
            conversationName = resolveConversationName(conversationId),
            firstMessageDate = contextMessages.first().date,
            lastMessageDate = contextMessages.last().date,
            participants = contextMessages
                .map { it.participantName() }
                .distinct()
        )

    private fun DiscussionTopicResult?.toTopic(): String = when (this) {
        is DiscussionTopicResult.Success -> topic
        DiscussionTopicResult.EmptyInput,
        DiscussionTopicResult.EmptyResponse,
        DiscussionTopicResult.MissingModel,
        DiscussionTopicResult.UnsupportedModel -> FALLBACK_TOPIC
        is DiscussionTopicResult.InferenceFailed,
        null -> FALLBACK_TOPIC
    }

    private fun List<Message.Standalone>.groupByLocalDay(
        conversationId: ConversationId
    ): List<SearchHitCluster> {
        val timeZone = TimeZone.currentSystemDefault()
        return groupBy { it.date.toLocalDateTime(timeZone).date }
            .values
            .map { messages ->
                SearchHitCluster(
                    conversationId = conversationId,
                    messages = messages.sortedBy { it.date }
                )
            }
    }

    private suspend fun resolveConversationName(conversationId: ConversationId): String {
        val result = runCatching { observeConversationDetails(conversationId).first() }.getOrNull()
        val name = when (result) {
            is ObserveConversationDetailsUseCase.Result.Success -> result.conversationDetails.displayName()
            is ObserveConversationDetailsUseCase.Result.Failure,
            null -> null
        }
        return name?.takeIf { it.isNotBlank() } ?: conversationId.toString()
    }

    private fun ConversationDetails.displayName(): String? = when (this) {
        is ConversationDetails.OneOne -> otherUser.name
        else -> conversation.name
    }

    private fun Message.Standalone.textForTopic(): String? = when (val content = content) {
        is MessageContent.Text -> content.value
        is MessageContent.Multipart -> content.value
        else -> null
    }?.takeIf { it.isNotBlank() }

    private fun Message.Standalone.participantName(): String =
        sender?.name
            ?.takeIf { it.isNotBlank() }
            ?: (this as? Message.Regular)?.senderUserName?.takeIf { it.isNotBlank() }
            ?: senderUserId.toString()

    private fun DiscussionClusterSummary.log() {
        AppJsonStyledLogger.log(
            level = KaliumLogLevel.INFO,
            leadingMessage = LOG_TAG,
            jsonStringKeyValues = mapOf(
                "discussionTopic" to topic,
                "conversationName" to conversationName,
                "firstMessageDate" to firstMessageDate.toString(),
                "lastMessageDate" to lastMessageDate.toString(),
                "participants" to participants
            )
        )
    }

    private data class SearchHitCluster(
        val conversationId: ConversationId,
        val messages: List<Message.Standalone>,
    )

    private data class DiscussionClusterContext(
        val conversationId: ConversationId,
        val contextMessages: List<Message.Standalone>,
        val topicMessages: List<DiscussionTopicMessage>
    )

    private companion object {
        val CONTEXT_WINDOW = 1.hours
        const val FALLBACK_TOPIC = "Discussion topic unavailable"
        const val LOG_TAG = "SemanticSearchDiscussionTopic"
    }
}

data class DiscussionClusterSummary(
    val topic: String,
    val conversationName: String,
    val firstMessageDate: Instant,
    val lastMessageDate: Instant,
    val participants: List<String>
)
