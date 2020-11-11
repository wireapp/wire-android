package com.wire.android.feature.conversation.data.local

import androidx.paging.DataSource
import com.wire.android.UnitTest
import com.wire.android.feature.conversation.list.datasources.local.ConversationDao
import com.wire.android.feature.conversation.list.datasources.local.ConversationEntity
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

    private lateinit var conversationLocalDataSource: ConversationLocalDataSource

    @Before
    fun setUp() {
        conversationLocalDataSource = ConversationLocalDataSource(conversationDao)
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
}
