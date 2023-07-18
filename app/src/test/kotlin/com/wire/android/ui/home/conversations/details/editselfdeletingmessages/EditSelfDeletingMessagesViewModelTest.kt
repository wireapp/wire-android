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
package com.wire.android.ui.home.conversations.details.editselfdeletingmessages

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestConversation
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.home.messagecomposer.SelfDeletionDuration
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.conversation.messagetimer.UpdateMessageTimerUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class EditSelfDeletingMessagesViewModelTest {

    @Test
    fun `given self deleting messages option enabled, when disabling it, then it updates proper state`() =
        runTest {
            // Given
            val (_, viewModel) = Arrangement()
                .withSelfDeletingMessagesGroupSettings(SelfDeletionTimer.Enabled(5.minutes))
                .withUpdateMessageTimerSuccess()
                .arrange()

            // When
            viewModel.updateSelfDeletingMessageOption(false)

            // Then
            assertEquals(false, viewModel.state.isEnabled)
            assertEquals(null, viewModel.state.locallySelected)
        }

    @Test
    fun `given self deleting messages option disabled, when enabling it, then it updates proper state`() =
        runTest {
            // Given
            val newTimer = SelfDeletionDuration.FiveMinutes
            val (_, viewModel) = Arrangement()
                .withSelfDeletingMessagesGroupSettings(SelfDeletionTimer.Enabled(Duration.ZERO))
                .withUpdateMessageTimerSuccess()
                .arrange()

            // When
            viewModel.updateSelfDeletingMessageOption(true)
            viewModel.onSelectDuration(newTimer)
            viewModel.applyNewDuration()

            // Then
            assertEquals(newTimer, viewModel.state.remotelySelected)
            assertEquals(newTimer, viewModel.state.locallySelected)
        }

    private class Arrangement {

        @MockK
        private lateinit var savedStateHandle: SavedStateHandle

        @MockK
        private lateinit var navigationManager: NavigationManager

        @MockK
        private lateinit var observerConversationMembers: ObserveParticipantsForConversationUseCase

        @MockK
        private lateinit var observeSelfDeletionTimerSettingsForConversation: ObserveSelfDeletionTimerSettingsForConversationUseCase

        @MockK
        private lateinit var qualifiedIdMapper: QualifiedIdMapper

        @MockK
        private lateinit var updateMessageTimer: UpdateMessageTimerUseCase

        private val viewModel by lazy {
            EditSelfDeletingMessagesViewModel(
                navigationManager = navigationManager,
                savedStateHandle = savedStateHandle,
                dispatcher = TestDispatcherProvider(),
                observeConversationMembers = observerConversationMembers,
                observeSelfDeletionTimerSettingsForConversation = observeSelfDeletionTimerSettingsForConversation,
                qualifiedIdMapper = qualifiedIdMapper,
                updateMessageTimer = updateMessageTimer
            )
        }

        init {
            val dummyConversationId = TestConversation.ID.toString()
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { savedStateHandle.get<String>(EXTRA_CONVERSATION_ID) } returns dummyConversationId
            coEvery { observerConversationMembers(any()) } returns flowOf(ConversationParticipantsData(isSelfAnAdmin = true))
            every { qualifiedIdMapper.fromStringToQualifiedID(any()) } returns TestConversation.ID
        }

        fun withSelfDeletingMessagesGroupSettings(settings: SelfDeletionTimer) = apply {
            coEvery { observeSelfDeletionTimerSettingsForConversation(any(), false) } returns flowOf(settings)
        }

        fun withUpdateMessageTimerSuccess() = apply {
            coEvery { updateMessageTimer(any(), any()) } returns UpdateMessageTimerUseCase.Result.Success
        }

        fun arrange() = this to viewModel
    }
}
