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
package com.wire.android.ui.home.conversations.composer.actions

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.ScopedArgsTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.di.scopedArgs
import com.wire.android.ui.home.messagecomposer.actions.SelfDeletingMessageActionArgs
import com.wire.android.ui.home.messagecomposer.actions.SelfDeletingMessageActionViewModelImpl
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ExtendWith(ScopedArgsTestExtension::class)
class SelfDeletingMessageActionViewModelTest {

    @Test
    fun `given self-deleting message timer, when observing, then the timer gets successfully updated`() =
        runTest {
            // Given
            val expectedDuration = 1.toDuration(DurationUnit.HOURS)
            val expectedTimer = SelfDeletionTimer.Enabled(expectedDuration)
            val (arrangement, viewModel) = Arrangement()
                .withObserveSelfDeletingStatus(expectedTimer)
                .arrange()

            // When

            // Then
            Assertions.assertInstanceOf(SelfDeletionTimer.Enabled::class.java, viewModel.state)
            assertEquals(expectedDuration, viewModel.state.duration)
        }

    @Test
    fun `given a valid observed enforced self-deleting message timer, when observing, then the timer gets successfully updated`() =
        runTest {
            // Given
            val expectedDuration = 1.toDuration(DurationUnit.DAYS)
            val expectedTimer = SelfDeletionTimer.Enforced.ByTeam(expectedDuration)
            val (arrangement, viewModel) = Arrangement()
                .withObserveSelfDeletingStatus(expectedTimer)
                .arrange()

            // When

            // Then
            coVerify(exactly = 1) {
                arrangement.observeConversationSelfDeletionStatus.invoke(
                    arrangement.conversationId,
                    true
                )
            }
            Assertions.assertInstanceOf(SelfDeletionTimer.Enforced.ByTeam::class.java, viewModel.state)
            assertEquals(expectedDuration, viewModel.state.duration)
        }

    private class Arrangement {

        val conversationId: ConversationId = ConversationId("some-dummy-value", "some.dummy.domain")

        init {
            // Tests setup
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockUri()
            every { savedStateHandle.scopedArgs<SelfDeletingMessageActionArgs>() } returns SelfDeletingMessageActionArgs(
                conversationId = conversationId
            )
        }

        @MockK
        private lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var observeConversationSelfDeletionStatus: ObserveSelfDeletionTimerSettingsForConversationUseCase

        private val viewModel by lazy {
            SelfDeletingMessageActionViewModelImpl(
                savedStateHandle = savedStateHandle,
                dispatchers = TestDispatcherProvider(),
                observeSelfDeletingMessages = observeConversationSelfDeletionStatus,
            )
        }

        fun withObserveSelfDeletingStatus(expectedSelfDeletionTimer: SelfDeletionTimer) = apply {
            coEvery { observeConversationSelfDeletionStatus(conversationId, true) } returns flowOf(expectedSelfDeletionTimer)
        }

        fun arrange() = this to viewModel
    }
}
