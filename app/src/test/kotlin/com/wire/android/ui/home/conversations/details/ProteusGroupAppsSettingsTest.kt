/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.home.conversations.details

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData
import com.wire.android.ui.home.conversations.details.updateappsaccess.UpdateAppsAccessViewModelArrangement
import com.wire.android.ui.home.conversations.details.updateappsaccess.UpdateAppsAccessViewModelTest
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import com.wire.kalium.logic.feature.featureConfig.AppsAllowedProtocol
import com.wire.kalium.logic.feature.featureConfig.AppsAllowedResult
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ProteusGroupAppsSettingsTest {
    @Test
    fun `cancelled settings whitelist lookup cannot overwrite the current result`() = runTest {
        val oldResult = CompletableDeferred<Boolean>()
        val freshResult = CompletableDeferred<Boolean>()
        val group = UpdateAppsAccessViewModelTest.testGroup
        val arrangement = UpdateAppsAccessViewModelArrangement()
            .withConversationMembersUpdate(ConversationParticipantsData(isSelfAnAdmin = true))
            .withConversationDetailUpdate(group.copy(conversation = group.conversation.copy(accessRole = emptyList())))
        coEvery { arrangement.hasWhitelistedServices() } coAnswers { withContext(NonCancellable) { oldResult.await() } }
        val (_, viewModel) = arrangement.arrange()
        advanceUntilIdle()
        coVerify(exactly = 1) { arrangement.hasWhitelistedServices() }

        arrangement.withConversationMembersUpdate(ConversationParticipantsData(isSelfAnAdmin = false))
        advanceUntilIdle()
        coEvery { arrangement.hasWhitelistedServices() } coAnswers { freshResult.await() }
        arrangement.withConversationMembersUpdate(ConversationParticipantsData(isSelfAnAdmin = true))
        advanceUntilIdle()
        oldResult.complete(true)
        advanceUntilIdle()
        coVerify(exactly = 2) { arrangement.hasWhitelistedServices() }
        assertFalse(viewModel.updateAppsAccessState.isUpdatingAppAccessAllowed)

        freshResult.complete(true)
        advanceUntilIdle()
        assertTrue(viewModel.updateAppsAccessState.isUpdatingAppAccessAllowed)
    }

    @Test
    fun `off apps require whitelist and access management permission while on apps remain visible`() = runTest {
        val cases = listOf(
            listOf(false, false, true, true, false, false),
            listOf(false, true, true, true, true, true),
            listOf(false, true, false, true, false, false),
            listOf(false, true, true, false, false, false),
            listOf(true, false, true, true, true, true),
            listOf(true, false, false, true, true, false),
        )
        cases.forEach { values ->
            val appsOn = values[0]
            val bots = values[1]
            val admin = values[2]
            val owner = values[3]
            val details = GroupDetailsViewModelTest.testGroup.let {
                it.copy(
                    conversation = it.conversation.copy(
                        accessRole = if (appsOn) listOf(Conversation.AccessRole.SERVICE) else emptyList()
                    ),
                    selfRole = if (admin) Conversation.Member.Role.Admin else Conversation.Member.Role.Member,
                )
            }
            val (arrangement, viewModel) = GroupConversationDetailsViewModelArrangement()
                .withSelfTeamId(TeamId(if (owner) "team_id" else "other_team"))
                .withSelfTeamUseCaseReturns(Team(if (owner) "team_id" else "other_team", "Team", "icon"))
                .withWhitelistedServices(bots)
                .withAppsAllowedResult(AppsAllowedResult.Disabled)
                .withConversationDetailUpdate(details)
                .arrange()
            advanceUntilIdle()

            assertEquals(values[4], viewModel.groupOptionsState.value.isAppsVisible, "$values")
            assertEquals(values[5], viewModel.groupOptionsState.value.isUpdatingAppsAllowed, "$values")
            coVerify(exactly = if (admin && owner) 1 else 0) { arrangement.hasWhitelistedServices() }
        }
    }

    @Test
    fun `opening details again checks the whitelist for newly added bots`() = runTest {
        suspend fun groupDetails() = GroupConversationDetailsViewModelArrangement()
            .withSelfTeamId(TeamId("team_id"))
            .withSelfTeamUseCaseReturns(Team("team_id", "Team", "icon"))
            .withConversationDetailUpdate(GroupDetailsViewModelTest.testGroup.copy(selfRole = Conversation.Member.Role.Admin))

        val (firstArrangement, firstViewModel) = groupDetails().withWhitelistedServices(false).arrange()
        advanceUntilIdle()
        assertFalse(firstViewModel.groupOptionsState.value.isAppsAllowed)
        assertFalse(firstViewModel.groupOptionsState.value.isAppsVisible)
        coVerify(exactly = 1) { firstArrangement.hasWhitelistedServices() }

        val refreshed = CompletableDeferred<Boolean>()
        val reopenedArrangement = groupDetails()
        coEvery { reopenedArrangement.hasWhitelistedServices() } coAnswers { refreshed.await() }
        val (_, reopenedViewModel) = reopenedArrangement.arrange()
        advanceUntilIdle()
        assertFalse(reopenedViewModel.groupOptionsState.value.isAppsVisible)
        refreshed.complete(true)
        advanceUntilIdle()

        assertTrue(reopenedViewModel.groupOptionsState.value.isAppsVisible)
        assertFalse(reopenedViewModel.groupOptionsState.value.isAppsAllowed)
        coVerify(exactly = 1) { reopenedArrangement.hasWhitelistedServices() }
    }

    @Test
    fun `MLS details and toggle never query the legacy whitelist`() = runTest {
        val protocol = com.wire.android.framework.TestConversation.MLS_PROTOCOL_INFO
        val (detailsArrangement, _) = GroupConversationDetailsViewModelArrangement()
            .withSelfTeamId(TeamId("team_id"))
            .withSelfTeamUseCaseReturns(Team("team_id", "Team", "icon"))
            .withAppsAllowedResult(AppsAllowedResult.Enabled(AppsAllowedProtocol.MLS))
            .withConversationDetailUpdate(
                GroupDetailsViewModelTest.testGroup.let {
                    it.copy(conversation = it.conversation.copy(protocol = protocol), selfRole = Conversation.Member.Role.Admin)
                }
            )
            .arrange()
        val (toggleArrangement, _) = UpdateAppsAccessViewModelArrangement()
            .withAppsAllowedResult(AppsAllowedResult.Enabled(AppsAllowedProtocol.MLS))
            .withConversationMembersUpdate(ConversationParticipantsData(isSelfAnAdmin = true))
            .withConversationDetailUpdate(
                UpdateAppsAccessViewModelTest.testGroup.let { it.copy(conversation = it.conversation.copy(protocol = protocol)) }
            )
            .arrange()
        advanceUntilIdle()
        coVerify(exactly = 0) { detailsArrangement.hasWhitelistedServices() }
        coVerify(exactly = 0) { toggleArrangement.hasWhitelistedServices() }
    }

    @Test
    fun `on apps can be disabled without bots and cannot be enabled again through the stale toggle`() = runTest {
        val (arrangement, viewModel) = UpdateAppsAccessViewModelArrangement()
            .withWhitelistedServices(false)
            .withAppsAllowedResult(AppsAllowedResult.Disabled)
            .withConversationMembersUpdate(ConversationParticipantsData(isSelfAnAdmin = true))
            .withConversationDetailUpdate(UpdateAppsAccessViewModelTest.testGroup)
            .withUpdateConversationAccessUseCaseReturns(UpdateConversationAccessRoleUseCase.Result.Success)
            .arrange()
        advanceUntilIdle()
        assertTrue(viewModel.updateAppsAccessState.isAppAccessAllowed)
        assertTrue(viewModel.updateAppsAccessState.isUpdatingAppAccessAllowed)
        viewModel.onAppsAccessUpdate(false)
        assertTrue(viewModel.updateAppsAccessState.shouldShowDisableAppsConfirmationDialog)
        viewModel.onServiceDialogConfirm()
        advanceUntilIdle()
        assertFalse(viewModel.updateAppsAccessState.isAppAccessAllowed)
        assertFalse(viewModel.updateAppsAccessState.isUpdatingAppAccessAllowed)
        viewModel.onAppsAccessUpdate(true)
        viewModel.onServiceDialogConfirm()
        advanceUntilIdle()
        coVerify(exactly = 1) { arrangement.changeAccessForAppsInConversationUseCase(any(), any(), any()) }
    }

    @Test
    fun `whitelist emissions preserve a pending disable confirmation`() = runTest {
        val result = CompletableDeferred<Boolean>()
        val arrangement = UpdateAppsAccessViewModelArrangement()
            .withConversationMembersUpdate(ConversationParticipantsData(isSelfAnAdmin = true))
            .withConversationDetailUpdate(UpdateAppsAccessViewModelTest.testGroup)
        coEvery { arrangement.hasWhitelistedServices() } coAnswers { result.await() }
        val (_, viewModel) = arrangement.arrange()
        advanceUntilIdle()
        viewModel.onAppsAccessUpdate(false)
        assertTrue(viewModel.updateAppsAccessState.shouldShowDisableAppsConfirmationDialog)
        result.complete(true)
        advanceUntilIdle()
        assertTrue(viewModel.updateAppsAccessState.shouldShowDisableAppsConfirmationDialog)
        assertFalse(viewModel.updateAppsAccessState.isAppAccessAllowed)
        viewModel.onAppsDialogDismiss()
        assertTrue(viewModel.updateAppsAccessState.isAppAccessAllowed)
    }

    @Test
    fun `direct toggle can enable apps only with confirmed bots and owning team authorization`() = runTest {
        listOf(false, true).forEach { bots ->
            val details = UpdateAppsAccessViewModelTest.testGroup.let {
                it.copy(conversation = it.conversation.copy(accessRole = emptyList()))
            }
            val (arrangement, viewModel) = UpdateAppsAccessViewModelArrangement()
                .withWhitelistedServices(bots)
                .withAppsAllowedResult(AppsAllowedResult.Disabled)
                .withConversationMembersUpdate(ConversationParticipantsData(isSelfAnAdmin = true))
                .withConversationDetailUpdate(details)
                .withUpdateConversationAccessUseCaseReturns(UpdateConversationAccessRoleUseCase.Result.Success)
                .arrange()
            advanceUntilIdle()
            viewModel.onAppsAccessUpdate(true)
            advanceUntilIdle()
            coVerify(exactly = 1) { arrangement.hasWhitelistedServices() }
            coVerify(exactly = if (bots) 1 else 0) { arrangement.changeAccessForAppsInConversationUseCase(any(), any(), any()) }
        }
    }

    @Test
    fun `direct toggle cannot bypass ownership or admin permission for disabling apps`() = runTest {
        listOf(false, true).forEach { admin ->
            val arrangement = UpdateAppsAccessViewModelArrangement()
                .withWhitelistedServices(true)
                .withConversationMembersUpdate(ConversationParticipantsData(isSelfAnAdmin = admin))
                .withConversationDetailUpdate(UpdateAppsAccessViewModelTest.testGroup)
            coEvery { arrangement.observeSelfUser() } returns kotlinx.coroutines.flow.flowOf(
                TestUser.SELF_USER.copy(teamId = if (admin) TeamId("other_team") else TeamId("team_id"))
            )
            val (_, viewModel) = arrangement.arrange()
            advanceUntilIdle()
            assertFalse(viewModel.updateAppsAccessState.isUpdatingAppAccessAllowed)
            viewModel.onAppsAccessUpdate(false)
            viewModel.onServiceDialogConfirm()
            advanceUntilIdle()
            coVerify(exactly = 0) { arrangement.changeAccessForAppsInConversationUseCase(any(), any(), any()) }
        }
    }

    @Test
    fun `disable completion preserves access roles observed while the request was running`() = runTest {
        val result = CompletableDeferred<UpdateConversationAccessRoleUseCase.Result>()
        val original = UpdateAppsAccessViewModelTest.testGroup
        val arrangement = UpdateAppsAccessViewModelArrangement()
            .withWhitelistedServices(true)
            .withConversationMembersUpdate(ConversationParticipantsData(isSelfAnAdmin = true))
            .withConversationDetailUpdate(original)
        coEvery { arrangement.changeAccessForAppsInConversationUseCase(any(), any(), any()) } coAnswers { result.await() }
        val (_, viewModel) = arrangement.arrange()
        advanceUntilIdle()
        viewModel.onAppsAccessUpdate(false)
        viewModel.onServiceDialogConfirm()
        advanceUntilIdle()
        val latestRoles = listOf(Conversation.AccessRole.SERVICE, Conversation.AccessRole.EXTERNAL)
        arrangement.withConversationDetailUpdate(original.copy(conversation = original.conversation.copy(accessRole = latestRoles)))
        advanceUntilIdle()
        result.complete(UpdateConversationAccessRoleUseCase.Result.Success)
        advanceUntilIdle()
        coEvery { arrangement.changeAccessForAppsInConversationUseCase(any(), any(), any()) } returns
            UpdateConversationAccessRoleUseCase.Result.Success
        viewModel.onAppsAccessUpdate(true)
        advanceUntilIdle()
        coVerify(exactly = 1) {
            arrangement.changeAccessForAppsInConversationUseCase(
                conversationId = original.conversation.id,
                accessRoles = latestRoles.toSet(),
                access = original.conversation.access.toSet(),
            )
        }
    }

    @Test
    fun `revoking admin permission closes the confirmation and prevents a stale confirm callback`() = runTest {
        val (arrangement, viewModel) = UpdateAppsAccessViewModelArrangement()
            .withWhitelistedServices(true)
            .withConversationMembersUpdate(ConversationParticipantsData(isSelfAnAdmin = true))
            .withConversationDetailUpdate(UpdateAppsAccessViewModelTest.testGroup)
            .arrange()
        advanceUntilIdle()
        viewModel.onAppsAccessUpdate(false)
        assertTrue(viewModel.updateAppsAccessState.shouldShowDisableAppsConfirmationDialog)
        arrangement.withConversationMembersUpdate(ConversationParticipantsData(isSelfAnAdmin = false))
        advanceUntilIdle()
        assertFalse(viewModel.updateAppsAccessState.shouldShowDisableAppsConfirmationDialog)
        assertFalse(viewModel.updateAppsAccessState.isLoadingAppsOption)
        viewModel.onServiceDialogConfirm()
        advanceUntilIdle()
        coVerify(exactly = 0) { arrangement.changeAccessForAppsInConversationUseCase(any(), any(), any()) }
    }

    @Test
    fun `personal group direct route does not treat two absent team ids as ownership`() = runTest {
        val arrangement = UpdateAppsAccessViewModelArrangement()
            .withWhitelistedServices(true)
            .withConversationMembersUpdate(ConversationParticipantsData(isSelfAnAdmin = true))
            .withConversationDetailUpdate(
                UpdateAppsAccessViewModelTest.testGroup.let { it.copy(conversation = it.conversation.copy(teamId = null)) }
            )
        coEvery { arrangement.observeSelfUser() } returns kotlinx.coroutines.flow.flowOf(TestUser.SELF_USER.copy(teamId = null))
        val (_, viewModel) = arrangement.arrange()
        advanceUntilIdle()
        assertFalse(viewModel.updateAppsAccessState.isUpdatingAppAccessAllowed)
        viewModel.onAppsAccessUpdate(false)
        viewModel.onServiceDialogConfirm()
        advanceUntilIdle()
        coVerify(exactly = 0) { arrangement.hasWhitelistedServices() }
        coVerify(exactly = 0) { arrangement.changeAccessForAppsInConversationUseCase(any(), any(), any()) }
    }
}
