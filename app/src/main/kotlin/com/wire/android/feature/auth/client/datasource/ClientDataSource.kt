package com.wire.android.feature.auth.client.datasource

import android.os.Build
import com.wire.android.core.config.DeviceConfig
import com.wire.android.core.crypto.CryptoBoxClient
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.feature.auth.client.Client
import com.wire.android.feature.auth.client.ClientRepository
import com.wire.android.feature.auth.client.SignalingKey
import com.wire.android.feature.auth.client.datasource.mapper.ClientTypeMapper
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse
import com.wire.android.feature.auth.client.datasource.remote.api.LocationResponse
import com.wire.android.shared.config.DeviceTypeMapper

class ClientDataSource(
    private val cryptoBoxClient: CryptoBoxClient,
    private val clientTypeMapper: ClientTypeMapper,
    private val deviceTypeMapper: DeviceTypeMapper,
    private val deviceConfig: DeviceConfig
) : ClientRepository {

    override suspend fun registerNewClient(password: String?): Either<Failure, ClientResponse> =
        Either.Right(ClientResponse("", "", "", LocationResponse("", "", ""), "", "", "", "", ""))

    override suspend fun createNewClient(userId: String, password: String): Either<Failure, Client> =
        cryptoBoxClient.createInitialPreKeys().map {
            Client(
                userId,
                clientTypeMapper.toStringValue(Permanent),
                deviceConfig.deviceName(),
                password,
                "${Build.MANUFACTURER} ${Build.MODEL}",
                deviceTypeMapper.toStringValue(deviceConfig.deviceType()),
                SignalingKey(), //TODO No longer required - to be implemented later in case of need
                it.createdKeys,
                it.lastKey
            )
        }
}
