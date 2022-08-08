package com.wire.android.ui.userprofile.other

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.framework.TestUser
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.EXTRA_USER_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.details.participants.usecase.ConversationRoleData
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveConversationRoleForUserUseCase
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.CoreFailure.Unknown
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.Member
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.connection.AcceptConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.AcceptConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.connection.CancelConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.CancelConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.IgnoreConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.IgnoreConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.conversation.CreateConversationResult
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoResult
import com.wire.kalium.logic.feature.user.GetUserInfoUseCase
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
    private lateinit var observeConversationRoleForUserUseCase: ObserveConversationRoleForUserUseCase

    @MockK
    private lateinit var userTypeMapper: UserTypeMapper

    @MockK
    private lateinit var qualifiedIdMapper: QualifiedIdMapper

    @MockK
    private lateinit var observeSelfUser: GetSelfUserUseCase

    @MockK
    private lateinit var blockUser: BlockUserUseCase

    @MockK
    private lateinit var updateConversationMutedStatus: UpdateConversationMutedStatusUseCase

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockUri()
        every { savedStateHandle.get<String>(eq(EXTRA_USER_ID)) } returns CONVERSATION_ID.toString()
        every { savedStateHandle.get<String>(eq(EXTRA_CONVERSATION_ID)) } returns CONVERSATION_ID.toString()
        coEvery { observeConversationRoleForUserUseCase.invoke(any(), any()) } returns flowOf(CONVERSATION_ROLE_DATA)
        coEvery { getUserInfo(any()) } returns GetUserInfoResult.Success(OTHER_USER, TEAM)
        coEvery { observeSelfUser() } returns flowOf(TestUser.SELF_USER)
        every { userTypeMapper.toMembership(any()) } returns Membership.None
        coEvery {
            qualifiedIdMapper.fromStringToQualifiedID("some_value@some_domain")
        } returns QualifiedID("some_value", "some_domain")
        initViewModel()
    }

    private fun initViewModel() {
        otherUserProfileScreenViewModel = OtherUserProfileScreenViewModel(
            savedStateHandle,
            navigationManager,
            observeSelfUser,
            updateConversationMutedStatus,
            blockUser,
            getOrCreateOneToOneConversation,
            getUserInfo,
            sendConnectionRequest,
            cancelConnectionRequest,
            acceptConnectionRequest,
            ignoreConnectionRequest,
            userTypeMapper,
            wireSessionImageLoader,
            observeConversationRoleForUserUseCase,
            TestDispatcherProvider(),
            qualifiedIdMapper
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
            assertEquals(ConnectionState.SENT, otherUserProfileScreenViewModel.state.connectionStatus)
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
            assertNotNull(otherUserProfileScreenViewModel.snackBarState)
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
            assertEquals(ConnectionState.NOT_CONNECTED, otherUserProfileScreenViewModel.state.connectionStatus)
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
            assertEquals(ConnectionState.NOT_CONNECTED, otherUserProfileScreenViewModel.state.connectionStatus)
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
            assertEquals(ConnectionState.ACCEPTED, otherUserProfileScreenViewModel.state.connectionStatus)
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

    @Test
    fun `given a group conversationId, when loading the data, then return group state`() =
        runTest {
            // given
            val expected = OtherUserProfileGroupState("some_name", Member.Role.Member, false)
            every { savedStateHandle.get<String>(eq(EXTRA_CONVERSATION_ID)) } returns CONVERSATION_ID.toString()
            coEvery { getOrCreateOneToOneConversation(USER_ID) } returns CreateConversationResult.Success(CONVERSATION)
            initViewModel()
            // when
            val groupState = otherUserProfileScreenViewModel.state.groupState
            // then
            coVerify {
                observeConversationRoleForUserUseCase(CONVERSATION_ID, USER_ID)
                navigationManager wasNot Called
            }
            assertEquals(groupState, expected)
        }

    @Test
    fun `given no conversationId, when loading the data, then return null group state`() =
        runTest {
            // given
            every { savedStateHandle.get<String>(eq(EXTRA_CONVERSATION_ID)) } returns null
            initViewModel()
            // when
            val groupState = otherUserProfileScreenViewModel.state.groupState
            // then
            coVerify {
                observeConversationRoleForUserUseCase(any(), any()) wasNot Called
                navigationManager wasNot Called
            }
            assertEquals(groupState, null)
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
            TeamId("some_team"),
            ConnectionState.NOT_CONNECTED,
            null,
            null,
            UserType.INTERNAL,
            UserAvailabilityStatus.AVAILABLE,
            null
        )
        val TEAM = Team("some_id", null)
        val CONVERSATION = Conversation(
            id = CONVERSATION_ID,
            name = "some_name",
            type = Conversation.Type.ONE_ON_ONE,
            teamId = null,
            protocol = Conversation.ProtocolInfo.Proteus,
            mutedStatus = MutedConversationStatus.AllAllowed,
            lastNotificationDate = null,
            lastModifiedDate = null,
            lastReadDate = null,
            access = listOf(Conversation.Access.INVITE),
            accessRole = listOf(Conversation.AccessRole.NON_TEAM_MEMBER)
        )
        val CONVERSATION_ROLE_DATA = ConversationRoleData("some_name", Member.Role.Member, Member.Role.Member)
    }
}
