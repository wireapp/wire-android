package com.wire.android.feature.conversation.list.ui

import androidx.paging.PagedList
import com.wire.android.UnitTest
import com.wire.android.core.events.EventsHandler
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse
import com.wire.android.feature.auth.client.usecase.MaximumNumberOfDevicesReached
import com.wire.android.feature.auth.client.usecase.RegisterClientUseCase
import com.wire.android.feature.conversation.list.usecase.GetConversationListUseCase
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.livedata.shouldBeUpdated
import com.wire.android.framework.livedata.shouldNotBeUpdated
import com.wire.android.shared.session.usecase.SetCurrentSessionToDormantUseCase
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
    private lateinit var registerClientUseCase: RegisterClientUseCase

    @MockK
    private lateinit var setCurrentSessionToDormantUseCase: SetCurrentSessionToDormantUseCase

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
            registerClientUseCase,
            setCurrentSessionToDormantUseCase,
            eventsHandler
        )
    }

    @Test
    fun `given fetchConversationList is called, when getConversationListUseCase emits items, then updates conversationListItemsLiveData`() {
        val items = mockk<PagedList<ConversationListItem>>()
        coEvery { getConversationListUseCase.run(any()) } returns flowOf(items)

        conversationListViewModel.fetchConversationList()

        conversationListViewModel.conversationListItemsLiveData.shouldBeUpdated {
            it shouldBeEqualTo items
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

    @Test
    fun `given clearSession is called, when the use case run with success, then update isCurrentSessionClearedLiveData`() {
        coEvery { setCurrentSessionToDormantUseCase.run(any()) } returns Either.Right(Unit)

        conversationListViewModel.clearSession()

        conversationListViewModel.isCurrentSessionDormantLiveData.shouldBeUpdated {
            it shouldBeEqualTo true
        }
        coVerify(exactly = 1) { setCurrentSessionToDormantUseCase.run(any()) }
    }

    @Test
    fun `given clearSession is called, when setCurrentSessionToDormantUseCase fail, then update isCurrentSessionClearedLiveData`() {
        val failure = mockk<Failure>()
        coEvery { setCurrentSessionToDormantUseCase.run(any()) } returns Either.Left(failure)

        conversationListViewModel.clearSession()

        conversationListViewModel.isCurrentSessionDormantLiveData.shouldBeUpdated {
            it shouldBeEqualTo false
        }
        coVerify(exactly = 1) { setCurrentSessionToDormantUseCase.run(any()) }
    }

    @Test
    fun `given registerClient is called, when registerClientUseCase run with success, then update isDeviceNumberLimitReachedLiveData`() {
        val clientResponse = mockk<ClientResponse>()
        coEvery { registerClientUseCase.run(any()) } returns Either.Right(clientResponse)

        conversationListViewModel.registerClient()

        conversationListViewModel.isDeviceNumberLimitReachedLiveData.shouldBeUpdated {
            it shouldBeEqualTo false
        }
        coVerify(exactly = 1) { registerClientUseCase.run(any()) }
    }

    @Test
    fun `given registerClient is called, when registerClientUseCase fail with maximumNumberOfDevicesReached, then update the LiveData`() {
        val failure = mockk<MaximumNumberOfDevicesReached>()
        coEvery { registerClientUseCase.run(any()) } returns Either.Left(failure)

        conversationListViewModel.registerClient()

        conversationListViewModel.isDeviceNumberLimitReachedLiveData.shouldBeUpdated {
            it shouldBeEqualTo true
        }
        coVerify(exactly = 1) { registerClientUseCase.run(any()) }
    }
}
