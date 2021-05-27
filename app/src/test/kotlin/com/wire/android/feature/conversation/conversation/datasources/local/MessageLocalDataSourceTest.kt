package com.wire.android.feature.conversation.conversation.datasources.local

import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test
import java.sql.SQLException

class MessageLocalDataSourceTest : UnitTest() {

    private lateinit var messageLocalDataSource: MessageLocalDataSource

    @MockK
    private lateinit var messageDao: MessageDao

    @Before
    fun setUp() {
        messageLocalDataSource = MessageLocalDataSource(messageDao)
    }

    @Test
    fun `given save is called, when dao insertion is successful, then returns success`() {
        val messageEntity = mockk<MessageEntity>()
        coEvery { messageDao.insert(messageEntity) } returns Unit

        val result = runBlocking { messageLocalDataSource.save(messageEntity) }

        result shouldSucceed { it shouldBe Unit }
        coVerify(exactly = 1) { messageDao.insert(messageEntity) }
    }

    @Test
    fun `given save is called, when dao insertion fails, then propagates failure`() {
        val messageEntity = mockk<MessageEntity>()
        coEvery { messageDao.insert(messageEntity) } throws SQLException()

        val result = runBlocking { messageLocalDataSource.save(messageEntity) }

        result shouldFail  { }
        coVerify(exactly = 1) { messageDao.insert(messageEntity) }
    }
}
