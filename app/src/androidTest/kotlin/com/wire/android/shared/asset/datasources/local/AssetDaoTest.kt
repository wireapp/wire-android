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
        val assetEntity = AssetEntity(downloadKey = TEST_KEY, storagePath = TEST_PATH)

        val assetRowId = assetDao.insert(assetEntity)

        val assetResult = assetDao.assetById(assetRowId.toInt())

        verifyAsset(assetResult, assetRowId.toInt(), TEST_KEY, TEST_PATH)
    }

    @Test
    fun insertAll_readAssetById_returnsInsertedItemsWithNewIds() = databaseTestRule.runTest {
        val assetEntity1 = AssetEntity(downloadKey = "key-1", storagePath = "$TEST_PATH-1")
        val assetEntity2 = AssetEntity(downloadKey = null, storagePath = "$TEST_PATH-2")
        val assetEntity3 = AssetEntity(downloadKey = "key-3", storagePath = null)

        val assetIds = assetDao.insertAll(listOf(assetEntity1, assetEntity2, assetEntity3))

        val assetResult1 = assetDao.assetById(assetIds[0].toInt())
        val assetResult2 = assetDao.assetById(assetIds[1].toInt())
        val assetResult3 = assetDao.assetById(assetIds[2].toInt())

        verifyAsset(assetResult1, assetIds[0].toInt(), "key-1", "$TEST_PATH-1")
        verifyAsset(assetResult2, assetIds[1].toInt(), null, "$TEST_PATH-2")
        verifyAsset(assetResult3, assetIds[2].toInt(), "key-3", null)
    }

    @Test
    fun assetById_existingAsset_returnsAsset() = databaseTestRule.runTest {
        val insertedAsset = AssetEntity(id = 1, downloadKey = TEST_KEY, storagePath = TEST_PATH)
        assetDao.insert(insertedAsset)

        val readAsset = assetDao.assetById(1)

        readAsset shouldBeEqualTo insertedAsset
    }

    @Test
    fun assetById_nonExistingAsset_returnsNull() = databaseTestRule.runTest {
        val readAsset = assetDao.assetById(1)

        readAsset shouldBeEqualTo null
    }

    @Test
    fun downloadKey_nonExistingAsset_returnsNull() = databaseTestRule.runTest {
        val result = assetDao.downloadKey(1)

        result shouldBeEqualTo null
    }

    @Test
    fun downloadKey_assetWithNullKey_returnsNull() = databaseTestRule.runTest {
        assetDao.insert(AssetEntity(id = 1, downloadKey = null, storagePath = TEST_PATH))

        val result = assetDao.downloadKey(1)

        result shouldBeEqualTo null
    }

    @Test
    fun downloadKey_assetWithNonNullKey_returnsKey() = databaseTestRule.runTest {
        assetDao.insert(AssetEntity(id = 1, downloadKey = TEST_KEY, storagePath = TEST_PATH))

        val result = assetDao.downloadKey(1)

        result shouldBeEqualTo TEST_KEY
    }

    @Test
    fun updatePath_nonExistingAsset_doesNothing() = databaseTestRule.runTest {
        assetDao.updatePath(816, TEST_PATH)

        val result = assetDao.assetById(816)

        result shouldBeEqualTo null
    }

    @Test
    fun updatePath_assetWithNullPath_updatesColumn() = databaseTestRule.runTest {
        val assetEntity = AssetEntity(id = 1, downloadKey = TEST_KEY, storagePath = null)
        assetDao.insert(assetEntity)

        val newPath = "/new/path/asset.png"
        assetDao.updatePath(1, newPath)

        val result = assetDao.assetById(1)

        verifyAsset(result, 1, TEST_KEY, newPath)
    }

    @Test
    fun updatePath_assetWithNonNullPath_updatesColumn() = databaseTestRule.runTest {
        val assetEntity = AssetEntity(id = 1, downloadKey = TEST_KEY, storagePath = TEST_PATH)
        assetDao.insert(assetEntity)

        val newPath = "/new/path/asset.png"
        assetDao.updatePath(1, newPath)

        val result = assetDao.assetById(1)

        verifyAsset(result, 1, TEST_KEY, newPath)
    }

    private fun verifyAsset(assetEntity: AssetEntity?, expectedId: Int, expectedKey: String?, expectedPath: String?) {
        assetEntity shouldNotBeEqualTo null
        assetEntity?.id shouldBeEqualTo expectedId
        assetEntity?.downloadKey shouldBeEqualTo expectedKey
        assetEntity?.storagePath shouldBeEqualTo expectedPath
    }

    companion object {
        private const val TEST_KEY = "asset-key"
        private const val TEST_PATH = "/wireapp/files/asset.png"
    }
}
