package com.wire.android.shared.asset.datasources.local

import com.wire.android.InstrumentationTest
import com.wire.android.core.storage.db.user.UserDatabase
import com.wire.android.framework.storage.db.DatabaseTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
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
        val assetEntity = AssetEntity(downloadKey = TEST_KEY, storageType = TEST_STORAGE_TYPE)

        val assetId = assetDao.insert(assetEntity)

        val assetResult = assetDao.assetById(assetId.toInt())

        verifyAsset(assetResult, assetId.toInt(), TEST_KEY, TEST_STORAGE_TYPE)
    }

    @Test
    fun insertAll_readAssetById_returnsInsertedItemsWithNewIds() = databaseTestRule.runTest {
        val assetEntity1 = AssetEntity(downloadKey = "key-1", storageType = "$TEST_STORAGE_TYPE-1")
        val assetEntity2 = AssetEntity(downloadKey = null, storageType = "$TEST_STORAGE_TYPE-2")
        val assetEntity3 = AssetEntity(downloadKey = "key-3", storageType = null)

        val assetIds = assetDao.insertAll(listOf(assetEntity1, assetEntity2, assetEntity3))

        val assetResult1 = assetDao.assetById(assetIds[0].toInt())
        val assetResult2 = assetDao.assetById(assetIds[1].toInt())
        val assetResult3 = assetDao.assetById(assetIds[2].toInt())

        verifyAsset(assetResult1, assetIds[0].toInt(), "key-1", "$TEST_STORAGE_TYPE-1")
        verifyAsset(assetResult2, assetIds[1].toInt(), null, "$TEST_STORAGE_TYPE-2")
        verifyAsset(assetResult3, assetIds[2].toInt(), "key-3", null)
    }

    @Test
    fun updateStorageType_existingAsset_updatesColumn() = databaseTestRule.runTest {
        val assetEntity = AssetEntity(downloadKey = TEST_KEY, storageType = TEST_STORAGE_TYPE)
        val id = assetDao.insert(assetEntity).toInt()

        val newStorageType = "external"
        assetDao.updateStorageType(id, newStorageType)

        val result = assetDao.assetById(id)

        verifyAsset(result, id, TEST_KEY, newStorageType)
    }

    @Test
    fun updateStorageType_nonExistingAsset_doesNothing() = databaseTestRule.runTest {
        assetDao.updateStorageType(816, TEST_STORAGE_TYPE)

        val result = assetDao.assetById(816)

        result shouldBeEqualTo null
    }

    private fun verifyAsset(assetEntity: AssetEntity?, expectedId: Int, expectedKey: String?, expectedStorageType: String?) {
        assetEntity shouldNotBeEqualTo null
        assetEntity?.id shouldBeEqualTo expectedId
        assetEntity?.downloadKey shouldBeEqualTo expectedKey
        assetEntity?.storageType shouldBeEqualTo expectedStorageType
    }

    companion object {
        private const val TEST_KEY = "asset-key"
        private const val TEST_STORAGE_TYPE = "internal"
    }
}
