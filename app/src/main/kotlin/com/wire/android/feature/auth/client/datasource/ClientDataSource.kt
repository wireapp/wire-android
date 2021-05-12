package com.wire.android.feature.auth.client.datasource

import com.wire.android.core.config.DeviceConfig
import com.wire.android.core.config.Permanent
import com.wire.android.core.crypto.CryptoBoxClient
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.feature.auth.client.Client
import com.wire.android.feature.auth.client.ClientRepository
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse
import com.wire.android.feature.auth.client.datasource.remote.api.LocationResponse

class ClientDataSource(
    private val cryptoBoxClient: CryptoBoxClient,
    private val deviceConfig: DeviceConfig
) : ClientRepository {

    override suspend fun registerNewClient(password: String?): Either<Failure, ClientResponse> =
        Either.Right(ClientResponse("", "", "", LocationResponse("", "", ""), "", "", "", "", ""))

    override suspend fun createNewClient(userId: String, password: String): Either<Failure, Client> =
        cryptoBoxClient.createInitialPreKeys().map {
            Client(
                userId,
                Permanent,
                deviceConfig.deviceName(),
                password,
                deviceConfig.deviceModelName(),
                deviceConfig.deviceClass(),
                it.createdKeys,
                it.lastKey
            )
        }
}
