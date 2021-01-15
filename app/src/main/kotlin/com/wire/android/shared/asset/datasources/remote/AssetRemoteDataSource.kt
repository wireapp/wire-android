package com.wire.android.shared.asset.datasources.remote

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler
import okhttp3.ResponseBody

class AssetRemoteDataSource(override val networkHandler: NetworkHandler, private val assetApi: AssetApi) : ApiService() {

    suspend fun publicAsset(key: String): Either<Failure, ResponseBody> = request { assetApi.publicAsset(key) }
}
