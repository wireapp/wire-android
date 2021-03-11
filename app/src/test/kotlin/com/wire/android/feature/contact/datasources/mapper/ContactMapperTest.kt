package com.wire.android.feature.contact.datasources.mapper

import com.wire.android.UnitTest
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.contact.datasources.local.ContactEntity
import com.wire.android.feature.contact.datasources.remote.ContactResponse
import com.wire.android.framework.collections.second
import com.wire.android.shared.asset.datasources.remote.AssetResponse
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Test
import java.io.File

class ContactMapperTest : UnitTest() {

    private lateinit var contactMapper: ContactMapper

    @Before
    fun setUp() {
        contactMapper = ContactMapper()
    }

    @Test
    fun `given profilePictureAssetKey is called, when contactResponse has an asset with size "complete", then returns asset key`() {
        val contactResponse = mockk<ContactResponse>()
        val contactAssetResponse = mockk<AssetResponse>()
        every { contactAssetResponse.size } returns ASSET_SIZE_COMPLETE
        every { contactAssetResponse.key } returns TEST_ASSET_KEY
        every { contactResponse.assets } returns listOf(contactAssetResponse)

        val result = contactMapper.profilePictureAssetKey(contactResponse)

        result shouldBeEqualTo TEST_ASSET_KEY
    }

    @Test
    fun `given profilePictureAssetKey is called, when contactResponse does not have an asset with size "complete", then returns null`() {
        val contactResponse = mockk<ContactResponse>()
        val contactAssetResponse = mockk<AssetResponse>()
        every { contactAssetResponse.size } returns "preview"
        every { contactAssetResponse.key } returns TEST_ASSET_KEY
        every { contactResponse.assets } returns listOf(contactAssetResponse)

        val result = contactMapper.profilePictureAssetKey(contactResponse)

        result shouldBeEqualTo null
    }

    @Test
    fun `given fromContactResponse is called, when profile picture is null, then returns a mapping with null picture path`() {
        val contactResponse = mockk<ContactResponse>()
        every { contactResponse.id } returns TEST_CONTACT_ID
        every { contactResponse.name } returns TEST_CONTACT_NAME

        val result = contactMapper.fromContactResponse(contactResponse, null)

        result shouldBeInstanceOf Contact::class
        result.id shouldBeEqualTo TEST_CONTACT_ID
        result.name shouldBeEqualTo TEST_CONTACT_NAME
        result.profilePicturePath shouldBeEqualTo null
    }

    @Test
    fun `given fromContactResponse is called, when profile picture is not null, then returns a mapping with picture's path`() {
        val contactResponse = mockk<ContactResponse>()
        every { contactResponse.id } returns TEST_CONTACT_ID
        every { contactResponse.name } returns TEST_CONTACT_NAME
        val profilePicture = mockk<File>()
        every { profilePicture.absolutePath } returns TEST_FILE_PATH

        val result = contactMapper.fromContactResponse(contactResponse, profilePicture)

        result shouldBeInstanceOf Contact::class
        result.id shouldBeEqualTo TEST_CONTACT_ID
        result.name shouldBeEqualTo TEST_CONTACT_NAME
        result.profilePicturePath shouldBeEqualTo TEST_FILE_PATH
    }

    @Test
    fun `given fromContactResponseListToEntityList is called, then returns list of entities with a proper mapping`() {
        val contactResponse1 = mockk<ContactResponse>()
        val id1 = "${TEST_CONTACT_ID}_1"
        val name1 = "${TEST_CONTACT_NAME}_1"
        every { contactResponse1.id } returns id1
        every { contactResponse1.name } returns name1
        val contactAssetResponse = mockk<AssetResponse>()
        every { contactAssetResponse.size } returns ASSET_SIZE_COMPLETE
        every { contactAssetResponse.key } returns TEST_ASSET_KEY
        every { contactResponse1.assets } returns listOf(contactAssetResponse)

        val contactResponse2 = mockk<ContactResponse>()
        val id2 = "${TEST_CONTACT_ID}_2"
        val name2 = "${TEST_CONTACT_NAME}_2"
        every { contactResponse2.id } returns id2
        every { contactResponse2.name } returns name2
        every { contactResponse2.assets } returns emptyList()

        val result = contactMapper.fromContactResponseListToEntityList(listOf(contactResponse1, contactResponse2))

        result.first().let {
            it shouldBeInstanceOf ContactEntity::class
            it.id shouldBeEqualTo id1
            it.name shouldBeEqualTo name1
            it.assetKey shouldBeEqualTo TEST_ASSET_KEY
        }
        result.second().let {
            it shouldBeInstanceOf ContactEntity::class
            it.id shouldBeEqualTo id2
            it.name shouldBeEqualTo name2
            it.assetKey shouldBeEqualTo null
        }
    }

    @Test
    fun `given fromContactEntity is called, when profile picture is null, then returns a mapping with null picture path`() {
        val contactEntity = mockk<ContactEntity>()
        every { contactEntity.id } returns TEST_CONTACT_ID
        every { contactEntity.name } returns TEST_CONTACT_NAME

        val result = contactMapper.fromContactEntity(contactEntity, null)

        result shouldBeInstanceOf Contact::class
        result.id shouldBeEqualTo TEST_CONTACT_ID
        result.name shouldBeEqualTo TEST_CONTACT_NAME
        result.profilePicturePath shouldBeEqualTo null
    }

    @Test
    fun `given fromContactEntity is called, when profile picture is not null, then returns a mapping with picture's path`() {
        val contactEntity = mockk<ContactEntity>()
        every { contactEntity.id } returns TEST_CONTACT_ID
        every { contactEntity.name } returns TEST_CONTACT_NAME
        val profilePicture = mockk<File>()
        every { profilePicture.absolutePath } returns TEST_FILE_PATH

        val result = contactMapper.fromContactEntity(contactEntity, profilePicture)

        result shouldBeInstanceOf Contact::class
        result.id shouldBeEqualTo TEST_CONTACT_ID
        result.name shouldBeEqualTo TEST_CONTACT_NAME
        result.profilePicturePath shouldBeEqualTo TEST_FILE_PATH
    }

    companion object {
        private const val ASSET_SIZE_COMPLETE = "complete"
        private const val TEST_ASSET_KEY = "asset_key_356"
        private const val TEST_CONTACT_ID = "contact_id_8900"
        private const val TEST_CONTACT_NAME = "Alice Abc"
        private const val TEST_FILE_PATH = "file://contacts/assets/0-23409-2456.jpg"
    }
}
