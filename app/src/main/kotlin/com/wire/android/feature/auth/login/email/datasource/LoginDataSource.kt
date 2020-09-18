package com.wire.android.feature.auth.login.email.datasource

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.feature.auth.login.email.LoginRepository
import com.wire.android.feature.auth.login.email.datasource.remote.LoginRemoteDataSource
import com.wire.android.shared.user.UserSession
import com.wire.android.shared.user.mapper.UserSessionMapper

class LoginDataSource(
    private val remoteDataSource: LoginRemoteDataSource,
    private val userSessionMapper: UserSessionMapper
) : LoginRepository {

    override suspend fun loginWithEmail(email: String, password: String): Either<Failure, UserSession> =
        remoteDataSource.loginWithEmail(email = email, password = password).map {
            userSessionMapper.fromLoginResponse(it)
        }
}
