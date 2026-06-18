/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.privacy

import androidx.lifecycle.SavedStateHandle
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.feature.privacy.auth.ConversationAuthenticator
import com.wire.android.feature.privacy.data.ConversationPrivacyRepository
import com.wire.android.feature.privacy.model.ConversationPrivacyLevel
import com.wire.android.feature.privacy.model.ConversationPrivacySettings
import com.wire.kalium.logic.data.id.QualifiedID
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class ConversationPrivacyViewModelTest {

    @Test
    fun givenNonHighlySensitiveLevel_whenSelected_thenLevelIsAppliedDirectly() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()

        viewModel.onLevelSelected(ConversationPrivacyLevel.SENSITIVE)

        coVerify(exactly = 1) { arrangement.repository.setLevel(any(), ConversationPrivacyLevel.SENSITIVE) }
        assertFalse(viewModel.state.needsChatPinConfirmation)
        assertFalse(viewModel.state.needsChatPinSetup)
    }

    @Test
    fun givenChatPinSet_whenHighlySensitiveSelected_thenConfirmationIsRequestedWithoutApplying() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withChatPinSet(true)
            .arrange()

        viewModel.onLevelSelected(ConversationPrivacyLevel.HIGHLY_SENSITIVE)

        assertTrue(viewModel.state.needsChatPinConfirmation)
        assertFalse(viewModel.state.needsChatPinSetup)
        coVerify(exactly = 0) { arrangement.repository.setLevel(any(), ConversationPrivacyLevel.HIGHLY_SENSITIVE) }
    }

    @Test
    fun givenNoChatPin_whenHighlySensitiveSelected_thenChatPinSetupIsRequested() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withChatPinSet(false)
            .arrange()

        viewModel.onLevelSelected(ConversationPrivacyLevel.HIGHLY_SENSITIVE)

        assertTrue(viewModel.state.needsChatPinSetup)
        assertFalse(viewModel.state.needsChatPinConfirmation)
        coVerify(exactly = 0) { arrangement.repository.setLevel(any(), ConversationPrivacyLevel.HIGHLY_SENSITIVE) }
    }

    @Test
    fun givenConfirmationRequested_whenCorrectPinEntered_thenHighlySensitiveIsApplied() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withChatPinSet(true)
            .withPinVerification(correct = true)
            .arrange()
        viewModel.onLevelSelected(ConversationPrivacyLevel.HIGHLY_SENSITIVE)

        viewModel.onChatPinConfirmed("1234")

        coVerify(exactly = 1) { arrangement.repository.setLevel(any(), ConversationPrivacyLevel.HIGHLY_SENSITIVE) }
        assertFalse(viewModel.state.needsChatPinConfirmation)
        assertFalse(viewModel.state.pinError)
    }

    @Test
    fun givenConfirmationRequested_whenWrongPinEntered_thenErrorIsShownAndLevelNotApplied() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withChatPinSet(true)
            .withPinVerification(correct = false)
            .arrange()
        viewModel.onLevelSelected(ConversationPrivacyLevel.HIGHLY_SENSITIVE)

        viewModel.onChatPinConfirmed("0000")

        assertTrue(viewModel.state.pinError)
        assertTrue(viewModel.state.needsChatPinConfirmation)
        coVerify(exactly = 0) { arrangement.repository.setLevel(any(), ConversationPrivacyLevel.HIGHLY_SENSITIVE) }
    }

    @Test
    fun givenChatPinSetupRequested_whenPinCreated_thenPinStoredAndHighlySensitiveApplied() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withChatPinSet(false)
            .arrange()
        viewModel.onLevelSelected(ConversationPrivacyLevel.HIGHLY_SENSITIVE)

        viewModel.onChatPinCreated("1234")

        coVerify(exactly = 1) { arrangement.authenticator.setChatPin("1234") }
        coVerify(exactly = 1) { arrangement.repository.setLevel(any(), ConversationPrivacyLevel.HIGHLY_SENSITIVE) }
        assertFalse(viewModel.state.needsChatPinSetup)
    }

    @Test
    fun givenAlreadySelectedLevel_whenSelectedAgain_thenNothingHappens() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange()

        viewModel.onLevelSelected(ConversationPrivacyLevel.NORMAL)

        coVerify(exactly = 0) { arrangement.repository.setLevel(any(), any()) }
    }

    private class Arrangement {
        @MockK
        private lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var repository: ConversationPrivacyRepository

        @MockK
        lateinit var authenticator: ConversationAuthenticator

        private val conversationId = QualifiedID("conv-id", "domain")

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { savedStateHandle.navArgs<ConversationPrivacyNavArgs>() } returns
                ConversationPrivacyNavArgs(conversationId)
            every { repository.observe(any()) } returns flowOf(ConversationPrivacySettings())
            every { authenticator.isChatPinSet() } returns flowOf(false)
            coEvery { authenticator.isChatPinSetOnce() } returns false
        }

        fun withChatPinSet(isSet: Boolean) = apply {
            every { authenticator.isChatPinSet() } returns flowOf(isSet)
            coEvery { authenticator.isChatPinSetOnce() } returns isSet
        }

        fun withPinVerification(correct: Boolean) = apply {
            coEvery { authenticator.verifyChatPin(any()) } returns correct
        }

        fun arrange() = this to ConversationPrivacyViewModel(savedStateHandle, repository, authenticator)
    }
}