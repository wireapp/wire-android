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
package com.wire.android.ui.home.conversations.migration

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.framework.TestConversation
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import com.wire.android.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class ConversationMigrationViewModelTest {

    @Test
    fun givenActiveOneOnOneMatchesCurrentConversation_thenMigratedConversationShouldBeNull() = runTest {
        val (_, conversationMigrationViewModel) = arrange {
            withConversationDetailsReturning(
                ConversationDetails.OneOne(
                    conversation = TestConversation.ONE_ON_ONE,
                    otherUser = TestUser.OTHER_USER.copy(activeOneOnOneConversationId = conversationId),
                    userType = UserType.NONE,
                )
            )
        }

        conversationMigrationViewModel.migratedConversationId shouldBeEqualTo null
    }

    @Test
    fun givenActiveOneOnOneDiffersFromCurrentConversation_thenMigratedConversationShouldBeTheOneInDetails() = runTest {
        val expectedActiveOneOnOneId = ConversationId("expectedActiveOneOnOneId", "testDomain")
        val (_, conversationMigrationViewModel) = arrange {
            withConversationDetailsReturning(
                ConversationDetails.OneOne(
                    conversation = TestConversation.ONE_ON_ONE,
                    otherUser = TestUser.OTHER_USER.copy(activeOneOnOneConversationId = expectedActiveOneOnOneId),
                    userType = UserType.NONE,
                )
            )
        }

        conversationMigrationViewModel.migratedConversationId shouldBeEqualTo expectedActiveOneOnOneId
    }

    private class Arrangement(private val configure: Arrangement.() -> Unit) {

        @MockK
        lateinit var observeConversationDetailsUseCase: ObserveConversationDetailsUseCase

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        init {
            MockKAnnotations.init(this)
            every { savedStateHandle.navArgs<ConversationNavArgs>() } returns ConversationNavArgs(conversationId)
        }

        fun withConversationDetailsReturning(conversationDetails: ConversationDetails) = apply {
            coEvery { observeConversationDetailsUseCase(conversationId) } returns
                    flowOf(ObserveConversationDetailsUseCase.Result.Success(conversationDetails))
        }

        fun arrange(): Pair<Arrangement, ConversationMigrationViewModel> = run {
            configure()
            this@Arrangement to ConversationMigrationViewModel(savedStateHandle, observeConversationDetailsUseCase)
        }
    }

    private companion object {
        val conversationId = TestConversation.ID

        fun arrange(configure: Arrangement.() -> Unit) = Arrangement(configure).arrange()
    }
}
