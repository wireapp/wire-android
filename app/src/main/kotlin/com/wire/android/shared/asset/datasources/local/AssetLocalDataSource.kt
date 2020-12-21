package com.wire.android.shared.asset.datasources.local

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.storage.db.DatabaseService

class AssetLocalDataSource(private val assetDao: AssetDao) : DatabaseService {

    suspend fun assetById(assetId: Int): Either<Failure, AssetEntity> = request {
        assetDao.assetById(assetId)
    }

    /**
     * @return [Failure] if operation fails, or id of the new [AssetEntity]
     */
    suspend fun createAsset(key: String? = null): Either<Failure, Int> = request {
        assetDao.insert(AssetEntity(downloadKey = key)).toInt()
    }
}
