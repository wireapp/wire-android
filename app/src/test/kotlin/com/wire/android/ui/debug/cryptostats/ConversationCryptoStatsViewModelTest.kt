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
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.wire.android.ui.debug.cryptostats

import com.wire.android.config.CoroutineTestExtension
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.debug.ConversationCryptoDetail
import com.wire.kalium.logic.feature.debug.ConversationCryptoProtocolType
import com.wire.kalium.logic.feature.debug.ConversationCryptoStats
import com.wire.kalium.logic.feature.debug.DetailGroupState
import com.wire.kalium.logic.feature.debug.GetConversationCryptoStatsResult
import com.wire.kalium.logic.feature.debug.GetConversationCryptoStatsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class ConversationCryptoStatsViewModelTest {

    @Test
    fun givenUseCaseReturnsSuccess_whenInitialising_thenStateContainsStats() = runTest {
        val stats = testStats()
        val (_, viewModel) = Arrangement()
            .withResult(GetConversationCryptoStatsResult.Success(stats))
            .arrange()

        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.error)
        assertNotNull(viewModel.state.value.stats)
        assertEquals(3, viewModel.state.value.stats?.totalConversations)
    }

    @Test
    fun givenUseCaseReturnsFailure_whenInitialising_thenStateContainsError() = runTest {
        val (_, viewModel) = Arrangement()
            .withResult(GetConversationCryptoStatsResult.Failure(CoreFailure.MissingClientRegistration))
            .arrange()

        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertNotNull(viewModel.state.value.error)
        assertNull(viewModel.state.value.stats)
    }

    @Test
    fun givenStatsLoaded_whenRefreshCalled_thenReloadsStats() = runTest {
        val firstStats = testStats()
        val secondStats = firstStats.copy(totalConversations = 5)
        val (_, viewModel) = Arrangement()
            .withResults(
                GetConversationCryptoStatsResult.Success(firstStats),
                GetConversationCryptoStatsResult.Success(secondStats),
            )
            .arrange()

        advanceUntilIdle()
        assertEquals(3, viewModel.state.value.stats?.totalConversations)

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(5, viewModel.state.value.stats?.totalConversations)
    }

    @Test
    fun givenStatsLoaded_whenProtocolFilterSet_thenFilteredDetailsMatch() = runTest {
        val (_, viewModel) = Arrangement()
            .withResult(GetConversationCryptoStatsResult.Success(testStats()))
            .arrange()

        advanceUntilIdle()

        viewModel.setProtocolFilter(ProtocolFilter.MLS)
        val filtered = viewModel.filteredDetails()
        assertEquals(1, filtered.size)
        assertEquals("MLS", filtered.first().protocolType)
    }

    @Test
    fun givenStatsLoaded_whenNotEstablishedFilterSet_thenFilteredDetailsMatch() = runTest {
        val (_, viewModel) = Arrangement()
            .withResult(GetConversationCryptoStatsResult.Success(testStats()))
            .arrange()

        advanceUntilIdle()

        viewModel.setEstablishmentFilter(EstablishmentFilter.NOT_ESTABLISHED)
        val filtered = viewModel.filteredDetails()
        assertEquals(1, filtered.size)
        assertEquals("No", filtered.first().establishedInCrypto)
    }

    @Test
    fun givenStatsLoaded_whenBothFiltersSet_thenFilteredDetailsMatchBoth() = runTest {
        val (_, viewModel) = Arrangement()
            .withResult(GetConversationCryptoStatsResult.Success(testStats()))
            .arrange()

        advanceUntilIdle()

        viewModel.setProtocolFilter(ProtocolFilter.MIXED)
        viewModel.setEstablishmentFilter(EstablishmentFilter.ESTABLISHED)
        val filtered = viewModel.filteredDetails()
        assertEquals(1, filtered.size)
        assertEquals("Mixed", filtered.first().protocolType)
        assertEquals("Yes", filtered.first().establishedInCrypto)
    }

    private class Arrangement {

        @MockK
        lateinit var getConversationCryptoStatsUseCase: GetConversationCryptoStatsUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withResult(result: GetConversationCryptoStatsResult) = apply {
            coEvery { getConversationCryptoStatsUseCase() } returns result
        }

        fun withResults(vararg results: GetConversationCryptoStatsResult) = apply {
            coEvery { getConversationCryptoStatsUseCase() } returnsMany results.toList()
        }

        fun arrange() = this to ConversationCryptoStatsViewModel(
            getConversationCryptoStats = getConversationCryptoStatsUseCase,
        )
    }

    private fun testStats() = ConversationCryptoStats(
        totalConversations = 3,
        proteusCount = 1,
        mlsCount = 1,
        mixedCount = 1,
        mlsNotEstablishedInCrypto = 1,
        mixedNotEstablishedInCrypto = 0,
        conversationDetails = listOf(
            ConversationCryptoDetail(
                conversationId = ConversationId("conv1", "domain"),
                conversationName = "Proteus Conv",
                protocolType = ConversationCryptoProtocolType.PROTEUS,
                groupId = null,
                dbGroupState = null,
                dbEpoch = null,
                ccEpoch = null,
                establishedInCrypto = null,
            ),
            ConversationCryptoDetail(
                conversationId = ConversationId("conv2", "domain"),
                conversationName = "MLS Conv",
                protocolType = ConversationCryptoProtocolType.MLS,
                groupId = "group-mls",
                dbGroupState = DetailGroupState.PENDING_JOIN,
                dbEpoch = 10UL,
                ccEpoch = 42UL,
                establishedInCrypto = false,
            ),
            ConversationCryptoDetail(
                conversationId = ConversationId("conv3", "domain"),
                conversationName = "Mixed Conv",
                protocolType = ConversationCryptoProtocolType.MIXED,
                groupId = "group-mixed",
                dbGroupState = DetailGroupState.ESTABLISHED,
                dbEpoch = 5UL,
                ccEpoch = 5UL,
                establishedInCrypto = true,
            ),
        ),
    )
}
