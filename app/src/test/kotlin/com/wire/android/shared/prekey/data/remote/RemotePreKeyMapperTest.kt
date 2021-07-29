package com.wire.android.shared.prekey.data.remote


import com.wire.android.UnitTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class RemotePreKeyMapperTest : UnitTest() {

    private val subject = RemotePreKeyMapper()

    @Test
    fun `given a PreKeyResponse, when mapping to PreKey, then the key string should match`() {
        val preKeyResponse = PreKeyResponse("42", 1)

        subject.fromRemoteResponse(preKeyResponse)
            .encodedData shouldBeEqualTo "42"
    }

    @Test
    fun `given a PreKeyResponse, when mapping to PreKey, then the ID should match`() {
        val preKeyResponse = PreKeyResponse("42", 1)

        subject.fromRemoteResponse(preKeyResponse)
            .id shouldBeEqualTo 1
    }
}