package com.wire.android.feature.conversation.list.datasources.local

import com.wire.android.core.storage.db.DatabaseTest
import com.wire.android.core.storage.db.user.UserDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldContainSame
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

@ExperimentalCoroutinesApi
class ConversationDaoTest : DatabaseTest() {

    private lateinit var conversationDao: ConversationDao
    private lateinit var userDatabase: UserDatabase

    @Before
    fun setUp() {
        userDatabase = buildDatabase()
        conversationDao = userDatabase.conversationDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        userDatabase.clearTestData()
    }

    @Test
    fun insertEntity_readConversations_containsInsertedItem() = runTest {
        conversationDao.insert(TEST_CONVERSATION_ENTITY)
        val conversations = conversationDao.conversations()

        conversations shouldContainSame listOf(TEST_CONVERSATION_ENTITY)
    }

    companion object {
        private val TEST_CONVERSATION_ENTITY = ConversationEntity(5, "Android Team")
    }
}
