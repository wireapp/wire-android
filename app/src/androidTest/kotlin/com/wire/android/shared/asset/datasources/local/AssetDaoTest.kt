package com.wire.android.shared.asset.datasources.local

import com.wire.android.InstrumentationTest
import com.wire.android.core.storage.db.user.UserDatabase
import com.wire.android.framework.storage.db.DatabaseTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class AssetDaoTest : InstrumentationTest() {

    @get:Rule
    val databaseTestRule = DatabaseTestRule.create<UserDatabase>(appContext)

    private lateinit var assetDao: AssetDao
    private lateinit var userDatabase: UserDatabase

    @Before
    fun setUp() {
        userDatabase = databaseTestRule.database
        assetDao = userDatabase.assetDao()
    }

    @Test
    fun insert_readAssetById_returnsInsertedItemWithNewId() = databaseTestRule.runTest {
        val assetEntity = AssetEntity(key = TEST_KEY, size = TEST_SIZE, type = TEST_TYPE)

        val assetId = assetDao.insert(assetEntity).toInt()

        val assetResult = assetDao.assetById(assetId)

        assetResult shouldBeEqualTo assetEntity.copy(id = assetId)
    }

    @Test
    fun insertAll_readAssetById_returnsInsertedItemsWithNewIds() = databaseTestRule.runTest {
        val assetEntity1 = AssetEntity(key = "$TEST_KEY-1", size = "$TEST_SIZE-1", type = "$TEST_TYPE-1")
        val assetEntity2 = AssetEntity(key = "$TEST_KEY-2", size = "$TEST_SIZE-2", type = "$TEST_TYPE-2")

        val assetIds = assetDao.insertAll(listOf(assetEntity1, assetEntity2))

        val id1 = assetIds[0].toInt()
        val id2 = assetIds[1].toInt()

        val assetResult1 = assetDao.assetById(id1)
        val assetResult2 = assetDao.assetById(id2)

        assetResult1 shouldBeEqualTo assetEntity1.copy(id = id1)
        assetResult2 shouldBeEqualTo assetEntity2.copy(id = id2)
    }

    @Test
    fun assetById_existingAsset_returnsAsset() = databaseTestRule.runTest {
        val insertedAsset = AssetEntity(id =1, key = TEST_KEY, size = TEST_SIZE, type = TEST_TYPE)
        assetDao.insert(insertedAsset)

        val readAsset = assetDao.assetById(1)

        readAsset shouldBeEqualTo insertedAsset
    }

    @Test
    fun assetById_nonExistingAsset_returnsNull() = databaseTestRule.runTest {
        val readAsset = assetDao.assetById(1)

        readAsset shouldBeEqualTo null
    }

    companion object {
        private const val TEST_SIZE = "complete"
        private const val TEST_KEY = "40wer-4309-mvcx"
        private const val TEST_TYPE = "image"
    }
}
