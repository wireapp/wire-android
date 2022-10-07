package com.wire.android.ui.userprofile.other

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.framework.TestUser
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.EXTRA_USER_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveConversationRoleForUserUseCase
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.client.GetOtherUserClientsUseCase
import com.wire.kalium.logic.feature.client.PersistOtherUserClientsUseCase
import com.wire.kalium.logic.feature.connection.AcceptConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.AcceptConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.BlockUserResult
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.connection.CancelConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.CancelConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.IgnoreConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.IgnoreConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.conversation.CreateConversationResult
import com.wire.kalium.logic.feature.conversation.GetOneToOneConversationUseCase
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleResult
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoResult
import com.wire.kalium.logic.feature.user.ObserveUserInfoUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf

internal class OtherUserProfileViewModelArrangement {
    @MockK
    lateinit var navigationManager: NavigationManager

    @MockK
    lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var getOrCreateOneToOneConversation: GetOrCreateOneToOneConversationUseCase

    @MockK
    lateinit var observeUserInfo: ObserveUserInfoUseCase

    @MockK
    lateinit var sendConnectionRequest: SendConnectionRequestUseCase

    @MockK
    lateinit var cancelConnectionRequest: CancelConnectionRequestUseCase

    @MockK
    lateinit var acceptConnectionRequest: AcceptConnectionRequestUseCase

    @MockK
    lateinit var ignoreConnectionRequest: IgnoreConnectionRequestUseCase

    @MockK
    lateinit var wireSessionImageLoader: WireSessionImageLoader

    @MockK
    lateinit var observeConversationRoleForUserUseCase: ObserveConversationRoleForUserUseCase

    @MockK
    lateinit var userTypeMapper: UserTypeMapper

    @MockK
    lateinit var qualifiedIdMapper: QualifiedIdMapper

    @MockK
    lateinit var updateConversationMemberRoleUseCase: UpdateConversationMemberRoleUseCase

    @MockK
    lateinit var removeMemberFromConversationUseCase: RemoveMemberFromConversationUseCase

    @MockK
    lateinit var observeSelfUser: GetSelfUserUseCase

    @MockK
    lateinit var blockUser: BlockUserUseCase

    @MockK
    lateinit var unblockUser: UnblockUserUseCase

    @MockK
    lateinit var updateConversationMutedStatus: UpdateConversationMutedStatusUseCase

    @MockK
    lateinit var otherUserClients: GetOtherUserClientsUseCase

    @MockK
    lateinit var persistOtherUserClientsUseCase: PersistOtherUserClientsUseCase

    @MockK
    lateinit var getConversationUseCase: GetOneToOneConversationUseCase

    private val viewModel by lazy {
        OtherUserProfileScreenViewModel(
            savedStateHandle,
            navigationManager,
            TestDispatcherProvider(),
            updateConversationMutedStatus,
            blockUser,
            unblockUser,
            getOrCreateOneToOneConversation,
            getConversationUseCase,
            observeUserInfo,
            sendConnectionRequest,
            cancelConnectionRequest,
            acceptConnectionRequest,
            ignoreConnectionRequest,
            userTypeMapper,
            wireSessionImageLoader,
            observeConversationRoleForUserUseCase,
            removeMemberFromConversationUseCase,
            updateConversationMemberRoleUseCase,
            otherUserClients,
            persistOtherUserClientsUseCase,
            qualifiedIdMapper
        )
    }

    init {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockUri()
        every { savedStateHandle.get<String>(eq(EXTRA_USER_ID)) } returns OtherUserProfileScreenViewModelTest.CONVERSATION_ID.toString()
        every { savedStateHandle.get<String>(eq(EXTRA_CONVERSATION_ID)) } returns
                OtherUserProfileScreenViewModelTest.CONVERSATION_ID.toString()
        coEvery {
            observeConversationRoleForUserUseCase.invoke(any(), any())
        } returns flowOf(OtherUserProfileScreenViewModelTest.CONVERSATION_ROLE_DATA)
        coEvery { observeUserInfo(any()) } returns flowOf(
            GetUserInfoResult.Success(
                OtherUserProfileScreenViewModelTest.OTHER_USER,
                OtherUserProfileScreenViewModelTest.TEAM
            )
        )
        coEvery { observeSelfUser() } returns flowOf(TestUser.SELF_USER)
        every { userTypeMapper.toMembership(any()) } returns Membership.None
        coEvery {
            qualifiedIdMapper.fromStringToQualifiedID("some_value@some_domain")
        } returns QualifiedID("some_value", "some_domain")
        coEvery { getOrCreateOneToOneConversation(OtherUserProfileScreenViewModelTest.USER_ID) } returns CreateConversationResult.Success(
            OtherUserProfileScreenViewModelTest.CONVERSATION
        )
        coEvery { navigationManager.navigate(command = any()) } returns Unit
    }

    suspend fun withBlockUserResult(result: BlockUserResult) = apply {
        coEvery { blockUser(any()) } returns result
    }

    suspend fun withUpdateConversationMemberRole(result: UpdateConversationMemberRoleResult) = apply {
        coEvery { updateConversationMemberRoleUseCase(any(), any(), any()) } returns result
    }

    fun withConversationIdInSavedState(conversationIdString: String?) = apply {
        every { savedStateHandle.get<String>(eq(EXTRA_CONVERSATION_ID)) } returns conversationIdString
    }

    fun withGetOneToOneConversation(result: CreateConversationResult) = apply {
        coEvery { getOrCreateOneToOneConversation(OtherUserProfileScreenViewModelTest.USER_ID) } returns result
    }

    fun withAcceptConnectionRequest(result: AcceptConnectionRequestUseCaseResult) = apply {
        coEvery { acceptConnectionRequest(any()) } returns result
    }

    fun withCancelConnectionRequest(result: CancelConnectionRequestUseCaseResult) = apply {
        coEvery { cancelConnectionRequest(any()) } returns result
    }

    fun withIgnoreConnectionRequest(result: IgnoreConnectionRequestUseCaseResult) = apply {
        coEvery { ignoreConnectionRequest(any()) } returns result
    }

    fun withSendConnectionRequest(result: SendConnectionRequestResult) = apply {
        coEvery { sendConnectionRequest(any()) } returns result
    }

    suspend fun withUserInfo(result: GetUserInfoResult) = apply {
        coEvery { observeUserInfo(any()) } returns flowOf(result)
    }

    fun withGetConversationDetails(result: GetOneToOneConversationUseCase.Result) = apply {
        coEvery { getConversationUseCase(any()) } returns result
    }

    fun arrange() = this to viewModel
}
