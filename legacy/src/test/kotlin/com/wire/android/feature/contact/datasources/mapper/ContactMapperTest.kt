package com.wire.android.feature.contact.datasources.mapper

import com.wire.android.UnitTest
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.ContactClient
import com.wire.android.feature.contact.DetailedContact
import com.wire.android.feature.contact.datasources.local.ContactClientEntity
import com.wire.android.feature.contact.datasources.local.ContactEntity
import com.wire.android.feature.contact.datasources.local.ContactWithClients
import com.wire.android.feature.contact.datasources.remote.ContactResponse
import com.wire.android.framework.collections.second
import com.wire.android.shared.asset.Asset
import com.wire.android.shared.asset.PublicAsset
import com.wire.android.shared.asset.datasources.remote.AssetResponse
import com.wire.android.shared.asset.mapper.AssetMapper
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldContainSame
import org.junit.Before
import org.junit.Test

class ContactMapperTest : UnitTest() {

    private lateinit var contactMapper: ContactMapper

    @MockK
    private lateinit var assetMapper: AssetMapper

    @Before
    fun setUp() {
        contactMapper = ContactMapper(assetMapper)
    }

    @Test
    fun `given fromContactResponseListToEntityList is called, then returns list of entities with a proper mapping`() {
        val contactResponseWithAsset = mockk<ContactResponse>()
        val contactAssetResponse = mockk<AssetResponse>()
        val assetList = mockk<List<AssetResponse>>()
        every { contactResponseWithAsset.id } returns TEST_CONTACT_ID_1
        every { contactResponseWithAsset.name } returns TEST_CONTACT_NAME_1
        every { contactAssetResponse.size } returns ASSET_SIZE_COMPLETE
        every { contactAssetResponse.key } returns TEST_ASSET_KEY
        every { contactResponseWithAsset.assets } returns assetList
        every { assetMapper.profilePictureAssetKey(assetList) } returns TEST_ASSET_KEY

        val contactResponse = mockk<ContactResponse>()
        every { contactResponse.id } returns TEST_CONTACT_ID_2
        every { contactResponse.name } returns TEST_CONTACT_NAME_2
        every { contactResponse.assets } returns emptyList()
        every { assetMapper.profilePictureAssetKey(contactResponse.assets) } returns null

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

    @Test
    fun `given fromContactEntity is called, then maps the ContactEntity and returns a Contact`() {
        val contactEntity = ContactEntity(
                id = TEST_CONTACT_ID_1,
                name = TEST_CONTACT_NAME_1,
                assetKey = TEST_ASSET_KEY
        )

        val result = contactMapper.fromContactEntity(contactEntity)

        result.let {
            it shouldBeInstanceOf Contact::class
            it.id shouldBeEqualTo TEST_CONTACT_ID_1
            it.name shouldBeEqualTo TEST_CONTACT_NAME_1
            it.profilePicture shouldBeInstanceOf Asset::class
        }
    }

    @Test
    fun `given a clientContactEntity, when calling fromContactClientEntityToContactClient, then return the correct client`() {
        val contactClientEntity = ContactClientEntity("user-id", "client-id")

        val result = contactMapper.fromContactClientEntityToContactClient(contactClientEntity)

        result.let {
            it shouldBeInstanceOf ContactClient::class
            it.id shouldBeEqualTo contactClientEntity.id
        }
    }

    @Test
    fun `given a contact, when calling fromContactToEntity, then return the correct contact entity`() {
        val contact = Contact("user-id", "user-name", PublicAsset("a key"))

        val result = contactMapper.fromContactToEntity(contact)

        result.let {
            it shouldBeInstanceOf ContactEntity::class
            it.id shouldBeEqualTo contact.id
            it.name shouldBeEqualTo contact.name
            it.assetKey shouldBeEqualTo (contact.profilePicture as PublicAsset).key
        }
    }

    @Test
    fun `given a contact with clients, when calling fromContactWithClients, then return the correct detailed contact`() {
        val contact = ContactEntity("user-id", "user-name", "a key")
        val client1 = ContactClientEntity(contact.id, "client-id")
        val client2 = ContactClientEntity(contact.id, "client-id2")
        val contactWithClients = ContactWithClients(contact, listOf(client1, client2))

        val result = contactMapper.fromContactWithClients(contactWithClients)

        result.let {
            it shouldBeInstanceOf DetailedContact::class
            it.contact shouldBeEqualTo Contact(contact.id, contact.name, PublicAsset(contact.assetKey!!))
            it.clients shouldContainSame listOf(ContactClient(client1.id), ContactClient(client2.id))
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
