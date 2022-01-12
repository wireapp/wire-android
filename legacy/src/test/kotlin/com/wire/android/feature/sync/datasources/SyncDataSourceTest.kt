package com.wire.android.feature.sync.datasources

import com.wire.android.UnitTest
import com.wire.android.feature.sync.datasources.local.SyncLocalDataSource
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

class SyncDataSourceTest : UnitTest() {

    @MockK
    private lateinit var syncLocalDataSource: SyncLocalDataSource

    private lateinit var syncDataSource: SyncDataSource

    @Before
    fun setUp() {
        syncDataSource = SyncDataSource(syncLocalDataSource)
    }

    @Test
    fun `given isSlowSyncRequired is called, when localDataSource returns true, then returns true`() {
        every { syncLocalDataSource.isSlowSyncRequired() } returns true

        val result = syncDataSource.isSlowSyncRequired()

        result shouldBe true
    }

    @Test
    fun `given isSlowSyncRequired is called, when localDataSource returns false, then returns false`() {
        every { syncLocalDataSource.isSlowSyncRequired() } returns false

        val result = syncDataSource.isSlowSyncRequired()

        result shouldBe false
    }

    @Test
    fun `given setSlowSyncCompleted is called, then calls localDataSource method`() {
        syncDataSource.setSlowSyncCompleted()

        verify(exactly = 1) { syncLocalDataSource.setSlowSyncCompleted() }
    }

}
