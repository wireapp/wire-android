package com.wire.android.feature.auth.registration.personal.usecase

import com.wire.android.core.exception.Conflict
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FeatureFailure
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.exception.NotFound
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.flatMap
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.auth.registration.RegistrationRepository
import com.wire.android.shared.session.SessionRepository
import com.wire.android.shared.user.User
import com.wire.android.shared.user.UserRepository
import kotlinx.coroutines.runBlocking

class RegisterPersonalAccountUseCase(
    private val registrationRepository: RegistrationRepository,
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository
) : UseCase<Unit, RegisterPersonalAccountParams> {

    override suspend fun run(params: RegisterPersonalAccountParams): Either<Failure, Unit> = with(params) {
        registrationRepository.registerPersonalAccount(name, email, password, activationCode).fold({
            handleRegistrationFailure(it)
        }) {
            when {
                it.user == null -> Either.Left(UserInfoMissing)
                it.refreshToken == null -> Either.Left(RefreshTokenMissing)
                else -> saveDataAndRetrieveSession(it.user, it.refreshToken)
            }
        }!!
    }

    private fun handleRegistrationFailure(failure: Failure): Either<Failure, Unit> = when (failure) {
        is Forbidden -> Either.Left(UnauthorizedEmail)
        is NotFound -> Either.Left(InvalidEmailActivationCode)
        is Conflict -> Either.Left(EmailInUse)
        else -> Either.Left(failure)
    }

    private fun saveDataAndRetrieveSession(user: User, refreshToken: String): Either<Failure, Unit> = runBlocking {
        userRepository.save(user).flatMap {
            runBlocking {
                sessionRepository.accessToken(refreshToken).fold({
                    Either.Left(SessionCannotBeCreated)
                }) {
                    runBlocking { sessionRepository.save(it) }
                }
            }!!
        }
    }
}

data class RegisterPersonalAccountParams(val name: String, val email: String, val password: String, val activationCode: String)

sealed class RegisterPersonalAccountFailure : FeatureFailure()

object UnauthorizedEmail : RegisterPersonalAccountFailure()
object InvalidEmailActivationCode : RegisterPersonalAccountFailure()
object EmailInUse : RegisterPersonalAccountFailure()
object UserInfoMissing : RegisterPersonalAccountFailure()
object RefreshTokenMissing : RegisterPersonalAccountFailure()
object SessionCannotBeCreated : RegisterPersonalAccountFailure()
