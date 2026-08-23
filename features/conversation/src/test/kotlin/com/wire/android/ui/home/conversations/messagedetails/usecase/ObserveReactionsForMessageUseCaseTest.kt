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

package com.wire.android.ui.home.conversations.messagedetails.usecase

import com.wire.android.config.TestDispatcherProvider
import com.wire.android.mapper.UIParticipantMapper
import com.wire.android.mapper.testUIParticipant
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.UserSummary
import com.wire.kalium.logic.data.message.reaction.MessageReaction
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo
import com.wire.kalium.logic.feature.message.ObserveMessageReactionsUseCase
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveReactionsForMessageUseCaseTest {

    @Test
    fun givenRepeatedEmoji_whenObservingReactions_thenGroupsMappedParticipantsByEmoji() = runTest {
        val reactions = listOf(reaction("😀", 0), reaction("❤️", 1), reaction("😀", 2))
        val (_, useCase) = Arrangement()
            .withReactions(reactions)
            .withParticipantMappings(reactions)
            .arrange()

        val result = useCase(CONVERSATION_ID, MESSAGE_ID).first()

        assertEquals(listOf(testUIParticipant(0), testUIParticipant(2)), result.reactions["😀"])
        assertEquals(listOf(testUIParticipant(1)), result.reactions["❤️"])
    }

    @Test
    fun givenDifferentReactionCounts_whenObservingReactions_thenOrdersEmojiByDescendingCount() = runTest {
        val reactions = listOf(
            reaction("😀", 0),
            reaction("❤️", 1),
            reaction("🔥", 2),
            reaction("❤️", 3),
            reaction("🔥", 4),
            reaction("❤️", 5),
        )
        val (_, useCase) = Arrangement()
            .withReactions(reactions)
            .withParticipantMappings(reactions)
            .arrange()

        val result = useCase(CONVERSATION_ID, MESSAGE_ID).first()

        assertEquals(listOf("❤️", "🔥", "😀"), result.reactions.keys.toList())
    }

    @Test
    fun givenMessageReactions_whenObservingReactions_thenMapsEveryReactionToItsParticipant() = runTest {
        val reactions = listOf(reaction("😀", 0), reaction("😀", 1), reaction("❤️", 2))
        val (arrangement, useCase) = Arrangement()
            .withReactions(reactions)
            .withParticipantMappings(reactions)
            .arrange()

        val result = useCase(CONVERSATION_ID, MESSAGE_ID).first()

        assertEquals(listOf(testUIParticipant(0), testUIParticipant(1)), result.reactions["😀"])
        assertEquals(listOf(testUIParticipant(2)), result.reactions["❤️"])
        reactions.forEach { reaction ->
            verify(exactly = 1) { arrangement.uiParticipantMapper.toUIParticipant(reaction) }
        }
    }

    private class Arrangement {

        @MockK
        lateinit var observeMessageReactions: ObserveMessageReactionsUseCase

        @MockK
        lateinit var uiParticipantMapper: UIParticipantMapper

        init {
            MockKAnnotations.init(this)
        }

        fun withReactions(reactions: List<MessageReaction>) = apply {
            every { observeMessageReactions(any(), any()) } returns flowOf(reactions)
        }

        fun withParticipantMappings(reactions: List<MessageReaction>) = apply {
            reactions.forEachIndexed { index, reaction ->
                every { uiParticipantMapper.toUIParticipant(reaction) } returns testUIParticipant(index)
            }
        }

        fun arrange() = this to ObserveReactionsForMessageUseCase(
            observeMessageReactions = observeMessageReactions,
            uiParticipantMapper = uiParticipantMapper,
            dispatchers = TestDispatcherProvider(),
        )
    }

    private companion object {
        val CONVERSATION_ID = ConversationId("conversation", "domain")
        const val MESSAGE_ID = "message"

        fun reaction(emoji: String, index: Int) = MessageReaction(
            emoji = emoji,
            isSelfUser = false,
            userSummary = UserSummary(
                userId = QualifiedID("user$index", "domain$index"),
                userName = "User $index",
                userHandle = "user$index",
                userPreviewAssetId = null,
                userType = UserTypeInfo.Regular(UserType.NONE),
                isUserDeleted = false,
                connectionStatus = ConnectionState.ACCEPTED,
                availabilityStatus = UserAvailabilityStatus.AVAILABLE,
                accentId = index,
            ),
        )
    }
}
