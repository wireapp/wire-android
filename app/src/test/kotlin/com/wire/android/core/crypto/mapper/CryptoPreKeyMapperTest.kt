package com.wire.android.core.crypto.mapper

import com.wire.android.AndroidTest
import com.wire.android.core.crypto.model.PreKey
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import com.wire.cryptobox.PreKey as CryptoPreKey

class CryptoPreKeyMapperTest : AndroidTest() {

    private val subject = CryptoPreKeyMapper()

    @Test
    fun `given a CryptoBox model, when converting to data model, it should be done correctly`() {
        val model = CryptoPreKey(42, byteArrayOf(0x00, 0x16, 0x32, 0x42))
        val result = subject.fromCryptoBoxModel(model)

        result shouldBeEqualTo PreKey(42, "ABYyQg==")
    }

    @Test
    fun `given a data model, when converting to CryptoBox model, it should be done correctly`() {
        val model = PreKey(42, "I wish you a very nice day")
        val result = subject.toCryptoBoxModel(model)

        val expectedResult = CryptoPreKey(
            42,
            byteArrayOf(35, 8, -84, -121, 42, 46, 106, -9, -85, -54, 120, -100, 121, -42, -78)
        )

        result.data shouldBeEqualTo expectedResult.data
        result.id shouldBeEqualTo expectedResult.id
    }

    @Test
    fun `given data is being converted, when converting it back, it should return something equal to the original`() {
        val original = PreKey(2, "Test")

        val converted = subject.toCryptoBoxModel(original)

        subject.fromCryptoBoxModel(converted) shouldBeEqualTo original
    }

    @Test
    fun `given CryptoBox model is being converted, when converting it back, it should return something equal to the original`() {
        val original = CryptoPreKey(42, byteArrayOf(0x00, 0x16, 0x32, 0x42))

        val converted = subject.fromCryptoBoxModel(original)

        val result = subject.toCryptoBoxModel(converted)

        result.id shouldBeEqualTo original.id
        result.data shouldBeEqualTo result.data
    }

}
