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
        val assetEntity = AssetEntity(downloadKey = TEST_KEY)

        val assetId = assetDao.insert(assetEntity)

        val assetResult = assetDao.assetById(assetId.toInt())

        verifyAsset(assetResult, assetId.toInt(), TEST_KEY)
    }

    @Test
    fun insertAll_readAssetById_returnsInsertedItemsWithNewIds() = databaseTestRule.runTest {
        val assetEntity1 = AssetEntity(downloadKey = "key-1")
        val assetEntity2 = AssetEntity(downloadKey = null)

        val assetIds = assetDao.insertAll(listOf(assetEntity1, assetEntity2))

        val assetResult1 = assetDao.assetById(assetIds[0].toInt())
        val assetResult2 = assetDao.assetById(assetIds[1].toInt())

        verifyAsset(assetResult1, assetIds[0].toInt(), "key-1")
        verifyAsset(assetResult2, assetIds[1].toInt(), null)
    }

    private fun verifyAsset(assetEntity: AssetEntity?, expectedId: Int, expectedKey: String?) {
        assetEntity shouldNotBeEqualTo null
        assetEntity?.id shouldBeEqualTo expectedId
        assetEntity?.downloadKey shouldBeEqualTo expectedKey
    }

    companion object {
        private const val TEST_KEY = "asset-key"
    }
}
