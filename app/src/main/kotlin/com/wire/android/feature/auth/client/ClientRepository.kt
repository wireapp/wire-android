package com.wire.android.feature.auth.client

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either

interface ClientRepository {

    fun registerNewClient(password: String? = null): Either<Failure, Unit>
}
