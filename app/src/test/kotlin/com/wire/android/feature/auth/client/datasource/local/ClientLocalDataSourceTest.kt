package com.wire.android.feature.auth.client.datasource.local

import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class ClientLocalDataSourceTest : UnitTest() {

    @MockK
    private lateinit var clientDao: ClientDao

    @MockK
    private lateinit var clientEntity: ClientEntity

    private lateinit var clientLocalDataSource: ClientLocalDataSource

    @Before
    fun setUp() {
        clientLocalDataSource = ClientLocalDataSource(clientDao)
    }

    @Test
    fun `given save is called, when dao insertion is successful, then returns success`() {
        coEvery { clientDao.insert(clientEntity) } returns Unit

        runBlockingTest {
            clientLocalDataSource.save(clientEntity) shouldSucceed { it shouldBe Unit }
        }
    }

    @Test
    fun `given save is called, when dao insertion fails, then returns failure`() {
        coEvery { clientDao.insert(clientEntity) } throws RuntimeException()

        runBlockingTest {
            clientLocalDataSource.save(clientEntity) shouldFail {}
        }
    }

    @Test
    fun `given the Dao returns a client by ID, when calling clientById, then return that`(){
        val clientId = "ABC"
        val expected: ClientEntity = mockk()
        coEvery { clientDao.clientById(clientId) } returns expected

        val result = runBlocking { clientLocalDataSource.clientById(clientId) }

        result shouldBeEqualTo expected
    }

    @Test
    fun `given the Dao returns a client by ID, when calling clientById, then local data source should use the same ID to request it`(){
        val clientId = "ABC"
        val expected: ClientEntity = mockk()
        coEvery { clientDao.clientById(clientId) } returns expected

        runBlockingTest { clientLocalDataSource.clientById(clientId) }

        coVerify(exactly = 1){ clientDao.clientById(clientId) }
    }
}
