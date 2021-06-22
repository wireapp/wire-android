package com.wire.android.shared.asset.datasources

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.shared.asset.AssetRepository
import com.wire.android.shared.asset.datasources.remote.AssetRemoteDataSource
import java.io.InputStream

class AssetDataSource(private val assetRemoteDataSource: AssetRemoteDataSource): AssetRepository {

    override suspend fun publicAsset(key: String): Either<Failure, InputStream> =
        assetRemoteDataSource.publicAsset(key).map { it.byteStream() }
}
