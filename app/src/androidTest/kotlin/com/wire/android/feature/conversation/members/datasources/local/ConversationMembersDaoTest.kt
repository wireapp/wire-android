package com.wire.android.feature.conversation.members.datasources.local

import com.wire.android.InstrumentationTest
import com.wire.android.core.storage.db.user.UserDatabase
import com.wire.android.feature.contact.datasources.local.ContactClientDao
import com.wire.android.feature.contact.datasources.local.ContactClientEntity
import com.wire.android.feature.contact.datasources.local.ContactDao
import com.wire.android.feature.contact.datasources.local.ContactEntity
import com.wire.android.feature.contact.datasources.local.ContactWithClients
import com.wire.android.feature.conversation.data.local.ConversationDao
import com.wire.android.feature.conversation.data.local.ConversationEntity
import com.wire.android.framework.storage.db.DatabaseTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ConversationMembersDaoTest : InstrumentationTest() {

    @get:Rule
    val databaseTestRule = DatabaseTestRule.create<UserDatabase>(appContext)

    private lateinit var conversationDao: ConversationDao

    private lateinit var contactClientDao: ContactClientDao

    private lateinit var contactDao: ContactDao

    private lateinit var conversationMembersDao: ConversationMembersDao

    @Before
    fun setUp() {
        val userDatabase = databaseTestRule.database
        conversationDao = userDatabase.conversationDao()
        contactClientDao = userDatabase.contactClientDao()
        contactDao = userDatabase.contactDao()
        conversationMembersDao = userDatabase.conversationMembersDao()
    }

    @Test
    fun insertEntity_readAll_returnsInsertedItem() = databaseTestRule.runTest {
        val entity =
            prepareConversationMemberEntity(conversationId = TEST_CONVERSATION_ID, contactId = TEST_CONTACT_ID)
        conversationMembersDao.insert(entity)

        val entities = conversationMembersDao.allConversationMembers()

        entities.size shouldBeEqualTo 1
        entities.first() shouldBeEqualTo entity
    }

    @Test
    fun insertAll_readAll_returnsInsertedItems() = databaseTestRule.runTest {
        val entity1 =
            prepareConversationMemberEntity(conversationId = "$TEST_CONVERSATION_ID-1", contactId = "$TEST_CONTACT_ID-1")
        val entity2 =
            prepareConversationMemberEntity(conversationId = "$TEST_CONVERSATION_ID-2", contactId = "$TEST_CONTACT_ID-2")

        conversationMembersDao.insertAll(listOf(entity1, entity2))

        val entities = conversationMembersDao.allConversationMembers()

        entities shouldContainSame listOf(entity1, entity2)
    }

    @Test
    fun entitiesForConversationIdExists_whenConversationRemovedFromDatabase_deletesEntriesWithConversationId() =
        databaseTestRule.runTest {
            val entity1 =
                prepareConversationMemberEntity(conversationId = "$TEST_CONVERSATION_ID-1", contactId = "$TEST_CONTACT_ID-1")
            val entity2 =
                prepareConversationMemberEntity(conversationId = "$TEST_CONVERSATION_ID-2", contactId = "$TEST_CONTACT_ID-2")
            conversationMembersDao.insertAll(listOf(entity1, entity2))

            conversationDao.deleteConversationById(entity1.conversationId)
            val remainingEntities = conversationMembersDao.allConversationMembers()

            remainingEntities shouldContainSame listOf(entity2)
        }

    private suspend fun prepareConversationMemberEntity(conversationId: String, contactId: String): ConversationMemberEntity {
        conversationDao.insert(
            ConversationEntity(id = conversationId, name = "Conversation $conversationId", type = TEST_CONVERSATION_TYPE)
        )
        return ConversationMemberEntity(conversationId = conversationId, contactId = contactId)
    }

    @Test
    fun conversationMembers_entitiesForConversationIdExists_returnsMemberIds() = databaseTestRule.runTest {
        conversationDao.insert(ConversationEntity(id = TEST_CONVERSATION_ID, name = "Android Chapter", type = TEST_CONVERSATION_TYPE))

        val entity1 = ConversationMemberEntity(TEST_CONVERSATION_ID, "contact-1")
        val entity2 = ConversationMemberEntity(TEST_CONVERSATION_ID, "contact-2")
        val entity3 = ConversationMemberEntity(TEST_CONVERSATION_ID, "contact-3")

        conversationMembersDao.insertAll(listOf(entity1, entity2, entity3))

        val memberIds = conversationMembersDao.conversationMembers(TEST_CONVERSATION_ID)

        memberIds shouldContainSame listOf("contact-1", "contact-2", "contact-3")
    }

    @Test
    fun conversationMembers_noEntitiesForConversationIdExists_returnsEmptyList() = databaseTestRule.runTest {
        val memberIds = conversationMembersDao.conversationMembers(TEST_CONVERSATION_ID)

        memberIds.isEmpty() shouldBeEqualTo true
    }

    @Test
    fun allConversationMembers_entitiesExists_returnsMemberIdsWithoutRepetition() = databaseTestRule.runTest {
        val conversationId1 = "conv-id-1"
        val conversationId2 = "conv-id-2"
        conversationDao.insert(ConversationEntity(id = conversationId1, name = "Android Chapter", type = TEST_CONVERSATION_TYPE))
        conversationDao.insert(ConversationEntity(id = conversationId2, name = "IOS Chapter", type = TEST_CONVERSATION_TYPE))

        val contactId1 = "contact-id-1"
        val contactId2 = "contact-id-2"
        val contactId3 = "contact-id-3"

        val entity1 = ConversationMemberEntity(conversationId1, contactId1)
        val entity2 = ConversationMemberEntity(conversationId1, contactId2)
        val entity3 = ConversationMemberEntity(conversationId2, contactId2)
        val entity4 = ConversationMemberEntity(conversationId2, contactId3)

        conversationMembersDao.insertAll(listOf(entity1, entity2, entity3, entity4))

        val memberIds = conversationMembersDao.allConversationMemberIds()

        memberIds shouldContainSame listOf(contactId1, contactId2, contactId3)
    }

    @Test
    fun allConversationMembers_noEntitiesForExists_returnsEmptyList() = databaseTestRule.runTest {
        val memberIds = conversationMembersDao.allConversationMemberIds()

        memberIds.isEmpty() shouldBeEqualTo true
    }

    @Test
    fun detailedConversationMembers_contactWithClientExists_returnsEntities() = databaseTestRule.runTest {
        val contact = ContactEntity(TEST_CONTACT_ID, "John Doe", null)
        val contactClient = ContactClientEntity(TEST_CONTACT_ID, "client-ID")
        contactDao.insert(contact)
        contactClientDao.insert(contactClient)
        conversationDao.insert(ConversationEntity(TEST_CONVERSATION_ID, "name", TEST_CONVERSATION_TYPE))
        conversationMembersDao.insert(ConversationMemberEntity(TEST_CONVERSATION_ID, TEST_CONTACT_ID))

        val contacts = conversationMembersDao.detailedConversationMembers(TEST_CONVERSATION_ID)

        contacts shouldContainSame listOf(
            ContactWithClients(contact, listOf(contactClient))
        )
    }

    @Test
    fun detailedConversationMembers_noEntitiesExist_returnsEmptyList() = databaseTestRule.runTest {
        val contacts = conversationMembersDao.detailedConversationMembers(TEST_CONVERSATION_ID)

        contacts.isEmpty() shouldBeEqualTo true
    }

    companion object {
        private const val TEST_CONVERSATION_ID = "conv-id"
        private const val TEST_CONTACT_ID = "contact-id"
        private const val TEST_CONVERSATION_TYPE = 0
    }
}
