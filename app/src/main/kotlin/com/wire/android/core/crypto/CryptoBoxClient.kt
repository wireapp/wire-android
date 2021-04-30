package com.wire.android.core.crypto

import android.content.Context
import com.wire.android.core.crypto.data.CryptoBoxClientPropertyStorage
import com.wire.android.core.crypto.mapper.PreKeyMapper
import com.wire.android.core.crypto.model.PreKeyInitialization
import com.wire.android.core.crypto.model.UserId
import com.wire.android.core.extension.plus
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.cryptobox.CryptoBox
import com.wire.cryptobox.CryptoException

//TODO: Inject this
class CryptoBoxClient(
    context: Context,
    private val clientPropertyStorage: CryptoBoxClientPropertyStorage,
    private val userId: UserId,
    private val preKeyMapper: PreKeyMapper
) {

    private var _cryptoBox: CryptoBox? = null
    private val cryptoBoxRootDirectory =
        context.filesDir + CRYPTOBOX_PARENT_FOLDER_NAME + userId.toString()

    private val cryptoBox: Either<CryptoException, CryptoBox>
        get() = _cryptoBox?.let { Either.Right(it) } ?: load().map {
            _cryptoBox = it
            it
        }

    private fun <T> useBox(action: CryptoBox.() -> T): Either<CryptoException, T> {
        return try {
            cryptoBox.map {
                it.action()
            }
        } catch (cryptoException: CryptoException) {
            Either.Left(cryptoException)
        }
    }

    //TODO: Replace CryptoException with a wrapper, so we can better control failures
    private fun load(): Either<CryptoException, CryptoBox> {
        if (cryptoBoxRootDirectory.exists()) {
            check(cryptoBoxRootDirectory.isDirectory) {
                "Provided cryptoBoxDirectory already exists and it is not a directory"
            }
        } else {
            cryptoBoxRootDirectory.mkdirs()
        }

        return try {
            Either.Right(CryptoBox.open(cryptoBoxRootDirectory.absolutePath))
        } catch (cryptoException: CryptoException) {
            Either.Left(cryptoException)
        }
    }

    fun delete() = useBox {
        close()
        _cryptoBox = null
        cryptoBoxRootDirectory.deleteRecursively()
    }

    fun createInitialPreKeys(): Either<CryptoException, PreKeyInitialization> = useBox {
        val lastKey = preKeyMapper.fromCryptoBoxModel(newLastPreKey())
        val keys = newPreKeys(0, PRE_KEYS_COUNT).map(preKeyMapper::fromCryptoBoxModel)
        clientPropertyStorage.updateLastPreKeyId(userId, keys.last().id)
        PreKeyInitialization(keys, lastKey)
    }

    companion object {
        private const val CRYPTOBOX_PARENT_FOLDER_NAME = "otr"
        private const val PRE_KEYS_COUNT = 100
    }
}
