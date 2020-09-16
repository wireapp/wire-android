package com.wire.android.shared.user.datasources.local

import com.wire.android.core.storage.db.DatabaseTest
import com.wire.android.core.storage.db.global.GlobalDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

@ExperimentalCoroutinesApi
class SessionDaoTest : DatabaseTest() {

    private lateinit var sessionDao: SessionDao
    private lateinit var globalDatabase: GlobalDatabase

    @Before
    fun setUp() {
        globalDatabase = buildDatabase()
        sessionDao = globalDatabase.sessionDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        globalDatabase.clearTestData()
    }

    @Test
    fun insertEntities_readSessions_containsInsertedItems() = runTest {
        val activeSession = prepareSession("userId-1", true)
        val inActiveSession = prepareSession("userId-2", false)

        sessionDao.insertSession(activeSession)
        sessionDao.insertSession(inActiveSession)

        val sessions = sessionDao.sessions()

        assertThat(sessions).containsExactlyInAnyOrder(activeSession, inActiveSession)
    }

    @Test
    fun insertCurrentSession_readCurrentSession_returnsInsertedItem() = runTest {
        val session = prepareSession(current = true)

        sessionDao.insertSession(session)

        assertThat(session).isEqualTo(sessionDao.currentSession())
    }

    @Test
    fun insertNotCurrentSession_readCurrentSession_returnsNull() = runTest {
        val session = prepareSession(current = false)

        sessionDao.insertSession(session)

        assertThat(sessionDao.currentSession()).isNull()
    }

    @Test
    fun sessionForUserExists_userDeleted_sessionIsDeletedAutomatically() = runTest {
        val user = UserEntity(TEST_USER_ID)
        val session = prepareSession(id = user.id, current = true)
        sessionDao.insertSession(session)
        assertThat(sessionDao.sessions()).contains(session)

        globalDatabase.userDao().delete(user)

        assertThat(sessionDao.sessions()).doesNotContain(session)
    }

    private suspend fun prepareSession(id: String = TEST_USER_ID, current: Boolean): SessionEntity {
        globalDatabase.userDao().insert(UserEntity(id))

        return SessionEntity(
            userId = id, accessToken = TEST_ACCESS_TOKEN, tokenType = TEST_TOKEN_TYPE,
            refreshToken = TEST_REFRESH_TOKEN, isCurrent = current
        )
    }

    companion object {
        private const val TEST_USER_ID = "123435-user-79"
        private const val TEST_ACCESS_TOKEN = "789-Access-Token"
        private const val TEST_TOKEN_TYPE = "Bearer"
        private const val TEST_REFRESH_TOKEN = "123-Refresh-Token"
    }
}
