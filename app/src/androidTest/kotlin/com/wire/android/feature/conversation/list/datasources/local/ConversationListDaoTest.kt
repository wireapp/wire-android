package com.wire.android.feature.conversation.list.datasources.local

import com.wire.android.InstrumentationTest
import com.wire.android.core.storage.db.user.UserDatabase
import com.wire.android.feature.contact.datasources.local.ContactDao
import com.wire.android.feature.contact.datasources.local.ContactEntity
import com.wire.android.feature.conversation.data.local.ConversationDao
import com.wire.android.feature.conversation.data.local.ConversationEntity
import com.wire.android.feature.conversation.members.datasources.local.ConversationMemberEntity
import com.wire.android.feature.conversation.members.datasources.local.ConversationMembersDao
import com.wire.android.framework.storage.db.DatabaseTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ConversationListDaoTest : InstrumentationTest() {

    @get:Rule
    val databaseTestRule = DatabaseTestRule.create<UserDatabase>(appContext)

    private lateinit var conversationDao: ConversationDao

    private lateinit var conversationMembersDao: ConversationMembersDao

    private lateinit var contactDao: ContactDao

    private lateinit var conversationListDao: ConversationListDao

    @Before
    fun setUp() {
        val userDatabase = databaseTestRule.database
        conversationDao = userDatabase.conversationDao()
        conversationMembersDao = userDatabase.conversationMembersDao()
        contactDao = userDatabase.contactDao()

        conversationListDao = userDatabase.conversationListDao()
    }

    @Test
    fun givenNoConversationExists_whenAllItemsAreCalled_thenReturnsEmptyList() =
        databaseTestRule.runTest {
            val items = conversationListDao.allConversationListItems()

            items.isEmpty() shouldBeEqualTo true
        }

    @Test
    fun givenConversationsExist_whenAllItemsAreCalled_thenReturnsListItemsWithConversation() =
        databaseTestRule.runTest {
            val conversationEntity1 = ConversationEntity("1", "Conversation 1", type = TEST_CONVERSATION_TYPE)
            val conversationEntity2 = ConversationEntity("2", "Conversation 2", type = TEST_CONVERSATION_TYPE)
            conversationDao.insert(conversationEntity1)
            conversationDao.insert(conversationEntity2)

            val items = conversationListDao.allConversationListItems()

            items.size shouldBeEqualTo 2
            val sortedItems = items.sortedBy { item -> item.conversation.id }
            sortedItems.first().conversation shouldBeEqualTo conversationEntity1
            sortedItems[1].conversation shouldBeEqualTo conversationEntity2
        }

    @Test
    fun givenAConversationWithMember_andMemberHasNoContactInfo_whenAllItemsAreCalled_thenReturnsListItemWithNoMembers() =
        databaseTestRule.runTest {
            conversationDao.insert(TEST_CONVERSATION_ENTITY)
            conversationMembersDao.insert(TEST_CONVERSATION_MEMBER_ENTITY)

            val items = conversationListDao.allConversationListItems()

            items.size shouldBeEqualTo 1

            items.first().let {
                it.conversation shouldBeEqualTo TEST_CONVERSATION_ENTITY
                it.members.isEmpty() shouldBeEqualTo true
            }
        }

    @Test
    fun givenAConversationWithMembers_andMembersHaveContactInfo_whenAllItemsAreCalled_thenReturnsListItemWithMembers() =
        databaseTestRule.runTest {
            conversationDao.insert(TEST_CONVERSATION_ENTITY)

            val contact1 = ContactEntity(id = "contact-1", name = "Contact A", assetKey = "asset-key-A")
            val contact2 = ContactEntity(id = "contact-2", name = "Contact B", assetKey = null)
            insertMemberForConversation(TEST_CONVERSATION_ENTITY, contact1)
            insertMemberForConversation(TEST_CONVERSATION_ENTITY, contact2)

            val items = conversationListDao.allConversationListItems()

            items.size shouldBeEqualTo 1
            items.first().let {
                it.conversation shouldBeEqualTo TEST_CONVERSATION_ENTITY
                it.members shouldContainSame listOf(contact1, contact2)
            }
        }

    @Test
    fun conversationListItemsInBatch_conversationsExistForGivenBatch_returnsConversationListItems() =
        databaseTestRule.runTest {
            val start = 5
            val count = 3

            val conversationAndMemberPairs = (1..10).map {
                val conversation = ConversationEntity(id = "id_$it", name = "Conversation #$it", type = it%5)
                val member = ContactEntity(id = "contact-$it", name = "Contact $it", assetKey = "asset-key-$it")
                conversationDao.insert(conversation)
                insertMemberForConversation(conversation, member)
                conversation to member
            }

            val items = conversationListDao.conversationListItemsInBatch(start, count)

            items.size shouldBeEqualTo count
            val pairsInBatch = conversationAndMemberPairs.drop(start - 1).take(count)
            items.forEachIndexed { index, item ->
                item.conversation shouldBeEqualTo pairsInBatch[index].first
                item.members.first() shouldBeEqualTo pairsInBatch[index].second
            }
        }

    @Test
    fun conversationListItemsInBatch_noConversationsExistForGivenBatch_returnsZeroItems() =
        databaseTestRule.runTest {
            val items = conversationListDao.conversationListItemsInBatch(start = 30, count = 10)

            items.isEmpty() shouldBeEqualTo true
        }

    private suspend fun insertMemberForConversation(conversationEntity: ConversationEntity, contactEntity: ContactEntity) {
        val conversationMemberEntity = ConversationMemberEntity(
            conversationId = conversationEntity.id,
            contactId = contactEntity.id
        )
        conversationMembersDao.insert(conversationMemberEntity)
        contactDao.insert(contactEntity)
    }

    companion object {
        private const val TEST_CONVERSATION_ID = "conv-id-1"
        private const val TEST_CONVERSATION_NAME = "Android Chapter"
        private const val TEST_CONVERSATION_TYPE = 2

        private val TEST_CONVERSATION_ENTITY = ConversationEntity(
            id = TEST_CONVERSATION_ID,
            name = TEST_CONVERSATION_NAME,
            type = TEST_CONVERSATION_TYPE
        )

        private const val TEST_CONTACT_ID = "contact-id-1"

        private val TEST_CONVERSATION_MEMBER_ENTITY = ConversationMemberEntity(
            conversationId = TEST_CONVERSATION_ID,
            contactId = TEST_CONTACT_ID
        )
    }
}
