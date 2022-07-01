package com.wire.android.ui.userprofile.other

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.mockUri
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.navigation.EXTRA_USER_DOMAIN
import com.wire.android.navigation.EXTRA_USER_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.CoreFailure.Unknown
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.conversation.ProtocolInfo
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.publicuser.model.OtherUser
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.connection.AcceptConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.AcceptConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.CancelConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.CancelConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.IgnoreConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.IgnoreConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.conversation.CreateConversationResult
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoResult
import com.wire.kalium.logic.feature.user.GetUserInfoUseCase
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class OtherUserProfileScreenViewModelTest {

    lateinit var otherUserProfileScreenViewModel: OtherUserProfileScreenViewModel

    @MockK
    lateinit var navigationManager: NavigationManager

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    private lateinit var getOrCreateOneToOneConversation: GetOrCreateOneToOneConversationUseCase

    @MockK
    private lateinit var getUserInfo: GetUserInfoUseCase

    @MockK
    private lateinit var sendConnectionRequest: SendConnectionRequestUseCase

    @MockK
    private lateinit var cancelConnectionRequest: CancelConnectionRequestUseCase

    @MockK
    private lateinit var acceptConnectionRequest: AcceptConnectionRequestUseCase

    @MockK
    private lateinit var ignoreConnectionRequest: IgnoreConnectionRequestUseCase

    @MockK
    private lateinit var wireSessionImageLoader: WireSessionImageLoader

    @MockK
    private lateinit var userTypeMapper : UserTypeMapper

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockUri()
        every { savedStateHandle.get<String>(eq(EXTRA_USER_ID)) } returns CONVERSATION_ID.value
        every { savedStateHandle.get<String>(eq(EXTRA_USER_DOMAIN)) } returns CONVERSATION_ID.domain
        coEvery { getUserInfo(any()) } returns GetUserInfoResult.Success(OTHER_USER, TEAM)
        every { userTypeMapper.toMembership(any()) } returns Membership.None

        otherUserProfileScreenViewModel = OtherUserProfileScreenViewModel(
            savedStateHandle,
            navigationManager,
            getOrCreateOneToOneConversation,
            getUserInfo,
            sendConnectionRequest,
            cancelConnectionRequest,
            acceptConnectionRequest,
            ignoreConnectionRequest,
            userTypeMapper,
            wireSessionImageLoader
        )
    }

    @Test
    fun `given a userId, when sending a connection request, then returns a Success result and update view state`() =
        runTest {
            // given
            coEvery { sendConnectionRequest(any()) } returns SendConnectionRequestResult.Success

            // when
            otherUserProfileScreenViewModel.sendConnectionRequest()

            // then
            coVerify {
                sendConnectionRequest(eq(USER_ID))
            }
            assertEquals(ConnectionStatus.Sent, otherUserProfileScreenViewModel.state.connectionStatus)
        }

    @Test
    fun `given a userId, when sending a connection request a fails, then returns a Failure result and update view error state`() =
        runTest {
            // given
            coEvery { sendConnectionRequest(any()) } returns SendConnectionRequestResult.Failure(Unknown(RuntimeException("some error")))

            // when
            otherUserProfileScreenViewModel.sendConnectionRequest()

            // then
            coVerify {
                sendConnectionRequest(eq(USER_ID))
                navigationManager wasNot Called
            }
            assertNotNull(otherUserProfileScreenViewModel.connectionOperationState)
        }

    @Test
    fun `given a userId, when ignoring a connection request, then returns a Success result and update view state`() =
        runTest {
            // given
            coEvery { ignoreConnectionRequest(any()) } returns IgnoreConnectionRequestUseCaseResult.Success

            // when
            otherUserProfileScreenViewModel.ignoreConnectionRequest()

            // then
            coVerify {
                ignoreConnectionRequest(eq(USER_ID))
            }
            assertEquals(ConnectionStatus.NotConnected, otherUserProfileScreenViewModel.state.connectionStatus)
        }

    @Test
    fun `given a userId, when canceling a connection request, then returns a Success result and update view state`() =
        runTest {
            // given
            coEvery { cancelConnectionRequest(any()) } returns CancelConnectionRequestUseCaseResult.Success

            // when
            otherUserProfileScreenViewModel.cancelConnectionRequest()

            // then
            coVerify {
                cancelConnectionRequest(eq(USER_ID))
            }
            assertEquals(ConnectionStatus.NotConnected, otherUserProfileScreenViewModel.state.connectionStatus)
        }

    @Test
    fun `given a userId, when accepting a connection request, then returns a Success result and update view state`() =
        runTest {
            // given
            coEvery { acceptConnectionRequest(any()) } returns AcceptConnectionRequestUseCaseResult.Success

            // when
            otherUserProfileScreenViewModel.acceptConnectionRequest()

            // then
            coVerify {
                acceptConnectionRequest(eq(USER_ID))
            }
            assertEquals(ConnectionStatus.Connected, otherUserProfileScreenViewModel.state.connectionStatus)
        }

    @Test
    fun `given a conversationId, when trying to open the conversation, then returns a Success result with the conversation`() =
        runTest {
            // given
            coEvery { getOrCreateOneToOneConversation(USER_ID) } returns CreateConversationResult.Success(CONVERSATION)
            coEvery { navigationManager.navigate(command = any()) } returns Unit

            // when
            otherUserProfileScreenViewModel.openConversation()

            // then
            coVerify {
                getOrCreateOneToOneConversation(USER_ID)
                navigationManager.navigate(any())
            }
        }

    @Test
    fun `given a conversationId, when trying to open the conversation and fails, then returns a Failure result and update error state`() =
        runTest {
            // given
            coEvery { getOrCreateOneToOneConversation(USER_ID) } returns
                    CreateConversationResult.Failure(Unknown(RuntimeException("some error")))

            // when
            otherUserProfileScreenViewModel.openConversation()

            // then
            coVerify {
                getOrCreateOneToOneConversation(USER_ID)
                navigationManager wasNot Called
            }
        }

    @Test
    fun `given a navigation case, when going back requested, then should delegate call to manager navigateBack`() = runTest {
        otherUserProfileScreenViewModel.navigateBack()

        coVerify(exactly = 1) { navigationManager.navigateBack() }
    }

    // todo: add tests for cancel request

    companion object {
        val USER_ID = UserId("some_value", "some_domain")
        val CONVERSATION_ID = ConversationId("some_value", "some_domain")
        val OTHER_USER = OtherUser(
            USER_ID,
            "some_name",
            "some_handle",
            "some_email",
            "some_phone",
            1,
            "some_team",
            ConnectionState.NOT_CONNECTED,
            null,
            null,
            UserAvailabilityStatus.AVAILABLE,
            UserType.INTERNAL
        )
        val TEAM = Team("some_id", null)
        val CONVERSATION = Conversation(
            CONVERSATION_ID,
            "some_name",
            Conversation.Type.ONE_ON_ONE,
            null,
            protocol = ProtocolInfo.Proteus,
            MutedConversationStatus.AllAllowed,
            null,
            null
        )
    }
}
