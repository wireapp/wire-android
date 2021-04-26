package com.wire.android.core.crypto.data

import com.wire.android.AndroidTest
import com.wire.android.core.crypto.model.UserID
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
        val userID = UserID("123")
        every { preKeyLocalDataSource.lastPreKeyId(userID) } returns any()

        subject.lastPreKeyIdForUser(userID)

        verify { preKeyLocalDataSource.lastPreKeyId(userID) }
    }

    @Test
    fun `given a value is stored locally, when reading it, it should be forwarded from the local data source`() {
        val userID = UserID("123")
        every { preKeyLocalDataSource.lastPreKeyId(userID) } returns 42

        subject.lastPreKeyIdForUser(userID) shouldBeEqualTo 42
    }

    @Test
    fun `given a value needs storing, when attempting to save it, then the right user ID should be used`() {
        val userID = UserID("123")

        subject.updateLastPreKeyIDForUser(userID, 42)

        verify { subject.updateLastPreKeyIDForUser(userID, 42) }
    }

}
