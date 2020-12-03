package com.wire.android.feature.conversation.list.datasources.local

import com.wire.android.InstrumentationTest
import com.wire.android.core.storage.db.user.UserDatabase
import com.wire.android.framework.storage.db.DatabaseTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        val entity1 = ConversationEntity(id = "id-1", name = "Conversation #1")
        val entity2 = ConversationEntity(id = "id-2", name = "Conversation #2")
        val entity3 = ConversationEntity(id = "id-3", name = "Conversation #3")
        val entities = listOf(entity1, entity2, entity3)

        conversationDao.insertAll(entities)
        val conversations = conversationDao.conversations()

        conversations shouldContainSame listOf(entity1, entity2, entity3)
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

    companion object {
        private val TEST_CONVERSATION_ENTITY = ConversationEntity("id-5", "Android Team")
    }
}
