package com.wire.android.feature.auth.login.email.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FeatureFailure
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.exception.TooManyRequests
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.auth.login.email.LoginRepository
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.SessionRepository
import com.wire.android.shared.user.UserRepository

class LoginWithEmailUseCase(
    private val loginRepository: LoginRepository,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) : UseCase<String, LoginWithEmailUseCaseParams> {

    override suspend fun run(params: LoginWithEmailUseCaseParams): Either<Failure, String> = suspending {
        loginRepository.loginWithEmail(email = params.email, password = params.password).coFold({
            handleFailure(it)
        }) { session ->
            if (session == Session.EMPTY) Either.Left(SessionCredentialsMissing)
            else {
                userRepository.selfUser(accessToken = session.accessToken, tokenType = session.tokenType).flatMap { user ->
                    sessionRepository.save(session, false).map { user.id }
                }
            }
        }!!
    }

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
