package com.wire.android.feature.conversation.list.datasources

import android.os.Build
import androidx.test.filters.SdkSuppress
import com.wire.android.InstrumentationTest
import com.wire.android.core.storage.db.user.UserDatabase
import com.wire.android.feature.contact.datasources.mapper.ContactMapper
import com.wire.android.feature.conversation.Group
import com.wire.android.feature.conversation.Unknown
import com.wire.android.feature.conversation.data.ConversationMapper
import com.wire.android.feature.conversation.data.ConversationTypeMapper
import com.wire.android.feature.conversation.data.local.ConversationDao
import com.wire.android.feature.conversation.data.local.ConversationEntity
import com.wire.android.feature.conversation.list.datasources.local.ConversationListDao
import com.wire.android.feature.conversation.list.datasources.local.ConversationListLocalDataSource
import com.wire.android.framework.storage.db.DatabaseTestRule
import io.mockk.MockKAnnotations
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessThan
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
@SdkSuppress(maxSdkVersion = Build.VERSION_CODES.O_MR1)
class ConversationListDataSourceIntegrationTest : InstrumentationTest() {

    @get:Rule
    val databaseTestRule = DatabaseTestRule.create<UserDatabase>(appContext)

    private lateinit var conversationDao: ConversationDao
    private lateinit var conversationListDao: ConversationListDao

    private lateinit var conversationListDataSource: ConversationListDataSource

    @Before
    fun setUp() {
        val userDatabase = databaseTestRule.database
        conversationDao = userDatabase.conversationDao()
        conversationListDao = userDatabase.conversationListDao()

        val conversationTypeMapper = ConversationTypeMapper()
        val conversationListLocalDataSource = ConversationListLocalDataSource(conversationListDao)
        val conversationListMapper = ConversationListMapper(ConversationMapper(conversationTypeMapper), ContactMapper())

        MockKAnnotations.init(this, relaxUnitFun = true, relaxed = true)
        conversationListDataSource = ConversationListDataSource(
            conversationListLocalDataSource, mockk(), conversationListMapper, conversationTypeMapper
        )
    }

    @Test
    fun conversationListInBatch_noItemsInDatabase_emitsEmptyList() {
        runBlocking {
            val result = conversationListDataSource.conversationListInBatch(pageSize = 10, Unknown)

            result.first().isEmpty() shouldBe true
        }
    }

    @Test
    fun conversationListInBatch_lessThanPageSizeItemsInDatabase_emitsExistingItems() {
        runBlocking {
            val itemCount = 8
            val pageCount = 10
            addConversations(itemCount)

            val result = conversationListDataSource.conversationListInBatch(pageSize = pageCount, Unknown)

            result.first().size shouldBeEqualTo itemCount
        }
    }

    @Test
    fun conversationListInBatch_moreThanPageSizeItemsInDatabase_emitsAllItemsWithPlaceholders() {
        runBlocking {
            val itemCount = 180
            val pageCount = 10
            addConversations(itemCount)

            val result = conversationListDataSource.conversationListInBatch(pageSize = pageCount, Unknown)

            result.first().size shouldBeEqualTo itemCount
            result.first().filterNotNull().size shouldBeLessThan itemCount
        }
    }

    @Test
    fun conversationListInBatch_itemsWithExcludedTypeExists_doesNotEmitItemsWithExcludedType() {
        runBlocking {
            val groupConvCount = 3
            val oneToOneConvCount = 5
            val pageCount = 10
            addConversations(count = groupConvCount, type = TYPE_VALUE_GROUP)
            addConversations(count = oneToOneConvCount, type = TYPE_VALUE_ONE_TO_ONE)

            val result = conversationListDataSource.conversationListInBatch(pageSize = pageCount, Group)

            result.first().size shouldBeEqualTo oneToOneConvCount
        }
    }

    private suspend fun addConversations(count: Int, type: Int = TYPE_VALUE_GROUP) {
        val conversationEntities = (1..count).map {
            ConversationEntity(id = "id_$it", name = "Conversation # $it", type = type)
        }
        conversationDao.insertAll(conversationEntities)
    }

    companion object {
        private const val TYPE_VALUE_GROUP = 0
        private const val TYPE_VALUE_ONE_TO_ONE = 2
    }
}
