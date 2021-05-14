package com.wire.android.core.crypto.mapper

import com.wire.android.core.crypto.CryptoBoxFailure
import com.wire.android.core.crypto.DuplicatedMessage
import com.wire.android.core.crypto.SessionNotFound
import com.wire.android.core.crypto.UnknownCryptoFailure
import com.wire.cryptobox.CryptoException

class CryptoExceptionMapper {

    fun fromNativeException(cryptoException: CryptoException): CryptoBoxFailure {
        return when (cryptoException.code) {
            CryptoException.Code.SESSION_NOT_FOUND -> SessionNotFound(cryptoException)
            CryptoException.Code.DUPLICATE_MESSAGE -> DuplicatedMessage(cryptoException)
            else -> UnknownCryptoFailure(cryptoException)
        }
    }
}
