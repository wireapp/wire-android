package com.wire.android.feature.auth.login.email.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FeatureFailure
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.exception.TooManyRequests
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.auth.login.email.LoginRepository
import com.wire.android.shared.activeuser.ActiveUserRepository
import kotlinx.coroutines.runBlocking

class LoginWithEmailUseCase(
    private val loginRepository: LoginRepository,
    private val activeUserRepository: ActiveUserRepository
) : UseCase<Unit, LoginWithEmailUseCaseParams> {

    override suspend fun run(params: LoginWithEmailUseCaseParams): Either<Failure, Unit> =
        loginRepository.loginWithEmail(email = params.email, password = params.password).fold({
            when (it) {
                //TODO: should we take "label" in backend response into account?
                Forbidden -> Either.Left(LoginAuthenticationFailure)
                TooManyRequests -> Either.Left(LoginTooFrequentFailure)
                else -> Either.Left(it)
            }
        }) {
            //TODO: find a suspendable Either solution
            runBlocking {
                activeUserRepository.saveActiveUser(it)
            }
        }!!
}

data class LoginWithEmailUseCaseParams(val email: String, val password: String)

sealed class LoginWithEmailUseCaseFailure : FeatureFailure()
object LoginAuthenticationFailure : LoginWithEmailUseCaseFailure()
object LoginTooFrequentFailure : LoginWithEmailUseCaseFailure()
