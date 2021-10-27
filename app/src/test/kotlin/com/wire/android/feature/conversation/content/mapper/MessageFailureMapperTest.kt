package com.wire.android.feature.conversation.content.mapper

import com.wire.android.UnitTest
import com.wire.android.feature.messaging.datasource.remote.api.MessageSendingErrorBody
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class MessageFailureMapperTest : UnitTest() {

    private lateinit var subject: MessageFailureMapper

    @Before
    fun setUp() {
        subject = MessageFailureMapper()
    }

    @Test
    fun `given a SendingErrorBody, when mapping to SendMessageFailure, the missing IDs should be the same`() {
        val missingClientsOfUsers: Map<String, List<String>> = mockk()
        val errorBody = MessageSendingErrorBody(missingClientsOfUsers, mockk(), mockk())

        val result = subject.fromMessageSendingErrorBody(errorBody)

        result.missingClientsOfUsers shouldBeEqualTo missingClientsOfUsers
    }

    @Test
    fun `given a SendingErrorBody, when mapping to SendMessageFailure, the redundant IDs should be the same`() {
        val redundantClientsOfUsers: Map<String, List<String>> = mockk()
        val errorBody = MessageSendingErrorBody(mockk(), redundantClientsOfUsers, mockk())

        val result = subject.fromMessageSendingErrorBody(errorBody)

        result.redundantClientsOfUsers shouldBeEqualTo redundantClientsOfUsers
    }

    @Test
    fun `given a SendingErrorBody, when mapping to SendMessageFailure, the deleted IDs should be the same`() {
        val deletedClientsOfUsers: Map<String, List<String>> = mockk()
        val errorBody = MessageSendingErrorBody(mockk(), mockk(), deletedClientsOfUsers)

        val result = subject.fromMessageSendingErrorBody(errorBody)

        result.deletedClientsOfUsers shouldBeEqualTo deletedClientsOfUsers
    }
}
