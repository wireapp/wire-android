package com.wire.android.navigation

import com.wire.android.model.ImageAsset
import com.wire.android.model.parseIntoPrivateImageAsset
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class NavigationUtilsTest {

    @Test
    fun `Given some correct string, when calling parseIntoQualifiedID, then it correctly parses it to QualifiedID`() {
        // Given
        val mockQualifiedIdValue = "mocked-value"
        val mockQualifiedIdDomain = "mocked.domain"
        val correctQualifiedIdString = "$mockQualifiedIdValue@$mockQualifiedIdDomain"

        // When
        val correctQualifiedId = correctQualifiedIdString.parseIntoQualifiedID()

        // Then
        assertEquals(correctQualifiedId.value, mockQualifiedIdValue)
        assertEquals(correctQualifiedId.domain, mockQualifiedIdDomain)
    }

    @Test
    fun `Given some correct string, when calling parseIntoPrivateImageAsset, then it correctly parses it to PrivateImageAsset`() {
        // Given
        val mockConversationIdValue = "mocked-conversation-id-value"
        val mockConversationIdDomain = "mocked.domain"
        val mockMessageId = "mocked-message-id"
        val correctImagePrivateAssetString = "$mockConversationIdValue@$mockConversationIdDomain:$mockMessageId"

        // When
        val privateImgAsset = correctImagePrivateAssetString.parseIntoPrivateImageAsset()

        // Then
        assertEquals(privateImgAsset.conversationId.value, mockConversationIdValue)
        assertEquals(privateImgAsset.conversationId.domain, mockConversationIdDomain)
        assertEquals(privateImgAsset.messageId, mockMessageId)
    }

    @Test
    fun `Given an incorrect string, when parsing it to QualifiedId, then it throws an exception`() {
        // Given
        val mockWrongImagePrivateAssetString = "wrong-private-asset/image"

        // When, Then
        assertThrows<Exception> { mockWrongImagePrivateAssetString.parseIntoQualifiedID() }
    }

    @Test
    fun `Given an incorrect string, when parsing it to PrivateImageAsset, then it throws an exception`() {
        // Given
        val mockWrongImagePrivateAssetString = "wrong-private-asset@image"

        // When, Then
        assertThrows<Exception> { mockWrongImagePrivateAssetString.parseIntoPrivateImageAsset() }
    }

    @Test
    fun `Given some correct QualifiedId object, it parses it correctly to string`() {
        // Given
        val mockQualifiedIdValue = "mocked-value"
        val mockQualifiedIdDomain = "mocked.domain"
        val actualQualifiedId = QualifiedID(value = mockQualifiedIdValue, domain = mockQualifiedIdDomain)
        val expectedQualifiedID = "$mockQualifiedIdValue@$mockQualifiedIdDomain"

        // When
        val mappedQualifiedId = actualQualifiedId.toString()

        // Then
        assertEquals(mappedQualifiedId, expectedQualifiedID)
    }

    @Test
    fun `Given some correct Image PrivateAsset object, it parses it correctly to string`() {
        // Given
        val mockQualifiedIdValue = "mocked-value"
        val mockQualifiedIdDomain = "mocked.domain"
        val mockMessageId = "mocked-message-id"
        val actualPrivateAssetImage = ImageAsset.PrivateAsset(
            QualifiedID(
                value = mockQualifiedIdValue,
                domain = mockQualifiedIdDomain
            ), mockMessageId
        )
        val expectedPrivateAssetImage = "$mockQualifiedIdValue@$mockQualifiedIdDomain:$mockMessageId"

        // When
        val mappedImagePrivateAsset = actualPrivateAssetImage.toString()

        // Then
        assertEquals(mappedImagePrivateAsset, expectedPrivateAssetImage)
    }
}
