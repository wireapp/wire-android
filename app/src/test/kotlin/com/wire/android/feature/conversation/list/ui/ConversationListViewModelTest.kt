package com.wire.android.feature.conversation.list.ui

import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import com.wire.android.UnitTest
import com.wire.android.core.events.EventsHandler
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.list.usecase.GetConversationsUseCase
import com.wire.android.feature.conversation.list.usecase.GetMembersOfConversationsUseCase
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.livedata.shouldBeUpdated
import com.wire.android.framework.livedata.shouldNotBeUpdated
import com.wire.android.shared.user.User
import com.wire.android.shared.user.usecase.GetCurrentUserUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ConversationListViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @MockK
    private lateinit var getCurrentUserUseCase: GetCurrentUserUseCase

    @MockK
    private lateinit var getConversationsUseCase: GetConversationsUseCase

    @MockK
    private lateinit var getMembersOfConversationsUseCase: GetMembersOfConversationsUseCase

    @MockK
    private lateinit var conversationListPagingDelegate: ConversationListPagingDelegate

    @MockK
    private lateinit var eventsHandler: EventsHandler

    private lateinit var conversationListViewModel: ConversationListViewModel

    @Before
    fun setUp() {
        conversationListViewModel = ConversationListViewModel(
            coroutinesTestRule.dispatcherProvider,
            getCurrentUserUseCase, getConversationsUseCase, getMembersOfConversationsUseCase,
            conversationListPagingDelegate, eventsHandler
        )
    }

    @Test
    fun `given fetchUserName is called, when GetCurrentUserUseCase is successful, then sets user name to userNameLiveData`() {
        val user = User(id = TEST_USER_ID, name = TEST_USER_NAME)
        coEvery { getCurrentUserUseCase.run(Unit) } returns Either.Right(user)

        conversationListViewModel.fetchUserName()

        conversationListViewModel.userNameLiveData shouldBeUpdated { it shouldBeEqualTo TEST_USER_NAME }
    }

    @Test
    fun `given fetchUserName is called, when GetCurrentUserUseCase fails, then does not set anything to userNameLiveData`() {
        coEvery { getCurrentUserUseCase.run(Unit) } returns Either.Left(ServerError)

        conversationListViewModel.fetchUserName()

        conversationListViewModel.userNameLiveData.shouldNotBeUpdated()
    }

    @Test
    fun `given conversationListItemsLiveData is called, then calls paging delegate for conversationItems with proper page size`() {
        val listItems = mockk<PagedList<ConversationListItem>>(relaxed = true, relaxUnitFun = true)
        val pagingLiveData = MutableLiveData(listItems)

        every { conversationListPagingDelegate.conversationList(any(), any()) } returns pagingLiveData

        //TODO assert pagedList contents:
//        conversationListViewModel.conversationListItemsLiveData.shouldBeUpdated {
//            it shouldBeEqualTo listItems
//        }
        verify(exactly = 1) { conversationListPagingDelegate.conversationList(CONVERSATIONS_PAGE_SIZE, any()) }
    }

    companion object {
        private const val TEST_USER_ID = "user-id-123"
        private const val TEST_USER_NAME = "User Name"
        private const val CONVERSATIONS_PAGE_SIZE = 30
    }
}
