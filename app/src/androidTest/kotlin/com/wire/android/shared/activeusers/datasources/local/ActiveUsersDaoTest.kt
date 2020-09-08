package com.wire.android.shared.activeusers.datasources.local

import com.wire.android.core.storage.db.DatabaseTest
import com.wire.android.core.storage.db.global.GlobalDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

@ExperimentalCoroutinesApi
class ActiveUsersDaoTest : DatabaseTest() {

    private lateinit var activeUsersDao: ActiveUsersDao
    private lateinit var globalDatabase: GlobalDatabase

    @Before
    fun setUp() {
        globalDatabase = buildDatabase()
        activeUsersDao = globalDatabase.activeUsersDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        globalDatabase.close()
    }

    @Test
    fun insert_canBeReadByActiveAccounts() = runTest {
        val entity = ActiveUserEntity(TEST_USER_ID)

        activeUsersDao.insert(entity)
        val activeUsers = activeUsersDao.activeUsers()

        assertThat(entity).isEqualTo(activeUsers[0])
    }

    //TODO: add insert replace strategy test

    companion object {
        private const val TEST_USER_ID = "123435weoiruwe"
    }
}
