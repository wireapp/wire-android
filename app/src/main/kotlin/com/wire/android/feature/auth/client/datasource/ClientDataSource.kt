package com.wire.android.feature.auth.client.datasource

import com.wire.android.core.crypto.CryptoBoxClient
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.core.functional.suspending
import com.wire.android.feature.auth.client.ClientRepository
import com.wire.android.feature.auth.client.datasource.local.ClientLocalDataSource
import com.wire.android.feature.auth.client.datasource.remote.ClientRemoteDataSource
import com.wire.android.feature.auth.client.datasource.remote.api.ClientRegistrationRequest
import com.wire.android.feature.auth.client.mapper.ClientMapper

class ClientDataSource(
    private val cryptoBoxClient: CryptoBoxClient,
    private val clientRemoteDataSource: ClientRemoteDataSource,
    private val clientLocalDataSource: ClientLocalDataSource,
    private val clientMapper: ClientMapper
) : ClientRepository {

    override suspend fun registerNewClient(authorizationToken: String, userId: String, password: String): Either<Failure, Unit> =
        suspending {
            createNewClient(userId, password).flatMap {
                clientRemoteDataSource.registerNewClient(authorizationToken, it)
            }.flatMap {
                val clientEntity = clientMapper.fromClientResponseToClientEntity(it)
                clientLocalDataSource.save(clientEntity)
            }
        }

    private fun createNewClient(userId: String, password: String): Either<Failure, ClientRegistrationRequest> =
        cryptoBoxClient.createInitialPreKeys().map {
            clientMapper.newRegistrationRequest(userId, password, it)
        }
}
