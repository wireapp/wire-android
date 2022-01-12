package com.wire.android.shared.asset.datasources.local

import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
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
    fun `given assetById is called, when dao call fails to return an entity, then propagates failure`() {
        coEvery { assetDao.assetById(any()) } answers { throw RuntimeException() }

        val result = runBlocking { assetLocalDataSource.assetById(TEST_ID) }

        result shouldFail {}
        coVerify(exactly = 1) { assetDao.assetById(TEST_ID) }
    }

    @Test
    fun `given assetById is called, when dao call returns an asset, then propagates the asset`() {
        val assetEntity = mockk<AssetEntity>()
        coEvery { assetDao.assetById(any()) } returns assetEntity

        val result = runBlocking { assetLocalDataSource.assetById(TEST_ID) }

        result shouldSucceed { it shouldBeEqualTo assetEntity }
        coVerify(exactly = 1) { assetDao.assetById(TEST_ID) }
    }

    @Test
    fun `given saveAssets is called with entities, when dao call fails to insert assets, then propagates failure`() {
        val assetEntities = mockk<List<AssetEntity>>()
        coEvery { assetDao.insertAll(any()) } answers { throw RuntimeException() }

        val result = runBlocking { assetLocalDataSource.saveAssets(assetEntities) }

        result shouldFail {}
        coVerify(exactly = 1) { assetDao.insertAll(assetEntities) }
    }

    @Test
    fun `given saveAssets is called with entities, when dao call insert assets and returns rowIds, then propagates inserted assets' ids`() {
        val assetEntities = mockk<List<AssetEntity>>()
        coEvery { assetDao.insertAll(any()) } returns TEST_ROW_IDS

        val result = runBlocking { assetLocalDataSource.saveAssets(assetEntities) }

        result shouldSucceed { it shouldContainSame TEST_IDS }
        coVerify(exactly = 1) { assetDao.insertAll(assetEntities) }
    }


    companion object {
        private const val TEST_ID = 12
        private val TEST_ROW_IDS = listOf(12L, 13L, 14L)
        private val TEST_IDS = TEST_ROW_IDS.map { it.toInt() }
    }
}
