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

package com.wire.android.ui.home.conversations.info

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.composer.mockConversationDetailsGroup
import com.wire.android.ui.home.conversations.composer.withMockConversationDetailsOneOnOne
import com.wire.android.util.EMPTY
import com.wire.android.util.ui.UIText
import com.wire.kalium.common.error.StorageFailure
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class ConversationInfoViewModelTest {

    @Test
    fun `given a self mentioned user, when getting user data, then return valid result`() = runTest {
        // Given
        val groupConversationDetails = mockConversationDetailsGroup("Conversation Name Goes Here")
        val (_, viewModel) = ConversationInfoViewModelArrangement()
            .withConversationDetailUpdate(conversationDetails = groupConversationDetails)
            .withMentionedUserId(TestUser.SELF_USER.id)
            .arrange()
        // When
        val result = viewModel.mentionedUserData(TestUser.SELF_USER.id.toString())
        // Then
        assertEquals(Pair(TestUser.SELF_USER.id, true), result)
    }

    @Test
    fun `given an other mentioned user, when getting user data, then return valid result`() = runTest {
        // Given
        val groupConversationDetails = mockConversationDetailsGroup("Conversation Name Goes Here")
        val (_, viewModel) = ConversationInfoViewModelArrangement()
            .withConversationDetailUpdate(conversationDetails = groupConversationDetails)
            .withMentionedUserId(TestUser.OTHER_USER.id)
            .arrange()
        // When
        val result = viewModel.mentionedUserData(TestUser.OTHER_USER.id.toString())
        // Then
        assertEquals(Pair(TestUser.OTHER_USER.id, false), result)
    }

    @Test
    fun `given a 1 on 1 conversation, when solving the conversation name, then the name of the other user is used`() = runTest {
        // Given
        val oneToOneConversationDetails = withMockConversationDetailsOneOnOne("Other User Name Goes Here")
        val (_, viewModel) = ConversationInfoViewModelArrangement()
            .withConversationDetailUpdate(
                conversationDetails = oneToOneConversationDetails
            )
            .arrange()
        launch { viewModel.observeConversationDetails() }.run {
            advanceUntilIdle()
            // When - Then
            assert(viewModel.conversationInfoViewState.conversationName is UIText.DynamicString)
            assertEquals(
                oneToOneConversationDetails.otherUser.name,
                (viewModel.conversationInfoViewState.conversationName as UIText.DynamicString).value
            )
            cancel()
        }
    }

    @Test
    fun `given a 1 on 1 conversation, when solving the conversation name, then unavailable user is used`() = runTest {
        // Given
        val oneToOneConversationDetails = withMockConversationDetailsOneOnOne(senderName = "", unavailable = true)
        val (_, viewModel) = ConversationInfoViewModelArrangement()
            .withConversationDetailUpdate(
                conversationDetails = oneToOneConversationDetails
            )
            .arrange()
        launch { viewModel.observeConversationDetails() }.run {
            advanceUntilIdle()
            // When - Then
            assert(viewModel.conversationInfoViewState.conversationName is UIText.StringResource)
            cancel()
        }
    }

    @Test
    fun `given a group conversation, when solving the conversation name, then the name of the conversation is used`() = runTest {
        // Given
        val groupConversationDetails = mockConversationDetailsGroup("Conversation Name Goes Here")
        val (_, viewModel) = ConversationInfoViewModelArrangement()
            .withConversationDetailUpdate(conversationDetails = groupConversationDetails)
            .arrange()
        launch { viewModel.observeConversationDetails() }.run {
            advanceUntilIdle()
            // When - Then
            assert(viewModel.conversationInfoViewState.conversationName is UIText.DynamicString)
            assertEquals(
                groupConversationDetails.conversation.name,
                (viewModel.conversationInfoViewState.conversationName as UIText.DynamicString).value
            )
            cancel()
        }
    }

    @Test
    fun `given the conversation name is updated, when solving the conversation name, then the state is updated accordingly`() = runTest {
        // Given
        val firstConversationDetails = mockConversationDetailsGroup("Conversation Name Goes Here")
        val secondConversationDetails = mockConversationDetailsGroup("Conversation Name Was Updated")
        val (arrangement, viewModel) = ConversationInfoViewModelArrangement()
            .withConversationDetailUpdate(
                conversationDetails = firstConversationDetails
            )
            .arrange()
        launch { viewModel.observeConversationDetails() }.run {
            advanceUntilIdle()
            // When - Then
            assert(viewModel.conversationInfoViewState.conversationName is UIText.DynamicString)
            assertEquals(
                firstConversationDetails.conversation.name,
                (viewModel.conversationInfoViewState.conversationName as UIText.DynamicString).value
            )

            // When - Then
            arrangement.withConversationDetailUpdate(conversationDetails = secondConversationDetails)
            advanceUntilIdle()

            assert(viewModel.conversationInfoViewState.conversationName is UIText.DynamicString)
            assertEquals(
                secondConversationDetails.conversation.name,
                (viewModel.conversationInfoViewState.conversationName as UIText.DynamicString).value
            )
            cancel()
        }
    }

    @Test
    fun `given the initial state, when solving the conversation name before the data is received, the name should be an empty string`() =
        runTest {
            // Given
            val (_, viewModel) = ConversationInfoViewModelArrangement()
                .arrange()

            // When - Then
            assert(viewModel.conversationInfoViewState.conversationName is UIText.DynamicString)
            assertEquals(String.EMPTY, (viewModel.conversationInfoViewState.conversationName as UIText.DynamicString).value)
        }

    @Test
    fun `given a 1 on 1 conversation, when the user is deleted, then the name of the conversation should be a string resource`() = runTest {
        // Given
        val oneToOneConversationDetails = withMockConversationDetailsOneOnOne("")
        val (_, viewModel) = ConversationInfoViewModelArrangement()
            .withConversationDetailUpdate(
                conversationDetails = oneToOneConversationDetails
            )
            .arrange()
        launch { viewModel.observeConversationDetails() }.run {
            advanceUntilIdle()
            // When - Then
            assert(viewModel.conversationInfoViewState.conversationName is UIText.StringResource)
            cancel()
        }
    }

    @Test
    fun `given a 1 on 1 conversation, when solving the conversation avatar, then the avatar of the other user is used`() = runTest {
        // Given
        val conversationDetails = withMockConversationDetailsOneOnOne("", ConversationId("userAssetId", "domain"))
        val otherUserAvatar = conversationDetails.otherUser.previewPicture
        val (_, viewModel) = ConversationInfoViewModelArrangement()
            .withConversationDetailUpdate(conversationDetails = conversationDetails)
            .arrange()
        launch { viewModel.observeConversationDetails() }.run {
            advanceUntilIdle()
            val actualAvatar = viewModel.conversationInfoViewState.conversationAvatar
            // When - Then
            assert(actualAvatar is ConversationAvatar.OneOne)
            assertEquals(otherUserAvatar, (actualAvatar as ConversationAvatar.OneOne).avatarAsset?.userAssetId)
            cancel()
        }
    }

    @Test
    fun `given a not-verified MLS conversation, then mlsVerificationStatus is not verified`() = runTest {
        // Given
        val groupConversationDetails = mockConversationDetailsGroup("Conversation Name Goes Here")
        val (_, viewModel) = ConversationInfoViewModelArrangement()
            .withConversationDetailUpdate(conversationDetails = groupConversationDetails)
            .arrange()

        // then
        launch { viewModel.observeConversationDetails() }.run {
            advanceUntilIdle()
            assertEquals(
                groupConversationDetails.conversation.protocol,
                viewModel.conversationInfoViewState.protocolInfo
            )
            assertEquals(
                groupConversationDetails.conversation.mlsVerificationStatus,
                viewModel.conversationInfoViewState.mlsVerificationStatus
            )
            cancel()
        }
    }

    @Test
    fun `given conversation is MLS verified, then mlsVerificationStatus is verified`() = runTest {
        // Given
        val groupConversationDetails = mockConversationDetailsGroup("Conversation Name Goes Here")
        val (_, viewModel) = ConversationInfoViewModelArrangement()
            .withConversationDetailUpdate(conversationDetails = groupConversationDetails)
            .arrange()

        // then
        launch { viewModel.observeConversationDetails() }.run {
            advanceUntilIdle()
            assertEquals(
                groupConversationDetails.conversation.protocol,
                viewModel.conversationInfoViewState.protocolInfo
            )
            assertEquals(
                groupConversationDetails.conversation.mlsVerificationStatus,
                viewModel.conversationInfoViewState.mlsVerificationStatus
            )
            cancel()
        }
    }

    @Test
    fun `given a verified Proteus conversation, then proteusVerificationStatus is verified`() = runTest {
        // Given
        val groupConversationDetails = mockConversationDetailsGroup("Conversation Name Goes Here")
        val (_, viewModel) = ConversationInfoViewModelArrangement()
            .withConversationDetailUpdate(conversationDetails = groupConversationDetails)
            .arrange()

        // then
        launch { viewModel.observeConversationDetails() }.run {
            advanceUntilIdle()
            assertEquals(
                groupConversationDetails.conversation.protocol,
                viewModel.conversationInfoViewState.protocolInfo
            )
            assertEquals(
                groupConversationDetails.conversation.mlsVerificationStatus,
                viewModel.conversationInfoViewState.mlsVerificationStatus
            )
            cancel()
        }
    }

    @Test
    fun `given Failure while getting an MLS conversation's verification status, then mlsVerificationStatus is null`() = runTest {
        // Given
        val (_, viewModel) = ConversationInfoViewModelArrangement()
            .arrange()

        // then
        assertEquals(
            null,
            viewModel.conversationInfoViewState.protocolInfo
        )
        assertEquals(
            null,
            viewModel.conversationInfoViewState.mlsVerificationStatus
        )
    }

    @Test
    fun `given conversation details are not found, when observing details, then call onNotFound`() = runTest {
        // Given
        val (arrangement, viewModel) = ConversationInfoViewModelArrangement()
            .withConversationDetailFailure(StorageFailure.DataNotFound)
            .arrange()
        launch { viewModel.observeConversationDetails() }.run {
            advanceUntilIdle()
            // When - Then
            assertEquals(true, viewModel.conversationInfoViewState.notFound)
            cancel()
        }
    }
}
