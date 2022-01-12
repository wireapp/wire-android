package com.wire.android.core.crypto

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.cryptobox.CryptoBox

interface CryptoBoxProvider {
    fun cryptoBoxAtPath(path: String): Either<Failure, CryptoBox>
}

object DefaultCryptoBoxProvider : CryptoBoxProvider {
    override fun cryptoBoxAtPath(path: String): Either<Failure, CryptoBox> = Either.Right(CryptoBox.open(path))
}
