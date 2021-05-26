package com.wire.android.feature.auth.client.datasource

import com.wire.android.core.crypto.CryptoBoxClient
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.core.functional.suspending
import com.wire.android.feature.auth.client.Client
import com.wire.android.feature.auth.client.ClientRepository
import com.wire.android.feature.auth.client.datasource.remote.ClientRemoteDataSource
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse
import com.wire.android.feature.auth.client.datasource.remote.api.UpdatePreKeysRequest
import com.wire.android.feature.auth.client.mapper.ClientMapper
import com.wire.android.feature.auth.client.mapper.PreKeyMapper

class ClientDataSource(
    private val cryptoBoxClient: CryptoBoxClient,
    private val clientRemoteDataSource: ClientRemoteDataSource,
    private val clientMapper: ClientMapper,
    private val preKeyMapper: PreKeyMapper
) : ClientRepository {

    override suspend fun registerNewClient(authorizationToken: String, userId: String, password: String): Either<Failure, ClientResponse> =
        suspending {
            createNewClient(userId, password).flatMap {
                val clientRegistrationRequest = clientMapper.toClientRegistrationRequest(it)
                clientRemoteDataSource.registerNewClient(authorizationToken, clientRegistrationRequest)
            }
        }

    private fun createNewClient(userId: String, password: String): Either<Failure, Client> =
        cryptoBoxClient.createInitialPreKeys().map {
            clientMapper.newClient(userId, password, it)
        }

    override suspend fun updatePreKeysIfNeeded(authorizationToken: String, clientId: String): Either<Failure, Unit> =
        suspending {
            clientRemoteDataSource.remainingPreKeys(authorizationToken, clientId).flatMap { remainingPreKeysIds ->
                cryptoBoxClient.createNewPreKeysIfNeeded(remainingPreKeysIds)
            }.map { preKeys ->
                preKeys.map(preKeyMapper::toPreKeyRequest)
            }.flatMap {
                if (it.isEmpty())
                    Either.Right(Unit)
                else
                    clientRemoteDataSource.saveNewPreKeys(authorizationToken, clientId, UpdatePreKeysRequest(it))
            }
        }
}
