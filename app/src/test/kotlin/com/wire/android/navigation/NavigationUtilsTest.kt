package com.wire.android.navigation

import com.wire.android.model.ImageAsset
import com.wire.android.model.parseIntoPrivateImageAsset
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class NavigationUtilsTest {

    @Test
    fun `Given some correct string, when calling parseIntoPrivateImageAsset, then it correctly parses it to PrivateImageAsset`() {
        val qualifiedIdMapper = QualifiedIdMapper(null)

        // Given
        val mockConversationIdValue = "mocked-conversation-id-value"
        val mockConversationIdDomain = "mocked.domain"
        val mockMessageId = "mocked-message-id"
        val mockIsSelfAsset = "true"
        val correctImagePrivateAssetString = "$mockConversationIdValue@$mockConversationIdDomain:$mockMessageId:$mockIsSelfAsset"

        // When
        val privateImgAsset = correctImagePrivateAssetString.parseIntoPrivateImageAsset(mockk(), qualifiedIdMapper)

        // Then
        assertEquals(privateImgAsset.conversationId.value, mockConversationIdValue)
        assertEquals(privateImgAsset.conversationId.domain, mockConversationIdDomain)
        assertEquals(privateImgAsset.messageId, mockMessageId)
        assertEquals(privateImgAsset.isSelfAsset, mockIsSelfAsset.toBoolean())
    }

    @Test
    fun `Given an incorrect string, when parsing it to PrivateImageAsset, then it throws an exception`() {
        val qualifiedIdMapper = QualifiedIdMapper(null)

        // Given
        val mockWrongImagePrivateAssetString = "wrong-private-asset@image"

        // When, Then
        assertThrows<Exception> { mockWrongImagePrivateAssetString.parseIntoPrivateImageAsset(mockk(), qualifiedIdMapper) }
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
            mockk(),
            QualifiedID(
                value = mockQualifiedIdValue,
                domain = mockQualifiedIdDomain
            ), mockMessageId, true
        )
        val expectedPrivateAssetImage = "$mockQualifiedIdValue@$mockQualifiedIdDomain:$mockMessageId:true"

        // When
        val mappedImagePrivateAsset = actualPrivateAssetImage.toString()

        // Then
        assertEquals(mappedImagePrivateAsset, expectedPrivateAssetImage)
    }
}
