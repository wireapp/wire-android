package com.wire.android.shared.user.datasources.remote

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler
import com.wire.android.shared.user.username.UsernameGeneralError
import com.wire.android.shared.user.username.UsernameAlreadyExists
import com.wire.android.shared.user.username.UsernameInvalid
import com.wire.android.shared.user.username.UsernameIsAvailable
import com.wire.android.shared.user.username.ValidateUsernameSuccess

class UserRemoteDataSource(override val networkHandler: NetworkHandler, private val userApi: UserApi) : ApiService() {

    suspend fun selfUser(accessToken: String, tokenType: String): Either<Failure, SelfUserResponse> = request {
        userApi.selfUser("$tokenType $accessToken")
    }

    suspend fun doesUsernameExist(username: String): Either<Failure, ValidateUsernameSuccess> =
        when (userApi.doesHandleExist(username).code()) {
            HANDLE_TAKEN -> Either.Left(UsernameAlreadyExists)
            HANDLE_INVALID -> Either.Left(UsernameInvalid)
            HANDLE_AVAILABLE -> Either.Right(UsernameIsAvailable)
            else -> Either.Left(UsernameGeneralError)
        }

    companion object {
        private const val HANDLE_TAKEN = 200
        private const val HANDLE_INVALID = 400
        private const val HANDLE_AVAILABLE = 404
    }
}
