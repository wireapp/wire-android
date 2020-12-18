package com.wire.android.shared.user.datasources.remote

import com.wire.android.shared.user.datasources.remote.username.ChangeHandleRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserApi {

    @GET(SELF)
    suspend fun selfUser(@Header(HEADER_KEY_AUTHORIZATION) authorizationHeader: String): Response<SelfUserResponse>

    @GET("$USERS$HANDLES/{handle}")
    suspend fun doesHandleExist(@Path(HANDLE_PATH) handle: String): Response<Unit>

    @PUT("$SELF$HANDLE")
    suspend fun updateHandle(@Body username: ChangeHandleRequest): Response<Unit>

    companion object {
        private const val HEADER_KEY_AUTHORIZATION = "Authorization"
        private const val HANDLE_PATH = "handle"
        private const val SELF = "/self"
        private const val HANDLE = "/handle"
        private const val USERS = "/users"
        private const val HANDLES = "/handles"
    }
}
