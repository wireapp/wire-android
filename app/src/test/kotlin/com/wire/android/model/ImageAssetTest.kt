package com.wire.android.model

import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserAssetId
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ImageAssetTest {

    @MockK
    private lateinit var imageLoader: WireSessionImageLoader

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }

    fun createUserAvatarAsset(userAssetId: UserAssetId) = ImageAsset.UserAvatarAsset(
        imageLoader, userAssetId
    )

    fun createPrivateAsset(
        conversationId: ConversationId,
        messageId: String,
        isSelfAsset: Boolean
    ) = ImageAsset.PrivateAsset(
        imageLoader, conversationId, messageId, isSelfAsset
    )

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
}
