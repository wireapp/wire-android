package com.wire.android.shared.asset.datasources.local

import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FileDoesNotExist
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.flatMap
import com.wire.android.core.functional.suspending
import com.wire.android.core.io.FileSystem
import com.wire.android.core.storage.db.DatabaseService
import java.io.File
import java.io.InputStream

class AssetLocalDataSource(private val assetDao: AssetDao, private val fileSystem: FileSystem) : DatabaseService {

    suspend fun assetById(id: Int): Either<Failure, File> = request {
        assetDao.assetById(id)
    }.flatMap {
        asset(it)
    }

    fun asset(assetEntity: AssetEntity): Either<Failure, File> =
        assetEntity.storagePath?.let { fileSystem.internalFile(it) }
            ?: Either.Left(FileDoesNotExist)

    suspend fun createAssets(downloadKeys: List<String?>): Either<Failure, List<Int>> = request {
        val assets = downloadKeys.map { AssetEntity(downloadKey = it) }
        assetDao.insertAll(assets).map { it.toInt() }
    }

    suspend fun saveAsset(id: Int, contents: InputStream, path: String): Either<Failure, File> = suspending {
        fileSystem.createInternalFile(path).flatMap { file ->
            fileSystem.writeToFile(file, contents)
        }.flatMap { file ->
            request { assetDao.updatePath(id, path) }.map { file }
        }
    }

    suspend fun downloadKey(id: Int): Either<Failure, String> = request { assetDao.downloadKey(id) }
}
