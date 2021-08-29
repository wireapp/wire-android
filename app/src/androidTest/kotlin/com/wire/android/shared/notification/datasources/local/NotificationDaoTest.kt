package com.wire.android.shared.notification.datasources.local

import com.wire.android.InstrumentationTest
import com.wire.android.core.storage.db.user.UserDatabase
import com.wire.android.feature.conversation.data.local.ConversationDao
import com.wire.android.feature.conversation.data.local.ConversationEntity
import com.wire.android.framework.storage.db.DatabaseTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldContainSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class NotificationDaoTest : InstrumentationTest() {

    @get:Rule
    val databaseTestRule = DatabaseTestRule.create<UserDatabase>(appContext)

    private lateinit var userDatabase: UserDatabase
    private lateinit var notificationDao: NotificationDao
    private lateinit var conversationDao: ConversationDao

    @Before
    fun setUp() {
        userDatabase = databaseTestRule.database
        notificationDao = userDatabase.notificationDao()
        conversationDao = userDatabase.conversationDao()
    }

    @Test
    fun insertNotification_readNotification_containsInsertedItems() = databaseTestRule.runTest {
        populateDatabaseWithConversation()
        val notification = NotificationEntity("1254-1", "message", TEST_CONVERSATION_ID)
        notificationDao.insert(notification)

        val result = notificationDao.notificationsByConversationId(TEST_CONVERSATION_ID)

        result shouldContainSame arrayOf(notification)
    }

    @Test
    fun deleteEntity_readNotifications_doesNotContainDeletedItem() = databaseTestRule.runTest {
        populateDatabaseWithConversation()
        val notification = NotificationEntity("1254-1", "message", TEST_CONVERSATION_ID)
        notificationDao.insert(notification)

        notificationDao.deleteNotificationsByConversationId(TEST_CONVERSATION_ID)

        notificationDao.notificationsByConversationId(TEST_CONVERSATION_ID).isEmpty() shouldBe true
    }

    private suspend fun populateDatabaseWithConversation() {
        val conversationEntity = ConversationEntity(TEST_CONVERSATION_ID, "Test", 0)
        conversationDao.insert(conversationEntity)
    }

    companion object {
        private const val TEST_CONVERSATION_ID = "1234"
    }
}
