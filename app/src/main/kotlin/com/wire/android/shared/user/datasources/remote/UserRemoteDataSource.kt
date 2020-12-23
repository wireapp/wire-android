package com.wire.android.shared.user.datasources.remote

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler
import com.wire.android.shared.user.datasources.remote.username.ChangeUsernameRequest
import com.wire.android.shared.user.datasources.remote.username.CheckHandlesExistRequest

class UserRemoteDataSource(override val networkHandler: NetworkHandler, private val userApi: UserApi) : ApiService() {

    suspend fun selfUser(accessToken: String, tokenType: String): Either<Failure, SelfUserResponse> = request {
        userApi.selfUser("$tokenType $accessToken")
    }

    suspend fun doesUsernameExist(username: String): Either<Failure, Unit> = request {
        userApi.doesHandleExist(username)
    }

    suspend fun updateUsername(username: String): Either<Failure, Unit> = request {
        userApi.updateUsername(ChangeUsernameRequest(username))
    }

    suspend fun checkUsernamesExist(usernames: List<String>): Either<Failure, List<String>> = request {
        userApi.checkHandlesExist(CheckHandlesExistRequest(handles = usernames))
    }
}
