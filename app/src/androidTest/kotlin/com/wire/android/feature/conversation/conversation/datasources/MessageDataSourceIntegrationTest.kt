package com.wire.android.feature.conversation.conversation.datasources

import com.wire.android.InstrumentationTest
import com.wire.android.core.extension.EMPTY
import com.wire.android.core.storage.db.user.UserDatabase
import com.wire.android.feature.conversation.conversation.datasources.local.MessageDao
import com.wire.android.feature.conversation.conversation.datasources.local.MessageEntity
import com.wire.android.feature.conversation.conversation.datasources.local.MessageLocalDataSource
import com.wire.android.feature.conversation.conversation.mapper.MessageMapper
import com.wire.android.feature.conversation.conversation.mapper.MessageStateMapper
import com.wire.android.feature.conversation.conversation.mapper.MessageTypeMapper
import com.wire.android.feature.conversation.data.local.ConversationDao
import com.wire.android.feature.conversation.data.local.ConversationEntity
import com.wire.android.framework.storage.db.DatabaseTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MessageDataSourceIntegrationTest : InstrumentationTest() {

    @get:Rule
    val databaseTestRule = DatabaseTestRule.create<UserDatabase>(appContext)

    private lateinit var messageDao: MessageDao
    private lateinit var conversationDao: ConversationDao
    private lateinit var messageDataSource: MessageDataSource
    private lateinit var messageMapper: MessageMapper
    @Before
    fun setUp() {
        val userDatabase = databaseTestRule.database
        messageDao = userDatabase.messageDao()
        conversationDao = userDatabase.conversationDao()

        val messageTypeMapper = MessageTypeMapper()
        val messageStateMapper = MessageStateMapper()
        val messageLocalDataSource = MessageLocalDataSource(messageDao)
        messageMapper = MessageMapper(messageTypeMapper, messageStateMapper)

        messageDataSource = MessageDataSource(messageLocalDataSource, messageMapper)
    }

    @Test
    fun conversationMessages_noItemsInDatabase_emitsEmptyList() {
        runBlocking {
            val result = messageDataSource.conversationMessages(TEST_CONVERSATION_ID)

            result.first().isEmpty() shouldBe true
        }
    }

    @Test
    fun conversationMessages_ItemsInDatabase_emitsItems() = databaseTestRule.runTest {
        val conversationEntity = ConversationEntity(TEST_CONVERSATION_ID, String.EMPTY, 0)
        val message1 = MessageEntity(
            "message-id1",
            TEST_CONVERSATION_ID,
            "unknown",
            String.EMPTY,
            "default",
            String.EMPTY,
            String.EMPTY
        )
        val message2 = MessageEntity(
            "message-id2",
            TEST_CONVERSATION_ID,
            String.EMPTY,
            String.EMPTY,
            String.EMPTY,
            String.EMPTY,
            String.EMPTY
        )
        conversationDao.insert(conversationEntity)
        messageDao.insert(message1)
        messageDao.insert(message2)

        val result = messageDataSource.conversationMessages(TEST_CONVERSATION_ID)
        result.first().size shouldBeEqualTo 2
    }

    companion object {
        private const val TEST_CONVERSATION_ID = "conversation-id"
    }
}
