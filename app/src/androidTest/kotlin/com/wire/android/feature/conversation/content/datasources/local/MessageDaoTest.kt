package com.wire.android.feature.conversation.content.datasources.local

import com.wire.android.InstrumentationTest
import com.wire.android.core.storage.db.user.UserDatabase
import com.wire.android.feature.contact.datasources.local.ContactDao
import com.wire.android.feature.contact.datasources.local.ContactEntity
import com.wire.android.feature.conversation.data.local.ConversationDao
import com.wire.android.feature.conversation.data.local.ConversationEntity
import com.wire.android.framework.storage.db.DatabaseTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MessageDaoTest : InstrumentationTest() {

    @get:Rule
    val databaseTestRule = DatabaseTestRule.create<UserDatabase>(appContext)

    private lateinit var messageDao: MessageDao
    private lateinit var contactDao: ContactDao
    private lateinit var conversationDao: ConversationDao

    private lateinit var conversationEntity: ConversationEntity
    private lateinit var messageEntity: MessageEntity
    private lateinit var contactEntity: ContactEntity

    @Before
    fun setUp() {
        val userDatabase = databaseTestRule.database
        messageDao = userDatabase.messageDao()
        contactDao = userDatabase.contactDao()
        conversationDao = userDatabase.conversationDao()

        conversationEntity = ConversationEntity(TEST_CONVERSATION_ID, TEST_CONVERSATION_NAME, TEST_CONVERSATION_TYPE)
        messageEntity = MessageEntity(
            id = TEST_MESSAGE_ID,
            conversationId = TEST_CONVERSATION_ID,
            senderUserId = TEST_USER_ID,
            type = TEST_MESSAGE_TYPE,
            content = TEST_MESSAGE_CONTENT,
            state = TEST_MESSAGE_STATE,
            time = TEST_MESSAGE_TIME,
            isRead = TEST_IS_READ
        )
        contactEntity = ContactEntity(TEST_USER_ID, TEST_CONTACT_NAME, TEST_CONTACT_ASSET_KEY)

        runBlocking {
            conversationDao.insert(conversationEntity)
            contactDao.insert(contactEntity)
            messageDao.insert(messageEntity)
        }
    }

    @Test
    fun messagesByConversationId_entitiesForConversationIdExists_returnsMessages() {
        runBlocking {
            val result = messageDao.messagesByConversationId(TEST_CONVERSATION_ID)

            with(result.first()) {
                size shouldBeEqualTo 1
                first() shouldBeInstanceOf CombinedMessageContactEntity::class
                first().messageEntity shouldBeEqualTo messageEntity
                first().contactEntity shouldBeEqualTo contactEntity
            }
        }
    }

    @Test
    fun messagesByConversationId_noEntitiesForConversationIdExists_returnsEmptyList() {
        runBlocking {
            val result = messageDao.messagesByConversationId("$TEST_CONVERSATION_ID#1")

            result.first().size shouldBeEqualTo 0
        }
    }

    @Test
    fun givenAMessageInConversationExists_whenConversationIsDeleted_thenMessagesAreDeleted() {
        runBlocking {
            conversationDao.delete(conversationEntity)

            val result = messageDao.messagesByConversationId(TEST_CONVERSATION_ID)

            result.first().size shouldBeEqualTo 0
        }
    }

    @Test
    fun givenAMessageInConversationExists_whenContactIsDeleted_thenMessagesAreDeleted() {
        runBlocking {
            contactDao.delete(contactEntity)

            val result = messageDao.messagesByConversationId(TEST_CONVERSATION_ID)

            result.first().size shouldBeEqualTo 0
        }
    }

    companion object {
        private const val TEST_CONVERSATION_ID = "conversation-id"
        private const val TEST_USER_ID = "user-id"
        private const val TEST_CONVERSATION_TYPE = 0
        private const val TEST_CONVERSATION_NAME = "conversation-name"
        private const val TEST_MESSAGE_ID = "message-id"
        private const val TEST_MESSAGE_TYPE = "message-type"
        private const val TEST_MESSAGE_CONTENT = "message-content"
        private const val TEST_MESSAGE_STATE = "message-state"
        private const val TEST_MESSAGE_TIME = "message-time"
        private const val TEST_IS_READ = true
        private const val TEST_CONTACT_NAME = "contact-name"
        private const val TEST_CONTACT_ASSET_KEY = "contact-asset-key"
    }
}
