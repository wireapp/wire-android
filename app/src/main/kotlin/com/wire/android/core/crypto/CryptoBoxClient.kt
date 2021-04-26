package com.wire.android.core.crypto

import com.wire.android.core.crypto.mapper.PreKeyMapper
import com.wire.android.core.crypto.model.PreKeyInitialization
import com.wire.android.core.crypto.storage.PreKeyRepository
import com.wire.cryptobox.CryptoBox
import java.io.File

class CryptoBoxClient(
    private val cryptoBoxRootDirectory: File,
    private val preKeyRepository: PreKeyRepository,
    private val preKeyMapper: PreKeyMapper
) {

    private var _cryptoBox: CryptoBox? = null

    private val cryptoBox: CryptoBox
        get() = _cryptoBox ?: load().also { loadedBox ->
            _cryptoBox = loadedBox
        }

    private fun <T> useBox(action: CryptoBox.() -> T): T {
        return cryptoBox.action()
    }

    private fun load(): CryptoBox {
        if (cryptoBoxRootDirectory.exists()) {
            require(cryptoBoxRootDirectory.isDirectory) {
                "Provided cryptoBoxDirectory already exists and it is not a directory"
            }
        } else {
            cryptoBoxRootDirectory.mkdirs()
        }

        return CryptoBox.open(cryptoBoxRootDirectory.absolutePath)
    }

    fun delete() {
        useBox { close() }
        _cryptoBox = null
        cryptoBoxRootDirectory.deleteRecursively()
    }

    fun createInitialPreKeys(): PreKeyInitialization = useBox {
        val lastKey = preKeyMapper.fromCryptoBoxModel(newLastPreKey())
        val keys = newPreKeys(0, PRE_KEYS_COUNT).map(preKeyMapper::fromCryptoBoxModel)
        preKeyRepository.updateLastPreKeyID(keys.last().id)
        PreKeyInitialization(keys, lastKey)
    }

    companion object {
        private const val PRE_KEYS_COUNT = 100
        private const val LOW_PRE_KEYS_THRESHOLD = 50
        private const val LOCAL_PRE_KEYS_LIMIT = 16 * 1024
    }
}
