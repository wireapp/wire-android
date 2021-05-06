package com.wire.android.shared.session.datasources.local

import com.wire.android.UnitTest
import com.wire.android.core.exception.NoEntityFound
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
class SessionLocalDataSourceTest : UnitTest() {

    @MockK
    private lateinit var sessionDao: SessionDao

    @MockK
    private lateinit var sessionEntity: SessionEntity

    private lateinit var sessionLocalDataSource: SessionLocalDataSource

    @Before
    fun setUp() {
        sessionLocalDataSource = SessionLocalDataSource(sessionDao)
    }

    @Test
    fun `given save is called, when dao insertion is successful, then returns success`() {
        coEvery { sessionDao.insert(sessionEntity) } returns Unit

        runBlockingTest {
            sessionLocalDataSource.save(sessionEntity) shouldSucceed { it shouldBe Unit }
        }
    }

    @Test
    fun `given save is called, when dao insertion fails, then returns failure`() {
        coEvery { sessionDao.insert(sessionEntity) } throws RuntimeException()

        runBlockingTest {
            sessionLocalDataSource.save(sessionEntity) shouldFail {}
        }
    }

    @Test
    fun `given currentSession is called, when dao returns an entity, then propagates it in Either`() {
        coEvery { sessionDao.currentSession() } returns sessionEntity

        runBlockingTest {
            sessionLocalDataSource.currentSession() shouldSucceed { it shouldBe sessionEntity }
        }
    }

    @Test
    fun `given currentSession is called, when dao returns null, then returns NoEntityFound error`() {
        coEvery { sessionDao.currentSession() } returns null

        runBlockingTest {
            sessionLocalDataSource.currentSession() shouldFail { it shouldBe NoEntityFound }
        }
    }

    @Test
    fun `given currentSession is called, when dao returns error, then returns error`() {
        coEvery { sessionDao.currentSession() } throws RuntimeException()

        runBlockingTest {
            sessionLocalDataSource.currentSession() shouldFail {}
        }
    }

    @Test
    fun `given setCurrentSessionToDormant is called, when dao operation is successful, then returns success`() {
        coEvery { sessionDao.setCurrentSessionToDormant() } returns Unit

        runBlockingTest {
            sessionLocalDataSource.setCurrentSessionToDormant() shouldSucceed { it shouldBe Unit }
        }
    }

    @Test
    fun `given setCurrentSessionToDormant is called, when dao operation fails, then returns failure`() {
        coEvery { sessionDao.setCurrentSessionToDormant() } throws RuntimeException()

        runBlockingTest {
            sessionLocalDataSource.setCurrentSessionToDormant() shouldFail {}
        }
    }

    @Test
    fun `given doesCurrentSessionExist is called, when dao returns true, then returns success with value of true`() =
        testDoesCurrentSessionExist(true)

    @Test
    fun `given doesCurrentSessionExist is called, when dao returns false, then returns success with value of false`() =
        testDoesCurrentSessionExist(false)

    private fun testDoesCurrentSessionExist(exists: Boolean) {
        coEvery { sessionDao.doesCurrentSessionExist() } returns exists

        runBlockingTest {
            sessionLocalDataSource.doesCurrentSessionExist() shouldSucceed { it shouldBe exists }
        }
    }

    @Test
    fun `given doesCurrentSessionExist is called, when dao operation fails, then returns failure`() {
        coEvery { sessionDao.doesCurrentSessionExist() } throws RuntimeException()
        runBlockingTest {
            sessionLocalDataSource.doesCurrentSessionExist() shouldFail {}
        }
    }

    @Test
    fun `given setSessionCurrent is called, when dao operation is successful, then returns success`() {
        coEvery { sessionDao.setSessionCurrent(USER_ID) } returns Unit

        runBlockingTest {
            sessionLocalDataSource.setSessionCurrent(USER_ID) shouldSucceed { it shouldBe Unit }
        }
    }

    @Test
    fun `given setSessionCurrent is called, when dao operation fails, then returns failure`() {
        coEvery { sessionDao.setSessionCurrent(USER_ID) } throws RuntimeException()

        runBlockingTest {
            sessionLocalDataSource.setSessionCurrent(USER_ID) shouldFail {}
        }
    }

    @Test
    fun `given userAuthorizationToken is called, when dao returns a valid authorizationToken, then propagates it in Either`() {
        val authorizationToken = "authorizationToken"
        coEvery { sessionDao.userAuthorizationToken(USER_ID) } returns authorizationToken

        runBlockingTest {
            sessionLocalDataSource.userAuthorizationToken(USER_ID) shouldSucceed { it shouldBe authorizationToken }
        }
    }

    @Test
    fun `given userAuthorizationToken is called, when dao returns null, then returns NoEntityFound error`() {
        coEvery { sessionDao.userAuthorizationToken(USER_ID) } returns null

        runBlockingTest {
            sessionLocalDataSource.userAuthorizationToken(USER_ID) shouldFail { it shouldBe NoEntityFound }
        }
    }

    @Test
    fun `given userAuthorizationToken is called, when dao returns error, then returns error`() {
        coEvery { sessionDao.userAuthorizationToken(USER_ID) } throws RuntimeException()

        runBlockingTest {
            sessionLocalDataSource.userAuthorizationToken(USER_ID) shouldFail {}
        }
    }
    companion object{
        private const val USER_ID = "user-id"
    }
}
