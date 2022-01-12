package com.wire.android.shared.asset.datasources.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

interface AssetApi {

    @Streaming
    @GET("$PUBLIC_ASSET/{key}")
    suspend fun publicAsset(@Path("key") key: String): Response<ResponseBody>

    companion object {
        private const val PUBLIC_ASSET = "/assets/v3"
    }
}
