package com.wire.android.feature.auth.registration.datasource

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.feature.auth.registration.RegistrationRepository
import com.wire.android.feature.auth.registration.datasource.remote.RegistrationRemoteDataSource
import com.wire.android.feature.auth.registration.personal.PersonalAccountRegistrationResult
import com.wire.android.shared.session.mapper.SessionMapper
import com.wire.android.shared.user.mapper.UserMapper

class RegistrationDataSource(
    private val remoteDataSource: RegistrationRemoteDataSource,
    private val userMapper: UserMapper,
    private val sessionMapper: SessionMapper
) : RegistrationRepository {

    override suspend fun registerPersonalAccount(
        name: String, email: String, username: String, password: String, activationCode: String
    ): Either<Failure, PersonalAccountRegistrationResult> =
        remoteDataSource.registerPersonalAccount(
            name = name,
            email = email,
            username = username,
            password = password,
            activationCode = activationCode
        ).map {
            PersonalAccountRegistrationResult(
                user = it.body()?.let { userMapper.fromRegisteredUserResponse(it) },
                refreshToken = sessionMapper.extractRefreshToken(it.headers())
            )
        }
}
