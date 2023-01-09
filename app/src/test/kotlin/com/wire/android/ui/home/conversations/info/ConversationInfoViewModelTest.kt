package com.wire.android.ui.home.conversations.info

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestUser
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.ui.home.conversations.ConversationAvatar
import com.wire.android.ui.home.conversations.mockConversationDetailsGroup
import com.wire.android.ui.home.conversations.withMockConversationDetailsOneOnOne
import com.wire.android.util.EMPTY
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ConversationInfoViewModelTest {

    @Test
    fun `given self user 1on1 message, when clicking on avatar, then open self profile`() = runTest {
        // Given
        val oneOneDetails = withMockConversationDetailsOneOnOne("Other User Name Goes Here")
        val userId = TestUser.USER_ID
        val (arrangement, viewModel) = ConversationInfoViewModelArrangement()
            .withConversationDetailUpdate(oneOneDetails)
            .withSelfUser()
            .arrange()
        // When
        viewModel.navigateToProfile(userId.toString())
        // Then
        coVerify(exactly = 1) {
            arrangement.navigationManager.navigate(NavigationCommand(NavigationItem.SelfUserProfile.getRouteWithArgs()))
        }
    }

    @Test
    fun `given self user group message, when clicking on avatar, then open self profile`() = runTest {
        // Given
        val groupDetails = mockConversationDetailsGroup("Conversation Name Goes Here")
        val userId = TestUser.USER_ID
        val (arrangement, viewModel) = ConversationInfoViewModelArrangement()
            .withConversationDetailUpdate(groupDetails)
            .withSelfUser()
            .arrange()
        // When
        viewModel.navigateToProfile(userId.toString())
        // Then
        coVerify(exactly = 1) {
            arrangement.navigationManager.navigate(NavigationCommand(NavigationItem.SelfUserProfile.getRouteWithArgs()))
        }
    }

    @Test
    fun `given other user 1on1 message, when clicking on avatar, then open other user profile without group data`() = runTest {
        // Given
        val oneOneDetails: ConversationDetails.OneOne = withMockConversationDetailsOneOnOne("Other User Name Goes Here")
        val userId = UserId("other", "domain")
        val (arrangement, viewModel) = ConversationInfoViewModelArrangement()
            .withConversationDetailUpdate(oneOneDetails)
            .withSelfUser()
            .arrange()
        coEvery {
            arrangement.qualifiedIdMapper.fromStringToQualifiedID("other@domain")
        } returns QualifiedID("other", "domain")

        // When
        viewModel.navigateToProfile(userId.toString())
        // Then
        coVerify(exactly = 1) {
            arrangement.navigationManager.navigate(NavigationCommand(NavigationItem.OtherUserProfile.getRouteWithArgs(listOf(userId))))
        }
    }

    @Test
    fun `given other user group message, when clicking on avatar, then open other user profile with group data`() = runTest {
        // Given
        val groupDetails: ConversationDetails.Group = mockConversationDetailsGroup("Conversation Name Goes Here")
        val userId = UserId("other", "domain")
        val (arrangement, viewModel) = ConversationInfoViewModelArrangement()
            .withConversationDetailUpdate(groupDetails)
            .withSelfUser()
            .arrange()
        coEvery {
            arrangement.qualifiedIdMapper.fromStringToQualifiedID("other@domain")
        } returns QualifiedID("other", "domain")

        // When
        viewModel.navigateToProfile(userId.toString())
        // Then
        coVerify(exactly = 1) {
            arrangement.navigationManager.navigate(
                NavigationCommand(NavigationItem.OtherUserProfile.getRouteWithArgs(listOf(userId, arrangement.conversationId)))
            )
        }
    }

    @Test
    fun `given a 1 on 1 conversation, when solving the conversation name, then the name of the other user is used`() = runTest {
        // Given
        val oneToOneConversationDetails = withMockConversationDetailsOneOnOne("Other User Name Goes Here")
        val (_, viewModel) = ConversationInfoViewModelArrangement()
            .withConversationDetailUpdate(
                conversationDetails = oneToOneConversationDetails
            )
            .withSelfUser()
            .arrange()

        // When - Then
        assert(viewModel.conversationInfoViewState.conversationName is UIText.DynamicString)
        assertEquals(
            oneToOneConversationDetails.otherUser.name,
            (viewModel.conversationInfoViewState.conversationName as UIText.DynamicString).value
        )
    }

    @Test
    fun `given a 1 on 1 conversation, when solving the conversation name, then unavailable user is used`() = runTest {
        // Given
        val oneToOneConversationDetails = withMockConversationDetailsOneOnOne(senderName = "", unavailable = true)
        val (_, viewModel) = ConversationInfoViewModelArrangement()
            .withConversationDetailUpdate(
                conversationDetails = oneToOneConversationDetails
            )
            .withSelfUser()
            .arrange()

        // When - Then
        assert(viewModel.conversationInfoViewState.conversationName is UIText.StringResource)
    }

    @Test
    fun `given a group conversation, when solving the conversation name, then the name of the conversation is used`() = runTest {
        // Given
        val groupConversationDetails = mockConversationDetailsGroup("Conversation Name Goes Here")
        val (_, viewModel) = ConversationInfoViewModelArrangement()
            .withConversationDetailUpdate(conversationDetails = groupConversationDetails)
            .withSelfUser()
            .arrange()

        // When - Then
        assert(viewModel.conversationInfoViewState.conversationName is UIText.DynamicString)
        assertEquals(
            groupConversationDetails.conversation.name,
            (viewModel.conversationInfoViewState.conversationName as UIText.DynamicString).value
        )
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
            .withSelfUser()
            .arrange()

        // When - Then
        assert(viewModel.conversationInfoViewState.conversationName is UIText.DynamicString)
        assertEquals(
            firstConversationDetails.conversation.name,
            (viewModel.conversationInfoViewState.conversationName as UIText.DynamicString).value
        )

        // When - Then
        arrangement.withConversationDetailUpdate(conversationDetails = secondConversationDetails)
        assert(viewModel.conversationInfoViewState.conversationName is UIText.DynamicString)
        assertEquals(
            secondConversationDetails.conversation.name,
            (viewModel.conversationInfoViewState.conversationName as UIText.DynamicString).value
        )
    }

    @Test
    fun `given the initial state, when solving the conversation name before the data is received, the name should be an empty string`() =
        runTest {
            // Given
            val (_, viewModel) = ConversationInfoViewModelArrangement()
                .withSelfUser()
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
            .withSelfUser()
            .arrange()

        // When - Then
        assert(viewModel.conversationInfoViewState.conversationName is UIText.StringResource)
    }

    @Test
    fun `given a 1 on 1 conversation, when solving the conversation avatar, then the avatar of the other user is used`() = runTest {
        // Given
        val conversationDetails = withMockConversationDetailsOneOnOne("", ConversationId("userAssetId", "domain"))
        val otherUserAvatar = conversationDetails.otherUser.previewPicture
        val (_, viewModel) = ConversationInfoViewModelArrangement()
            .withConversationDetailUpdate(conversationDetails = conversationDetails)
            .withSelfUser()
            .arrange()
        val actualAvatar = viewModel.conversationInfoViewState.conversationAvatar
        // When - Then
        assert(actualAvatar is ConversationAvatar.OneOne)
        assertEquals(otherUserAvatar, (actualAvatar as ConversationAvatar.OneOne).avatarAsset?.userAssetId)
    }
}
