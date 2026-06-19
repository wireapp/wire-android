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
package com.wire.android.ui.home.threads

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestMessage
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.UiTextResolver
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.MessagePreviewContent
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.feature.message.GlobalThreadSummary
import com.wire.kalium.logic.feature.message.ObserveGlobalThreadsResult
import com.wire.kalium.logic.feature.message.ObserveGlobalThreadsUseCase
import com.wire.kalium.logic.feature.message.SetThreadFollowStateResult
import com.wire.kalium.logic.feature.message.SetThreadFollowStateUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class GlobalThreadsViewModelTest {

    @Test
    fun givenThread_whenUnfollowThread_thenSetThreadFollowStateToFalse() = runTest {
        val threadSummary = globalThreadSummary()
        val (arrangement, viewModel) = Arrangement()
            .withThreads(listOf(threadSummary))
            .arrange()

        advanceUntilIdle()
        viewModel.unfollowThread(viewModel.state.threads.single())
        advanceUntilIdle()

        coVerify(exactly = 1) {
            arrangement.setThreadFollowState(threadSummary.conversationId, threadSummary.threadId, false)
        }
    }

    private class Arrangement {

        @MockK
        lateinit var observeGlobalThreads: ObserveGlobalThreadsUseCase

        @MockK
        lateinit var setThreadFollowState: SetThreadFollowStateUseCase

        private val threadsFlow = MutableStateFlow<ObserveGlobalThreadsResult>(ObserveGlobalThreadsResult.Success(emptyList()))

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { observeGlobalThreads() } returns threadsFlow
            coEvery { setThreadFollowState(any(), any(), any()) } returns SetThreadFollowStateResult.Success
        }

        fun withThreads(threads: List<GlobalThreadSummary>) = apply {
            threadsFlow.value = ObserveGlobalThreadsResult.Success(threads)
        }

        fun arrange() = this to GlobalThreadsViewModel(
            observeGlobalThreads = observeGlobalThreads,
            setThreadFollowState = setThreadFollowState,
            uiTextResolver = testUiTextResolver,
        )
    }
}

private fun globalThreadSummary(
    conversationId: ConversationId = ConversationId("conversation-id", "domain"),
    threadId: String = "thread-id",
) = GlobalThreadSummary(
    conversationId = conversationId,
    conversationName = "Conversation",
    conversationType = Conversation.Type.Group.Regular,
    otherUserPreviewAssetId = null,
    otherUserAvailabilityStatus = UserAvailabilityStatus.NONE,
    otherUserConnectionStatus = null,
    otherUserId = null,
    otherUserAccentId = null,
    otherUserDeleted = false,
    rootMessageId = "root-message-id",
    threadId = threadId,
    visibleReplyCount = 1L,
    createdAt = Instant.parse("2026-01-01T12:00:00.000Z"),
    lastReplyDate = null,
    rootMessage = TestMessage.PREVIEW.copy(
        conversationId = conversationId,
        content = MessagePreviewContent.WithUser.Text("Sender", "Root message"),
    ),
    rootMessageSelfDeletionDurationMillis = null,
)

private val testUiTextResolver = object : UiTextResolver {
    override fun resolve(text: UIText): String = when (text) {
        is UIText.DynamicString -> text.value
        is UIText.StringResource -> "res_${text.resId}"
        is UIText.PluralResource -> "plural_${text.resId}_${text.count}"
    }

    override fun localeTag(): String = "test-locale"
}
