package com.wire.android.feature.auth.client.usecase

import android.util.Log
import com.wire.android.core.exception.BadRequest
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FeatureFailure
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.auth.client.ClientRepository
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse
import com.wire.android.shared.crypto.CryptoBoxRepository
import com.wire.android.shared.session.SessionRepository

class RegisterClientUseCase(
    private val clientRepository: ClientRepository,
    private val sessionRepository: SessionRepository,
    private val cryptoBoxRepository: CryptoBoxRepository,
    private val localClientGeneratorUseCase: LocalClientGeneratorUseCase
) : UseCase<ClientResponse, RegisterClientParams> {
    override suspend fun run(params: RegisterClientParams): Either<Failure, ClientResponse> {
        return suspending {

            sessionRepository.userAuthorizationToken(params.userId).flatMap { authorizationToken ->
                cryptoBoxRepository.generatePreKeys().flatMap { preKey ->
                    localClientGeneratorUseCase.run(LocalClientGeneratorParams(params.userId, params.password, preKey)).flatMap { client ->
                        clientRepository.registerNewClient(authorizationToken, client).fold({
                            when (it) {
                                is Forbidden -> Either.Left(DevicesLimitReached)
                                is BadRequest -> Either.Left(MalformedPreKeys)
                                else -> Either.Left(it)
                            }
                        }) { Either.Right(it) }!!
                    }

                }
            }
        }
    }

}

data class RegisterClientParams(val userId: String, val password: String)

sealed class RegisterClientCodeFailure : FeatureFailure()
object MalformedPreKeys : RegisterClientCodeFailure()
object DevicesLimitReached : RegisterClientCodeFailure()
