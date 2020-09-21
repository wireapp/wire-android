package com.wire.android.shared.session.datasources.local

import com.wire.android.UnitTest
import com.wire.android.core.functional.onSuccess
import com.wire.android.framework.functional.assertRight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
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
}
