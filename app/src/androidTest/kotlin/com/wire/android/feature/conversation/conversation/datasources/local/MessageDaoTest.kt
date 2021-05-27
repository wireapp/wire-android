package com.wire.android.feature.conversation.conversation.datasources.local

import com.wire.android.InstrumentationTest
import com.wire.android.core.extension.EMPTY
import com.wire.android.core.storage.db.user.UserDatabase
import com.wire.android.feature.conversation.data.local.ConversationDao
import com.wire.android.feature.conversation.data.local.ConversationEntity
import com.wire.android.framework.storage.db.DatabaseTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MessageDaoTest : InstrumentationTest() {

    @get:Rule
    val databaseTestRule = DatabaseTestRule.create<UserDatabase>(appContext)

    private lateinit var messageDao: MessageDao
    private lateinit var conversationDao: ConversationDao

    @Before
    fun setUp() {
        val userDatabase = databaseTestRule.database
        messageDao = userDatabase.messageDao()
        conversationDao = userDatabase.conversationDao()
    }

    @Test
    fun messagesByConversationId_entitiesForConversationIdExists_returnsMessages() {
        val conversation = ConversationEntity(TEST_CONVERSATION_ID, TEST_CONVERSATION_NAME, 0)
        val message =  MessageEntity(
            TEST_MESSAGE_ID,
            TEST_CONVERSATION_ID,
            String.EMPTY,
            String.EMPTY,
            String.EMPTY,
            String.EMPTY,
            String.EMPTY
        )

        runBlocking {
            conversationDao.insert(conversation)
            messageDao.insert(message)

            val result = messageDao.messagesByConversationId(TEST_CONVERSATION_ID)

            result.first().size shouldBeEqualTo 1
            result.first().first() shouldBeEqualTo message
        }
    }

    @Test
    fun messagesByConversationId_noEntitiesForConversationIdExists_returnsEmptyList() {
        runBlocking {
            val result = messageDao.messagesByConversationId(TEST_CONVERSATION_ID)
            result.first().size shouldBeEqualTo 0
        }
    }

    companion object {
        private const val TEST_MESSAGE_ID = "message-id"
        private const val TEST_CONVERSATION_ID = "conversation-id"
        private const val TEST_CONVERSATION_NAME = "conversation-name"
    }
}
