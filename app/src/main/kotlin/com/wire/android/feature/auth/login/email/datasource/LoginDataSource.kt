package com.wire.android.feature.auth.login.email.datasource

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.feature.auth.login.email.LoginRepository
import com.wire.android.feature.auth.login.email.datasource.remote.LoginRemoteDataSource

class LoginDataSource(private val remoteDataSource: LoginRemoteDataSource) : LoginRepository {

    override suspend fun loginWithEmail(email: String, password: String): Either<Failure, String> =
        remoteDataSource.loginWithEmail(email = email, password = password).map { it.userId }
}
