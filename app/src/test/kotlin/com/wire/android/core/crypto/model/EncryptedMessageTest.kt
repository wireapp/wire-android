package com.wire.android.core.crypto.model

import com.wire.android.UnitTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.Test

import org.junit.Assert.*

class EncryptedMessageTest: UnitTest() {

    @Test
    fun `given two identical objects, when checking identities, they should be equals`() {
        val x = EncryptedMessage(byteArrayOf(1, 2, 3))
        val y = EncryptedMessage(byteArrayOf(1, 2, 3))
        x shouldBeEqualTo y
        x.hashCode() shouldBeEqualTo y.hashCode()
    }

    @Test
    fun `given two different objects, when checking identities, they should not be equals`() {
        val x = EncryptedMessage(byteArrayOf(1, 2, 3))
        val y = EncryptedMessage(byteArrayOf(3, 2, 1))
        x shouldNotBeEqualTo y
        x.hashCode() shouldNotBeEqualTo y.hashCode()
    }
}
