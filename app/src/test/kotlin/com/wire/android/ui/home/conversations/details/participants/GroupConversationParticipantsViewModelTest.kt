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
 *
 *
 */

package com.wire.android.ui.home.conversations.details.participants

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.mockUri
import com.wire.android.mapper.testUIParticipant
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsViewModel
import com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.publicuser.RefreshUsersWithoutMetadataUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class GroupConversationParticipantsViewModelTest {

    private fun testSize(givenParticipantsSize: Int, expectedParticipantsSize: Int) = runTest {
        // Given
        val members = buildList {
            for (i in 1..givenParticipantsSize) {
                add(testUIParticipant(i))
            }
        }
        val (_, viewModel) = GroupConversationParticipantsViewModelArrangement()
            .withConversationParticipantsUpdate(members)
            .arrange()
        // When - Then
        assert(viewModel.groupParticipantsState.data.participants.size == expectedParticipantsSize)
        assert(viewModel.groupParticipantsState.data.allParticipantsCount == members.size)
    }

    @Test
    fun `given a group members, when solving the participants list, then right sizes are passed`() {
        val maxNumber = GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS
        testSize(givenParticipantsSize = maxNumber + 1, expectedParticipantsSize = maxNumber)
        testSize(givenParticipantsSize = maxNumber - 1, expectedParticipantsSize = maxNumber - 1)
    }

    @Test
    fun `given a group members, when clicking on other profile, then navigate to other profile screen`() = runTest {
        // Given
        val member = testUIParticipant(0).copy(isSelf = false)
        val (arrangement, viewModel) = GroupConversationParticipantsViewModelArrangement().arrange()
        // When
        viewModel.openProfile(member)
        // Then
        coVerify {
            arrangement.navigationManager.navigate(
                NavigationCommand(
                    NavigationItem.OtherUserProfile.getRouteWithArgs(
                        listOf(member.id, arrangement.qualifiedId)
                    )
                )
            )
        }
    }

    @Test
    fun `given a group members, when clicking on self profile, then navigate to self profile screen`() = runTest {
        // Given
        val member = testUIParticipant(0).copy(isSelf = true)
        val (arrangement, viewModel) = GroupConversationParticipantsViewModelArrangement().arrange()
        // When
        viewModel.openProfile(member)
        // Then
        coVerify {
            arrangement.navigationManager.navigate(NavigationCommand(NavigationItem.SelfUserProfile.getRouteWithArgs()))
        }
    }
}

internal class GroupConversationParticipantsViewModelArrangement {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var navigationManager: NavigationManager

    @MockK
    private lateinit var qualifiedIdMapper: QualifiedIdMapper

    @MockK
    private lateinit var refreshUsersWithoutMetadata: RefreshUsersWithoutMetadataUseCase

    @MockK
    lateinit var observeParticipantsForConversationUseCase: ObserveParticipantsForConversationUseCase
    private val conversationMembersChannel = Channel<ConversationParticipantsData>(capacity = Channel.UNLIMITED)
    private val viewModel by lazy {
        GroupConversationParticipantsViewModel(
            savedStateHandle,
            navigationManager,
            observeParticipantsForConversationUseCase,
            refreshUsersWithoutMetadata,
            qualifiedIdMapper
        )
    }
    val conversationId = "some-dummy-value@some.dummy.domain"
    val qualifiedId = ConversationId("some-dummy-value", "some.dummy.domain")

    init {
        // Tests setup
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockUri()
        every {
            qualifiedIdMapper.fromStringToQualifiedID("some-dummy-value@some.dummy.domain")
        } returns QualifiedID("some-dummy-value", "some.dummy.domain")
        every { savedStateHandle.get<String>(EXTRA_CONVERSATION_ID) } returns conversationId
        // Default empty values
        coEvery { observeParticipantsForConversationUseCase(any(), any()) } returns flowOf()
    }

    suspend fun withConversationParticipantsUpdate(participants: List<UIParticipant>): GroupConversationParticipantsViewModelArrangement {
        coEvery { observeParticipantsForConversationUseCase(any(), any()) } returns conversationMembersChannel.consumeAsFlow()
        conversationMembersChannel.send(
            ConversationParticipantsData(
                participants = participants.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
                allParticipantsCount = participants.size
            )
        )
        return this
    }

    fun arrange() = this to viewModel
}
