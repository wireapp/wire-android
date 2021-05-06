package com.wire.android.shared.crypto

import com.wire.android.core.crypto.model.PreKeyInitialization
import com.wire.android.core.exception.CryptoBoxFailure
import com.wire.android.core.functional.Either

interface CryptoBoxRepository {
    suspend fun generatePreKeys(): Either<CryptoBoxFailure, PreKeyInitialization>
}
