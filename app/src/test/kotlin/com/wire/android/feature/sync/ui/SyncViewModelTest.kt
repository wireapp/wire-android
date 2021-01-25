package com.wire.android.feature.sync.ui

import androidx.lifecycle.MutableLiveData
import androidx.work.WorkInfo
import com.wire.android.UnitTest
import com.wire.android.feature.sync.slow.SlowSyncWorkHandler
import com.wire.android.framework.livedata.shouldBeUpdated
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class SyncViewModelTest : UnitTest() {

    @MockK
    private lateinit var slowSyncWorkHandler: SlowSyncWorkHandler

    private lateinit var syncViewModel: SyncViewModel

    @Before
    fun setUp() {
        syncViewModel = SyncViewModel(slowSyncWorkHandler)
    }

    @Test
    fun `given startSync is called, then starts slow sync`() {
        syncViewModel.startSync()

        verify(exactly = 1) { slowSyncWorkHandler.enqueueWork() }
    }

    @Test
    fun `given slowSync is called, when slow sync work state changes, then updates syncStatusLiveData`() {
        val workInfo = mockk<WorkInfo>()
        val workState = mockk<WorkInfo.State>()
        every { workInfo.state } returns workState

        val workInfoLiveData = MutableLiveData(workInfo)
        every { slowSyncWorkHandler.enqueueWork() } returns workInfoLiveData

        syncViewModel.startSync()

        syncViewModel.syncStatusLiveData shouldBeUpdated { it shouldBeEqualTo workState }
    }
}
