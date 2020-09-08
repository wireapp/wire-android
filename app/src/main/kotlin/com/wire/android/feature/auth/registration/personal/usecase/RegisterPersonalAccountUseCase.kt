package com.wire.android.feature.auth.registration.personal.usecase

import com.wire.android.core.exception.Conflict
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FeatureFailure
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.exception.NotFound
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.auth.registration.RegistrationRepository
import com.wire.android.shared.activeusers.ActiveUsersRepository
import kotlinx.coroutines.runBlocking

class RegisterPersonalAccountUseCase(
    private val registrationRepository: RegistrationRepository,
    private val activeUsersRepository: ActiveUsersRepository
) : UseCase<Unit, RegisterPersonalAccountParams> {

    override suspend fun run(params: RegisterPersonalAccountParams): Either<Failure, Unit> = with(params) {
        registrationRepository.registerPersonalAccount(name, email, password, activationCode).fold({
            when (it) {
                is Forbidden -> Either.Left(UnauthorizedEmail)
                is NotFound -> Either.Left(InvalidEmailActivationCode)
                is Conflict -> Either.Left(EmailInUse)
                else -> Either.Left(it)
            }
        }) {
            //TODO: find a suspendable Either solution
            runBlocking {
                activeUsersRepository.saveActiveUser(it)
            }
        }!!
    }
}

data class RegisterPersonalAccountParams(val name: String, val email: String, val password: String, val activationCode: String)

sealed class RegisterPersonalAccountFailure : FeatureFailure()

object UnauthorizedEmail : RegisterPersonalAccountFailure()
object InvalidEmailActivationCode : RegisterPersonalAccountFailure()
object EmailInUse : RegisterPersonalAccountFailure()
