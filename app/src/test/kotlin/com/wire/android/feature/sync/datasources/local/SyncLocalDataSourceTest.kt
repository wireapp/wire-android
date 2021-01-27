package com.wire.android.feature.sync.datasources.local

import android.content.SharedPreferences
import com.wire.android.UnitTest
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class SyncLocalDataSourceTest : UnitTest() {

    @MockK
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var syncLocalDataSource: SyncLocalDataSource

    @Before
    fun setUp() {
        syncLocalDataSource = SyncLocalDataSource(sharedPreferences)
    }

    @Test
    fun `given isSlowSyncRequired is called, when sharedPreferences returns "true" for sync completed value, then returns false`() {
        every { sharedPreferences.getBoolean(SLOW_SYNC_COMPLETED_KEY, any()) } returns true

        val result = syncLocalDataSource.isSlowSyncRequired()

        result shouldBeEqualTo false
        verify(exactly = 1) { sharedPreferences.getBoolean(SLOW_SYNC_COMPLETED_KEY, false) }
    }

    @Test
    fun `given isSlowSyncRequired is called, when sharedPreferences returns "false" for sync completed value, then returns true`() {
        every { sharedPreferences.getBoolean(SLOW_SYNC_COMPLETED_KEY, any()) } returns false

        val result = syncLocalDataSource.isSlowSyncRequired()

        result shouldBeEqualTo true
        verify(exactly = 1) { sharedPreferences.getBoolean(SLOW_SYNC_COMPLETED_KEY, false) }
    }

    @Test
    fun `given setSlowSyncCompleted is called, then writes "true" to shared preferences for sync completed value`() {
        val editor = mockk<SharedPreferences.Editor>(relaxUnitFun = true)
        every { editor.putBoolean(any(), any()) } returns editor
        every { sharedPreferences.edit() } returns editor

        syncLocalDataSource.setSlowSyncCompleted()

        verify(exactly = 1) { editor.putBoolean(SLOW_SYNC_COMPLETED_KEY, true) }
    }

    companion object {
        private const val SLOW_SYNC_COMPLETED_KEY = "slow_sync_completed"
    }
}
