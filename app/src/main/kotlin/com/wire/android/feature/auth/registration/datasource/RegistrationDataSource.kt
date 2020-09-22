package com.wire.android.feature.auth.registration.datasource

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.feature.auth.registration.RegistrationRepository
import com.wire.android.feature.auth.registration.datasource.remote.RegistrationRemoteDataSource
import com.wire.android.shared.user.User
import com.wire.android.shared.user.mapper.UserMapper

class RegistrationDataSource(
    private val remoteDataSource: RegistrationRemoteDataSource,
    private val userMapper: UserMapper
) : RegistrationRepository {

    override suspend fun registerPersonalAccount(
        name: String, email: String, password: String, activationCode: String
    ): Either<Failure, User> =
        remoteDataSource.registerPersonalAccount(name = name, email = email, password = password, activationCode = activationCode)
            .map { userMapper.fromRegisteredUserResponse(it) }
}
