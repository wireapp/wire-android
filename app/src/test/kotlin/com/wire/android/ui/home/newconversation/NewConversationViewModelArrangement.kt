package com.wire.android.ui.home.newconversation

import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestUser
import com.wire.android.mapper.ContactMapper
import com.wire.android.model.ImageAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.PlainId
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.publicuser.model.UserSearchResult
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.conversation.CreateGroupConversationUseCase
import com.wire.kalium.logic.feature.publicuser.GetAllContactsResult
import com.wire.kalium.logic.feature.publicuser.GetAllContactsUseCase
import com.wire.kalium.logic.feature.publicuser.search.Result
import com.wire.kalium.logic.feature.publicuser.search.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.search.SearchUsersUseCase
import com.wire.kalium.logic.feature.user.IsMLSEnabledUseCase
import com.wire.kalium.logic.CoreFailure
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK

internal class NewConversationViewModelArrangement {
    init {
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Default empty values
        coEvery { isMLSEnabledUseCase() } returns true
        coEvery { searchUsers(any()) } returns Result.Success(userSearchResult = UserSearchResult(listOf(PUBLIC_USER)))
        coEvery { searchKnownUsers(any()) } returns Result.Success(userSearchResult = UserSearchResult(listOf(KNOWN_USER)))
        coEvery { getAllContacts() } returns GetAllContactsResult.Success(listOf())
        coEvery { createGroupConversation(any(), any(), any()) } returns CreateGroupConversationUseCase.Result.Success(CONVERSATION)
        coEvery { contactMapper.fromOtherUser(PUBLIC_USER) } returns Contact(
            id = "publicValue",
            domain = "domain",
            name = "publicUsername",
            avatarData = UserAvatarData(
                asset = ImageAsset.UserAvatarAsset(wireSessionImageLoader, UserAssetId("value", "domain")),
                availabilityStatus = UserAvailabilityStatus.NONE
            ),
            label = "publicHandle",
            connectionState = ConnectionState.NOT_CONNECTED,
            membership = Membership.Federated
        )

        coEvery { contactMapper.fromOtherUser(KNOWN_USER) } returns Contact(
            id = "knownValue",
            domain = "domain",
            name = "knownUsername",
            avatarData = UserAvatarData(
                asset = ImageAsset.UserAvatarAsset(wireSessionImageLoader, UserAssetId("value", "domain")),
                availabilityStatus = UserAvailabilityStatus.NONE
            ),
            label = "knownHandle",
            connectionState = ConnectionState.NOT_CONNECTED,
            membership = Membership.Federated
        )
    }

    @MockK
    lateinit var navigationManager: NavigationManager

    @MockK
    lateinit var searchUsers: SearchUsersUseCase

    @MockK
    lateinit var searchKnownUsers: SearchKnownUsersUseCase

    @MockK
    lateinit var getAllContacts: GetAllContactsUseCase

    @MockK
    lateinit var createGroupConversation: CreateGroupConversationUseCase

    @MockK
    lateinit var sendConnectionRequestUseCase: SendConnectionRequestUseCase

    @MockK
    lateinit var isMLSEnabledUseCase: IsMLSEnabledUseCase

    @MockK
    lateinit var contactMapper: ContactMapper

    @MockK
    lateinit var wireSessionImageLoader: WireSessionImageLoader

    private companion object {
        val CONVERSATION_ID = ConversationId(value = "userId", domain = "domainId")
        val CONVERSATION = Conversation(
            id = CONVERSATION_ID,
            name = null,
            type = Conversation.Type.ONE_ON_ONE,
            teamId = null,
            protocol = Conversation.ProtocolInfo.Proteus,
            mutedStatus = MutedConversationStatus.AllAllowed,
            removedBy = null,
            lastNotificationDate = null,
            lastModifiedDate = null,
            lastReadDate = "2022-04-04T16:11:28.388Z",
            access = listOf(Conversation.Access.INVITE),
            accessRole = listOf(Conversation.AccessRole.NON_TEAM_MEMBER),
            creatorId = PlainId("")
        )

        val PUBLIC_USER = OtherUser(
            TestUser.USER_ID.copy(value = "publicValue"),
            name = "publicUsername",
            handle = "publicHandle",
            email = "publicEmail",
            phone = "publicPhone",
            accentId = 0,
            teamId = TeamId("publicTeamId"),
            connectionStatus = ConnectionState.ACCEPTED,
            previewPicture = UserAssetId("value", "domain"),
            completePicture = UserAssetId("value", "domain"),
            availabilityStatus = UserAvailabilityStatus.AVAILABLE,
            userType = UserType.FEDERATED,
            botService = null,
            deleted = false
        )

        val KNOWN_USER = OtherUser(
            TestUser.USER_ID.copy(value = "knownValue"),
            name = "knownUsername",
            handle = "knownHandle",
            email = "knownEmail",
            phone = "knownPhone",
            accentId = 0,
            teamId = TeamId("knownTeamId"),
            connectionStatus = ConnectionState.ACCEPTED,
            previewPicture = UserAssetId("value", "domain"),
            completePicture = UserAssetId("value", "domain"),
            availabilityStatus = UserAvailabilityStatus.AVAILABLE,
            userType = UserType.FEDERATED,
            botService = null,
            deleted = false
        )
    }

    private val viewModel by lazy {
        NewConversationViewModel(
            navigationManager = navigationManager,
            searchPublicUsers = searchUsers,
            searchKnownUsers = searchKnownUsers,
            getAllKnownUsers = getAllContacts,
            createGroupConversation = createGroupConversation,
            contactMapper = contactMapper,
            sendConnectionRequest = sendConnectionRequestUseCase,
            dispatchers = TestDispatcherProvider(),
            isMLSEnabled = isMLSEnabledUseCase
        )
    }

    fun withFailureKnownSearchResponse() = apply {
        coEvery { searchKnownUsers(any()) } returns Result.Failure.InvalidRequest
    }

    fun withFailurePublicSearchResponse() = apply {
        coEvery { searchUsers(any()) } returns Result.Failure.InvalidRequest
    }

    fun withSyncFailureOnCreatingGroup() = apply {
        coEvery { createGroupConversation(any(), any(), any()) } returns CreateGroupConversationUseCase.Result.SyncFailure
    }

    fun withUnknownFailureOnCreatingGroup() = apply {
        coEvery { createGroupConversation(any(), any(), any()) } returns CreateGroupConversationUseCase.Result.UnknownFailure(
            CoreFailure.MissingClientRegistration
        )
    }

    fun arrange() = this to viewModel
}
