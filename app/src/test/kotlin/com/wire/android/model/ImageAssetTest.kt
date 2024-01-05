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

package com.wire.android.model

import android.net.Uri
import androidx.core.net.toUri
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserAssetId
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ImageAssetTest {

    @MockK
    private lateinit var imageLoader: WireSessionImageLoader

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        val mockUri = mockk<Uri>()
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockUri
    }

    fun createUserAvatarAsset(userAssetId: UserAssetId) = ImageAsset.UserAvatarAsset(
        imageLoader, userAssetId
    )

    fun createPrivateAsset(
        conversationId: ConversationId,
        messageId: String,
        isSelfAsset: Boolean
    ) = ImageAsset.PrivateAsset(imageLoader, conversationId, messageId, isSelfAsset)

    fun createLocalAsset(
        dataUri: Uri,
        imageKey: String
    ): ImageAsset.LocalImageAsset {
        return ImageAsset.LocalImageAsset(imageLoader, dataUri, imageKey)
    }

    @Test
    fun givenEqualUserAvatarAssets_whenGettingUniqueKey_thenResultsShouldBeEqual() {
        val userAssetId = UserAssetId(value = "xu", domain = "bi")
        val subject1 = createUserAvatarAsset(userAssetId)
        val subject2 = createUserAvatarAsset(userAssetId)

        subject1.uniqueKey shouldBeEqualTo subject2.uniqueKey
    }

    @Test
    fun givenDifferentUserAvatarAssets_whenGettingUniqueKey_thenResultsShouldBeDifferent() {
        val userAssetId = UserAssetId(value = "xu", domain = "bi")
        val baseSubject = createUserAvatarAsset(userAssetId)
        val alteredValueSubject = createUserAvatarAsset(userAssetId.copy(value = "anotherValue"))
        val alteredDomainSubject = createUserAvatarAsset(userAssetId.copy(domain = "anotherDomain"))

        baseSubject.uniqueKey shouldNotBeEqualTo alteredValueSubject.uniqueKey
        baseSubject.uniqueKey shouldNotBeEqualTo alteredDomainSubject.uniqueKey
    }

    @Test
    fun givenEqualPrivateAssets_whenGettingUniqueKey_thenResultsShouldBeEqual() {
        val conversationId = ConversationId("xu", "bi")
        val messageId = "messageId"
        val isSelfAsset = true

        val subject1 = createPrivateAsset(
            conversationId,
            messageId,
            isSelfAsset
        )
        val subject2 = createPrivateAsset(
            conversationId,
            messageId,
            isSelfAsset
        )

        subject1.uniqueKey shouldBeEqualTo subject2.uniqueKey
    }

    @Test
    fun givenDifferentPrivateAssets_whenGettingUniqueKey_thenResultsShouldBeDifferent() {
        val conversationId = ConversationId("xu", "bi")
        val messageId = "messageId"
        val isSelfAsset = true

        val baseSubject = createPrivateAsset(
            conversationId,
            messageId,
            isSelfAsset
        )
        val alteredConversationIdSubject = createPrivateAsset(
            conversationId.copy(value = "SomeOtherValue"),
            messageId,
            isSelfAsset
        )
        val alteredMessageIdSubject = createPrivateAsset(
            conversationId,
            "someOtherMessageId",
            isSelfAsset
        )
        val alteredSelfAssetSubject = createPrivateAsset(
            conversationId,
            messageId,
            isSelfAsset.not()
        )

        baseSubject.uniqueKey shouldNotBeEqualTo alteredConversationIdSubject.uniqueKey
        baseSubject.uniqueKey shouldNotBeEqualTo alteredMessageIdSubject.uniqueKey
        baseSubject.uniqueKey shouldNotBeEqualTo alteredSelfAssetSubject.uniqueKey
    }

    @Test
    fun givenEqualUriAndKeyLocalAssets_whenGettingUniqueKey_thenResultsShouldBeEqual() {
        val assetKey = "assetKey"
        val localAssetUri = "local-uri".toUri()

        val subject1 = createLocalAsset(
            localAssetUri,
            assetKey,
        )
        val subject2 = createLocalAsset(
            localAssetUri,
            assetKey,
        )

        subject1.uniqueKey shouldBeEqualTo subject2.uniqueKey
    }

    @Test
    fun givenSameUriButDifferentKeyLocalAssets_whenGettingUniqueKey_thenResultsShouldBeDifferent() {
        val assetUri = "assetUri".toUri()
        val assetKey = "assetKey"

        val baseSubject = createLocalAsset(
            assetUri,
            assetKey
        )
        val alteredAssetKeySubject = createLocalAsset(
            assetUri,
            "someOtherAssetKey",
        )

        baseSubject.uniqueKey shouldNotBeEqualTo alteredAssetKeySubject.uniqueKey
    }

    @Test
    fun givenSameKeyButDifferentUriLocalAssets_whenGettingUniqueKey_thenResultsShouldBeTheSame() {
        val assetUri1 = "assetUri1".toUri()
        val assetUri2 = "assetUri2".toUri()
        val assetKey = "assetKey"

        val baseSubject = createLocalAsset(
            assetUri1,
            assetKey
        )
        val alteredUriSubject = createLocalAsset(
            assetUri2,
            assetKey
        )

        baseSubject.uniqueKey shouldBeEqualTo alteredUriSubject.uniqueKey
    }
}
