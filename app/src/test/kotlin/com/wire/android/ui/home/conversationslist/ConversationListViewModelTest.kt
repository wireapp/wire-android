package com.wire.android.ui.home.conversationslist

import androidx.compose.material3.ExperimentalMaterial3Api
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.model.UserStatus
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationLastEvent
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.conversationslist.model.UserInfo
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.connection.ObserveConnectionListUseCase
import com.wire.kalium.logic.feature.conversation.ConversationUpdateStatusResult
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.message.MarkMessagesAsNotifiedUseCase
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
//TODO write more tests
class ConversationListViewModelTest {

    private lateinit var conversationListViewModel: ConversationListViewModel

    @MockK
    lateinit var navigationManager: NavigationManager

    @MockK
    lateinit var observeConversationDetailsList: ObserveConversationListDetailsUseCase

    @MockK
    lateinit var updateConversationMutedStatus: UpdateConversationMutedStatusUseCase

    @MockK
    lateinit var observeConnectionList: ObserveConnectionListUseCase

    @MockK
    lateinit var markMessagesAsNotified: MarkMessagesAsNotifiedUseCase

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        mockUri()
        conversationListViewModel =
            ConversationListViewModel(
                navigationManager,
                observeConversationDetailsList,
                updateConversationMutedStatus,
                markMessagesAsNotified,
                observeConnectionList,
                TestDispatcherProvider()
            )

        coEvery { observeConversationDetailsList.invoke() } returns flowOf(listOf())
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

    companion object {
        private val conversationId = ConversationId("some_id", "some_domain")

        private val conversationItem = ConversationItem.PrivateConversation(
            userInfo = UserInfo(
                avatarAsset = null,
                availabilityStatus = UserStatus.NONE
            ),
            conversationInfo = ConversationInfo(
                name = "",
                membership = Membership.None
            ),
            conversationId = conversationId,
            mutedStatus = MutedConversationStatus.AllAllowed,
            isLegalHold = false,
            lastEvent = ConversationLastEvent.None,
        )
    }
}
