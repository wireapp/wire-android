package com.wire.android.feature.conversation.list.datasources

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.data.ConversationTypeMapper
import com.wire.android.feature.conversation.list.datasources.local.ConversationListItemEntity
import com.wire.android.feature.conversation.list.datasources.local.ConversationListLocalDataSource
import com.wire.android.feature.conversation.list.ui.ConversationListItem
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.Before
import org.junit.Test

class ConversationListDataSourceTest : UnitTest() {

    @MockK
    private lateinit var conversationListLocalDataSource: ConversationListLocalDataSource

    @MockK
    private lateinit var conversationListMapper: ConversationListMapper

    @MockK
    private lateinit var conversationTypeMapper: ConversationTypeMapper


    private lateinit var conversationListDataSource: ConversationListDataSource

    @Before
    fun setUp() {
        conversationListDataSource = ConversationListDataSource(
            conversationListLocalDataSource,
            conversationListMapper, conversationTypeMapper
        )
    }

    @Test
    fun `given conversationListInBatch is called, then calls local data source with given start and count parameters`() {
        coEvery { conversationListLocalDataSource.conversationListInBatch(any(), any()) } returns Either.Left(mockk())

        runBlocking { conversationListDataSource.conversationListInBatch(TEST_BATCH_START, TEST_BATCH_COUNT) }

        coVerify(exactly = 1) { conversationListLocalDataSource.conversationListInBatch(TEST_BATCH_START, TEST_BATCH_COUNT) }
    }

    @Test
    fun `given conversationListInBatch is called, when local data source returns the batch, then maps and propagates items`() {
        val entities = listOf<ConversationListItemEntity>(mockk(), mockk())
        coEvery { conversationListLocalDataSource.conversationListInBatch(any(), any()) } returns Either.Right(entities)
        val conversations = listOf<ConversationListItem>(mockk(), mockk())
        every { conversationListMapper.fromEntity(any()) } returnsMany conversations

        val result = runBlocking { conversationListDataSource.conversationListInBatch(60, 20) }

        result shouldSucceed { it shouldContainSame conversations }
        verify(exactly = 2) { conversationListMapper.fromEntity(any()) }
    }

    @Test
    fun `given conversationListInBatch is called, when local data source fails to return the batch, then propagates the failure`() {
        val failure = mockk<Failure>()
        coEvery { conversationListLocalDataSource.conversationListInBatch(any(), any()) } returns Either.Left(failure)

        val result = runBlocking { conversationListDataSource.conversationListInBatch(60, 20) }

        result shouldFail { it shouldBeEqualTo failure }
        verify(inverse = true) { conversationListMapper.fromEntity(any()) }
    }

    companion object {
        private const val TEST_BATCH_START = 60
        private const val TEST_BATCH_COUNT = 20
    }
}
