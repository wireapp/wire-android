package com.wire.android.feature.auth.login.email.datasource

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.login.email.LoginRepository

class LoginDataSource : LoginRepository {

    override fun loginWithEmail(email: String, password: String): Either<Failure, Unit> =
        Either.Right(Unit) //TODO: real implementation
}
