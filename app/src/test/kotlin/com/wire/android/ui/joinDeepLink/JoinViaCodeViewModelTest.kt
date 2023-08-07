/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.joinDeepLink

import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.ui.joinConversation.JoinConversationViaCodeViewModel
import com.wire.android.ui.joinConversation.JoinViaDeepLinkDialogState
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.conversation.JoinConversationViaCodeUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class JoinViaCodeViewModelTest {

    @Test
    fun `given valid code, when joining conversion success, then stat is updated`() = runTest {
        val (code, key, domain) = Triple("code", "key", "domain")
        val conversationId = ConversationId("id", "domain")
        val (arrangement, viewModel) = Arrangement()
            .withJoinConversationCode(
                JoinConversationViaCodeUseCase.Result.Success.Changed(conversationId)
            )
            .arrange()

        viewModel.joinConversationViaCode(code, key, domain)
        advanceUntilIdle()
        coVerify(exactly = 1) {
            arrangement.joinViaCode(
                code = code,
                key = key,
                domain = domain,
                password = null
            )
        }
        viewModel.state `should be equal to` JoinViaDeepLinkDialogState.Success(conversationId)
    }

    @Test
    fun `given valid code, when joining conversion and user us already a member, then update state`() = runTest {
        val (code, key, domain) = Triple("code", "key", "domain")
        val conversationId = ConversationId("id", "domain")
        val (_, viewModel) = Arrangement()
            .withJoinConversationCode(
                JoinConversationViaCodeUseCase.Result.Success.Unchanged(conversationId)
            )
            .arrange()

        viewModel.joinConversationViaCode(code, key, domain)
        advanceUntilIdle()

        viewModel.state `should be equal to` JoinViaDeepLinkDialogState.Success(conversationId)
    }

    @Test
    fun `given invalid code, when try to join conversation, then state is updated`() = runTest {
        val (code, key, domain) = Triple("code", "key", "domain")
        val (arrangement, viewModel) = Arrangement()
            .withJoinConversationCodeError(
                JoinConversationViaCodeUseCase.Result.Failure.Generic(NetworkFailure.NoNetworkConnection(RuntimeException("Error")))
            )
            .arrange()

        assertEquals(
            JoinViaDeepLinkDialogState.Idle,
            viewModel.state
        )
        viewModel.joinConversationViaCode(code, key, domain)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            arrangement.joinViaCode(
                code = code,
                key = key,
                domain = domain,
                password = null
            )
        }

        viewModel.state `should be equal to` JoinViaDeepLinkDialogState.UnknownError
    }

    @Test
    fun `given password, when try to join conversation, then password Is passed to the use case`() = runTest {
        val (code, key, domain) = Triple("code", "key", "domain")
        val (arrangement, viewModel) = Arrangement()
            .withJoinConversationCodeError(
                JoinConversationViaCodeUseCase.Result.Failure.Generic(NetworkFailure.NoNetworkConnection(RuntimeException("Error")))
            )
            .withPassword("password")
            .arrange()

        viewModel.joinConversationViaCode(code, key, domain)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            arrangement.joinViaCode(
                code = code,
                key = key,
                domain = domain,
                password = "password"
            )
        }
    }

    @Test
    fun `given password field is empty, when try to join conversation, then password Is not passed to the use case`() = runTest {
        val (code, key, domain) = Triple("code", "key", "domain")
        val (arrangement, viewModel) = Arrangement()
            .withJoinConversationCodeError(
                JoinConversationViaCodeUseCase.Result.Failure.Generic(NetworkFailure.NoNetworkConnection(RuntimeException("Error")))
            )
            .withPassword("")
            .arrange()

        viewModel.joinConversationViaCode(code, key, domain)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            arrangement.joinViaCode(
                code = code,
                key = key,
                domain = domain,
                password = null
            )
        }
    }

    @Test
    fun `given wrong password error state, when updating password, then error is cleared`() = runTest {
        val (_, viewModel) = Arrangement()
            .withPassword("password")
            .arrange()

        viewModel.state = JoinViaDeepLinkDialogState.WrongPassword

        viewModel.onPasswordUpdated(TextFieldValue("password"))

        viewModel.state `should be equal to` JoinViaDeepLinkDialogState.Idle
    }

    private class Arrangement {
        @MockK
        lateinit var joinViaCode: JoinConversationViaCodeUseCase

        init {
            MockKAnnotations.init(this)
        }

        fun withJoinConversationCode(
            result: JoinConversationViaCodeUseCase.Result
        ): Arrangement = apply {
            coEvery { joinViaCode(any(), any(), any(), any()) } returns result
        }

        fun withJoinConversationCodeError(
            result: JoinConversationViaCodeUseCase.Result.Failure
        ): Arrangement = apply {
            coEvery {
                joinViaCode(any(), any(), any(), any())
            } returns result
        }

        fun withPassword(password: String): Arrangement = apply {
            viewModel.onPasswordUpdated(TextFieldValue(password))
        }

        private val viewModel: JoinConversationViaCodeViewModel = JoinConversationViaCodeViewModel(joinViaCode)

        fun arrange() = Pair(this, viewModel)
    }
}
