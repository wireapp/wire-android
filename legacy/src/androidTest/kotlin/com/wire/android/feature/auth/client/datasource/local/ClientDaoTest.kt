package com.wire.android.feature.auth.client.datasource.local

import com.wire.android.InstrumentationTest
import com.wire.android.core.storage.db.user.UserDatabase
import com.wire.android.framework.storage.db.DatabaseTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ClientDaoTest : InstrumentationTest() {

    @get:Rule
    val databaseTestRule = DatabaseTestRule.create<UserDatabase>(appContext)

    private lateinit var clientDao: ClientDao
    private lateinit var userDatabase: UserDatabase

    @Before
    fun setUp() {
        userDatabase = databaseTestRule.database
        clientDao = userDatabase.clientDao()
    }

    @Test
    fun insertClient_readClient_containsInsertedItems() = databaseTestRule.runTest {
        val client1 = ClientEntity("1254-1")
        val client2 = ClientEntity("1254-2")
        clientDao.insert(client1)
        clientDao.insert(client2)

        val result = clientDao.clients()

        result shouldContainSame arrayOf(client1, client2)
    }

    @Test
    fun insertClient_readClientById_containsInsertedItem() = databaseTestRule.runTest {
        val client = ClientEntity("12352-33")
        clientDao.insert(client)

        val result = clientDao.clientById(client.id)

        result shouldBeEqualTo client
    }
}
