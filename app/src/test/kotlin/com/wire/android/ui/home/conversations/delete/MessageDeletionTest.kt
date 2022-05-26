package com.wire.android.ui.home.conversations.delete

import org.amshove.kluent.internal.assertEquals
import org.junit.Test

class MessageDeletionTest {

    @Test
    fun `Given a MessageDeletion object, when mapping to String, it maps correctly`() {
        // Given
        val mockMessageId = "some-mocked-message-id"
        val mockIsSelfMessage = false
        val mockMessageDeletion = MessageDeletion(mockMessageId, mockIsSelfMessage)
        val expectedMessageDeletionString = "$mockMessageDeletion:$mockIsSelfMessage"

        // When, Then
        assertEquals(mockMessageDeletion.toString(), expectedMessageDeletionString)
    }

    @Test
    fun `Given a correct MessageDeletion string, when mapping to MessageDeletion object, it maps correctly`() {
        // Given
        val mockMessageId = "some-mocked-message-id"
        val mockIsSelfMessage = false
        val expectedMessageDeletion = MessageDeletion(mockMessageId, mockIsSelfMessage)
        val mockMessageDeletionString = "$expectedMessageDeletion:$mockIsSelfMessage"

        // When, Then
        assertEquals(mockMessageDeletionString.parseIntoMessageDeletion(), expectedMessageDeletion)
    }
}
