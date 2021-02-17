package com.wire.android.feature.conversation.list.datasources.local

import com.wire.android.UnitTest
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class ConversationListLocalDataSourceTest : UnitTest() {

    @MockK
    private lateinit var conversationListDao: ConversationListDao

    private lateinit var conversationListLocalDataSource: ConversationListLocalDataSource

    @Before
    fun setUp() {
        conversationListLocalDataSource = ConversationListLocalDataSource(conversationListDao)
    }

    @Test
    fun `given conversationListInBatch is called, then calls dao for paged factory`() {
        val excludeType = 1
        conversationListLocalDataSource.conversationListInBatch(excludeType = excludeType)

        verify(exactly = 1) { conversationListDao.conversationListItemsInBatch(excludeType) }
    }
}
