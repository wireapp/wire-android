package com.wire.android.feature.auth.client.usecase

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.wire.android.core.crypto.model.PreKeyInitialization
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.IOAccessDenied
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.auth.client.Client
import com.wire.android.feature.auth.client.ClientType
import com.wire.android.feature.auth.client.SignalingKey
import com.wire.android.shared.crypto.usecase.DeviceTypeUseCase

class LocalClientGeneratorUseCase(
    private val deviceTypeUseCase: DeviceTypeUseCase,
    private val context: Context
) : UseCase<Client, LocalClientGeneratorParams> {
    override suspend fun run(params: LocalClientGeneratorParams): Either<Failure, Client> {
        return try {
            Either.Right(
                Client(
                    params.userId,
                    ClientType.PERMANENT.type,
                    Settings.Secure.getString(context.contentResolver, "bluetooth_name"),
                    params.password,
                    "${Build.MANUFACTURER} ${Build.MODEL}",
                    deviceTypeUseCase.run(),
                    SignalingKey(TEMP_KEY, TEMP_KEY),//TODO to be implemented later
                    params.preKeyInitialization.createdKeys,
                    params.preKeyInitialization.lastKey
                )
            )
        } catch (e: IllegalArgumentException) {
            Either.Left(IOAccessDenied)
        }
    }

    companion object {
        private const val TEMP_KEY = "pdtN27SqRvrh15qr1BvOLV9jP4RAnITITY5DDzfAz24"
    }
}

data class LocalClientGeneratorParams(
    val userId: String,
    val password: String,
    val preKeyInitialization: PreKeyInitialization
)
