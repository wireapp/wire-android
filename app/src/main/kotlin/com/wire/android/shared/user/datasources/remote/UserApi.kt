package com.wire.android.shared.user.datasources.remote

import com.wire.android.shared.user.datasources.remote.username.ChangeUsernameRequest
import com.wire.android.shared.user.datasources.remote.username.CheckHandlesExistRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserApi {

    @GET(SELF)
    suspend fun selfUser(@Header(HEADER_KEY_AUTHORIZATION) authorizationHeader: String): Response<SelfUserResponse>

    @GET("$USERS$HANDLES/{handle}")
    suspend fun doesHandleExist(@Path(HANDLE_PATH) handle: String): Response<Unit>

    @POST("$USERS$HANDLES")
    suspend fun checkHandlesExist(@Body handle: CheckHandlesExistRequest): Response<List<String>>

    @PUT("$SELF$HANDLE")
    suspend fun updateUsername(@Body username: ChangeUsernameRequest): Response<Unit>

    companion object {
        private const val HEADER_KEY_AUTHORIZATION = "Authorization"
        private const val HANDLE_PATH = "handle"
        private const val SELF = "/self"
        private const val HANDLE = "/handle"
        private const val USERS = "/users"
        private const val HANDLES = "/handles"
    }
}
