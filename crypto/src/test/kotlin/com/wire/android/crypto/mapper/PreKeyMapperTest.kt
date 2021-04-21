package com.wire.android.crypto.mapper

import com.wire.android.crypto.AndroidTest
import com.wire.android.crypto.model.PreKey
import org.amshove.kluent.`should be equal to`
import org.junit.Test

class PreKeyMapperTest : AndroidTest() {

    private val subject = PreKeyMapper()

    @Test
    fun `given a CryptoBox model, when converting to data model, it should be done correctly`() {
        val model = com.wire.cryptobox.PreKey(42, byteArrayOf(0x00, 0x16, 0x32, 0x42))
        val result = subject.fromCryptoBoxModel(model)

        result `should be equal to` PreKey(42, "ABYyQg==")
    }

    @Test
    fun `given a data model, when converting to CryptoBox model, it should be done correctly`() {
        val model = PreKey(42, "I wish you a very nice day")
        val result = subject.toCryptoBoxModel(model)

        val expectedResult = com.wire.cryptobox.PreKey(
            42,
            byteArrayOf(35, 8, -84, -121, 42, 46, 106, -9, -85, -54, 120, -100, 121, -42, -78)
        )

        result.data `should be equal to` expectedResult.data
        result.id `should be equal to` expectedResult.id
    }

    @Test
    fun `given data is being converted, when converting it back, it should return something equal to the original`() {
        val original = PreKey(2, "Test")

        val converted = subject.toCryptoBoxModel(original)

        subject.fromCryptoBoxModel(converted) `should be equal to` original
    }

    @Test
    fun `given CryptoBox model is being converted, when converting it back, it should return something equal to the original`() {
        val original = com.wire.cryptobox.PreKey(42, byteArrayOf(0x00, 0x16, 0x32, 0x42))

        val converted = subject.fromCryptoBoxModel(original)

        val result = subject.toCryptoBoxModel(converted)

        result.id `should be equal to` original.id
        result.data `should be equal to` result.data
    }

}
