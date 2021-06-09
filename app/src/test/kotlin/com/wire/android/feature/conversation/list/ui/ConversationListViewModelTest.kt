package com.wire.android.feature.conversation.list.ui

import androidx.paging.PagingData
import com.wire.android.UnitTest
import com.wire.android.core.events.EventsHandler
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.list.usecase.GetConversationListUseCase
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.livedata.shouldBeUpdated
import com.wire.android.framework.livedata.shouldNotBeUpdated
import com.wire.android.shared.team.Team
import com.wire.android.shared.team.usecase.GetUserTeamUseCase
import com.wire.android.shared.team.usecase.NotATeamUser
import com.wire.android.shared.user.User
import com.wire.android.shared.user.usecase.GetCurrentUserUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ConversationListViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @MockK
    private lateinit var getConversationListUseCase: GetConversationListUseCase

    @MockK
    private lateinit var getCurrentUserUseCase: GetCurrentUserUseCase

    @MockK
    private lateinit var getUserTeamUseCase: GetUserTeamUseCase

    @MockK
    private lateinit var eventsHandler: EventsHandler

    private lateinit var conversationListViewModel: ConversationListViewModel

    @Before
    fun setUp() {
        conversationListViewModel = ConversationListViewModel(
            coroutinesTestRule.dispatcherProvider,
            getConversationListUseCase,
            getCurrentUserUseCase,
            getUserTeamUseCase,
            eventsHandler
        )
    }

    @Test
    fun `given fetchConversationList is called, when getConversationListUseCase emits items, then updates conversationListItemsLiveData`() {
            val items = mockk<PagingData<ConversationListItem>>(relaxed = true)
            coEvery { getConversationListUseCase.run(any()) } returns flowOf(items)

            conversationListViewModel.fetchConversationList()

            coroutinesTestRule.runTest {
                conversationListViewModel.conversationListItemsLiveData.shouldBeUpdated {
                    it shouldBeInstanceOf PagingData::class
                }
            }
            coVerify(exactly = 1) { getConversationListUseCase.run(any()) }
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
}
