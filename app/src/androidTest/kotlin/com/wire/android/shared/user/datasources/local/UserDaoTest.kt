package com.wire.android.shared.user.datasources.local

import com.wire.android.InstrumentationTest
import com.wire.android.core.storage.db.global.GlobalDatabase
import com.wire.android.framework.storage.db.DatabaseTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class UserDaoTest : InstrumentationTest() {

    @get:Rule
    val databaseTestRule = DatabaseTestRule.create<GlobalDatabase>(appContext)

    private lateinit var userDao: UserDao

    @Before
    fun setUp() {
        val globalDatabase = databaseTestRule.database
        userDao = globalDatabase.userDao()
    }

    @Test
    fun insertEntity_readUsers_containsInsertedItem() = databaseTestRule.runTest {
        userDao.insert(TEST_USER_ENTITY)
        val activeUsers = userDao.users()

        activeUsers shouldContainSame listOf(TEST_USER_ENTITY)
    }

    @Test
    fun insertEntityThatExists_readUsers_userWithSameIdShouldBeReplaced() = databaseTestRule.runTest {
        userDao.insert(TEST_USER_ENTITY)

        val oldUser = userDao.userById(TEST_USER_ID)

        userDao.insert(REPLACEMENT_TEST_USER_ENTITY)

        val replacedUser = userDao.userById(TEST_USER_ID)

        oldUser.name shouldBeEqualTo ORIGINAL_NAME
        replacedUser.name shouldBeEqualTo CHANGED_NAME
        oldUser.id shouldBeEqualTo replacedUser.id
    }

    @Test
    fun updateEntityThatExists_readUsers_userWithSameIdShouldBeUpdated() = databaseTestRule.runTest {
        userDao.insert(TEST_USER_ENTITY)

        val oldUser = userDao.userById(TEST_USER_ID)

        userDao.update(REPLACEMENT_TEST_USER_ENTITY)

        val updatedUser = userDao.userById(TEST_USER_ID)

        oldUser.name shouldBeEqualTo ORIGINAL_NAME
        updatedUser.name shouldBeEqualTo CHANGED_NAME
        oldUser.id shouldBeEqualTo updatedUser.id
    }

    @Test
    fun deleteEntity_readUsers_doesNotContainDeletedItem() = databaseTestRule.runTest {
        userDao.insert(TEST_USER_ENTITY)

        userDao.delete(TEST_USER_ENTITY)

        userDao.users().isEmpty() shouldBe true
    }

    companion object {
        private const val TEST_USER_ID = "123435weoiruwe"
        private const val ORIGINAL_NAME = "originalName"
        private const val CHANGED_NAME = "changedName"
        private val TEST_USER_ENTITY = UserEntity(TEST_USER_ID, ORIGINAL_NAME)
        private val REPLACEMENT_TEST_USER_ENTITY = UserEntity(TEST_USER_ID, CHANGED_NAME)
    }
}
