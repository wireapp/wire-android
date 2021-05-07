package com.wire.android.shared.crypto.datasources

import com.wire.android.core.crypto.CryptoBoxClient
import com.wire.android.core.crypto.model.PreKeyInitialization
import com.wire.android.core.exception.CryptoBoxFailure
import com.wire.android.core.functional.Either
import com.wire.android.shared.crypto.CryptoBoxRepository

class CryptoBoxDataSource(private val cryptoBoxClient: CryptoBoxClient) : CryptoBoxRepository {

    override suspend fun generatePreKeys(): Either<CryptoBoxFailure, PreKeyInitialization> =
        cryptoBoxClient.createInitialPreKeys()
}
