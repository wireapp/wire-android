package com.wire.android.feature.auth.client.datasource.local

import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.shouldBe
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
}
