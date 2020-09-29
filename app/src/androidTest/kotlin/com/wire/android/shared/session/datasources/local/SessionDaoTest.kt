package com.wire.android.shared.session.datasources.local

import com.wire.android.core.storage.db.DatabaseTest
import com.wire.android.core.storage.db.global.GlobalDatabase
import com.wire.android.shared.user.datasources.local.UserEntity
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
        val session1 = prepareSession(1, "userId-1", true)
        val session2 = prepareSession(2, "userId-2", false)
        sessionDao.insert(session1)
        sessionDao.insert(session2)

        val sessions = sessionDao.sessions()

        assertThat(sessions).containsExactlyInAnyOrder(session1, session2)
    }

    @Test
    fun insertCurrentSession_readCurrentSession_returnsInsertedItem() = runTest {
        val session = prepareSession(current = true)

        sessionDao.insert(session)

        assertThat(session).isEqualTo(sessionDao.currentSession())
    }

    @Test
    fun insertNotCurrentSession_readCurrentSession_returnsNull() = runTest {
        val session = prepareSession(current = false)

        sessionDao.insert(session)

        assertThat(sessionDao.currentSession()).isNull()
    }

    @Test
    fun sessionForUserExists_userDeleted_sessionIsDeletedAutomatically() = runTest {
        val user = UserEntity(TEST_USER_ID, TEST_USER_NAME)
        val session = prepareSession(userId = user.id, current = true)
        sessionDao.insert(session)
        assertThat(sessionDao.sessions()).contains(session)

        globalDatabase.userDao().delete(user)

        assertThat(sessionDao.sessions()).doesNotContain(session)
    }

    @Test
    fun insertSession_userIdNotUnique_updatesExistingSessionWithSameUserId() = runTest {
        val session1 = prepareSession(id = 1, userId = TEST_USER_ID, current = true)
        sessionDao.insert(session1)

        val session2 = prepareSession(id = 2, userId = TEST_USER_ID, current = false)
        sessionDao.insert(session2)

        assertThat(sessionDao.sessions()).containsExactly(session2)
    }

    @Test
    fun setCurrentSessionToDormant_aSessionExistsAsCurrent_setsIsCurrentToFalse() = runTest {
        val session1 = prepareSession(id = 1, userId = "userId-1", current = true)
        val session2 = prepareSession(id = 2, userId = "userId-2", current = false)
        sessionDao.insert(session1)
        sessionDao.insert(session2)

        sessionDao.setCurrentSessionToDormant()

        val sessions = sessionDao.sessions()
        assertThat(sessions).containsExactlyInAnyOrder(session1.copy(isCurrent = false), session2)
    }

    @Test
    fun doesCurrentSessionExist_aSessionExistsAsCurrent_returnsTrue() = runTest {
        val session = prepareSession(id = 1, userId = "userId-1", current = true)
        sessionDao.insert(session)

        val result = sessionDao.doesCurrentSessionExist()

        assertThat(result).isTrue()
    }

    @Test
    fun doesCurrentSessionExist_noSessionExistsAsCurrent_returnsFalse() = runTest {
        val session = prepareSession(id = 1, userId = "userId-1", current = false)
        sessionDao.insert(session)

        val result = sessionDao.doesCurrentSessionExist()

        assertThat(result).isFalse()
    }

    private suspend fun prepareSession(id : Int = 1, userId: String = TEST_USER_ID, current: Boolean): SessionEntity {
        globalDatabase.userDao().insert(UserEntity(userId, TEST_USER_NAME))

        return SessionEntity(
            id = id, userId = userId, accessToken = TEST_ACCESS_TOKEN, tokenType = TEST_TOKEN_TYPE,
            refreshToken = TEST_REFRESH_TOKEN, isCurrent = current
        )
    }

    companion object {
        private const val TEST_USER_ID = "123435-user-79"
        private const val TEST_ACCESS_TOKEN = "789-Access-Token"
        private const val TEST_TOKEN_TYPE = "Bearer"
        private const val TEST_REFRESH_TOKEN = "123-Refresh-Token"
        private const val TEST_USER_NAME = "Name"
    }
}
