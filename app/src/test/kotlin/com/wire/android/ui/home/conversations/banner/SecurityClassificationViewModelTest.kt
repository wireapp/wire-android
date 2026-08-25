/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.banner

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.ui.common.banner.SecurityClassificationArgs
import com.wire.android.ui.common.banner.SecurityClassificationViewModelImpl
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class SecurityClassificationViewModelTest {

    @Test
    fun `given conversationId, when observing conversation classification, then should update state`() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement(
            conversationId = CONVERSATION_ID,
            userId = null
        )
            .arrange()

        // When
        arrangement.classificationChannel.send(SecurityClassificationType.CLASSIFIED)
        advanceUntilIdle()

        // Then
        assertEquals(SecurityClassificationType.CLASSIFIED, viewModel.state())
    }

    @Test
    fun `given userId, when fetching user classification, then should update state`() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement(
            conversationId = null,
            userId = USER_ID
        )
            .arrange()

        // When
        arrangement.classificationUserChannel.send(SecurityClassificationType.CLASSIFIED)
        advanceUntilIdle()

        // Then
        assertEquals(SecurityClassificationType.CLASSIFIED, viewModel.state())
    }

    @Test
    fun `given session-owned entry, when observing classification, then injected account selects graph regardless of global session`() = runTest {
        val (arrangement, viewModel) = Arrangement(
            conversationId = CONVERSATION_ID,
            userId = null
        )
            .arrange()

        arrangement.classificationChannel.send(SecurityClassificationType.CLASSIFIED)
        advanceUntilIdle()

        assertEquals(SecurityClassificationType.CLASSIFIED, viewModel.state())
        coVerify(exactly = 1) {
            arrangement.coreLogic.getSessionScope(USER_ID)
        }
    }

    private companion object {
        val CONVERSATION_ID = ConversationId("some-dummy-value", "some.dummy.domain")
        val USER_ID = UserId("user_value", "user.domain")
    }

    private class Arrangement(
        conversationId: ConversationId?,
        userId: UserId?
    ) {

        @MockK
        lateinit var coreLogic: CoreLogic

        val classificationChannel = Channel<SecurityClassificationType>(capacity = Channel.UNLIMITED)
        val classificationUserChannel = Channel<SecurityClassificationType>(capacity = Channel.UNLIMITED)
        private val navArg: SecurityClassificationArgs = when {
            conversationId != null -> SecurityClassificationArgs.Conversation(conversationId)
            userId != null -> SecurityClassificationArgs.User(userId)
            else -> throw IllegalArgumentException("Either conversationId or userId must be provided")
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery {
                coreLogic.getSessionScope(any()).observeSecurityClassificationLabel(any())
            } returns classificationChannel.consumeAsFlow()
            coEvery { coreLogic.getSessionScope(any()).getOtherUserSecurityClassificationLabel(any()) } returns
                classificationUserChannel.consumeAsFlow()
        }

        fun arrange() = this to SecurityClassificationViewModelImpl(
            coreLogic = coreLogic,
            currentAccount = USER_ID,
            args = navArg,
        )
    }
}
