package com.wire.android.core.crypto.mapper

import com.wire.android.core.exception.CryptoBoxFailure
import com.wire.android.core.exception.MessageAlreadyDecrypted
import com.wire.android.core.exception.SessionNotFound
import com.wire.android.core.exception.UnknownCryptoFailure
import com.wire.cryptobox.CryptoException

class CryptoExceptionMapper {

    fun fromNativeException(cryptoException: CryptoException): CryptoBoxFailure {
        return when (cryptoException.code) {
            CryptoException.Code.SESSION_NOT_FOUND -> SessionNotFound
            CryptoException.Code.DUPLICATE_MESSAGE -> MessageAlreadyDecrypted
            else -> UnknownCryptoFailure(cryptoException)
        }
    }
}
