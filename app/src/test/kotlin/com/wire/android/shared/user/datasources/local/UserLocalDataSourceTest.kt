package com.wire.android.shared.user.datasources.local

import com.wire.android.UnitTest
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

class UserLocalDataSourceTest : UnitTest() {

    @MockK
    private lateinit var userDao: UserDao

    @MockK
    private lateinit var userEntity: UserEntity

    private lateinit var userLocalDataSource: UserLocalDataSource

    @Before
    fun setUp() {
        userLocalDataSource = UserLocalDataSource(userDao)
    }

    @Test
    fun `given save is called, when dao insertion is successful, then propagates success`() {
        coEvery { userDao.insert(userEntity) } returns Unit

        runBlocking {
            userLocalDataSource.save(userEntity) shouldSucceed { it shouldBe Unit }
        }
    }

    @Test
    fun `given save is called, when dao insertion fails, then propagates failure`() {
        coEvery { userDao.insert(userEntity) } throws RuntimeException()

        runBlocking {
            userLocalDataSource.save(userEntity) shouldFail {}
        }
    }

    @Test
    fun `given userById is called, when dao insertion is successful, then propagates success`() {
        coEvery { userDao.userById(TEST_ID) } returns userEntity

        runBlocking {
            userLocalDataSource.userById(TEST_ID) shouldSucceed { it shouldBe userEntity }
        }
    }

    @Test
    fun `given userById is called, when dao insertion fails, then propagates failure`() {
        coEvery { userDao.userById(TEST_ID) } throws RuntimeException()

        runBlocking {
            userLocalDataSource.userById(TEST_ID) shouldFail {}
        }
    }

    @Test
    fun `given update is called, when dao insertion is successful, then propagates success`() {
        coEvery { userDao.update(userEntity) } returns Unit

        runBlocking {
            userLocalDataSource.update(userEntity) shouldSucceed { it shouldBe Unit }
        }
    }

    @Test
    fun `given update is called, when dao insertion fails, then propagates failure`() {
        coEvery { userDao.update(userEntity) } throws RuntimeException()

        runBlocking {
            userLocalDataSource.update(userEntity) shouldFail {}
        }
    }

    companion object {
        private const val TEST_ID = "user-id"
    }
}
