/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.datastore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.wire.android.util.crypto.AppCryptoServiceInfo
import com.wire.android.util.crypto.appCryptoServiceInfo
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.security.InvalidKeyException
import java.security.KeyStore
import javax.crypto.AEADBadTagException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object EncryptionManager {

    private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"

    private val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
    private val cipher = Cipher.getInstance(TRANSFORMATION)
    private val charset = Charset.defaultCharset()

    /**
     * Which providers serve DataStore crypto, for the security providers debug screen.
     *
     * Lives here, next to the call sites, so it shares their algorithm constants: change a constant and
     * this follows automatically instead of quietly reporting the old one.
     *
     * The key generator is only resolved, never initialised with a [KeyGenParameterSpec] or asked for a
     * key, so nothing is written to the Android keystore.
     */
    fun cryptoServices(): List<AppCryptoServiceInfo> = listOfNotNull(
        appCryptoServiceInfo("DataStore keystore", "KeyStore.getInstance(\"$ANDROID_KEY_STORE\")") {
            KeyStore.getInstance(ANDROID_KEY_STORE).run { type to provider }
        },
        appCryptoServiceInfo("DataStore key generation", "KeyGenerator.getInstance(\"$ALGORITHM\")") {
            KeyGenerator.getInstance(ALGORITHM).run { algorithm to provider }
        },
        appCryptoServiceInfo("DataStore cipher", "Cipher.getInstance(\"$TRANSFORMATION\")") {
            Cipher.getInstance(TRANSFORMATION).run { algorithm to provider }
        },
    )

    private fun getKey(keyAlias: String): SecretKey {
        val existingKey = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createKey(keyAlias)
    }

    private fun createKey(keyAlias: String): SecretKey {
        return KeyGenerator.getInstance(ALGORITHM).apply {
            init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(BLOCK_MODE)
                    .setEncryptionPaddings(PADDING)
                    .setUserAuthenticationRequired(false)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
        }.generateKey()
    }

    @Throws(
        UnsupportedOperationException::class,
        InvalidKeyException::class,
        IllegalStateException::class,
        IllegalBlockSizeException::class,
        BadPaddingException::class,
        AEADBadTagException::class,
        UnsupportedEncodingException::class
    )
    fun encrypt(keyAlias: String, text: String): String {
        cipher.init(Cipher.ENCRYPT_MODE, getKey(keyAlias))
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(text.toByteArray())
        return listOf(encryptedBytes, iv)
            .map { String(Base64.encode(it, Base64.NO_WRAP), charset) }
            .joinToString(":")
    }

    @Throws(
        UnsupportedOperationException::class,
        InvalidKeyException::class,
        IllegalStateException::class,
        IllegalBlockSizeException::class,
        BadPaddingException::class,
        AEADBadTagException::class,
        UnsupportedEncodingException::class
    )
    @Suppress("MagicNumber")
    fun decrypt(keyAlias: String, encryptedText: String): String {
        val (encryptedData, iv) = encryptedText.split(":")
            .map { Base64.decode(it.toByteArray(charset), Base64.NO_WRAP) }
            .let { it[0] to it[1] }
        cipher.init(Cipher.DECRYPT_MODE, getKey(keyAlias), GCMParameterSpec(128, iv))
        val decryptedBytes = cipher.doFinal(encryptedData)
        return String(decryptedBytes)
    }
}
