package com.wire.android.shared.prekey.data.remote

import com.wire.android.UnitTest
import com.wire.android.core.crypto.model.PreKey
import com.wire.android.framework.collections.second
import com.wire.android.shared.user.QualifiedId
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.Before
import org.junit.Test

class RemotePrekeyListMapperTest : UnitTest() {

    @MockK
    private lateinit var prekeyMapper: RemotePreKeyMapper

    private lateinit var subject: RemotePreKeyListMapper

    @Before
    fun setup() {
        subject = RemotePreKeyListMapper(prekeyMapper)
    }

    @Test
    fun `given a PreKeyMap, when mapping to a list of UserPreKeyInfo, the users IDs should be converted correctly`() {
        val preKeyResponse = mapOf(
            "domA" to mapOf(
                "userA" to mapOf("clientA" to PreKeyResponse("keyA", 1)),
                "userB" to mapOf("clientB" to PreKeyResponse("key", 32))
            ),
            "domB" to mapOf(
                "userB" to mapOf("clientB" to PreKeyResponse("keyC", 18))
            )
        )

        val result = subject.fromRemotePreKeyInfoMap(preKeyResponse)

        result.size shouldBeEqualTo 3
        result.map { it.userId } shouldContainSame listOf(
            QualifiedId("domA", "userA"),
            QualifiedId("domA", "userB"),
            QualifiedId("domB", "userB")
        )
    }

    @Test
    fun `given a PreKeyMap, when mapping to a list of UserPreKeyInfo, the PreKeyMapper should be used`() {
        val preKeyResponse = mapOf(
            "domA" to mapOf(
                "userA" to mapOf(
                    "clientA" to PreKeyResponse("keyA", 1),
                    "clientB" to PreKeyResponse("keyB", 1)
                )
            )
        )
        every { prekeyMapper.fromRemoteResponse(any()) } returns PreKey(2, "2")

        subject.fromRemotePreKeyInfoMap(preKeyResponse)

        verify(exactly = 2) { prekeyMapper.fromRemoteResponse(any()) }
    }

    @Test
    fun `given a PreKeyMap, when mapping to a list of UserPreKeyInfo, the PreKeyMapper should receive the correct arguments`() {
        val firstKey = PreKeyResponse("keyA", 1)
        val secondKey = PreKeyResponse("keyB", 4)
        val preKeyResponse = mapOf(
            "domA" to mapOf(
                "userA" to mapOf(
                    "clientA" to firstKey,
                    "clientB" to secondKey
                )
            )
        )
        every { prekeyMapper.fromRemoteResponse(any()) } returns PreKey(2, "2")

        subject.fromRemotePreKeyInfoMap(preKeyResponse)

        verify(exactly = 1) { prekeyMapper.fromRemoteResponse(firstKey) }
        verify(exactly = 1) { prekeyMapper.fromRemoteResponse(secondKey) }
    }

    @Test
    fun `given a PreKeyMap, when mapping to a list of UserPreKeyInfo, the user IDs should be converted correctly`() {
        val preKeyResponse = mapOf(
            "domA" to mapOf(
                "userA" to mapOf("clientA" to PreKeyResponse("keyA", 1)),
                "userB" to mapOf("clientB" to PreKeyResponse("key", 32))
            ),
            "domB" to mapOf(
                "userB" to mapOf("clientB" to PreKeyResponse("keyC", 18))
            )
        )

        val result = subject.fromRemotePreKeyInfoMap(preKeyResponse)

        result.size shouldBeEqualTo 3
        result.map { it.userId } shouldContainSame listOf(
            QualifiedId("domA", "userA"),
            QualifiedId("domA", "userB"),
            QualifiedId("domB", "userB")
        )
    }

    @Test
    fun `given a PreKeyMap, when mapping to a list of UserPreKeyInfo, the clients should be returned in the right users`() {
        val preKeyResponse = mapOf(
            "domA" to mapOf(
                "userA" to mapOf(
                    "clientA" to PreKeyResponse("keyA", 1),
                    "clientB" to PreKeyResponse("keyA", 1)
                ),
                "userB" to mapOf("clientC" to PreKeyResponse("keyA", 1))
            )
        )

        val result = subject.fromRemotePreKeyInfoMap(preKeyResponse)

        val clientsA = result.first().clientsInfo.map { it.clientId }
        clientsA.size shouldBeEqualTo 2
        clientsA shouldContainSame listOf("clientA", "clientB")

        val clientsB = result.second().clientsInfo.map { it.clientId }
        clientsB.size shouldBeEqualTo 1
        clientsB shouldContainSame listOf("clientC")
    }

    @Test
    fun `given a PreKeyMap, when mapping to a list of UserPreKeyInfo, the keys should be returned in the right clients`() {
        class KeyMappingTestSet(val clientId: String, val response: PreKeyResponse, val mapped: PreKey)

        val firstKeySet = KeyMappingTestSet("a", PreKeyResponse("keyA", 1), PreKey(1, "keyA"))
        val secondKeySet = KeyMappingTestSet("b", PreKeyResponse("keyB", 4), PreKey(4, "keyB"))
        val thirdKeySet = KeyMappingTestSet("c", PreKeyResponse("keyC", 4), PreKey(4, "keyC"))

        val preKeyResponse = mapOf(
            "domA" to mapOf(
                "userA" to mapOf(
                    firstKeySet.clientId to firstKeySet.response,
                    secondKeySet.clientId to secondKeySet.response
                ),
                "userB" to mapOf(thirdKeySet.clientId to thirdKeySet.response)
            )
        )

        every { prekeyMapper.fromRemoteResponse(firstKeySet.response) } returns firstKeySet.mapped
        every { prekeyMapper.fromRemoteResponse(secondKeySet.response) } returns secondKeySet.mapped
        every { prekeyMapper.fromRemoteResponse(thirdKeySet.response) } returns thirdKeySet.mapped

        val result = subject.fromRemotePreKeyInfoMap(preKeyResponse)

        val userAInfo = result.first().clientsInfo
        val firstClient = userAInfo.first()
        firstClient.clientId shouldBeEqualTo firstKeySet.clientId
        firstClient.preKey shouldBeEqualTo firstKeySet.mapped

        val secondClient = userAInfo.second()

        secondClient.clientId shouldBeEqualTo secondKeySet.clientId
        secondClient.preKey shouldBeEqualTo secondKeySet.mapped

        val thirdClient = result.second().clientsInfo.first()
        thirdClient.clientId shouldBeEqualTo thirdKeySet.clientId
        thirdClient.preKey shouldBeEqualTo thirdKeySet.mapped
    }

}