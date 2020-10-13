package com.wire.android.feature.auth.login.email.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FeatureFailure
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.exception.TooManyRequests
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.flatMap
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.auth.login.email.LoginRepository
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.SessionRepository
import com.wire.android.shared.user.UserRepository
import kotlinx.coroutines.runBlocking

class LoginWithEmailUseCase(
    private val loginRepository: LoginRepository,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) : UseCase<Unit, LoginWithEmailUseCaseParams> {

    override suspend fun run(params: LoginWithEmailUseCaseParams): Either<Failure, Unit> =
        loginRepository.loginWithEmail(email = params.email, password = params.password).fold({
            handleFailure(it)
        }) { session ->
            if (session == Session.EMPTY) Either.Left(SessionCredentialsMissing)
            else {
                //TODO: find a suspendable Either solution
                runBlocking {
                    userRepository.selfUser(accessToken = session.accessToken, tokenType = session.tokenType).flatMap {
                        runBlocking {
                            sessionRepository.save(session)
                        }
                    }
                }
            }
        }!!

    private fun handleFailure(failure: Failure) = when (failure) {
        //TODO: should we take "label" in backend response into account?
        Forbidden -> Either.Left(LoginAuthenticationFailure)
        TooManyRequests -> Either.Left(LoginTooFrequentFailure)
        else -> Either.Left(failure)
    }
}

data class LoginWithEmailUseCaseParams(val email: String, val password: String)

sealed class LoginWithEmailUseCaseFailure : FeatureFailure()
object LoginAuthenticationFailure : LoginWithEmailUseCaseFailure()
object LoginTooFrequentFailure : LoginWithEmailUseCaseFailure()
object SessionCredentialsMissing : LoginWithEmailUseCaseFailure()
