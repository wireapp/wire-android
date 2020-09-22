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
class UserDaoTest : DatabaseTest() {

    private lateinit var userDao: UserDao
    private lateinit var globalDatabase: GlobalDatabase

    @Before
    fun setUp() {
        globalDatabase = buildDatabase()
        userDao = globalDatabase.userDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        globalDatabase.clearTestData()
    }

    @Test
    fun insertEntity_readUsers_containsInsertedItem() = runTest {
        userDao.insert(TEST_USER_ENTITY)
        val activeUsers = userDao.users()

        assertThat(activeUsers).containsExactly(TEST_USER_ENTITY)
    }
    //TODO: add insert replace strategy test

    @Test
    fun deleteEntity_readUsers_doesNotContainDeletedItem() = runTest {
        userDao.insert(TEST_USER_ENTITY)

        userDao.delete(TEST_USER_ENTITY)
        assertThat(userDao.users()).isEmpty()
    }

    companion object {
        private const val TEST_USER_ID = "123435weoiruwe"
        private val TEST_USER_ENTITY = UserEntity(TEST_USER_ID, "name")
    }
}
