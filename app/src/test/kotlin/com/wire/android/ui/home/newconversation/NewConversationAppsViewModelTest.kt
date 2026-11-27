/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

@file:Suppress("MaxLineLength")

package com.wire.android.ui.home.newconversation

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.ui.home.newconversation.NewConversationViewModelArrangement.Companion.CONVERSATION
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.CreateConversationParam
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.feature.conversation.createconversation.ConversationCreationResult
import com.wire.kalium.logic.feature.featureConfig.AppsAllowedProtocol
import com.wire.kalium.logic.feature.featureConfig.AppsAllowedResult
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class NewConversationAppsViewModelTest {

    @Test
    fun `apps availability follows the creation protocol and team capability`() = runTest {
        val cases = listOf(
            Triple(SupportedProtocol.MLS, AppsAllowedResult.Disabled, false),
            Triple(SupportedProtocol.MLS, AppsAllowedResult.Enabled(AppsAllowedProtocol.PROTEUS), false),
            Triple(SupportedProtocol.MLS, AppsAllowedResult.Enabled(AppsAllowedProtocol.MIXED(SupportedProtocol.PROTEUS)), false),
            Triple(SupportedProtocol.MLS, AppsAllowedResult.Enabled(AppsAllowedProtocol.MIXED(SupportedProtocol.MLS)), true),
            Triple(SupportedProtocol.MLS, AppsAllowedResult.Enabled(AppsAllowedProtocol.MLS), true),
            Triple(SupportedProtocol.PROTEUS, AppsAllowedResult.Enabled(AppsAllowedProtocol.PROTEUS), true),
            Triple(SupportedProtocol.PROTEUS, AppsAllowedResult.Enabled(AppsAllowedProtocol.MIXED(SupportedProtocol.PROTEUS)), true),
        )
        cases.forEach { (protocol, capability, expected) ->
            val (_, viewModel) = NewConversationViewModelArrangement()
                .withGetSelfUser(isTeamMember = true)
                .withDefaultProtocol(protocol)
                .withAppsAllowedResult(capability)
                .withWhitelistedServices(true)
                .arrange()

            viewModel.startGroupCreation(isChannel = false)
            advanceUntilIdle()
            assertEquals(expected, viewModel.groupOptionsState.isAppsAvailable, "$protocol / $capability")
            assertEquals(expected, viewModel.groupOptionsState.isAllowAppsEnabled, "$protocol / $capability")
        }
    }

    @Test
    fun `apps remain unavailable until the default protocol loads`() = runTest {
        val protocol = CompletableDeferred<SupportedProtocol>()
        val arrangement = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .withAppsAllowedResult(AppsAllowedResult.Enabled(AppsAllowedProtocol.MLS))
        coEvery { arrangement.getDefaultProtocol() } coAnswers { protocol.await() }
        val (_, viewModel) = arrangement.arrange()

        assertFalse(viewModel.groupOptionsState.isAppsAvailable)
        viewModel.onAllowServicesStatusChanged(true)
        assertFalse(viewModel.groupOptionsState.isAllowAppsEnabled)

        protocol.complete(SupportedProtocol.MLS)
        advanceUntilIdle()
        assertTrue(viewModel.groupOptionsState.isAppsAvailable)
        assertTrue(viewModel.groupOptionsState.isAllowAppsEnabled)
    }

    @Test
    fun `apps remain unavailable until the team capability loads`() = runTest {
        val capability = CompletableDeferred<AppsAllowedResult>()
        val arrangement = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .withDefaultProtocol(SupportedProtocol.MLS)
        coEvery { arrangement.observeIsAppsAllowedForUsage() } returns flow { emit(capability.await()) }
        val (_, viewModel) = arrangement.arrange()

        assertFalse(viewModel.groupOptionsState.isAppsAvailable)
        assertFalse(viewModel.groupOptionsState.isAllowAppsEnabled)
        capability.complete(AppsAllowedResult.Enabled(AppsAllowedProtocol.MLS))
        advanceUntilIdle()
        assertTrue(viewModel.groupOptionsState.isAppsAvailable)
        assertTrue(viewModel.groupOptionsState.isAllowAppsEnabled)
    }

    @Test
    fun `capability updates preserve apps selection and clear it when revoked`() = runTest {
        val capabilities = MutableSharedFlow<AppsAllowedResult>(replay = 1)
        val enabled = AppsAllowedResult.Enabled(AppsAllowedProtocol.MLS)
        capabilities.emit(enabled)
        val arrangement = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .withDefaultProtocol(SupportedProtocol.MLS)
        coEvery { arrangement.observeIsAppsAllowedForUsage() } returns capabilities
        val (_, viewModel) = arrangement.arrange()

        viewModel.onAllowServicesStatusChanged(false)
        capabilities.emit(enabled)
        advanceUntilIdle()
        assertTrue(viewModel.groupOptionsState.isAppsAvailable)
        assertFalse(viewModel.groupOptionsState.isAllowAppsEnabled)

        viewModel.onAllowServicesStatusChanged(true)
        capabilities.emit(AppsAllowedResult.Enabled(AppsAllowedProtocol.MIXED(SupportedProtocol.PROTEUS)))
        advanceUntilIdle()
        assertFalse(viewModel.groupOptionsState.isAppsAvailable)
        assertFalse(viewModel.groupOptionsState.isAllowAppsEnabled)
        viewModel.onAllowServicesStatusChanged(true)
        assertFalse(viewModel.groupOptionsState.isAllowAppsEnabled)
    }

    @Test
    fun `reset restores apps defaults without another capability observer`() = runTest {
        val capabilities = MutableSharedFlow<AppsAllowedResult>(replay = 1)
        capabilities.emit(AppsAllowedResult.Enabled(AppsAllowedProtocol.MLS))
        val arrangement = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .withDefaultProtocol(SupportedProtocol.MLS)
        coEvery { arrangement.observeIsAppsAllowedForUsage() } returns capabilities
        val (_, viewModel) = arrangement.arrange()

        viewModel.onAllowServicesStatusChanged(false)
        viewModel.resetState()
        advanceUntilIdle()

        assertTrue(viewModel.groupOptionsState.isAppsAvailable)
        assertTrue(viewModel.groupOptionsState.isAllowAppsEnabled)
        assertEquals(1, capabilities.subscriptionCount.value)
        coVerify(exactly = 1) { arrangement.observeIsAppsAllowedForUsage() }
    }

    @Test
    fun `unavailable apps cannot enter group or channel creation requests`() = runTest {
        val (arrangement, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .withDefaultProtocol(SupportedProtocol.MLS)
            .withAppsAllowedResult(AppsAllowedResult.Enabled(AppsAllowedProtocol.MIXED(SupportedProtocol.PROTEUS)))
            .arrange()
        coEvery { arrangement.createChannel(any(), any(), any()) } returns ConversationCreationResult.Success(CONVERSATION)

        viewModel.onAllowServicesStatusChanged(true)
        assertFalse(viewModel.groupOptionsState.isAllowAppsEnabled)
        // Guard the request even if stale UI state contains an enabled selection.
        viewModel.groupOptionsState = viewModel.groupOptionsState.copy(isAllowAppsEnabled = true)
        viewModel.createGroup()
        viewModel.createChannel()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            arrangement.createRegularGroup(
                any(),
                any(),
                match {
                    it.protocol == CreateConversationParam.Protocol.MLS &&
                        Conversation.AccessRole.SERVICE !in it.accessRole.orEmpty()
                }
            )
            arrangement.createChannel(
                any(),
                any(),
                match {
                    it.protocol == CreateConversationParam.Protocol.MLS &&
                        Conversation.AccessRole.SERVICE !in it.accessRole.orEmpty()
                }
            )
        }
    }

    @Test
    fun `available apps selection controls the service role in creation requests`() = runTest {
        listOf(
            SupportedProtocol.MLS to AppsAllowedProtocol.MLS,
            SupportedProtocol.PROTEUS to AppsAllowedProtocol.PROTEUS,
        ).forEach { (protocol, appsProtocol) ->
            val (arrangement, viewModel) = NewConversationViewModelArrangement()
                .withGetSelfUser(isTeamMember = true)
                .withDefaultProtocol(protocol)
                .withAppsAllowedResult(AppsAllowedResult.Enabled(appsProtocol))
                .withWhitelistedServices(true)
                .arrange()
            coEvery { arrangement.createChannel(any(), any(), any()) } returns ConversationCreationResult.Success(CONVERSATION)

            viewModel.startGroupCreation(isChannel = false)
            advanceUntilIdle()
            viewModel.createGroup()
            viewModel.createChannel()
            advanceUntilIdle()
            viewModel.onAllowServicesStatusChanged(false)
            viewModel.createGroup()
            viewModel.createChannel()
            advanceUntilIdle()

            listOf(true, false).forEach { servicesAllowed ->
                coVerify(exactly = 1) {
                    arrangement.createRegularGroup(
                        any(),
                        any(),
                        match {
                            (Conversation.AccessRole.SERVICE in it.accessRole.orEmpty()) == servicesAllowed
                        }
                    )
                    arrangement.createChannel(
                        any(),
                        any(),
                        match {
                            (Conversation.AccessRole.SERVICE in it.accessRole.orEmpty()) == servicesAllowed
                        }
                    )
                }
            }
        }
    }

    @Test
    fun `proteus without whitelisted services hides apps and omits the service role`() = runTest {
        val (arrangement, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .withAppsAllowedResult(AppsAllowedResult.Enabled(AppsAllowedProtocol.PROTEUS))
            .withWhitelistedServices(false)
            .arrange()
        coEvery { arrangement.createChannel(any(), any(), any()) } returns ConversationCreationResult.Success(CONVERSATION)

        viewModel.startGroupCreation(isChannel = false)
        advanceUntilIdle()
        assertFalse(viewModel.groupOptionsState.isAppsAvailable)
        viewModel.onAllowServicesStatusChanged(true)
        assertFalse(viewModel.groupOptionsState.isAllowAppsEnabled)
        viewModel.createGroup()
        viewModel.createChannel()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            arrangement.createRegularGroup(
                any(),
                any(),
                match { Conversation.AccessRole.SERVICE !in it.accessRole.orEmpty() }
            )
            arrangement.createChannel(
                any(),
                any(),
                match { Conversation.AccessRole.SERVICE !in it.accessRole.orEmpty() }
            )
        }
    }

    @Test
    fun `proteus apps stay hidden while whitelist lookup is pending or unsuccessful`() = runTest {
        val result = CompletableDeferred<Boolean>()
        val arrangement = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .withAppsAllowedResult(AppsAllowedResult.Enabled(AppsAllowedProtocol.PROTEUS))
        coEvery { arrangement.hasWhitelistedServices() } coAnswers { result.await() }
        val (_, viewModel) = arrangement.arrange()

        viewModel.startGroupCreation(isChannel = false)
        advanceUntilIdle()
        assertFalse(viewModel.groupOptionsState.isAppsAvailable)
        viewModel.onAllowServicesStatusChanged(true)
        assertFalse(viewModel.groupOptionsState.isAllowAppsEnabled)
        result.complete(false)
        advanceUntilIdle()
        assertFalse(viewModel.groupOptionsState.isAppsAvailable)
        assertFalse(viewModel.groupOptionsState.isAllowAppsEnabled)
    }

    @Test
    fun `proteus whitelist is checked once per creation and refreshed when starting again`() = runTest {
        val capabilities = MutableSharedFlow<AppsAllowedResult>(replay = 1)
        val enabled = AppsAllowedResult.Enabled(AppsAllowedProtocol.PROTEUS)
        capabilities.emit(enabled)
        val arrangement = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .withWhitelistedServices(true)
        coEvery { arrangement.observeIsAppsAllowedForUsage() } returns capabilities
        val (_, viewModel) = arrangement.arrange()

        coVerify(exactly = 0) { arrangement.hasWhitelistedServices() }
        viewModel.startGroupCreation(isChannel = false)
        advanceUntilIdle()
        assertTrue(viewModel.groupOptionsState.isAppsAvailable)
        viewModel.onAllowServicesStatusChanged(false)
        capabilities.emit(enabled)
        advanceUntilIdle()
        assertFalse(viewModel.groupOptionsState.isAllowAppsEnabled)
        coVerify(exactly = 1) { arrangement.hasWhitelistedServices() }

        coEvery { arrangement.hasWhitelistedServices() } returns false
        viewModel.resetState()
        advanceUntilIdle()
        assertFalse(viewModel.groupOptionsState.isAppsAvailable)
        assertFalse(viewModel.groupOptionsState.isAllowAppsEnabled)
        coVerify(exactly = 1) { arrangement.hasWhitelistedServices() }

        viewModel.startGroupCreation(isChannel = true)
        advanceUntilIdle()
        assertFalse(viewModel.groupOptionsState.isAppsAvailable)
        coVerify(exactly = 2) { arrangement.hasWhitelistedServices() }
    }

    @Test
    fun `obsolete whitelist results cannot enable apps after reset or capability revocation`() = runTest {
        listOf(true, false).forEach { reset ->
            val oldResult = CompletableDeferred<Boolean>()
            val capabilities = MutableSharedFlow<AppsAllowedResult>(replay = 1)
            val enabled = AppsAllowedResult.Enabled(AppsAllowedProtocol.PROTEUS)
            capabilities.emit(enabled)
            val arrangement = NewConversationViewModelArrangement().withGetSelfUser(isTeamMember = true)
            coEvery { arrangement.observeIsAppsAllowedForUsage() } returns capabilities
            coEvery { arrangement.hasWhitelistedServices() } coAnswers {
                withContext(NonCancellable) { oldResult.await() }
            }
            val (_, viewModel) = arrangement.arrange()
            viewModel.startGroupCreation(isChannel = false)
            advanceUntilIdle()
            assertFalse(viewModel.groupOptionsState.isAppsAvailable)
            coEvery { arrangement.hasWhitelistedServices() } returns false

            if (reset) {
                viewModel.resetState()
                viewModel.startGroupCreation(isChannel = false)
            } else {
                capabilities.emit(AppsAllowedResult.Disabled)
                capabilities.emit(enabled)
            }
            advanceUntilIdle()
            oldResult.complete(true)
            advanceUntilIdle()

            assertFalse(viewModel.groupOptionsState.isAppsAvailable)
            assertFalse(viewModel.groupOptionsState.isAllowAppsEnabled)
            coVerify(exactly = 2) { arrangement.hasWhitelistedServices() }
        }
    }

    @Test
    fun `MLS creation never queries legacy service availability`() = runTest {
        val (arrangement, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .withDefaultProtocol(SupportedProtocol.MLS)
            .withAppsAllowedResult(AppsAllowedResult.Enabled(AppsAllowedProtocol.MLS))
            .arrange()

        viewModel.startGroupCreation(isChannel = false)
        advanceUntilIdle()
        assertTrue(viewModel.groupOptionsState.isAppsAvailable)
        viewModel.resetState()
        advanceUntilIdle()
        coVerify(exactly = 0) { arrangement.hasWhitelistedServices() }
    }
}
