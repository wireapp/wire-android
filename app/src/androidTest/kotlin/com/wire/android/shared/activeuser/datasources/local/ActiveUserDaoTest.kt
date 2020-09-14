package com.wire.android.shared.activeuser.datasources.local

import com.wire.android.core.storage.db.DatabaseTest
import com.wire.android.core.storage.db.global.GlobalDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

@ExperimentalCoroutinesApi
class ActiveUserDaoTest : DatabaseTest() {

    private lateinit var activeUserDao: ActiveUserDao
    private lateinit var globalDatabase: GlobalDatabase

    @Before
    fun setUp() {
        globalDatabase = buildDatabase()
        activeUserDao = globalDatabase.activeUserDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        globalDatabase.close()
    }

    @Test
    fun insertEntity_readActiveUsers_containsInsertedItem() = runTest {
        val entity = ActiveUserEntity(TEST_USER_ID)

        activeUserDao.insert(entity)
        val activeUsers = activeUserDao.activeUsers()

        assertThat(entity).isEqualTo(activeUsers.first())
    }

    //TODO: add insert replace strategy test

    companion object {
        private const val TEST_USER_ID = "123435weoiruwe"
    }
}
