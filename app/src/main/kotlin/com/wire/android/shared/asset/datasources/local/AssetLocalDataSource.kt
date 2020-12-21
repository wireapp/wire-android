package com.wire.android.shared.asset.datasources.local

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.core.functional.suspending
import com.wire.android.core.io.FileSystem
import com.wire.android.core.storage.db.DatabaseService
import java.io.File
import java.io.InputStream

class AssetLocalDataSource(private val assetDao: AssetDao, private val fileSystem: FileSystem) : DatabaseService {

    suspend fun assetById(assetId: Int): Either<Failure, AssetEntity> = request {
        assetDao.assetById(assetId)
    }

    /**
     * @return [Failure] if operation fails, or id of the new [AssetEntity]
     */
    suspend fun createAsset(key: String? = null): Either<Failure, Int> = request {
        assetDao.insert(AssetEntity(downloadKey = key)).toInt()
    }

    /**
     * @return path of saved asset
     */
    suspend fun saveInternalAsset(id: Int, inputStream: InputStream): Either<Failure, String> = suspending {
        fileSystem.createInternalFile(assetsPath(id))
            .flatMap { file ->
                fileSystem.writeToFile(file, inputStream)
            }.flatMap { file ->
                updateStorageType(id, AssetStorage.Internal).map {
                    file.absolutePath
                }
            }
    }

    fun assetPath(assetEntity: AssetEntity): Either<Failure, String?> =
        when (assetStorage(assetEntity)) {
            AssetStorage.Internal -> {
                val relativePath = assetsPath(assetEntity.id)
                fileSystem.internalFile(relativePath).map { it.absolutePath }
            }
            else -> Either.Right(null) //TODO: add case for external storage
        }

    private suspend fun updateStorageType(id: Int, assetStorage: AssetStorage): Either<Failure, Unit> = request {
        assetDao.updateStorageType(id, assetStorage.type)
    }

    private fun assetsPath(id: Int) = "assets${File.separator}$id"

    private fun assetStorage(assetEntity: AssetEntity): AssetStorage? =
        when (assetEntity.storageType) {
            AssetStorage.Internal.type -> AssetStorage.Internal
            AssetStorage.External.type -> AssetStorage.External
            else -> null
        }
}
