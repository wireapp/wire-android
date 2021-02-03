package com.wire.android.feature.conversation.list.ui

import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import com.wire.android.UnitTest
import com.wire.android.core.events.EventsHandler
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.livedata.shouldBeUpdated
import com.wire.android.framework.livedata.shouldNotBeUpdated
import com.wire.android.shared.team.Team
import com.wire.android.shared.team.usecase.GetUserTeamUseCase
import com.wire.android.shared.team.usecase.NotATeamUser
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
    private lateinit var getUserTeamUseCase: GetUserTeamUseCase

    @MockK
    private lateinit var conversationListPagingDelegate: ConversationListPagingDelegate

    @MockK
    private lateinit var eventsHandler: EventsHandler

    private lateinit var conversationListViewModel: ConversationListViewModel

    @Before
    fun setUp() {
        conversationListViewModel = ConversationListViewModel(
            coroutinesTestRule.dispatcherProvider,
            getCurrentUserUseCase, getUserTeamUseCase,
            conversationListPagingDelegate, eventsHandler
        )
    }

    @Test
    fun `given fetchToolbarData is called, when GetCurrentUserUseCase fails, then does not set anything to toolbarDataLiveData`() {
        coEvery { getCurrentUserUseCase.run(Unit) } returns Either.Left(ServerError)

        conversationListViewModel.fetchToolbarData()

        conversationListViewModel.toolbarDataLiveData.shouldNotBeUpdated()
    }

    @Test
    fun `given fetchToolbarData is called and current user is fetched, when GetUserTeamUseCase is successful, then updates toolbarData`() {
        val user = mockk<User>()
        coEvery { getCurrentUserUseCase.run(Unit) } returns Either.Right(user)
        val team = mockk<Team>()
        coEvery { getUserTeamUseCase.run(any()) } returns Either.Right(team)

        conversationListViewModel.fetchToolbarData()

        conversationListViewModel.toolbarDataLiveData shouldBeUpdated {
            it.user shouldBeEqualTo user
            it.team shouldBeEqualTo team
        }
    }


    @Test
    fun `given fetchToolbarData called & user fetched, when GetUserTeamUseCase fails with NotATeamUser, then updates toolbarData`() {
        val user = mockk<User>()
        coEvery { getCurrentUserUseCase.run(Unit) } returns Either.Right(user)
        coEvery { getUserTeamUseCase.run(any()) } returns Either.Left(NotATeamUser)

        conversationListViewModel.fetchToolbarData()

        conversationListViewModel.toolbarDataLiveData shouldBeUpdated {
            it.user shouldBeEqualTo user
            it.team shouldBeEqualTo null
        }
    }

    @Test
    fun `given fetchToolbarData called & user fetched, when GetUserTeamUseCase fails with other error, then does not update toolbarData`() {
        val user = mockk<User>()
        coEvery { getCurrentUserUseCase.run(Unit) } returns Either.Right(user)
        coEvery { getUserTeamUseCase.run(any()) } returns Either.Left(mockk())

        conversationListViewModel.fetchToolbarData()

        conversationListViewModel.toolbarDataLiveData.shouldNotBeUpdated()
    }

    @Test
    fun `given conversationListItemsLiveData is called, then calls paging delegate for conversationItems with proper page size`() {
        val listItems = mockk<PagedList<ConversationListItem>>(relaxed = true, relaxUnitFun = true)
        val pagingLiveData = MutableLiveData(listItems)

        every { conversationListPagingDelegate.conversationList(any()) } returns pagingLiveData

        //TODO assert pagedList contents:
//        conversationListViewModel.conversationListItemsLiveData.shouldBeUpdated {
//            it shouldBeEqualTo listItems
//        }
        verify(exactly = 1) { conversationListPagingDelegate.conversationList(CONVERSATIONS_PAGE_SIZE) }
    }

    companion object {
        private const val CONVERSATIONS_PAGE_SIZE = 30
    }
}
