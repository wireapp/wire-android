package com.wire.android.feature.contact.datasources.mapper

import com.wire.android.UnitTest
import com.wire.android.feature.contact.datasources.local.ContactEntity
import com.wire.android.feature.contact.datasources.remote.ContactResponse
import com.wire.android.framework.collections.second
import com.wire.android.shared.asset.PublicAsset
import com.wire.android.shared.asset.datasources.remote.AssetResponse
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Test

class ContactMapperTest : UnitTest() {

    private lateinit var contactMapper: ContactMapper

    @Before
    fun setUp() {
        contactMapper = ContactMapper()
    }

    @Test
    fun `given fromContactResponseListToEntityList is called, then returns list of entities with a proper mapping`() {
        val contactResponseWithAsset = mockk<ContactResponse>()
        every { contactResponseWithAsset.id } returns TEST_CONTACT_ID_1
        every { contactResponseWithAsset.name } returns TEST_CONTACT_NAME_1
        val contactAssetResponse = mockk<AssetResponse>()
        every { contactAssetResponse.size } returns ASSET_SIZE_COMPLETE
        every { contactAssetResponse.key } returns TEST_ASSET_KEY
        every { contactResponseWithAsset.assets } returns listOf(contactAssetResponse)

        val contactResponse = mockk<ContactResponse>()
        every { contactResponse.id } returns TEST_CONTACT_ID_2
        every { contactResponse.name } returns TEST_CONTACT_NAME_2
        every { contactResponse.assets } returns emptyList()

        val result = contactMapper.fromContactResponseListToEntityList(
            listOf(contactResponseWithAsset, contactResponse)
        )

        result.first().let {
            it shouldBeInstanceOf ContactEntity::class
            it.id shouldBeEqualTo TEST_CONTACT_ID_1
            it.name shouldBeEqualTo TEST_CONTACT_NAME_1
            it.assetKey shouldBeEqualTo TEST_ASSET_KEY
        }
        result.second().let {
            it shouldBeInstanceOf ContactEntity::class
            it.id shouldBeEqualTo TEST_CONTACT_ID_2
            it.name shouldBeEqualTo TEST_CONTACT_NAME_2
            it.assetKey shouldBeEqualTo null
        }
    }

    @Test
    fun `given fromContactEntityList is called, then returns list of contacts with a proper mapping`() {
        val contactEntityWithAsset = mockk<ContactEntity>()
        every { contactEntityWithAsset.id } returns TEST_CONTACT_ID_1
        every { contactEntityWithAsset.name } returns TEST_CONTACT_NAME_1
        every { contactEntityWithAsset.assetKey } returns TEST_ASSET_KEY

        val contactEntity = mockk<ContactEntity>()
        every { contactEntity.id } returns TEST_CONTACT_ID_2
        every { contactEntity.name } returns TEST_CONTACT_NAME_2
        every { contactEntity.assetKey } returns null

        val result = contactMapper.fromContactEntityList(
            listOf(contactEntityWithAsset, contactEntity)
        )

        result.first().let {
            it.id shouldBeEqualTo TEST_CONTACT_ID_1
            it.name shouldBeEqualTo TEST_CONTACT_NAME_1
            it.profilePicture shouldBeEqualTo PublicAsset(TEST_ASSET_KEY)
        }
        result.second().let {
            it.id shouldBeEqualTo TEST_CONTACT_ID_2
            it.name shouldBeEqualTo TEST_CONTACT_NAME_2
            it.profilePicture shouldBeEqualTo null
        }
    }

    companion object {
        private const val ASSET_SIZE_COMPLETE = "complete"
        private const val TEST_ASSET_KEY = "asset_key_356"

        private const val TEST_CONTACT_ID_1 = "contact_id_8900"
        private const val TEST_CONTACT_NAME_1 = "Alice Abc"
        private const val TEST_CONTACT_ID_2 = "contact_id_9234"
        private const val TEST_CONTACT_NAME_2 = "Bob Bcd"
    }
}
