package com.wire.android.feature.conversation.members.datasources.local

import com.wire.android.InstrumentationTest
import com.wire.android.core.storage.db.user.UserDatabase
import com.wire.android.feature.conversation.list.datasources.local.ConversationDao
import com.wire.android.feature.conversation.list.datasources.local.ConversationEntity
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

    private lateinit var conversationMembersDao: ConversationMembersDao

    @Before
    fun setUp() {
        val userDatabase = databaseTestRule.database
        conversationDao = userDatabase.conversationDao()
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

    @Test
    fun conversationMembers_entitiesForConversationIdExists_returnsMemberIds() = databaseTestRule.runTest {
        conversationDao.insert(ConversationEntity(id = TEST_CONVERSATION_ID, name = "Android Chapter"))

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

    private suspend fun prepareConversationMemberEntity(conversationId: String, contactId: String): ConversationMemberEntity {
        conversationDao.insert(ConversationEntity(id = conversationId, name = "Conversation $conversationId"))
        return ConversationMemberEntity(conversationId = conversationId, contactId = contactId)
    }

    companion object {
        private const val TEST_CONVERSATION_ID = "conv-id"
        private const val TEST_CONTACT_ID = "contact-id"
    }
}
