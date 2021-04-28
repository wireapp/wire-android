package com.wire.android.core.crypto.data

import com.wire.android.AndroidTest
import com.wire.android.core.crypto.model.UserId
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.amshove.kluent.any
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class PreKeyDataSourceTest : AndroidTest() {

    @MockK
    private lateinit var preKeyLocalDataSource: PreKeyLocalDataSource

    private lateinit var subject: PreKeyDataSource

    @Before
    fun setup() {
        subject = PreKeyDataSource(preKeyLocalDataSource)
    }

    @Test
    fun `given a value is stored, when attempting to read it, then the right user ID should be used`() {
        val userId = UserId("123")
        every { preKeyLocalDataSource.lastPreKeyId(userId) } returns any()

        subject.lastPreKeyIdForUser(userId)

        verify { preKeyLocalDataSource.lastPreKeyId(userId) }
    }

    @Test
    fun `given a value is stored locally, when reading it, it should be forwarded from the local data source`() {
        val userId = UserId("123")
        every { preKeyLocalDataSource.lastPreKeyId(userId) } returns 42

        subject.lastPreKeyIdForUser(userId) shouldBeEqualTo 42
    }

    @Test
    fun `given a value needs storing, when attempting to save it, then the right user ID should be used`() {
        val userId = UserId("123")

        subject.updateLastPreKeyIdForUser(userId, 42)

        verify { subject.updateLastPreKeyIdForUser(userId, 42) }
    }

}
