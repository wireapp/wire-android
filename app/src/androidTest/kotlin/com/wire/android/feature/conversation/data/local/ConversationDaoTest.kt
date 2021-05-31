package com.wire.android.feature.conversation.data.local

import com.wire.android.InstrumentationTest
import com.wire.android.core.storage.db.user.UserDatabase
import com.wire.android.framework.storage.db.DatabaseTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ConversationDaoTest : InstrumentationTest() {

    @get:Rule
    val databaseTestRule = DatabaseTestRule.create<UserDatabase>(appContext)

    private lateinit var conversationDao: ConversationDao

    @Before
    fun setUp() {
        val userDatabase = databaseTestRule.database
        conversationDao = userDatabase.conversationDao()
    }

    @Test
    fun insertEntity_readConversations_containsInsertedItem() = databaseTestRule.runTest {
        conversationDao.insert(TEST_CONVERSATION_ENTITY)
        val conversations = conversationDao.conversations()

        conversations shouldContainSame listOf(TEST_CONVERSATION_ENTITY)
    }

    @Test
    fun insertAll_readConversations_containsInsertedItems() = databaseTestRule.runTest {
        val entity1 = ConversationEntity(id = "id-1", name = "Conversation #1", type = 1)
        val entity2 = ConversationEntity(id = "id-2", name = "Conversation #2", type = 2)
        val entity3 = ConversationEntity(id = "id-3", name = "Conversation #3", type = 3)
        val entities = listOf(entity1, entity2, entity3)

        conversationDao.insertAll(entities)
        val conversations = conversationDao.conversations()

        conversations shouldContainSame listOf(entity1, entity2, entity3)
    }

    @Test
    fun updateConversations_conversationExists_updatesItems() = databaseTestRule.runTest {
        val entity1 = ConversationEntity(id = "id-1", name = "Conversation #1", type = 1)
        val entity2 = ConversationEntity(id = "id-2", name = "Conversation #2", type = 2)
        conversationDao.insertAll(listOf(entity1, entity2))

        val newName = "New Conversation Name"
        val updatedEntity1 = entity1.copy(name = newName)
        val newType = 3
        val updatedEntity2 = entity2.copy(type = newType)
        conversationDao.updateConversations(listOf(updatedEntity1, updatedEntity2))

        val conversations = conversationDao.conversations()

        conversations shouldContainSame listOf(updatedEntity1, updatedEntity2)
    }

    @Test
    fun updateConversations_conversationDoesNotExist_doesNothing() = databaseTestRule.runTest {
        val entity = ConversationEntity(id = "id-1", name = "Conversation #1", type = 1)
        conversationDao.insert(entity)

        val updatedEntity = ConversationEntity(id = "id-2", name = "Conversation #2", type = 2)
        conversationDao.updateConversations(listOf(updatedEntity))

        val conversations = conversationDao.conversations()

        conversations shouldContainSame listOf(entity)
    }

    @Test
    fun deleteConversationById_conversationWithIdExists_deletesConversation() = databaseTestRule.runTest {
        conversationDao.insert(TEST_CONVERSATION_ENTITY)

        conversationDao.deleteConversationById(TEST_CONVERSATION_ENTITY.id)
        val remainingConversations = conversationDao.conversations()

        remainingConversations.isEmpty() shouldBeEqualTo true
    }

    @Test
    fun deleteConversationById_conversationWithIdDoesNotExist_doesNothing() = databaseTestRule.runTest {
        conversationDao.insert(TEST_CONVERSATION_ENTITY)

        conversationDao.deleteConversationById("some-other-id")
        val remainingConversations = conversationDao.conversations()

        remainingConversations shouldContainSame listOf(TEST_CONVERSATION_ENTITY)
    }

    @Test
    fun count_conversationsExistInDatabase_returnsNumberOfConversations() = databaseTestRule.runTest {
        val count = 125
        val conversations = (1..count).map {
            ConversationEntity(id = "id_$it", name = "Conversation #$it", type = it % 5)
        }
        conversationDao.insertAll(conversations)

        val result = conversationDao.count()

        result shouldBeEqualTo count
    }

    @Test
    fun count_noConversationsExistInDatabase_returnsZero() = databaseTestRule.runTest {
        val result = conversationDao.count()

        result shouldBeEqualTo 0
    }

    @Test
    fun deleteEntity_readConversations_doesNotContainDeletedItem() = databaseTestRule.runTest {
        conversationDao.insert(TEST_CONVERSATION_ENTITY)

        conversationDao.delete(TEST_CONVERSATION_ENTITY)

        conversationDao.conversations().isEmpty() shouldBe true
    }

    companion object {
        private val TEST_CONVERSATION_ENTITY = ConversationEntity("id-5", "Android Team", 0)
    }
}
