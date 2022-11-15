package com.wire.android.migration

import android.content.Context
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.migration.feature.MigrateActiveAccountsUseCase
import com.wire.android.migration.feature.MigrateClientsDataUseCase
import com.wire.android.migration.feature.MigrateConversationsUseCase
import com.wire.android.migration.feature.MigrateMessagesUseCase
import com.wire.android.migration.feature.MigrateServerConfigUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MigrationManagerTest {

    @Test
    fun givenDBFileExistsAndMigrationCompleted_whenCheckingWhetherToMigrate_thenReturnFalse() = runTest {
        val (_, manager) = Arrangement()
            .withDBFileExists(true)
            .withMigrationCompleted(true)
            .arrange()
        assert(!manager.shouldMigrate())
    }

    @Test
    fun givenDBFileNotExistsAndMigrationCompleted_whenCheckingWhetherToMigrate_thenReturnFalse() = runTest {
        val (_, manager) = Arrangement()
            .withDBFileExists(false)
            .withMigrationCompleted(true)
            .arrange()
        assert(!manager.shouldMigrate())
    }

    @Test
    fun givenDBFileExistsAndMigrationNotCompleted_whenCheckingWhetherToMigrate_thenReturnTrue() = runTest {
        val (_, manager) = Arrangement()
            .withDBFileExists(true)
            .withMigrationCompleted(false)
            .arrange()
        assert(manager.shouldMigrate())
    }

    @Test
    fun givenDBFileNotExistsAndMigrationNotCompleted_whenCheckingWhetherToMigrate_thenSetMigrationCompletedAndReturnFalse() = runTest {
        val (arrangement, manager) = Arrangement()
            .withDBFileExists(false)
            .withMigrationCompleted(false)
            .arrange()
        assert(!manager.shouldMigrate())
        coVerify(exactly = 1) { arrangement.globalDataStore.setMigrationCompleted() }
    }

    private class Arrangement {
        @MockK
        lateinit var applicationContext: Context

        @MockK
        lateinit var globalDataStore: GlobalDataStore

        @MockK
        lateinit var migrateServerConfigUseCase: MigrateServerConfigUseCase

        @MockK
        lateinit var dbFile: File

        @MockK
        lateinit var migrateActiveAccounts: MigrateActiveAccountsUseCase

        @MockK
        lateinit var migrateClientsData: MigrateClientsDataUseCase

        @MockK
        lateinit var migrateConversations: MigrateConversationsUseCase

        @MockK
        lateinit var migrateMessages: MigrateMessagesUseCase

        private val manager: MigrationManager by lazy {
            MigrationManager(
                applicationContext,
                globalDataStore,
                migrateServerConfigUseCase,
                migrateActiveAccounts,
                migrateClientsData,
                migrateConversations,
                migrateMessages
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { applicationContext.getDatabasePath(any()) } returns dbFile
        }

        fun withDBFileExists(exists: Boolean): Arrangement {
            every { dbFile.isFile } returns exists
            every { dbFile.exists() } returns exists
            return this
        }

        fun withMigrationCompleted(completed: Boolean): Arrangement {
            coEvery { globalDataStore.isMigrationCompleted() } returns completed
            return this
        }

        fun arrange() = this to manager
    }
}
