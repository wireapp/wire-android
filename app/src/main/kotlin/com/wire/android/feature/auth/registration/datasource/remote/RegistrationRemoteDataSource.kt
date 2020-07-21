package com.wire.android.feature.auth.registration.datasource.remote

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.locale.LocaleConfig
import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler
import java.util.UUID

class RegistrationRemoteDataSource(
    private val api: RegistrationApi,
    private val localeConfig: LocaleConfig,
    override val networkHandler: NetworkHandler
) : ApiService() {

    suspend fun registerPersonalAccountWithEmail(
        name: String,
        email: String,
        password: String,
        activationCode: String
    ): Either<Failure, UserResponse> = request {
        api.register(
            RegisterPersonalAccountWithEmailRequest(
                name = name, email = email, password = password, emailCode = activationCode,
                locale = localeConfig.currentLocale().toLanguageTag(),
                label = UUID.randomUUID().toString() //TODO: what is it used for? do not hardcode here!
            )
        )
    }
}
