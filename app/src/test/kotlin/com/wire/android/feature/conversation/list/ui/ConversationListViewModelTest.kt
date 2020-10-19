package com.wire.android.feature.conversation.list.ui

import com.wire.android.UnitTest
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.list.usecase.GetConversationsUseCase
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.functional.assertRight
import com.wire.android.framework.livedata.assertNotUpdated
import com.wire.android.framework.livedata.awaitValue
import com.wire.android.shared.auth.activeuser.GetActiveUserUseCase
import com.wire.android.shared.user.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

@ExperimentalCoroutinesApi
class ConversationListViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @Mock
    private lateinit var getActiveUserUseCase: GetActiveUserUseCase

    @Mock
    private lateinit var getConversationsUseCase: GetConversationsUseCase

    private lateinit var conversationListViewModel: ConversationListViewModel

    @Before
    fun setUp() {
        conversationListViewModel =
            ConversationListViewModel(coroutinesTestRule.dispatcherProvider, getActiveUserUseCase, getConversationsUseCase)
    }

    @Test
    fun `given fetchUserName is called, when GetActiveUserUseCase is successful, then sets user name to userNameLiveData`() =
        coroutinesTestRule.runTest {
            val user = User(id = TEST_USER_ID, name = TEST_USER_NAME)
            `when`(getActiveUserUseCase.run(Unit)).thenReturn(Either.Right(user))

            conversationListViewModel.fetchUserName()

            assertThat(conversationListViewModel.userNameLiveData.awaitValue()).isEqualTo(TEST_USER_NAME)
        }

    @Test
    fun `given fetchUserName is called, when GetActiveUserUseCase is successful, then does not set anything to userNameLiveData`() =
        coroutinesTestRule.runTest {
            `when`(getActiveUserUseCase.run(Unit)).thenReturn(Either.Left(ServerError))

            conversationListViewModel.fetchUserName()

            conversationListViewModel.userNameLiveData.assertNotUpdated()
        }

    @Test
    fun `given fetchConversations is called, when GetConversationsUseCase is successful, then sets value to conversationsLiveData`() =
        coroutinesTestRule.runTest {
            `when`(getConversationsUseCase.run(Unit)).thenReturn(Either.Right(TEST_CONVERSATIONS))

            conversationListViewModel.fetchConversations()

            conversationListViewModel.conversationsLiveData.awaitValue().assertRight {
                assertThat(it).isEqualTo(TEST_CONVERSATIONS)
            }
        }

    @Test
    fun `given fetchConversations is called, when GetConversationsUseCase fails, then sets error to conversationsLiveData`() =
        coroutinesTestRule.runTest {
            `when`(getConversationsUseCase.run(Unit)).thenReturn(Either.Left(ServerError))

            conversationListViewModel.fetchConversations()

            assertThat(conversationListViewModel.conversationsLiveData.awaitValue().isLeft).isTrue()
        }

    companion object {
        private const val TEST_USER_ID = "user-id-123"
        private const val TEST_USER_NAME = "User Name"
        private val TEST_CONVERSATIONS = listOf(Conversation("Conv 1"), Conversation("Conv 2"))
    }
}
