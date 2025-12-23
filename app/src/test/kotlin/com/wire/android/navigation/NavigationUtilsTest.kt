/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.navigation

import com.wire.android.model.ImageAsset
import com.wire.android.model.parseIntoPrivateImageAsset
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
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
        val mockIsEphemeral = "true"

        val correctImagePrivateAssetString =
            "$mockConversationIdValue@$mockConversationIdDomain:$mockMessageId:$mockIsSelfAsset:$mockIsEphemeral"

        // When
        val privateImgAsset = correctImagePrivateAssetString.parseIntoPrivateImageAsset(qualifiedIdMapper)

        // Then
        assertEquals(privateImgAsset.conversationId.value, mockConversationIdValue)
        assertEquals(privateImgAsset.conversationId.domain, mockConversationIdDomain)
        assertEquals(privateImgAsset.messageId, mockMessageId)
        assertEquals(privateImgAsset.isSelfAsset, mockIsSelfAsset.toBoolean())
        assertEquals(privateImgAsset.isEphemeral, mockIsEphemeral.toBoolean())
    }

    @Test
    fun `Given an incorrect string, when parsing it to PrivateImageAsset, then it throws an exception`() {
        val qualifiedIdMapper = QualifiedIdMapper(null)

        // Given
        val mockWrongImagePrivateAssetString = "wrong-private-asset@image"

        // When, Then
        assertThrows<Exception> { mockWrongImagePrivateAssetString.parseIntoPrivateImageAsset(qualifiedIdMapper) }
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
            conversationId = QualifiedID(
                value = mockQualifiedIdValue,
                domain = mockQualifiedIdDomain
            ),
            messageId = mockMessageId,
            isSelfAsset = true,
            isEphemeral = true
        )
        val expectedPrivateAssetImage = "$mockQualifiedIdValue@$mockQualifiedIdDomain:$mockMessageId:true:true"

        // When
        val mappedImagePrivateAsset = actualPrivateAssetImage.toString()

        // Then
        assertEquals(mappedImagePrivateAsset, expectedPrivateAssetImage)
    }

    @Test
    fun `given route without segments and parameters, when getting primary route, then return only host part which is the same route`() {
        // Given
        val route = "route"
        // When
        val result = route.getBaseRoute()
        // Then
        assertEquals(route, result)
    }

    @Test
    fun `given route with segments but without parameters, when getting primary route, then return only host part`() {
        // Given
        val host = "route"
        val route = "$host/segment1/segment2"
        // When
        val result = route.getBaseRoute()
        // Then
        assertEquals(host, result)
    }

    @Test
    fun `given route without segments but with parameters, when getting primary route, then return only host part`() {
        // Given
        val host = "route"
        val route = "$host?param1=value1&param2=value2"
        // When
        val result = route.getBaseRoute()
        // Then
        assertEquals(host, result)
    }

    @Test
    fun `given route with segments and parameters, when getting primary route, then return only host part`() {
        // Given
        val host = "route"
        val route = "$host/segment1/segment2?param1=value1&param2=value2"
        // When
        val result = route.getBaseRoute()
        // Then
        assertEquals(host, result)
    }
}
