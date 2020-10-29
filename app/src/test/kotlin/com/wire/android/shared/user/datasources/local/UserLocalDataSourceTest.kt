package com.wire.android.shared.user.datasources.local

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
    fun `given save is called, when dao insertion is successful, then returns success`() {
        coEvery { userDao.insert(userEntity) } returns Unit

        runBlockingTest {
            userLocalDataSource.save(userEntity) shouldSucceed { it shouldBe Unit }
        }
    }

    @Test
    fun `given save is called, when dao insertion fails, then returns failure`() {
        coEvery { userDao.insert(userEntity) } throws RuntimeException()

        runBlockingTest {
            userLocalDataSource.save(userEntity) shouldFail {}
        }
    }
}
