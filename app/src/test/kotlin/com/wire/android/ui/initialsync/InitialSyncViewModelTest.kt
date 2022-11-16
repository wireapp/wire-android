package com.wire.android.ui.initialsync

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.datastore.UserDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.test.runTest
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
        // when
        viewModel.waitUntilSyncIsCompleted()
        // then
        verify(exactly = 1) { viewModel.navigateToConvScreen() }
    }

    @Test
    fun `given sync is not live, when observing initial sync state, then stay on this screen`() = runTest {
        // given
        val (viewModel, arrangement) = Arrangement()
            .withSyncState(SyncState.Waiting)
            .arrange()
        // when
        viewModel.waitUntilSyncIsCompleted()
        arrangement.withSyncState(SyncState.GatheringPendingEvents)
        arrangement.withSyncState(SyncState.SlowSync)
        // then
        verify(exactly = 0) { viewModel.navigateToConvScreen() }
    }

    private class Arrangement {

        @MockK
        lateinit var navigationManager: NavigationManager
        @MockK
        lateinit var observeSyncState: ObserveSyncStateUseCase
        @MockK
        lateinit var userDataStoreProvider: UserDataStoreProvider
        @MockK
        lateinit var userDataStore: UserDataStore
        val userId = UserId("id", "domain")

        val viewModel by lazy {
            InitialSyncViewModel(navigationManager, observeSyncState, userDataStoreProvider, userId, TestDispatcherProvider())
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
