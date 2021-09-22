package com.wire.android.feature.auth.client.datasource.remote

import com.wire.android.UnitTest
import com.wire.android.feature.auth.client.datasource.remote.api.ClientsOfUsersResponse
import com.wire.android.feature.auth.client.datasource.remote.api.SimpleClientResponse
import com.wire.android.shared.user.QualifiedId
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class ClientRemoteMapperTest : UnitTest(){

    private val subject = ClientRemoteMapper()

    @Test
    fun `given a Qualified ID, when converting to DTO, the domain should stay the same`(){
        val originalQualifiedId = QualifiedId("domain", "id")

        val result = subject.fromQualifiedIdToQualifiedIdDTO(originalQualifiedId)

        result.domain shouldBeEqualTo originalQualifiedId.domain
    }

    @Test
    fun `given a Qualified ID, when converting to DTO, the id should stay the same`(){
        val originalQualifiedId = QualifiedId("domain", "id")

        val result = subject.fromQualifiedIdToQualifiedIdDTO(originalQualifiedId)

        result.id shouldBeEqualTo originalQualifiedId.id
    }

    @Test
    fun `given an ordinary response of users clients, when converting to a map of qualified client id list, the values should be correct`(){
        // Given Domain 1: First user
        val client111 = SimpleClientResponse("client111", "mobile")
        val client112 = SimpleClientResponse("client112", "computer")
        val user11 = "user11" to listOf(client111, client112)

        // Given Domain 1: Second user
        val client121 = SimpleClientResponse("client121", "")
        val user12 = "user12" to listOf(client121)

        // Given Domain 1
        val domain1 = "domain1" to mapOf(user11, user12)

        // Given Domain 2: First user
        val client211 = SimpleClientResponse("client211", "another one")
        val user21 = "user21" to listOf(client211)
        val domain2 = "domain2" to mapOf(user21)

        // Given Domain 2
        val originalResponse = ClientsOfUsersResponse(mapOf(domain1, domain2))

        // When
        val result = subject.fromClientsOfUsersResponseToMapOfQualifiedClientIds(originalResponse).entries.toList()

        //Then
        result.size shouldBeEqualTo 3
        result[0].toPair() shouldBeEqualTo (QualifiedId(domain1.first, user11.first) to listOf(client111.id, client112.id))
        result[1].toPair() shouldBeEqualTo (QualifiedId(domain1.first, user12.first) to listOf(client121.id))
        result[2].toPair() shouldBeEqualTo (QualifiedId(domain2.first, user21.first) to listOf(client211.id))
    }

    @Test
    fun `given a client response without clients, when converting to a map of qualified client id list, the result should contain no clients`(){
        // Given Domain 1: First user
        val user11 = "user11" to listOf<SimpleClientResponse>()

        // Given Domain 1
        val domain1 = "domain1" to mapOf(user11)

        val originalResponse = ClientsOfUsersResponse(mapOf(domain1))

        // When
        val result = subject.fromClientsOfUsersResponseToMapOfQualifiedClientIds(originalResponse).entries.toList()

        //Then
        result.size shouldBeEqualTo 1
        result[0].toPair() shouldBeEqualTo (QualifiedId(domain1.first, user11.first) to listOf())
    }
}
