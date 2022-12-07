package com.wire.android.ui.home.conversations.details

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestUser
import com.wire.android.mapper.testUIParticipant
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetContent
import com.wire.android.ui.common.bottomsheet.conversation.ConversationTypeDetail
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsViewModelTest.Companion.dummyConversationId
import com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.LegalHoldStatus
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.feature.conversation.ClearConversationContentUseCase
import com.wire.kalium.logic.feature.conversation.ConversationUpdateStatusResult
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.team.DeleteTeamConversationUseCase
import com.wire.kalium.logic.feature.team.GetSelfTeamUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class GroupConversationDetailsViewModelTest {
    @Test
    fun `given a group conversation, when solving the conversation name, then the name of the conversation is used`() = runTest {
        // Given
        val members = buildList {
            for (i in 1..5) {
                add(testUIParticipant(i))
            }
        }
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size
        )

        val details = testGroup.copy(conversation = testGroup.conversation.copy(name = "group name"))
        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .arrange()

        // When - Then
        assertEquals(details.conversation.name, viewModel.groupOptionsState.value.groupName)
    }

    @Test
    fun `given the conversation name is updated, when solving the conversation name, then the state is updated accordingly`() = runTest {
        // Given
        val members = buildList {
            for (i in 1..5) {
                add(testUIParticipant(i))
            }
        }
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size
        )

        val details1 = testGroup.copy(conversation = testGroup.conversation.copy(name = "Group name 1"))
        val details2 = testGroup.copy(conversation = testGroup.conversation.copy(name = "Group name 2"))
        val (arrangement, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details1)
            .withConversationMembersUpdate(conversationParticipantsData)
            .arrange()

        // When - Then
        assertEquals(details1.conversation.name, viewModel.groupOptionsState.value.groupName)
        // When - Then
        arrangement.withConversationDetailUpdate(details2)
        assertEquals(details2.conversation.name, viewModel.groupOptionsState.value.groupName)
    }

    @Test
    fun `given a group conversation, when solving the state, then the state is correct`() = runTest {
        // Given
        val members = buildList {
            for (i in 1..5) {
                add(testUIParticipant(i))
            }
        }
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size,
            isSelfAnAdmin = true
        )

        val details = testGroup.copy(
            conversation = testGroup.conversation.copy(
                name = "group name",
                teamId = TeamId("team_id"),
                accessRole = listOf(
                    Conversation.AccessRole.GUEST,
                    Conversation.AccessRole.TEAM_MEMBER,
                    Conversation.AccessRole.NON_TEAM_MEMBER,
                    Conversation.AccessRole.SERVICE
                )
            )
        )
        val selfTeam = Team("team_id", "team_name", "icon")
        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withSelfTeamUseCaseReturns(selfTeam)
            .arrange()

        // When - Then
        assertEquals(details.conversation.name, viewModel.groupOptionsState.value.groupName)
        assertEquals(conversationParticipantsData.isSelfAnAdmin, viewModel.groupOptionsState.value.isUpdatingAllowed)
        assertEquals(details.conversation.name, viewModel.groupOptionsState.value.groupName)
        assertEquals(details.conversation.isTeamGroup(), viewModel.groupOptionsState.value.areAccessOptionsAvailable)
        assertEquals(
            (details.conversation.isGuestAllowed() || details.conversation.isNonTeamMemberAllowed()),
            viewModel.groupOptionsState.value.isGuestAllowed
        )
        assertEquals(
            conversationParticipantsData.isSelfAnAdmin && details.conversation.teamId?.value == selfTeam.id,
            viewModel.groupOptionsState.value.isUpdatingGuestAllowed
        )
    }

    @Test
    fun `when enabling Guests, then use case is called with the correct values`() = runTest {
        // Given
        val members = buildList {
            for (i in 1..5) {
                add(testUIParticipant(i))
            }
        }
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size
        )

        val details = testGroup

        val (arrangement, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withSavedStateConversationId(details.conversation.id)
            .withUpdateConversationAccessUseCaseReturns(
                UpdateConversationAccessRoleUseCase.Result.Success
            ).withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .arrange()

        viewModel.onGuestUpdate(true)
        coVerify(exactly = 1) {
            arrangement.updateConversationAccessRoleUseCase(
                conversationId = details.conversation.id,
                allowServices = any(),
                allowGuest = true,
                allowNonTeamMember = true
            )
        }
    }

    @Test
    fun `when disabling Guests , then the dialog must state must be updated`() = runTest {
        // Given
        val members = buildList {
            for (i in 1..5) {
                add(testUIParticipant(i))
            }
        }
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size
        )

        val details = testGroup

        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withSavedStateConversationId(details.conversation.id)
            .withUpdateConversationAccessUseCaseReturns(
                UpdateConversationAccessRoleUseCase.Result.Success
            ).withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .arrange()

        viewModel.onGuestUpdate(false)
        assertEquals(true, viewModel.groupOptionsState.value.changeGuestOptionConfirmationRequired)
    }

    @Test
    fun `when disable Guests guest dialog conferment, then use case is called with the correct values`() = runTest {
        // Given
        val members = buildList {
            for (i in 1..5) {
                add(testUIParticipant(i))
            }
        }
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size
        )

        val details = testGroup

        val (arrangement, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withSavedStateConversationId(details.conversation.id)
            .withUpdateConversationAccessUseCaseReturns(
                UpdateConversationAccessRoleUseCase.Result.Success
            ).withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .arrange()

        viewModel.onGuestDialogConfirm()
        assertEquals(false, viewModel.groupOptionsState.value.changeGuestOptionConfirmationRequired)
        coVerify(exactly = 1) {
            arrangement.updateConversationAccessRoleUseCase(
                conversationId = details.conversation.id,
                allowServices = any(),
                allowGuest = false,
                allowNonTeamMember = false
            )
        }
    }

    @Test
    fun `when disabling Services , then the dialog must state must be updated`() = runTest {
        // Given
        val members = buildList {
            for (i in 1..5) {
                add(testUIParticipant(i))
            }
        }
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size
        )

        val details = testGroup

        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withSavedStateConversationId(details.conversation.id)
            .withUpdateConversationAccessUseCaseReturns(
                UpdateConversationAccessRoleUseCase.Result.Success
            ).withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .arrange()

        viewModel.onServicesUpdate(false)
        assertEquals(true, viewModel.groupOptionsState.value.changeServiceOptionConfirmationRequired)
    }

    @Test
    fun `when enabling Services, use case is called with the correct values`() = runTest {
        // Given
        val members = buildList {
            for (i in 1..5) {
                add(testUIParticipant(i))
            }
        }
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size
        )

        val details = testGroup

        val (arrangement, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withSavedStateConversationId(details.conversation.id)
            .withUpdateConversationAccessUseCaseReturns(
                UpdateConversationAccessRoleUseCase.Result.Success
            ).withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .arrange()

        viewModel.onServicesUpdate(true)
        coVerify(exactly = 1) {
            arrangement.updateConversationAccessRoleUseCase(
                conversationId = details.conversation.id,
                allowServices = true,
                allowGuest = any(),
                allowNonTeamMember = any()
            )
        }
    }

    @Test
    fun `when disable Service guest dialog conferment, then use case is called with the correct values`() = runTest {
        // Given
        val members = buildList {
            for (i in 1..5) {
                add(testUIParticipant(i))
            }
        }
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size
        )

        val details = testGroup

        val (arrangement, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withSavedStateConversationId(details.conversation.id)
            .withUpdateConversationAccessUseCaseReturns(
                UpdateConversationAccessRoleUseCase.Result.Success
            ).withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .arrange()

        viewModel.onServiceDialogConfirm()
        assertEquals(false, viewModel.groupOptionsState.value.changeServiceOptionConfirmationRequired)
        coVerify(exactly = 1) {
            arrangement.updateConversationAccessRoleUseCase(
                conversationId = details.conversation.id,
                allowServices = false,
                allowGuest = any(),
                allowNonTeamMember = any()
            )
        }
    }

    @Test
    fun `given a group conversation, when self is admin and in owner team, then should be able to edit Guests option`() = runTest {
        // Given
        val conversationParticipantsData = ConversationParticipantsData(isSelfAnAdmin = true)
        val details = testGroup.copy(conversation = testGroup.conversation.copy(teamId = TeamId("team_id")))
        val selfTeam = Team("team_id", "team_name", "icon")
        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withSelfTeamUseCaseReturns(selfTeam)
            .arrange()

        // When - Then
        assertEquals(true, viewModel.groupOptionsState.value.isUpdatingGuestAllowed)
    }

    @Test
    fun `given a group conversation, when self is admin and not in owner team, then should not be able to edit Guests option`() = runTest {
        // Given
        val conversationParticipantsData = ConversationParticipantsData(isSelfAnAdmin = true)
        val details = testGroup.copy(conversation = testGroup.conversation.copy(teamId = TeamId("team_id")))
        val selfTeam = Team("other_team_id", "team_name", "icon")
        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withSelfTeamUseCaseReturns(selfTeam)
            .arrange()

        // When - Then
        assertEquals(false, viewModel.groupOptionsState.value.isUpdatingGuestAllowed)
    }

    @Test
    fun `given a group conversation, then conversationSheetContent is valid`() = runTest {
        // Given
        val conversationParticipantsData = ConversationParticipantsData(isSelfAnAdmin = true)
        val details = testGroup.copy(conversation = testGroup.conversation.copy(teamId = TeamId("team_id")))
        val selfTeam = Team("other_team_id", "team_name", "icon")
        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withSelfTeamUseCaseReturns(selfTeam)
            .arrange()

        val expected = ConversationSheetContent(
            title = details.conversation.name.orEmpty(),
            conversationId = details.conversation.id,
            mutingConversationState = details.conversation.mutedStatus,
            conversationTypeDetail = ConversationTypeDetail.Group(details.conversation.id, details.isSelfUserCreator),
            isSelfUserMember = true,
            isTeamConversation = details.conversation.isTeamGroup()
        )
        // When - Then
        assertEquals(expected, viewModel.conversationSheetContent)
    }

    companion object {
        val dummyConversationId = ConversationId("some-dummy-value", "some.dummy.domain")
        val testGroup = ConversationDetails.Group(
            Conversation(
                id = dummyConversationId,
                name = "Conv Name",
                type = Conversation.Type.ONE_ON_ONE,
                teamId = TeamId("team_id"),
                protocol = Conversation.ProtocolInfo.Proteus,
                mutedStatus = MutedConversationStatus.AllAllowed,
                removedBy = null,
                lastNotificationDate = null,
                lastModifiedDate = null,
                access = listOf(Conversation.Access.CODE, Conversation.Access.INVITE),
                accessRole = listOf(Conversation.AccessRole.NON_TEAM_MEMBER, Conversation.AccessRole.GUEST),
                lastReadDate = "2022-04-04T16:11:28.388Z",
                creatorId = null
            ),
            legalHoldStatus = LegalHoldStatus.DISABLED,
            hasOngoingCall = false,
            lastMessage = null,
            isSelfUserCreator = false,
            isSelfUserMember = true,
            unreadEventCount = emptyMap()
        )
    }
}

