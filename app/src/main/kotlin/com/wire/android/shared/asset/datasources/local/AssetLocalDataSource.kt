package com.wire.android.shared.asset.datasources.local

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.storage.db.DatabaseService

class AssetLocalDataSource(private val assetDao: AssetDao) : DatabaseService {

    suspend fun assetById(id: Int): Either<Failure, AssetEntity> = request {
        assetDao.assetById(id)
    }

    suspend fun saveAssets(entities: List<AssetEntity>): Either<Failure, List<Int>> = request {
        val rowIds = assetDao.insertAll(entities)
        rowIds.map { it.toInt() }
    }
}
