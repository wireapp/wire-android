package com.wire.android.shared.session.datasources.local

import com.wire.android.UnitTest
import com.wire.android.core.functional.onSuccess
import com.wire.android.framework.functional.assertRight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`

@ExperimentalCoroutinesApi
class SessionLocalDataSourceTest : UnitTest() {

    @Mock
    private lateinit var sessionDao: SessionDao

    @Mock
    private lateinit var sessionEntity: SessionEntity

    private lateinit var sessionLocalDataSource: SessionLocalDataSource

    @Before
    fun setUp() {
        sessionLocalDataSource = SessionLocalDataSource(sessionDao)
    }

    @Test
    fun `given save is called, when dao insertion is successful, then returns success`() {
        runBlockingTest {
            `when`(sessionDao.insert(sessionEntity)).thenReturn(Unit)

            sessionLocalDataSource.save(sessionEntity).assertRight()
        }
    }

    @Test
    fun `given save is called, when dao insertion fails, then returns failure`() {
        runBlockingTest {
            `when`(sessionDao.insert(sessionEntity)).thenThrow(RuntimeException())

            sessionLocalDataSource.save(sessionEntity).onSuccess { fail("Expected a failure") }
        }
    }

    @Test
    fun `given currentSession is called, when dao emits an item, then propagates it directly`() {
        `when`(sessionDao.currentSession()).thenReturn(flowOf(sessionEntity))

        runBlocking {
            sessionLocalDataSource.currentSession().collect {
                assertThat(it).isEqualTo(sessionEntity)
            }
        }
    }

    @Test
    fun `given currentSession is called, when dao emits null, then propagates null directly`() {
        `when`(sessionDao.currentSession()).thenReturn(flow { emit(null) })

        runBlocking {
            sessionLocalDataSource.currentSession().collect {
                assertThat(it).isEqualTo(null)
            }
        }
    }

    @Test
    fun `given setCurrentSessionToDormant is called, when dao operation is successful, then returns success`() {
        runBlockingTest {
            `when`(sessionDao.setCurrentSessionToDormant()).thenReturn(Unit)

            sessionLocalDataSource.setCurrentSessionToDormant().assertRight()
        }
    }

    @Test
    fun `given setCurrentSessionToDormant is called, when dao operation fails, then returns failure`() {
        runBlockingTest {
            `when`(sessionDao.setCurrentSessionToDormant()).thenThrow(RuntimeException())

            sessionLocalDataSource.setCurrentSessionToDormant().onSuccess { fail("Expected a failure") }
        }
    }

    @Test
    fun `given doesCurrentSessionExist is called, when dao returns true, then returns success with value of true`() {
        testDoesCurrentSessionExist(true)
    }

    @Test
    fun `given doesCurrentSessionExist is called, when dao returns false, then returns success with value of false`() {
        testDoesCurrentSessionExist(false)
    }

    private fun testDoesCurrentSessionExist(exists: Boolean) = runBlockingTest {
        `when`(sessionDao.doesCurrentSessionExist()).thenReturn(exists)

        sessionLocalDataSource.doesCurrentSessionExist().assertRight {
            assertThat(it).isEqualTo(exists)
        }
    }

    @Test
    fun `given doesCurrentSessionExist is called, when dao operation fails, then returns failure`() {
        runBlockingTest {
            `when`(sessionDao.doesCurrentSessionExist()).thenThrow(RuntimeException())

            sessionLocalDataSource.doesCurrentSessionExist().onSuccess { fail("Expected a failure") }
        }
    }
}
