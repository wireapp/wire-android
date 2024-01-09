/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.migration

import android.app.NotificationManager
import android.content.Context
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.migration.feature.MarkUsersAsNeedToBeMigrated
import com.wire.android.migration.feature.MigrateActiveAccountsUseCase
import com.wire.android.migration.feature.MigrateClientsDataUseCase
import com.wire.android.migration.feature.MigrateConversationsUseCase
import com.wire.android.migration.feature.MigrateMessagesUseCase
import com.wire.android.migration.feature.MigrateServerConfigUseCase
import com.wire.android.migration.feature.MigrateUsersUseCase
import com.wire.android.migration.userDatabase.ScalaConversationData
import com.wire.android.util.newServerConfig
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.functional.Either
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MigrationManagerTest {

    @Test
    fun whenMigrating_thenMarkUsersAsNeedToBeMigrated() = runTest {
        val serverConfig = newServerConfig(1)
        val userId = UserId("user-id", "domain")
        val (arrangement, manager) = Arrangement()
            .withMarkUsersAsNeedToBeMigrated()
            .withMigrateServerConfig(Either.Right(serverConfig))
            .withMigrateAccounts(
                MigrateActiveAccountsUseCase.Result(
                    mapOf(userId.value to Either.Right(userId)),
                    false
                )
            )
            .withMigrateClientsData(Either.Right(Unit))
            .withMigrateUsers(Either.Right(userId))
            .withMigrateConversations(Either.Right(listOf()))
            .withMigrateMessages(emptyMap())
            .arrange()
        manager.migrate(
            arrangement.coroutineScope,
            { _ -> },
            arrangement.coroutineDispatcher
        )
        coVerify(exactly = 1) { arrangement.markUsersAsNeedToBeMigrated() }
    }

    @Test
    fun givenDBFileExistsAndMigrationCompleted_whenCheckingWhetherToMigrate_thenReturnFalse() = runTest {
        val (arrangement, manager) = Arrangement()
            .withDBFileExists(true)
            .withMigrationCompleted(true)
            .arrange()
        assert(!manager.shouldMigrate())
        coVerify(exactly = 0) { arrangement.globalDataStore.setWelcomeScreenPresented() }
    }

    @Test
    fun givenDBFileNotExistsAndMigrationCompleted_whenCheckingWhetherToMigrate_thenReturnFalse() = runTest {
        val (arrangement, manager) = Arrangement()
            .withDBFileExists(false)
            .withMigrationCompleted(true)
            .arrange()
        assert(!manager.shouldMigrate())
        coVerify(exactly = 0) { arrangement.globalDataStore.setWelcomeScreenPresented() }
    }

    @Test
    fun givenDBFileExistsAndMigrationNotCompleted_whenCheckingWhetherToMigrate_thenReturnTrue() = runTest {
        val (arrangement, manager) = Arrangement()
            .withDBFileExists(true)
            .withMigrationCompleted(false)
            .arrange()

        assert(manager.shouldMigrate())
        coVerify(exactly = 1) { arrangement.globalDataStore.setWelcomeScreenNotPresented() }
    }

    @Test
    fun givenDBFileNotExistsAndMigrationNotCompleted_whenCheckingWhetherToMigrate_thenSetMigrationCompletedAndReturnFalse() = runTest {
        val (arrangement, manager) = Arrangement()
            .withDBFileExists(false)
            .withMigrationCompleted(false)
            .arrange()
        assert(!manager.shouldMigrate())
        coVerify(exactly = 1) {
            arrangement.globalDataStore.setWelcomeScreenPresented()
            arrangement.globalDataStore.setMigrationCompleted()
        }
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
        lateinit var migrateUsers: MigrateUsersUseCase

        @MockK
        lateinit var migrateConversations: MigrateConversationsUseCase

        @MockK
        lateinit var migrateMessages: MigrateMessagesUseCase

        @MockK
        lateinit var markUsersAsNeedToBeMigrated: MarkUsersAsNeedToBeMigrated

        @MockK
        lateinit var notificationManager: NotificationManager

        val coroutineScope: CoroutineScope = TestScope()

        val coroutineDispatcher = StandardTestDispatcher()

        private val manager: MigrationManager by lazy {
            MigrationManager(
                applicationContext,
                globalDataStore,
                migrateServerConfigUseCase,
                migrateActiveAccounts,
                migrateClientsData,
                migrateUsers,
                migrateConversations,
                migrateMessages,
                markUsersAsNeedToBeMigrated,
                notificationManager
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

        fun withMarkUsersAsNeedToBeMigrated(throwable: Throwable? = null) = apply {
            if (throwable != null) coEvery { markUsersAsNeedToBeMigrated() } throws throwable
            else coEvery { markUsersAsNeedToBeMigrated() } returns Unit
        }

        fun withMigrateServerConfig(result: Either<CoreFailure, ServerConfig>) = apply {
            coEvery { migrateServerConfigUseCase() } returns result
        }

        fun withMigrateAccounts(result: MigrateActiveAccountsUseCase.Result) = apply {
            coEvery { migrateActiveAccounts(any()) } returns result
        }

        fun withMigrateClientsData(result: Either<CoreFailure, Unit>) = apply {
            coEvery { migrateClientsData(any(), any()) } returns result
        }

        fun withMigrateUsers(result: Either<CoreFailure, UserId>) = apply {
            coEvery { migrateUsers(any()) } returns result
        }

        fun withMigrateConversations(result: Either<CoreFailure, List<ScalaConversationData>>) = apply {
            coEvery { migrateConversations(any()) } returns result
        }

        fun withMigrateMessages(result: Map<String, CoreFailure>) = apply {
            coEvery { migrateMessages(any(), any(), coroutineScope) } returns result
        }

        fun arrange() = this to manager
    }
}
