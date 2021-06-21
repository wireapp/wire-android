package com.wire.android.feature.sync.ui

import androidx.lifecycle.MutableLiveData
import androidx.work.WorkInfo
import com.wire.android.UnitTest
import com.wire.android.core.functional.Either
import com.wire.android.feature.sync.slow.SlowSyncWorkHandler
import com.wire.android.feature.sync.slow.usecase.CheckSlowSyncRequiredUseCase
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.livedata.shouldBeUpdated
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SyncViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @MockK
    private lateinit var checkSlowSyncRequiredUseCase: CheckSlowSyncRequiredUseCase

    @MockK
    private lateinit var slowSyncWorkHandler: SlowSyncWorkHandler

    private lateinit var syncViewModel: SyncViewModel

    @Before
    fun setUp() {
        syncViewModel = SyncViewModel(checkSlowSyncRequiredUseCase, slowSyncWorkHandler, coroutinesTestRule.dispatcherProvider)
    }

    @Test
    fun `given startSync is called, when checkSlowSyncRequiredUseCase succeeds with true, then starts slow sync`() {
        coEvery { checkSlowSyncRequiredUseCase.run(Unit) } returns Either.Right(true)

        syncViewModel.startSync()

        coVerify { checkSlowSyncRequiredUseCase.run(Unit) }
        verify(exactly = 1) { slowSyncWorkHandler.enqueueWork() }
    }

    @Test
    fun `given startSync is called, when checkSlowSyncRequiredUseCase succeeds with false, then does not start slow sync`() {
        coEvery { checkSlowSyncRequiredUseCase.run(Unit) } returns Either.Right(false)

        syncViewModel.startSync()

        coVerify { checkSlowSyncRequiredUseCase.run(Unit) }
        verify { slowSyncWorkHandler wasNot Called }
    }

    @Test
    fun `given startSync is called, when checkSlowSyncRequiredUseCase fails, then does not start slow sync`() {
        coEvery { checkSlowSyncRequiredUseCase.run(Unit) } returns Either.Left(mockk())

        syncViewModel.startSync()

        coVerify { checkSlowSyncRequiredUseCase.run(Unit) }
        verify { slowSyncWorkHandler wasNot Called }
    }

    @Test
    fun `given slowSync is started, when slow sync work state changes, then updates syncStatusLiveData`() {
        coEvery { checkSlowSyncRequiredUseCase.run(Unit) } returns Either.Right(true)

        val workInfo = mockk<WorkInfo>()
        val workState = mockk<WorkInfo.State>()
        every { workInfo.state } returns workState

        val workInfoLiveData = MutableLiveData(workInfo)
        every { slowSyncWorkHandler.enqueueWork() } returns workInfoLiveData

        syncViewModel.startSync()

        syncViewModel.syncStatusLiveData shouldBeUpdated { it shouldBeEqualTo workState }
    }
}
