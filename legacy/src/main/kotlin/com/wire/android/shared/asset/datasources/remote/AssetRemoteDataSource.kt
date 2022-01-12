package com.wire.android.shared.asset.datasources.remote

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler
import okhttp3.ResponseBody

class AssetRemoteDataSource(private val assetApi: AssetApi, override val networkHandler: NetworkHandler): ApiService() {

    suspend fun publicAsset(key: String): Either<Failure, ResponseBody> = request { assetApi.publicAsset(key) }
}
