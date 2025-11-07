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

package com.wire.android.ui.initialsync

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.datastore.UserDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class InitialSyncViewModelTest {

    @Test
    fun `given sync is live, when observing initial sync state, then navigate home`() = runTest {
        // given
        val (viewModel, _) = Arrangement()
            .withSyncState(SyncState.Live)
            .arrange()

        advanceUntilIdle()

        // then
        assertTrue(viewModel.isSyncCompleted)
    }

    @Test
    fun `given sync is not live, when observing initial sync state, then stay on this screen`() = runTest {
        // given
        val (viewModel, arrangement) = Arrangement()
            .withSyncState(SyncState.Waiting)
            .arrange()
        // when
        arrangement.withSyncState(SyncState.GatheringPendingEvents)
        arrangement.withSyncState(SyncState.SlowSync)

        advanceUntilIdle()

        // then
        assertFalse(viewModel.isSyncCompleted)
    }

    private class Arrangement {

        @MockK
        lateinit var observeSyncState: ObserveSyncStateUseCase

        @MockK
        lateinit var userDataStoreProvider: UserDataStoreProvider

        @MockK
        lateinit var userDataStore: UserDataStore
        val userId = UserId("id", "domain")

        val viewModel by lazy {
            InitialSyncViewModel(observeSyncState, userDataStoreProvider, userId, TestDispatcherProvider())
        }

        private val syncStateChannel = Channel<SyncState>(capacity = Channel.UNLIMITED)

        init {
            // Tests setup
            MockKAnnotations.init(this, relaxUnitFun = true)
            // Default empty values
            mockUri()
           coEvery { userDataStoreProvider.getOrCreate(any()) } returns userDataStore
        }

        suspend fun withSyncState(syncState: SyncState): Arrangement {
            every { observeSyncState.invoke() } returns syncStateChannel.consumeAsFlow()
            syncStateChannel.send(syncState)
            return this
        }

        fun arrange() = viewModel to this
    }
}
