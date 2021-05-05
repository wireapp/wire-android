package com.wire.android.core.crypto

import com.wire.android.core.exception.FeatureFailure

// Is it a feature, tho? As in... it's not being used by UseCases directly
sealed class CryptoBoxFailure(val cause: Throwable? = null) : FeatureFailure()

class InitializationFailure: CryptoBoxFailure()
class SessionNotFound(cause: Throwable): CryptoBoxFailure(cause)

/**
 * The message was already decrypted, and can not be decrypted again.
 */
class DuplicatedMessage(cause: Throwable): CryptoBoxFailure(cause)
class UnknownCryptoFailure(cause: Throwable): CryptoBoxFailure(cause)

