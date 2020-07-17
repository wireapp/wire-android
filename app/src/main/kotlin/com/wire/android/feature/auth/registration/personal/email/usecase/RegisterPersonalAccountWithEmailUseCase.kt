package com.wire.android.feature.auth.registration.personal.email.usecase

import com.wire.android.core.exception.Conflict
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FeatureFailure
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.exception.NotFound
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.auth.registration.RegistrationRepository

class RegisterPersonalAccountWithEmailUseCase(
    private val registrationRepository: RegistrationRepository
) : UseCase<Unit, EmailRegistrationParams> {

    override suspend fun run(params: EmailRegistrationParams): Either<Failure, Unit> = with(params) {
        registrationRepository.registerPersonalAccountWithEmail(name, email, password, activationCode).fold({
            when (it) {
                is Forbidden -> Either.Left(UnauthorizedEmail)
                is NotFound -> Either.Left(InvalidEmailActivationCode)
                is Conflict -> Either.Left(EmailInUse)
                else -> Either.Left(it)
            }
        }) { Either.Right(it) }!!
    }
}

data class EmailRegistrationParams(val name: String, val email: String, val password: String, val activationCode: String)

sealed class RegisterPersonalAccountWithEmailFailure : FeatureFailure()

object UnauthorizedEmail : RegisterPersonalAccountWithEmailFailure()
object InvalidEmailActivationCode : RegisterPersonalAccountWithEmailFailure()
object EmailInUse : RegisterPersonalAccountWithEmailFailure()
