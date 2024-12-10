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
package com.wire.android.debug

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.datastore.GlobalDataStore
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.functional.Either
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class DatabaseProfilingManagerTest {

    @Test
    fun `given valid session and logging enabled, when observing, then profiling should be enabled`() =
        runTest {
            // given
            val account = AccountInfo.Valid(UserId("user", "domain"))
            val (arrangement, databaseProfilingManager) = Arrangement()
                .withAllValidSessions(flowOf(Either.Right(listOf(account))))
                .withIsLoggingEnabled(flowOf(true))
                .arrange()

            // when
            val job = launch {
                databaseProfilingManager.observeAndUpdateProfiling()
            }
            advanceUntilIdle()
            // then
            assertEquals(true, arrangement.profilingValues[account.userId])
            job.cancel()
        }

    @Test
    fun `given valid session and logging disabled, when observing, then profiling is disabled`() =
        runTest {
            // given
            val account = AccountInfo.Valid(UserId("user", "domain"))
            val (arrangement, databaseProfilingManager) = Arrangement()
                .withAllValidSessions(flowOf(Either.Right(listOf(account))))
                .withIsLoggingEnabled(flowOf(false))
                .arrange()
            // when
            val job = launch {
                databaseProfilingManager.observeAndUpdateProfiling()
            }
            advanceUntilIdle()
            // then
            assertEquals(false, arrangement.profilingValues[account.userId])
            job.cancel()
        }

    @Test
    fun `given valid session, when observing and logging changes from disabled to enabled, then profiling is enabled`() =
        runTest {
            // given
            val account = AccountInfo.Valid(UserId("user", "domain"))
            val (arrangement, databaseProfilingManager) = Arrangement()
                .withAllValidSessions(flowOf(Either.Right(listOf(account))))
                .withIsLoggingEnabled(flowOf(false))
                .arrange()
            // when
            val job = launch {
                databaseProfilingManager.observeAndUpdateProfiling()
            }
            arrangement.withIsLoggingEnabled(flowOf(true))
            advanceUntilIdle()
            // then
            assertEquals(true, arrangement.profilingValues[account.userId])
            job.cancel()
        }

    @Test
    fun `given two valid sessions, when observing and logging changes from disabled to enabled, then profiling is enabled for both`() =
        runTest {
            // given
            val account1 = AccountInfo.Valid(UserId("user1", "domain"))
            val account2 = AccountInfo.Valid(UserId("user2", "domain"))
            val (arrangement, databaseProfilingManager) = Arrangement()
                .withAllValidSessions(flowOf(Either.Right(listOf(account1, account2))))
                .withIsLoggingEnabled(flowOf(false))
                .arrange()
            // when
            val job = launch {
                databaseProfilingManager.observeAndUpdateProfiling()
            }
            arrangement.withIsLoggingEnabled(flowOf(true))
            advanceUntilIdle()
            // then
            assertEquals(true, arrangement.profilingValues[account1.userId])
            assertEquals(true, arrangement.profilingValues[account2.userId])
            job.cancel()
        }

    @Test
    fun `given valid session and logging enabled, when observing and new session appears, then profiling is enabled for both`() =
        runTest {
            // given
            val account1 = AccountInfo.Valid(UserId("user1", "domain"))
            val account2 = AccountInfo.Valid(UserId("user2", "domain"))
            val validSessionsFlow = MutableStateFlow(Either.Right(listOf(account1)))
            val (arrangement, databaseProfilingManager) = Arrangement()
                .withAllValidSessions(validSessionsFlow)
                .withIsLoggingEnabled(flowOf(true))
                .arrange()
            // when
            val job = launch {
                databaseProfilingManager.observeAndUpdateProfiling()
            }
            validSessionsFlow.value = Either.Right(listOf(account1, account2))
            advanceUntilIdle()
            // then
            assertEquals(true, arrangement.profilingValues[account1.userId])
            assertEquals(true, arrangement.profilingValues[account2.userId])
            job.cancel()
        }

    private class Arrangement {

        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        private lateinit var globalDataStore: GlobalDataStore

        var profilingValues: PersistentMap<UserId, Boolean> = persistentMapOf()
            private set

        init {
            MockKAnnotations.init(this, relaxed = true, relaxUnitFun = true)
            coEvery { coreLogic.getSessionScope(any()).debug.changeProfiling(any()) } answers {
                profilingValues = profilingValues.put(firstArg(), secondArg())
            }
            coEvery { coreLogic.getSessionScope(any()) } answers {
                val userId = firstArg<UserId>()
                mockk {
                    coEvery { debug.changeProfiling(any()) } answers {
                        val profilingValue = firstArg<Boolean>()
                        profilingValues = profilingValues.put(userId, profilingValue)
                    }
                }
            }
        }

        fun withIsLoggingEnabled(isLoggingEnabledFlow: Flow<Boolean>) = apply {
            coEvery { globalDataStore.isLoggingEnabled() } returns isLoggingEnabledFlow
        }

        fun withAllValidSessions(allValidSessionsFlow: Flow<Either<StorageFailure, List<AccountInfo>>>) = apply {
            coEvery { coreLogic.getGlobalScope().sessionRepository.allValidSessionsFlow() } returns allValidSessionsFlow
        }

        fun arrange() = this to DatabaseProfilingManager(coreLogic, globalDataStore)
    }
}
