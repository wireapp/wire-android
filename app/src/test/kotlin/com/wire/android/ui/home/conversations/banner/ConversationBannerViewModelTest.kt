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

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.mockUri
import com.wire.android.framework.TestConversationDetails
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.banner.usecase.ObserveConversationMembersByTypesUseCase
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo
import com.wire.kalium.logic.feature.conversation.NotifyConversationIsOpenUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class ConversationBannerViewModelTest {

    @Test
    fun `given a group members, when at least one is not internal team member, then banner should not be null`() = runTest {
        // Given
        val qualifiedId = ConversationId("some-dummy-value", "some.dummy.domain")

        val (arrangement, viewModel) = Arrangement()
            .withConversationParticipantsUserTypesUpdate(listOf(UserTypeInfo.Regular(UserType.EXTERNAL)))
            .withGroupConversation()
            .arrange()
        // When
        arrangement.observeConversationMembersByTypesUseCase(qualifiedId).test {
            awaitItem()
            assertNotEquals(null, viewModel.bannerState)
            awaitComplete()
        }
    }

    @Test
    fun `given a one to one conversation, when other user is not internal member, then banner should be null`() = runTest {
        // Given
        val qualifiedId = ConversationId("some-dummy-value", "some.dummy.domain")

        val (arrangement, viewModel) = Arrangement()
            .withOneOnOneConversation()
            .arrange()
        // When
        arrangement.observeConversationMembersByTypesUseCase(qualifiedId).test {
            awaitComplete()
            assertEquals(null, viewModel.bannerState)
        }
    }

    @Test
    fun `given a group members, when all of them are internal team members, then banner should be null`() = runTest {
        // Given
        val qualifiedId = ConversationId("some-dummy-value", "some.dummy.domain")

        val (arrangement, viewModel) = Arrangement()
            .withConversationParticipantsUserTypesUpdate(
                listOf(
                    UserTypeInfo.Regular(UserType.INTERNAL),
                    UserTypeInfo.Regular(UserType.INTERNAL)
                )
            )
            .withGroupConversation()
            .arrange()
        // When
        arrangement.observeConversationMembersByTypesUseCase(qualifiedId).test {
            awaitItem()
            assertEquals(null, viewModel.bannerState)
            awaitComplete()
        }
    }

    @Test
    fun givenGroupConversation_whenInitialisingViewModel_thenShouldCallNotifyConversationIsOpen() = runTest {
        val (arrangement, _) = Arrangement()
            .withGroupConversation()
            .arrange()

        coVerify(exactly = 1) {
            arrangement.notifyConversationIsOpenUseCase(arrangement.conversationId)
        }
    }

    @Test
    fun givenOneOnOneConversation_whenInitialisingViewModel_thenShouldCallNotifyConversationIsOpen() = runTest {
        val (arrangement, _) = Arrangement()
            .withOneOnOneConversation()
            .arrange()

        coVerify(exactly = 1) {
            arrangement.notifyConversationIsOpenUseCase(arrangement.conversationId)
        }
    }
}

private class Arrangement {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var observeConversationMembersByTypesUseCase: ObserveConversationMembersByTypesUseCase

    @MockK
    lateinit var observeConversationDetailsUseCase: ObserveConversationDetailsUseCase

    @MockK
    lateinit var notifyConversationIsOpenUseCase: NotifyConversationIsOpenUseCase

    private val viewModel by lazy {
        ConversationBannerViewModel(
            savedStateHandle,
            observeConversationMembersByTypesUseCase,
            observeConversationDetailsUseCase,
            notifyConversationIsOpenUseCase
        )
    }
    val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")

    init {
        // Tests setup
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockUri()
        every { savedStateHandle.navArgs<ConversationNavArgs>() } returns ConversationNavArgs(conversationId = conversationId)
        // Default empty values
        coEvery { observeConversationMembersByTypesUseCase(any()) } returns flowOf()
        coEvery { notifyConversationIsOpenUseCase(any()) } returns Unit
    }

    suspend fun withConversationParticipantsUserTypesUpdate(participants: List<UserTypeInfo>) = apply {
        coEvery { observeConversationMembersByTypesUseCase(any()) } returns flowOf(participants.toSet())
    }

    suspend fun withGroupConversation() = apply {
        coEvery { observeConversationDetailsUseCase(any()) }
            .returns(flowOf(ObserveConversationDetailsUseCase.Result.Success(TestConversationDetails.GROUP)))
    }

    suspend fun withOneOnOneConversation() = apply {
        coEvery { observeConversationDetailsUseCase(any()) }
            .returns(flowOf(ObserveConversationDetailsUseCase.Result.Success(TestConversationDetails.CONVERSATION_ONE_ONE)))
    }

    fun arrange() = this to viewModel
}
