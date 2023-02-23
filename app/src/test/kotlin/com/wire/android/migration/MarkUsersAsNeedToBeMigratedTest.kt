package com.wire.android.migration

import android.content.Context
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.migration.failure.UserMigrationStatus
import com.wire.android.migration.feature.MarkUsersAsNeedToBeMigrated
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class MarkUsersAsNeedToBeMigratedTest {

    @Test
    fun givenUserScalaDBsExists_thenAllUsersAreMarkedAsNeedToBeMigrated() = runTest {
        val userId1 = UUID.randomUUID().toString()
        val userId2 = UUID.randomUUID().toString()

        val (arrangement, useCase) = Arrangement()
            .withLocalDBList(listOf(userId1, userId2, "waldo"))
            .withMarkUsersAsNeedToBeMigrated()
            .arrange()
        useCase()
        coVerify(exactly = 1) { arrangement.globalDataStore.setUserMigrationStatus(userId1, UserMigrationStatus.NotStarted) }
        coVerify(exactly = 1) { arrangement.globalDataStore.setUserMigrationStatus(userId2, UserMigrationStatus.NotStarted) }
        coVerify(exactly = 2) { arrangement.globalDataStore.setUserMigrationStatus(any(), any()) }
    }

    @Test
    fun givenUserScalaDBsDoesNotExist_thenNoUsersAreMarkedAsNeedToBeMigrated() = runTest {
        val (arrangement, useCase) = Arrangement()
            .withLocalDBList(listOf("waldo"))
            .withMarkUsersAsNeedToBeMigrated()
            .arrange()
        useCase()
        coVerify(exactly = 0) { arrangement.globalDataStore.setUserMigrationStatus(any(), any()) }
    }

    private class Arrangement {

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        @MockK
        lateinit var applicationContext: Context

        @MockK
        lateinit var globalDataStore: GlobalDataStore

        val useCase = MarkUsersAsNeedToBeMigrated(
            applicationContext,
            globalDataStore
        )

        fun withLocalDBList(list: List<String>) = apply {
            every { applicationContext.databaseList() } returns list.toTypedArray()
        }

        fun withMarkUsersAsNeedToBeMigrated() = apply {
            coEvery { globalDataStore.setUserMigrationStatus(any(), UserMigrationStatus.NotStarted) } returns Unit
        }

        fun arrange() = this to useCase
    }
}
