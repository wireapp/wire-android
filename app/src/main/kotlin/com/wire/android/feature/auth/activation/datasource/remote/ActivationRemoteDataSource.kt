package com.wire.android.feature.auth.activation.datasource.remote

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler

class ActivationRemoteDataSource(
    private val activationApi: ActivationApi,
    override val networkHandler: NetworkHandler
) : ApiService() {

    suspend fun sendEmailActivationCode(email: String): Either<Failure, Unit> =
        request { activationApi.sendActivationCode(SendActivationCodeRequest(email = email)) }
}
