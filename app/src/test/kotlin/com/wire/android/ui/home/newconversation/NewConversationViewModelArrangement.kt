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
import com.wire.kalium.logic.data.conversation.ProtocolInfo
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.publicuser.model.OtherUser
import com.wire.kalium.logic.data.publicuser.model.UserSearchResult
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.conversation.CreateGroupConversationUseCase
import com.wire.kalium.logic.feature.publicuser.GetAllContactsResult
import com.wire.kalium.logic.feature.publicuser.GetAllContactsUseCase
import com.wire.kalium.logic.feature.publicuser.Result
import com.wire.kalium.logic.feature.publicuser.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.SearchUsersUseCase
import com.wire.kalium.logic.functional.Either
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK

internal class NewConversationViewModelArrangement {
    init {
        MockKAnnotations.init(this, relaxUnitFun = true)

        // Default empty values
        coEvery { searchUsers(any()) } returns Result.Success(userSearchResult = UserSearchResult(listOf(PUBLIC_USER)))
        coEvery { searchKnownUsers(any()) } returns Result.Success(userSearchResult = UserSearchResult(listOf(KNOWN_USER)))
        coEvery { getAllContacts() } returns GetAllContactsResult.Success(listOf())
        coEvery { createGroupConversation(any(), any(), any()) } returns Either.Right(CONVERSATION)
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
            protocol = ProtocolInfo.Proteus,
            MutedConversationStatus.AllAllowed,
            null,
            null
        )

        val PUBLIC_USER = OtherUser(
            TestUser.USER_ID.copy(value = "publicValue"),
            name = "publicUsername",
            handle = "publicHandle",
            email = "publicEmail",
            phone = "publicPhone",
            accentId = 0,
            team = "publicTeamId",
            connectionStatus = ConnectionState.ACCEPTED,
            previewPicture = UserAssetId("value", "domain"),
            completePicture = UserAssetId("value", "domain"),
            availabilityStatus = UserAvailabilityStatus.AVAILABLE,
            userType = UserType.FEDERATED,
        )

        val KNOWN_USER = OtherUser(
            TestUser.USER_ID.copy(value = "knownValue"),
            name = "knownUsername",
            handle = "knownHandle",
            email = "knownEmail",
            phone = "knownPhone",
            accentId = 0,
            team = "knownTeamId",
            connectionStatus = ConnectionState.ACCEPTED,
            previewPicture = UserAssetId("value", "domain"),
            completePicture = UserAssetId("value", "domain"),
            availabilityStatus = UserAvailabilityStatus.AVAILABLE,
            userType = UserType.FEDERATED,
        )
    }

    private val viewModel by lazy {
        NewConversationMembersViewModel(
            navigationManager = navigationManager,
            searchUsers = searchUsers,
            searchKnownUsers = searchKnownUsers,
            getAllContacts = getAllContacts,
            createGroupConversation = createGroupConversation,
            contactMapper = contactMapper,
            sendConnectionRequest = sendConnectionRequestUseCase,
            dispatchers = TestDispatcherProvider()
        )
    }

    fun withFailureKnownSearchResponse(): NewConversationViewModelArrangement {
        coEvery { searchKnownUsers(any()) } returns Result.Failure.InvalidRequest
        return this
    }

    fun withFailurePublicSearchResponse(): NewConversationViewModelArrangement {
        coEvery { searchUsers(any()) } returns Result.Failure.InvalidRequest
        return this
    }

    fun arrange() = this to viewModel
}
