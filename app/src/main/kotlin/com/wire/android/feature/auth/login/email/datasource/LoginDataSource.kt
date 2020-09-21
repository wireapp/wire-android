package com.wire.android.feature.auth.login.email.datasource

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.feature.auth.login.email.LoginRepository
import com.wire.android.feature.auth.login.email.datasource.remote.LoginRemoteDataSource
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.mapper.SessionMapper

class LoginDataSource(
    private val remoteDataSource: LoginRemoteDataSource,
    private val sessionMapper: SessionMapper
) : LoginRepository {

    override suspend fun loginWithEmail(email: String, password: String): Either<Failure, Session> =
        remoteDataSource.loginWithEmail(email = email, password = password).map {
            sessionMapper.fromLoginResponse(it)
        }
}
