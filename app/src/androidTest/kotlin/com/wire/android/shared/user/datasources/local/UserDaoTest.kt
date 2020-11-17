package com.wire.android.shared.user.datasources.local

import com.wire.android.InstrumentationTest
import com.wire.android.core.storage.db.global.GlobalDatabase
import com.wire.android.framework.storage.db.DatabaseTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBe
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
    //TODO: add insert replace strategy test

    @Test
    fun deleteEntity_readUsers_doesNotContainDeletedItem() = databaseTestRule.runTest {
        userDao.insert(TEST_USER_ENTITY)

        userDao.delete(TEST_USER_ENTITY)

        userDao.users().isEmpty() shouldBe true
    }

    companion object {
        private const val TEST_USER_ID = "123435weoiruwe"
        private val TEST_USER_ENTITY = UserEntity(TEST_USER_ID, "name")
    }
}
