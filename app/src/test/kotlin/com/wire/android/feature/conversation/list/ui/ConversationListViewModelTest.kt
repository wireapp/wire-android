package com.wire.android.feature.conversation.list.ui

import com.wire.android.UnitTest
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.framework.coroutines.CoroutinesTestRule
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
import java.util.concurrent.TimeoutException

@ExperimentalCoroutinesApi
class ConversationListViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @Mock
    private lateinit var getActiveUserUseCase: GetActiveUserUseCase

    private lateinit var conversationListViewModel: ConversationListViewModel

    @Before
    fun setUp() {
        conversationListViewModel = ConversationListViewModel(coroutinesTestRule.dispatcherProvider, getActiveUserUseCase)
    }

    @Test
    fun `given fetchUserName is called, when GetActiveUserUseCase is successful, then sets user name to userNameLiveData`() =
        coroutinesTestRule.runTest {
            val user = User(TEST_USER_NAME)
            `when`(getActiveUserUseCase.run(Unit)).thenReturn(Either.Right(user))

            conversationListViewModel.fetchUserName()

            assertThat(conversationListViewModel.userNameLiveData.awaitValue()).isEqualTo(TEST_USER_NAME)
        }

    @Test(expected = TimeoutException::class)
    fun `given fetchUserName is called, when GetActiveUserUseCase is successful, then does not set anything to userNameLiveData`() =
        coroutinesTestRule.runTest {
            `when`(getActiveUserUseCase.run(Unit)).thenReturn(Either.Left(ServerError))

            conversationListViewModel.fetchUserName()

            conversationListViewModel.userNameLiveData.awaitValue()
        }

    companion object {
        private const val TEST_USER_NAME = "User Name"
    }
}
