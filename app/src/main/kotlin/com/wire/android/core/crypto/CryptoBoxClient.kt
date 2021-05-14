package com.wire.android.core.crypto

import android.content.Context
import com.wire.android.core.crypto.data.CryptoBoxClientPropertyStorage
import com.wire.android.core.crypto.mapper.CryptoExceptionMapper
import com.wire.android.core.crypto.mapper.PreKeyMapper
import com.wire.android.core.crypto.model.EncryptedMessage
import com.wire.android.core.crypto.model.PlainMessage
import com.wire.android.core.crypto.model.PreKey
import com.wire.android.core.crypto.model.PreKeyInitialization
import com.wire.android.core.crypto.model.CryptoSessionId
import com.wire.android.core.crypto.model.UserId
import com.wire.android.core.extension.plus
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.flatMap
import com.wire.android.core.functional.handleFailure
import com.wire.android.core.functional.map
import com.wire.android.core.functional.onSuccess
import com.wire.cryptobox.CryptoBox
import com.wire.cryptobox.CryptoException
import com.wire.cryptobox.CryptoSession

class CryptoBoxClient(
    context: Context,
    private val clientPropertyStorage: CryptoBoxClientPropertyStorage,
    private val userId: UserId,
    private val preKeyMapper: PreKeyMapper,
    private val exceptionMapper: CryptoExceptionMapper
) {

    private var _cryptoBox: CryptoBox? = null
    private val cryptoBoxRootDirectory =
        context.filesDir + CRYPTOBOX_PARENT_FOLDER_NAME + userId.toString()

    private val cryptoBox: Either<CryptoBoxFailure, CryptoBox>
        get() = _cryptoBox?.let { Either.Right(it) } ?: load().map {
            _cryptoBox = it
            it
        }

    fun delete() = useBox {
        close()
        _cryptoBox = null
        cryptoBoxRootDirectory.deleteRecursively()
    }

    fun createInitialPreKeys(): Either<CryptoBoxFailure, PreKeyInitialization> = useBox {
        val lastKey = preKeyMapper.fromCryptoBoxModel(newLastPreKey())
        val keys = newPreKeys(0, PRE_KEYS_COUNT).map(preKeyMapper::fromCryptoBoxModel)
        clientPropertyStorage.updateLastPreKeyId(userId, keys.last().id)
        PreKeyInitialization(keys, lastKey)
    }

    /**
     * Attempts to encrypt a message for the specified session.
     * This function assumes that the session already exists.
     * @param cryptoSessionId the ID of the session.
     * @param message the original message, in plain-text.
     * @param onEncrypt action to be performed after the message is encrypted, should return true if the message was
     * successfully sent, so the session state can be persisted.
     */
    fun encryptMessage(
        cryptoSessionId: CryptoSessionId,
        message: PlainMessage,
        onEncrypt: (encryptedMessage: Either<CryptoBoxFailure, EncryptedMessage>) -> Boolean
    ) {
        val sessionMessage = withSession(cryptoSessionId) { session ->
            session to EncryptedMessage(session.encrypt(message.data))
        }
        if (onEncrypt(sessionMessage.map { it.second })) {
            sessionMessage.onSuccess { (session, _) ->
                session.save()
            }
        }
    }

    /**
     * Attempts to decrypt a message for a specified session.
     * The first message sent by the sender should be a PreKey message, which has enough data for CryptoBox to start a session.
     * If the session is not found, this assumes that it is a PreKey message and attempts to establish a session based on its content.
     * @param cryptoSessionId the ID of the session.
     * @param message the encrypted message.
     * @param onDecrypt action to be performed after the message is decrypted, should return true if the message was
     * successfully stored locally, so the session state can be persisted.
     * @see
     */
    fun decryptMessage(
        cryptoSessionId: CryptoSessionId,
        message: EncryptedMessage,
        onDecrypt: (plainMessage: Either<CryptoBoxFailure, PlainMessage>) -> Boolean
    ) {
        val sessionMessagePair = withSession(cryptoSessionId) { session ->
            session to session.decrypt(message.data)
        }.handleFailure { failure ->
            if (failure !is SessionNotFound)
                return@handleFailure Either.Left(failure)

            initiateSessionFromReceivedMessage(cryptoSessionId, message)
        }

        val plainMessage = sessionMessagePair.map { PlainMessage(it.second) }

        if (onDecrypt(plainMessage)) {
            sessionMessagePair.onSuccess { (session, _) ->
                session.save()
            }
        }
    }

    private fun initiateSessionFromReceivedMessage(cryptoSessionId: CryptoSessionId, message: EncryptedMessage) = useBox {
        initSessionFromMessage(cryptoSessionId.value, message.data)
    }.map { sessionMessage ->
        sessionMessage.session to sessionMessage.message
    }

    /**
     * Verifies if a session exists, or creates one with the provided [preKey].
     */
    fun assertSession(cryptoSessionId: CryptoSessionId, preKey: PreKey): Either<CryptoBoxFailure, Unit> {
        return session(cryptoSessionId).handleFailure { failure ->
            if (failure !is SessionNotFound)
                return@handleFailure Either.Left(failure)

            useBox {
                initSessionFromPreKey(cryptoSessionId.value, preKeyMapper.toCryptoBoxModel(preKey))
            }
        }.map {}
    }

    private fun <T> withSession(cryptoSessionId: CryptoSessionId, action: (session: CryptoSession) -> T): Either<CryptoBoxFailure, T> {
        return session(cryptoSessionId).flatMap { session ->
            useBox { action(session) }
        }
    }

    private fun session(cryptoSessionId: CryptoSessionId): Either<CryptoBoxFailure, CryptoSession> {
        return useBox {
            getSession(cryptoSessionId.value)
        }
    }

    private fun <T> useBox(action: CryptoBox.() -> T): Either<CryptoBoxFailure, T> {
        return try {
            cryptoBox.map {
                it.action()
            }
        } catch (cryptoException: CryptoException) {
            Either.Left(exceptionMapper.fromNativeException(cryptoException))
        }
    }

    private fun load(): Either<CryptoBoxFailure, CryptoBox> {
        if (cryptoBoxRootDirectory.exists() && !cryptoBoxRootDirectory.isDirectory) {
            return Either.Left(InitializationFailure())
        } else {
            cryptoBoxRootDirectory.mkdirs()
        }

        return try {
            Either.Right(CryptoBox.open(cryptoBoxRootDirectory.absolutePath))
        } catch (cryptoException: CryptoException) {
            Either.Left(exceptionMapper.fromNativeException(cryptoException))
        }
    }

    companion object {
        private const val CRYPTOBOX_PARENT_FOLDER_NAME = "otr"
        private const val PRE_KEYS_COUNT = 100
    }
}
