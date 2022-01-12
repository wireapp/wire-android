package com.wire.android.shared.prekey.data

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.shared.prekey.data.remote.PreKeyRemoteDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class PreKeyDataSourceTest : UnitTest() {

    @MockK
    private lateinit var remotePreKeyDataSource: PreKeyRemoteDataSource

    private lateinit var preKeyDataSource: PreKeyDataSource

    @Before
    fun setUp() {
        preKeyDataSource = PreKeyDataSource(remotePreKeyDataSource)
    }

    @Test
    fun `given map of contactIds, when fetching preKeys of clients by users, then pass the correct ids to the remote data source`() {
        val contactIds = mockk<Map<String, List<String>>>()
        coEvery { remotePreKeyDataSource.preKeysForMultipleUsers(any()) } returns Either.Right(mockk())

        runBlockingTest { preKeyDataSource.preKeysOfClientsByUsers(contactIds) }

        coVerify { remotePreKeyDataSource.preKeysForMultipleUsers(contactIds) }
    }

    @Test
    fun `given remote data source fails, when fetching preKeys of clients by users, then the failure is propagated`() {
        val failure = mockk<Failure>()
        coEvery { remotePreKeyDataSource.preKeysForMultipleUsers(any()) } returns Either.Left(failure)

        runBlockingTest {
            preKeyDataSource.preKeysOfClientsByUsers(mockk())
                .shouldFail {
                    it shouldBeEqualTo failure
                }
        }
    }

    @Test
    fun `given remote data source succeeds, when fetching preKeys of clients by users, then the result is propagated`() {
        val result = mockk<List<UserPreKeyInfo>>()
        coEvery { remotePreKeyDataSource.preKeysForMultipleUsers(any()) } returns Either.Right(result)

        runBlockingTest {
            preKeyDataSource.preKeysOfClientsByUsers(mockk())
                .shouldSucceed { it shouldBeEqualTo result }
        }
    }
}