internal class GroupConversationDetailsViewModelArrangement {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var navigationManager: NavigationManager

    @MockK
    lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

    @MockK
    lateinit var deleteTeamConversation: DeleteTeamConversationUseCase

    @MockK
    lateinit var removeMemberFromConversation: RemoveMemberFromConversationUseCase

    @MockK
    lateinit var observerSelfUser: GetSelfUserUseCase

    @MockK
    lateinit var observeParticipantsForConversationUseCase: ObserveParticipantsForConversationUseCase

    @MockK
    lateinit var updateConversationAccessRoleUseCase: UpdateConversationAccessRoleUseCase

    @MockK
    lateinit var getSelfTeamUseCase: GetSelfTeamUseCase

    @MockK
    lateinit var updateConversationMutedStatus: UpdateConversationMutedStatusUseCase

    @MockK
    lateinit var clearConversationContentUseCase: ClearConversationContentUseCase

    @MockK
    private lateinit var qualifiedIdMapper: QualifiedIdMapper

    private val conversationDetailsChannel = Channel<ConversationDetails>(capacity = Channel.UNLIMITED)

    private val observeParticipantsForConversationChannel = Channel<ConversationParticipantsData>(capacity = Channel.UNLIMITED)

    private val viewModel by lazy {
        GroupConversationDetailsViewModel(
            navigationManager = navigationManager,
            dispatcher = TestDispatcherProvider(),
            observerSelfUser = observerSelfUser,
            observeConversationDetails = observeConversationDetails,
            deleteTeamConversation = deleteTeamConversation,
            removeMemberFromConversation = removeMemberFromConversation,
            observeConversationMembers = observeParticipantsForConversationUseCase,
            updateConversationAccessRole = updateConversationAccessRoleUseCase,
            getSelfTeam = getSelfTeamUseCase,
            savedStateHandle = savedStateHandle,
            qualifiedIdMapper = qualifiedIdMapper,
            updateConversationMutedStatus = updateConversationMutedStatus,
            clearConversationContent = clearConversationContentUseCase
        )
    }

