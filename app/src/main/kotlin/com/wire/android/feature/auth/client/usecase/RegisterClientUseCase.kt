package com.wire.android.feature.auth.client.usecase

import com.wire.android.core.exception.BadRequest
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FeatureFailure
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.auth.client.ClientRepository
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse

class RegisterClientUseCase(private val clientRepository: ClientRepository) : UseCase<ClientResponse, RegisterClientParams> {
    override suspend fun run(params: RegisterClientParams): Either<Failure, ClientResponse> =
        clientRepository.registerNewClient(params.password).fold({
            when (it) {
                is Forbidden -> Either.Left(DevicesLimitReached)
                is BadRequest -> Either.Left(MalformedPreKeys)
                else -> Either.Left(it)
            }
        }) { Either.Right(it) }!!

}

data class RegisterClientParams(val password: String)

sealed class RegisterClientCodeFailure : FeatureFailure()
object MalformedPreKeys : RegisterClientCodeFailure()
object DevicesLimitReached : RegisterClientCodeFailure()
