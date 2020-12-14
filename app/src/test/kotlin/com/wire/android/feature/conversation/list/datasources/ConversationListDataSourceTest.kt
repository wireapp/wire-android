package com.wire.android.feature.conversation.list.datasources

import com.wire.android.UnitTest
import com.wire.android.feature.conversation.list.datasources.local.ConversationListLocalDataSource
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class ConversationListDataSourceTest : UnitTest(){

    @MockK
    private lateinit var conversationListLocalDataSource: ConversationListLocalDataSource

    @MockK
    private lateinit var conversationListMapper: ConversationListMapper

    private lateinit var conversationListDataSource: ConversationListDataSource

    @Before
    fun setUp() {
        conversationListDataSource = ConversationListDataSource(conversationListLocalDataSource, conversationListMapper)
    }

    @Test
    fun `given conversationListDataSourceFactory is called, then returns a mapping of local data source factory`() {
        conversationListDataSource.conversationListDataSourceFactory()

        verify(exactly = 1) { conversationListLocalDataSource.conversationListDataSourceFactory() }
    }
}