    init {
        // Tests setup
        val dummyConversationId = dummyConversationId.toString()
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { savedStateHandle.get<String>(EXTRA_CONVERSATION_ID) } returns dummyConversationId
        // Default empty values
        coEvery { observeConversationDetails(any()) } returns flowOf()
        coEvery { observerSelfUser() } returns flowOf(TestUser.SELF_USER)
        coEvery { observeParticipantsForConversationUseCase(any(), any()) } returns flowOf()
        coEvery { getSelfTeamUseCase() } returns flowOf(null)
        coEvery {
            qualifiedIdMapper.fromStringToQualifiedID("some-dummy-value@some.dummy.domain")
        } returns QualifiedID("some-dummy-value", "some.dummy.domain")
        coEvery {
            qualifiedIdMapper.fromStringToQualifiedID("conv_id@domain")
        } returns QualifiedID("conv_id", "domain")
        coEvery { updateConversationMutedStatus(any(), any(), any()) } returns ConversationUpdateStatusResult.Success
    }

    fun withSavedStateConversationId(conversationId: ConversationId) = apply {
        every { savedStateHandle.get<String>(EXTRA_CONVERSATION_ID) } returns conversationId.toString()
    }

    suspend fun withConversationDetailUpdate(conversationDetails: ConversationDetails) = apply {
        coEvery { observeConversationDetails(any()) } returns conversationDetailsChannel.consumeAsFlow()
            .map { ObserveConversationDetailsUseCase.Result.Success(it) }
        conversationDetailsChannel.send(conversationDetails)
    }

    suspend fun withConversationMembersUpdate(conversationParticipantsData: ConversationParticipantsData) = apply {
        coEvery { observeParticipantsForConversationUseCase(any()) } returns observeParticipantsForConversationChannel.consumeAsFlow()
        observeParticipantsForConversationChannel.send(conversationParticipantsData)
    }

    suspend fun withUpdateConversationAccessUseCaseReturns(result: UpdateConversationAccessRoleUseCase.Result) = apply {
        coEvery { updateConversationAccessRoleUseCase(any(), any(), any(), any()) } returns result
    }

    suspend fun withSelfTeamUseCaseReturns(result: Team?) = apply {
        coEvery { getSelfTeamUseCase() } returns flowOf(result)
    }

    fun arrange() = this to viewModel
}
