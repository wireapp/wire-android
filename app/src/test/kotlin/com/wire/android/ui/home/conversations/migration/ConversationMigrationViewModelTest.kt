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

import com.wire.android.assertions.shouldBeEqualTo
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestConversation
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class ConversationMigrationViewModelTest {

    @Test
    fun givenActiveOneOnOneMatchesCurrentConversation_thenMigratedConversationShouldBeNull() = runTest {
        val (_, conversationMigrationViewModel) = arrange {
            withConversationDetailsReturning(
                ConversationDetails.OneOne(
                    conversation = TestConversation.ONE_ON_ONE,
                    otherUser = TestUser.OTHER_USER.copy(activeOneOnOneConversationId = conversationId),
                    userType = UserTypeInfo.Regular(UserType.NONE),
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
                    userType = UserTypeInfo.Regular(UserType.NONE),
                )
            )
        }

        conversationMigrationViewModel.migratedConversationId shouldBeEqualTo expectedActiveOneOnOneId
    }

    private class Arrangement(private val configure: Arrangement.() -> Unit) {

        @MockK
        lateinit var observeConversationDetailsUseCase: ObserveConversationDetailsUseCase

        init {
            MockKAnnotations.init(this)
        }

        fun withConversationDetailsReturning(conversationDetails: ConversationDetails) = apply {
            coEvery { observeConversationDetailsUseCase(conversationId) } returns
                    flowOf(ObserveConversationDetailsUseCase.Result.Success(conversationDetails))
        }

        fun arrange(): Pair<Arrangement, ConversationMigrationViewModel> = run {
            configure()
            this@Arrangement to ConversationMigrationViewModel(ConversationNavArgs(conversationId), observeConversationDetailsUseCase)
        }
    }

    private companion object {
        val conversationId = TestConversation.ID

        fun arrange(configure: Arrangement.() -> Unit) = Arrangement(configure).arrange()
    }
}
