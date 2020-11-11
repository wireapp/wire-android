package com.wire.android.feature.conversation.list.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagedList
import com.wire.android.UnitTest
import com.wire.android.core.events.EventsHandler
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.core.ui.dialog.GeneralErrorMessage
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.list.usecase.GetConversationsParams
import com.wire.android.feature.conversation.list.usecase.GetConversationsUseCase
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.livedata.shouldBeUpdated
import com.wire.android.framework.livedata.shouldNotBeUpdated
import com.wire.android.shared.auth.activeuser.GetActiveUserUseCase
import com.wire.android.shared.user.User
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class ConversationListViewModelTest : UnitTest() {

    @MockK
    private lateinit var getActiveUserUseCase: GetActiveUserUseCase

    @MockK
    private lateinit var getConversationsUseCase: GetConversationsUseCase

    @MockK
    private lateinit var eventsHandler: EventsHandler

    private lateinit var getConversationParams: GetConversationsParams

    private lateinit var conversationListViewModel: ConversationListViewModel

    @Before
    fun setUp() {
        conversationListViewModel = ConversationListViewModel(getActiveUserUseCase, getConversationsUseCase, eventsHandler)
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
    fun `given conversationsLiveData observed, when getConversationsUseCase returns conversations, then sets value to liveData`() {
        val conversations = mockk<PagedList<Conversation>>()
        val useCaseResultLiveData: LiveData<Either<Failure, PagedList<Conversation>>> =
            MutableLiveData(Either.Right(conversations))
        every { getConversationsUseCase(any(), any()) } returns useCaseResultLiveData

        conversationListViewModel.conversationsLiveData shouldBeUpdated { result ->
            result shouldSucceed {
                it shouldBeEqualTo conversations
            }
        }

        val useCaseParamsSlot = slot<GetConversationsParams>()
        verify { getConversationsUseCase(conversationListViewModel.viewModelScope, capture(useCaseParamsSlot)) }
        useCaseParamsSlot.captured.size shouldBeEqualTo 30 //TODO update this assertion when inject config
    }

    @Test
    fun `given conversationsLiveData observed, when getConversationsUseCase fails, then sets GeneralErrorMessage to liveData`() {
        val useCaseResultLiveData: LiveData<Either<Failure, PagedList<Conversation>>> = MutableLiveData(Either.Left(ServerError))
        every { getConversationsUseCase(any(), any()) } returns useCaseResultLiveData

        conversationListViewModel.conversationsLiveData shouldBeUpdated { result ->
            result shouldFail { it shouldBeEqualTo GeneralErrorMessage }
        }
    }

    companion object {
        private const val TEST_USER_ID = "user-id-123"
        private const val TEST_USER_NAME = "User Name"
    }
}
