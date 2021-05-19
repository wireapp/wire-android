package com.wire.android.shared.session.datasources.local

import com.wire.android.InstrumentationTest
import com.wire.android.core.storage.db.global.GlobalDatabase
import com.wire.android.framework.storage.db.DatabaseTestRule
import com.wire.android.shared.user.datasources.local.UserEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldNotContain
import org.amshove.kluent.shouldBeNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SessionDaoTest : InstrumentationTest() {

    @get:Rule
    val databaseTestRule = DatabaseTestRule.create<GlobalDatabase>(appContext)

    private lateinit var sessionDao: SessionDao
    private lateinit var globalDatabase: GlobalDatabase

    @Before
    fun setUp() {
        globalDatabase = databaseTestRule.database
        sessionDao = globalDatabase.sessionDao()
    }

    @Test
    fun insertEntities_readSessions_containsInsertedItems() = databaseTestRule.runTest {
        val session1 = prepareSession(1, "userId-1", true)
        val session2 = prepareSession(2, "userId-2", false)
        sessionDao.insert(session1)
        sessionDao.insert(session2)

        val sessions = sessionDao.sessions()

        sessions shouldContainSame arrayOf(session1, session2)
    }

    @Test
    fun insertCurrentSession_readCurrentSession_returnsInsertedItem() = databaseTestRule.runTest {
        val session = prepareSession(current = true)

        sessionDao.insert(session)

        sessionDao.currentSession() shouldBeEqualTo session
    }

    @Test
    fun insertNotCurrentSession_readCurrentSession_returnsNull() = databaseTestRule.runTest {
        val session = prepareSession(current = false)

        sessionDao.insert(session)

        sessionDao.currentSession() shouldBeEqualTo null
    }

    @Test
    fun sessionForUserExists_userDeleted_sessionIsDeletedAutomatically() = databaseTestRule.runTest {
        val user = UserEntity(TEST_USER_ID, TEST_USER_NAME)
        val session = prepareSession(userId = user.id, current = true)
        sessionDao.insert(session)
        sessionDao.sessions() shouldContain session

        globalDatabase.userDao().delete(user)

        sessionDao.sessions() shouldNotContain session
    }

    @Test
    fun insertSession_userIdNotUnique_updatesExistingSessionWithSameUserId() = databaseTestRule.runTest {
        val session1 = prepareSession(id = 1, userId = TEST_USER_ID, current = true)
        sessionDao.insert(session1)

        val session2 = prepareSession(id = 2, userId = TEST_USER_ID, current = false)
        sessionDao.insert(session2)

        sessionDao.sessions() shouldContainSame arrayOf(session2)
    }

    @Test
    fun currentSession_aSessionExistsAsCurrent_emitsThatSession() = databaseTestRule.runTest {
        val session = prepareSession(id = 1, userId = TEST_USER_ID, current = true)
        sessionDao.insert(session)

        sessionDao.currentSession() shouldBeEqualTo session
    }

    @Test
    fun currentSession_noSessionExistsAsCurrent_emitsNull() = databaseTestRule.runTest {
        val session = prepareSession(id = 1, userId = TEST_USER_ID, current = false)
        sessionDao.insert(session)

        sessionDao.currentSession() shouldBeEqualTo null
    }

    @Test
    fun currentSession_currentSessionIsSetToDormant_emitsNull() = databaseTestRule.runTest {
        val session = prepareSession(id = 1, userId = TEST_USER_ID, current = true)
        sessionDao.insert(session)

        sessionDao.currentSession() shouldBeEqualTo session

        sessionDao.insert(session.copy(isCurrent = false))

        sessionDao.currentSession() shouldBeEqualTo null
    }

    @Test
    fun setCurrentSessionToDormant_aSessionExistsAsCurrent_setsIsCurrentToFalse() = databaseTestRule.runTest {
        val session1 = prepareSession(id = 1, userId = "userId-1", current = true)
        val session2 = prepareSession(id = 2, userId = "userId-2", current = false)
        sessionDao.insert(session1)
        sessionDao.insert(session2)

        sessionDao.setCurrentSessionToDormant()

        val sessions = sessionDao.sessions()
        sessions shouldContainSame arrayOf(session1.copy(isCurrent = false), session2)
    }

    @Test
    fun setSessionCurrent_aSessionOfSpecificUserExistsAsDormant_setsIsCurrentToTrue() = databaseTestRule.runTest {
        val session = prepareSession(id = 1, userId = "userId", current = false)
        sessionDao.insert(session)

        sessionDao.setSessionCurrent("userId")

        val currentSession = sessionDao.currentSession()
        currentSession shouldBeEqualTo session.copy(isCurrent = true)
    }

    @Test
    fun setSessionCurrent_aSessionWithUserIdDoesNotExist_doNotUpdateAnyRecord() = databaseTestRule.runTest {
        val session = prepareSession(id = 1, userId = "", current = false)
        sessionDao.insert(session)

        sessionDao.setSessionCurrent("userId")

        val currentSession = sessionDao.currentSession()
        currentSession.shouldBeNull()
    }

    @Test
    fun doesCurrentSessionExist_aSessionExistsAsCurrent_returnsTrue() = databaseTestRule.runTest {
        val session = prepareSession(id = 1, userId = "userId-1", current = true)
        sessionDao.insert(session)

        val result = sessionDao.doesCurrentSessionExist()

        result shouldBe true
    }

    @Test
    fun doesCurrentSessionExist_noSessionExistsAsCurrent_returnsFalse() = databaseTestRule.runTest {
        val session = prepareSession(id = 1, userId = "userId-1", current = false)
        sessionDao.insert(session)

        val result = sessionDao.doesCurrentSessionExist()

        result shouldBe false
    }

    @Test
    fun userSession_aSessionOfUserExists_returnsThatSession() = databaseTestRule.runTest {
        val session = prepareSession(id = 1, userId = TEST_USER_ID)
        sessionDao.insert(session)

        sessionDao.userSession(TEST_USER_ID) shouldBeEqualTo session
    }

    @Test
    fun userSession_noSessionExistsWithUserId_returnsNull() = databaseTestRule.runTest {
        val userId = "user-id"
        val session = prepareSession(id = 1, userId = userId, current = false)
        sessionDao.insert(session)

        sessionDao.userSession(TEST_USER_ID) shouldBeEqualTo null
    }

    private suspend fun prepareSession(id : Int = 1, userId: String = TEST_USER_ID, current: Boolean = false): SessionEntity {
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
