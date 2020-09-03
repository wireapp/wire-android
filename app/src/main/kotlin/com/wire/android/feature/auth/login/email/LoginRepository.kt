package com.wire.android.feature.auth.login.email

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either

interface LoginRepository {
    fun loginWithEmail(email: String, password: String) : Either<Failure, Unit>
}
