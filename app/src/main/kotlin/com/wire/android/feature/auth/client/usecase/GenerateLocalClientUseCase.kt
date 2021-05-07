package com.wire.android.feature.auth.client.usecase

import android.os.Build
import com.wire.android.core.crypto.model.PreKeyInitialization
import com.wire.android.core.device.DeviceNameUseCase
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.auth.client.Client
import com.wire.android.feature.auth.client.ClientType
import com.wire.android.feature.auth.client.SignalingKey
import com.wire.android.core.device.DeviceTypeUseCase
import com.wire.android.core.exception.ClientRegistrationFailure
import java.lang.IllegalArgumentException

class GenerateLocalClientUseCase(
    private val deviceTypeUseCase: DeviceTypeUseCase,
    private val deviceNameUseCase: DeviceNameUseCase
) : UseCase<Client, GenerateLocalClientParams> {
    override suspend fun run(params: GenerateLocalClientParams): Either<Failure, Client> {
        return try {
            Either.Right(
                Client(
                    params.userId,
                    ClientType.PERMANENT.type,
                    deviceNameUseCase.run(),
                    params.password,
                    "${Build.MANUFACTURER} ${Build.MODEL}",
                    deviceTypeUseCase.run(),
                    SignalingKey(TEMP_KEY, TEMP_KEY),//TODO to be implemented later
                    params.preKeyInitialization.createdKeys,
                    params.preKeyInitialization.lastKey
                )
            )
        } catch (exception: IllegalArgumentException) {
            Either.Left(ClientRegistrationFailure)
        }
    }

    companion object {
        private const val TEMP_KEY = "pdtN27SqRvrh15qr1BvOLV9jP4RAnITITY5DDzfAz24"
    }
}

data class GenerateLocalClientParams(
    val userId: String,
    val password: String,
    val preKeyInitialization: PreKeyInitialization
)
