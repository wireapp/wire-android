package com.wire.android.shared.user.datasources.remote

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler

class UserRemoteDataSource(override val networkHandler: NetworkHandler, private val userApi: UserApi) : ApiService() {

    suspend fun selfUser(accessToken: String, tokenType: String): Either<Failure, SelfUserResponse> = request {
        userApi.selfUser("$tokenType $accessToken")
    }
}
