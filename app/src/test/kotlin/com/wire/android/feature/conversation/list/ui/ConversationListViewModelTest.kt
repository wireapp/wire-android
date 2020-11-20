package com.wire.android.feature.conversation.list.ui

import androidx.paging.PagedList
import com.wire.android.UnitTest
import com.wire.android.core.events.EventsHandler
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.list.usecase.GetConversationsUseCase
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.livedata.shouldBeUpdated
import com.wire.android.framework.livedata.shouldNotBeUpdated
import com.wire.android.shared.auth.activeuser.GetActiveUserUseCase
import com.wire.android.shared.user.User
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ConversationListViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @MockK
    private lateinit var getActiveUserUseCase: GetActiveUserUseCase

    @MockK
    private lateinit var getConversationsUseCase: GetConversationsUseCase

    @MockK
    private lateinit var eventsHandler: EventsHandler

    private lateinit var conversationListViewModel: ConversationListViewModel

    @Before
    fun setUp() {
        conversationListViewModel =
            ConversationListViewModel(coroutinesTestRule.dispatcherProvider, getActiveUserUseCase,
                getConversationsUseCase, eventsHandler)
    }

    @Test
    fun `given fetchUserName is called, when GetActiveUserUseCase is successful, then sets user name to userNameLiveData`() {
        val user = User(id = TEST_USER_ID, name = TEST_USER_NAME)
        coEvery { getActiveUserUseCase.run(Unit) } returns Either.Right(user)

        conversationListViewModel.fetchUserName()

        conversationListViewModel.userNameLiveData shouldBeUpdated { it shouldBeEqualTo TEST_USER_NAME }
    }

    @Test
    fun `given fetchUserName is called, when GetActiveUserUseCase is successful, then does not set anything to userNameLiveData`() {
        coEvery { getActiveUserUseCase.run(Unit) } returns Either.Left(ServerError)

        conversationListViewModel.fetchUserName()

        conversationListViewModel.userNameLiveData.shouldNotBeUpdated()
    }

    @Test
    fun `given fetchConversations is called, when GetConversationsUseCase emits success, then sets success to conversationsLiveData`() {
        val conversations = mockk<PagedList<Conversation>>()
        coEvery { getConversationsUseCase.run(any()) } returns flowOf(Either.Right(conversations))

        conversationListViewModel.fetchConversations()

        conversationListViewModel.conversationsLiveData shouldBeUpdated { result ->
            result shouldSucceed { it shouldBeEqualTo conversations }
        }
    }

    @Test
    fun `given fetchConversations is called, when GetConversationsUseCase emits failure, then sets failure to conversationsLiveData`() {
        val failure = mockk<Failure>()
        coEvery { getConversationsUseCase.run(any()) } returns flowOf(Either.Left(failure))

        conversationListViewModel.fetchConversations()

        conversationListViewModel.conversationsLiveData shouldBeUpdated { result ->
            result shouldFail  { it shouldBeEqualTo failure }
        }
    }

    companion object {
        private const val TEST_USER_ID = "user-id-123"
        private const val TEST_USER_NAME = "User Name"
    }
}
