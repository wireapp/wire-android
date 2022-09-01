package com.wire.android.ui.home.conversationslist

import androidx.compose.material3.ExperimentalMaterial3Api
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.model.UserAvatarData
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversationslist.model.BadgeEventType
import com.wire.android.ui.home.conversationslist.model.BlockingState
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationLastEvent
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.AnswerCallUseCase
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.connection.BlockUserResult
import com.wire.kalium.logic.feature.conversation.ConversationListDetails
import com.wire.kalium.logic.feature.conversation.ConversationUpdateStatusResult
import com.wire.kalium.logic.feature.conversation.ObserveConversationsAndConnectionsUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.team.DeleteTeamConversationUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3Api::class)
@ExtendWith(CoroutineTestExtension::class)
// TODO write more tests
class ConversationListViewModelTest {

    private lateinit var conversationListViewModel: ConversationListViewModel

    @MockK
    lateinit var navigationManager: NavigationManager

    @MockK
    lateinit var updateConversationMutedStatus: UpdateConversationMutedStatusUseCase

    @MockK
    lateinit var observeConversationsAndConnections: ObserveConversationsAndConnectionsUseCase

    @MockK
    lateinit var removeMemberFromConversationUseCase: RemoveMemberFromConversationUseCase

    @MockK
    lateinit var deleteTeamConversationUseCase: DeleteTeamConversationUseCase

    @MockK
    lateinit var joinCall: AnswerCallUseCase

    @MockK
    lateinit var getSelf: GetSelfUserUseCase

    @MockK
    lateinit var blockUser: BlockUserUseCase

    @MockK
    private lateinit var wireSessionImageLoader: WireSessionImageLoader

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        mockUri()
        conversationListViewModel =
            ConversationListViewModel(
                navigationManager,
                TestDispatcherProvider(),
                updateConversationMutedStatus,
                joinCall,
                observeConversationsAndConnections,
                removeMemberFromConversationUseCase,
                deleteTeamConversationUseCase,
                getSelf,
                blockUser,
                wireSessionImageLoader,
                UserTypeMapper(),
            )

        coEvery { observeConversationsAndConnections() } returns flowOf(ConversationListDetails(listOf(), 0L, 0L, 0L))
    }

    @Test
    fun `given a valid conversation muting state, when calling muteConversation, then should call with call the UseCase`() = runTest {
        coEvery { updateConversationMutedStatus(any(), any(), any()) } returns ConversationUpdateStatusResult.Success
        conversationListViewModel.muteConversation(conversationId, MutedConversationStatus.AllMuted)

        coVerify(exactly = 1) { updateConversationMutedStatus(conversationId, MutedConversationStatus.AllMuted, any()) }
    }

    @Test
    fun `given a conversations list, when opening a new conversation, then should delegate call to manager to NewConversation`() = runTest {
        conversationListViewModel.openNewConversation()

        coVerify(exactly = 1) { navigationManager.navigate(NavigationCommand(NavigationItem.NewConversation.getRouteWithArgs())) }
    }

    @Test
    fun `given a conversations list, when opening a conversation, then should delegate call to manager to Conversation with args`() =
        runTest {
            conversationListViewModel.openConversation(conversationItem.conversationId)

            coVerify(exactly = 1) {
                navigationManager.navigate(
                    NavigationCommand(
                        NavigationItem.Conversation.getRouteWithArgs(
                            listOf(
                                conversationId
                            )
                        )
                    )
                )
            }
        }

    @Test
    fun `given a conversation id, when joining an ongoing call, then verify that answer call usecase is called`() = runTest {
        coEvery { joinCall(any()) } returns Unit

        conversationListViewModel.joinOngoingCall(conversationId = conversationId)

        coVerify(exactly = 1) { joinCall(conversationId = conversationId) }
        coVerify(exactly = 1) {
            navigationManager.navigate(
                NavigationCommand(
                    NavigationItem.OngoingCall.getRouteWithArgs(
                        listOf(
                            conversationId
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `given a valid conversation muting state, when calling block user, then should call BlockUserUseCase`() = runTest {
        coEvery { blockUser(any()) } returns BlockUserResult.Success
        conversationListViewModel.blockUser(userId, "someName")

        coVerify(exactly = 1) { blockUser(userId) }
    }

    companion object {
        private val conversationId = ConversationId("some_id", "some_domain")
        private val userId: UserId = UserId("someUser", "some_domain")

        private val conversationItem = ConversationItem.PrivateConversation(
            userAvatarData = UserAvatarData(),
            conversationInfo = ConversationInfo(
                name = "",
                membership = Membership.None
            ),
            conversationId = conversationId,
            mutedStatus = MutedConversationStatus.AllAllowed,
            isLegalHold = false,
            lastEvent = ConversationLastEvent.None,
            badgeEventType = BadgeEventType.None,
            userId = userId,
            blockingState = BlockingState.CAN_NOT_BE_BLOCKED
        )
    }
}
