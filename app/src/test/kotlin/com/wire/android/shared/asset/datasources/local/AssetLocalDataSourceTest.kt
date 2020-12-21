package com.wire.android.shared.asset.datasources.local

import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class AssetLocalDataSourceTest : UnitTest() {

    @MockK
    private lateinit var assetDao: AssetDao

    private lateinit var assetLocalDataSource: AssetLocalDataSource

    @Before
    fun setUp() {
        assetLocalDataSource = AssetLocalDataSource(assetDao)
    }

    @Test
    fun `given assetById is called, when dao call returns an asset, then propagates success`() {
        val assetEntity = mockk<AssetEntity>()
        coEvery { assetDao.assetById(any()) } returns assetEntity

        val result = runBlocking { assetLocalDataSource.assetById(TEST_ID) }

        result shouldSucceed { it shouldBeEqualTo assetEntity }
        coVerify(exactly = 1) { assetDao.assetById(TEST_ID) }
    }

    @Test
    fun `given assetById is called, when dao call fails, then propagates failure`() {
        coEvery { assetDao.assetById(any()) } answers { throw RuntimeException() }

        val result = runBlocking { assetLocalDataSource.assetById(TEST_ID) }

        result shouldFail {}
        coVerify(exactly = 1) { assetDao.assetById(TEST_ID) }
    }

    @Test
    fun `given createAsset is called with key and localDir, then calls assetDao to insert entity with correct params`() {
        coEvery { assetDao.insert(any()) } returns TEST_ID_LONG

        val result = runBlocking { assetLocalDataSource.createAsset(TEST_DOWNLOAD_KEY) }

        val assetEntitySlot = slot<AssetEntity>()
        coVerify(exactly = 1) { assetDao.insert(capture(assetEntitySlot)) }
        assetEntitySlot.captured.downloadKey shouldBeEqualTo TEST_DOWNLOAD_KEY
    }

    @Test
    fun `given createAsset is called, when dao call returns an id of the inserted asset, then propagates success`() {
        coEvery { assetDao.insert(any()) } returns TEST_ID_LONG

        val result = runBlocking { assetLocalDataSource.createAsset(TEST_DOWNLOAD_KEY) }

        result shouldSucceed { it shouldBeEqualTo TEST_ID }
    }

    @Test
    fun `given createAsset is called, when dao call fails, then propagates failure`() {
        coEvery { assetDao.insert(any()) } answers { throw RuntimeException() }

        val result = runBlocking { assetLocalDataSource.createAsset(TEST_DOWNLOAD_KEY) }

        result shouldFail {}
    }

    companion object {
        private const val TEST_ID = 12
        private const val TEST_ID_LONG = TEST_ID.toLong()
        private const val TEST_DOWNLOAD_KEY = "key-123"
    }
}
