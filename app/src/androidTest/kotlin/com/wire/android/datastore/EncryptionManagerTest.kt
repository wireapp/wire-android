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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EncryptionManagerTest {

    @Test
    fun givenKeyAlias_whenEncryptingAndDecryptingWithTheSameKeyAlias_thenTheOriginalValueReturned() {
        val data = "dataToBeEncrypted123!"
        val keyAlias = "key_alias"

        val encryptedData = EncryptionManager.encrypt(keyAlias, data)
        val decryptedData = EncryptionManager.decrypt(keyAlias, encryptedData)

        assertNotEquals(data, encryptedData)
        assertEquals(data, decryptedData)
    }

    @Test
    fun givenTwoKeyAliases_whenEncryptingWithOneKeyAliasAndDecryptingWithOtherKeyAlias_thenExceptionThrown() {
        val data = "dataToBeEncrypted123!"
        val keyAlias1 = "key_alias1"
        val keyAlias2 = "key_alias2"

        val encryptedData = EncryptionManager.encrypt(keyAlias1, data)
        assertThrows(Exception::class.java) { EncryptionManager.decrypt(keyAlias2, encryptedData) }
    }
}
