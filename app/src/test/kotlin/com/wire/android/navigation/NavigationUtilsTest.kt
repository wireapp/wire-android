package com.wire.android.navigation

import com.wire.android.model.ImageAsset
import com.wire.kalium.logic.data.id.QualifiedID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class NavigationUtilsTest {

    @Test
    fun `Parsing string correctly to QualifiedID`() {
        val mockQualifiedIdValue = "mocked-value"
        val mockQualifiedIdDomain = "mocked.domain"
        val correctQualifiedIdString = "$mockQualifiedIdDomain@$mockQualifiedIdValue"
        val correctQualifiedId = correctQualifiedIdString.parseIntoQualifiedID()

        assertEquals(correctQualifiedId.value, mockQualifiedIdValue)
        assertEquals(correctQualifiedId.domain, mockQualifiedIdDomain)
    }

    @Test
    fun `Parsing string correctly to PrivateImageAsset`() {
        val mockConversationIdValue = "mocked-conversation-id-value"
        val mockConversationIdDomain = "mocked.domain"
        val mockMessageId = "mocked-message-id"
        val correctImagePrivateAssetString = "$mockConversationIdDomain@$mockConversationIdValue:$mockMessageId"
        val privateImgAsset = correctImagePrivateAssetString.parseIntoPrivateImageAsset()

        assertEquals(privateImgAsset.conversationId.value, mockConversationIdValue)
        assertEquals(privateImgAsset.conversationId.domain, mockConversationIdDomain)
        assertEquals(privateImgAsset.messageId, mockMessageId)
    }

    @Test
    fun `Parsing incorrect string to QualifiedId throws exception`() {
        val mockWrongImagePrivateAssetString = "wrong-private-asset/image"

        assertThrows<Exception> { mockWrongImagePrivateAssetString.parseIntoQualifiedID() }
    }

    @Test
    fun `Parsing incorrect string to PrivateImageAsset throws exception`() {
        val mockWrongImagePrivateAssetString = "wrong-private-asset@image"

        assertThrows<Exception> { mockWrongImagePrivateAssetString.parseIntoPrivateImageAsset() }
    }

    @Test
    fun `Map QualifiedId correctly to string`() {
        val mockQualifiedIdValue = "mocked-value"
        val mockQualifiedIdDomain = "mocked.domain"
        val mockQualifiedId = QualifiedID(mockQualifiedIdValue, mockQualifiedIdDomain)

        val mappedQualifiedId = mockQualifiedId.mapIntoArgumentString()

        assertEquals(mappedQualifiedId, "$mockQualifiedIdDomain@$mockQualifiedIdValue")
    }

    @Test
    fun `Map correct Image PrivateAsset to string`() {
        val mockQualifiedIdValue = "mocked-value"
        val mockQualifiedIdDomain = "mocked.domain"
        val mockMessageId = "mocked-message-id"
        val mockPrivateImageAsset = ImageAsset.PrivateAsset(QualifiedID(mockQualifiedIdValue, mockQualifiedIdDomain), mockMessageId)

        val mappedImagePrivateAsset = mockPrivateImageAsset.mapIntoArgumentsString()

        assertEquals(mappedImagePrivateAsset, "$mockQualifiedIdDomain@$mockQualifiedIdValue:$mockMessageId")
    }
}
