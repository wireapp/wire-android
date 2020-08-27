package com.wire.android.feature.auth.login.email.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FeatureFailure
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.exception.TooManyRequests
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.auth.login.email.LoginRepository

class LoginWithEmailUseCase(private val loginRepository: LoginRepository) : UseCase<Unit, LoginWithEmailUseCaseParams> {

    override suspend fun run(params: LoginWithEmailUseCaseParams): Either<Failure, Unit> =
        loginRepository.loginWithEmail(email = params.email, password = params.password).fold({
            when (it) {
                //TODO: should we take "label" in backend response into account?
                Forbidden -> Either.Left(LoginAuthenticationFailure)
                TooManyRequests -> Either.Left(LoginTooFrequentFailure)
                else -> Either.Left(it)
            }
        }) { Either.Right(Unit) }!!
}

data class LoginWithEmailUseCaseParams(val email: String, val password: String)

sealed class LoginWithEmailUseCaseFailure : FeatureFailure()
object LoginAuthenticationFailure : LoginWithEmailUseCaseFailure()
object LoginTooFrequentFailure : LoginWithEmailUseCaseFailure()
