package com.wire.android.feature.conversation.data.local

import androidx.paging.DataSource
import com.wire.android.UnitTest
import com.wire.android.feature.conversation.members.datasources.local.ConversationMemberEntity
import com.wire.android.feature.conversation.members.datasources.local.ConversationMembersDao
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test
import java.sql.SQLException

class ConversationLocalDataSourceTest : UnitTest() {

    @MockK
    private lateinit var conversationDao: ConversationDao

    @MockK
    private lateinit var conversationMembersDao: ConversationMembersDao

    private lateinit var conversationLocalDataSource: ConversationLocalDataSource

    @Before
    fun setUp() {
        conversationLocalDataSource = ConversationLocalDataSource(conversationDao, conversationMembersDao)
    }

    @Test
    fun `given conversationsDataFactory is called, then calls conversationsDao for factory`() {
        val daoFactory = mockk<DataSource.Factory<Int, ConversationEntity>>()
        every { conversationDao.conversationsInBatch() } returns daoFactory

        val dataSourceFactory = conversationLocalDataSource.conversationsDataFactory()

        dataSourceFactory shouldBeEqualTo daoFactory
        verify(exactly = 1) { conversationDao.conversationsInBatch() }
    }

    @Test
    fun `given saveConversations is called, when dao insertion is successful, then returns success`() {
        val conversationEntities = mockk<List<ConversationEntity>>()
        coEvery { conversationDao.insertAll(conversationEntities) } returns Unit

        val result = runBlocking { conversationLocalDataSource.saveConversations(conversationEntities) }

        result shouldSucceed { it shouldBe Unit }
        coVerify { conversationDao.insertAll(conversationEntities) }
    }

    @Test
    fun `given saveConversations is called, when dao insertion fails, then propagates failure`() {
        val conversationEntities = mockk<List<ConversationEntity>>()
        coEvery { conversationDao.insertAll(conversationEntities) } throws SQLException()

        val result = runBlocking { conversationLocalDataSource.saveConversations(conversationEntities) }

        result shouldFail { }
        coVerify { conversationDao.insertAll(conversationEntities) }
    }

    @Test
    fun `given saveMemberIdsForConversations is called, when dao operation is successful, then returns success`() {
        val conversationMemberEntities = mockk<List<ConversationMemberEntity>>()
        coEvery { conversationMembersDao.insertAll(conversationMemberEntities) } returns Unit

        val result = runBlocking { conversationLocalDataSource.saveMemberIdsForConversations(conversationMemberEntities) }

        result shouldSucceed { it shouldBe Unit }
        coVerify { conversationMembersDao.insertAll(conversationMemberEntities) }
    }

    @Test
    fun `given saveMemberIdsForConversations is called, when dao operation fails, then propagates failure`() {
        val conversationMemberEntities = mockk<List<ConversationMemberEntity>>()
        coEvery { conversationMembersDao.insertAll(conversationMemberEntities) } throws SQLException()

        val result = runBlocking { conversationLocalDataSource.saveMemberIdsForConversations(conversationMemberEntities) }

        result shouldFail { }
        coVerify { conversationMembersDao.insertAll(conversationMemberEntities) }
    }

    @Test
    fun `given conversationMemberIds is called, when dao operation returns member ids, then propagates member ids`() {
        val conversationId = "conv-id-1"
        val memberIds = listOf("member-id-1", "member-id-2")
        coEvery { conversationMembersDao.conversationMembers(conversationId) } returns memberIds

        val result = runBlocking { conversationLocalDataSource.conversationMemberIds(conversationId) }

        result shouldSucceed { it shouldBe memberIds }
        coVerify { conversationMembersDao.conversationMembers(conversationId) }
    }

    @Test
    fun `given conversationMemberIds is called, when dao operation fails, then propagates failure`() {
        val conversationId = "conv-id-1"
        coEvery { conversationMembersDao.conversationMembers(conversationId) } throws SQLException()

        val result = runBlocking { conversationLocalDataSource.conversationMemberIds(conversationId) }

        result shouldFail { }
        coVerify { conversationMembersDao.conversationMembers(conversationId) }
    }

    @Test
    fun `given allConversationMemberIds is called, when dao operation returns member ids, then propagates member ids`() {
        val memberIds = mockk<List<String>>()
        coEvery { conversationMembersDao.allConversationMemberIds() } returns memberIds

        val result = runBlocking { conversationLocalDataSource.allConversationMemberIds() }

        result shouldSucceed { it shouldBe memberIds }
        coVerify { conversationMembersDao.allConversationMemberIds() }
    }

    @Test
    fun `given allConversationMemberIds is called, when dao operation fails, then propagates failure`() {
        coEvery { conversationMembersDao.allConversationMemberIds() } throws SQLException()

        val result = runBlocking { conversationLocalDataSource.allConversationMemberIds() }

        result shouldFail { }
        coVerify { conversationMembersDao.allConversationMemberIds() }
    }
}
