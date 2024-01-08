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

import android.content.Context
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.migration.failure.UserMigrationStatus
import com.wire.android.migration.userDatabase.ShouldTriggerMigrationForUserUserCase
import com.wire.kalium.logic.data.user.UserId
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ShouldTriggerMigrationForUserUserCaseTest {

    @Test
    fun givenUserNeedToBeMigratedAndDBExists_thenReturnTrue() = runTest {
        val (arrangement, useCase) = Arrangement()
            .withUserHaveAValidDB()
            .withUserMigrationStatus(UserMigrationStatus.NotStarted)
            .arrange()

        val userId = UserId("userId", "domain")
        assertTrue(useCase(userId))

        verify(exactly = 1) { arrangement.globalDataStore.getUserMigrationStatus(userId.value) }
    }

    @Test
    fun givenUserNeedToBeMigratedAndDBIsInvalid_thenMarkAsNotNeededAndReturnFalse() = runTest {
        val (arrangement, useCase) = Arrangement()
            .withUserDoesNotHaveScalaDB()
            .withUserMigrationStatus(UserMigrationStatus.NotStarted)
            .arrange()

        val userId = UserId("userId", "domain")
        assertFalse(useCase(userId))

        verify(exactly = 1) { arrangement.globalDataStore.getUserMigrationStatus(userId.value) }
        coVerify(exactly = 1) { arrangement.globalDataStore.setUserMigrationStatus(userId.value, UserMigrationStatus.NoNeed) }
    }

    @Test
    fun givenUserNotNeedMigration_thenReturnFalse() = runTest {
        val (arrangement, useCase) = Arrangement()
            .withUserMigrationStatus(UserMigrationStatus.NoNeed)
            .arrange()

        val userId = UserId("userId", "domain")
        assertFalse(useCase(userId))

        verify(exactly = 1) { arrangement.globalDataStore.getUserMigrationStatus(userId.value) }
        verify(exactly = 0) { arrangement.applicationContext.getDatabasePath(userId.value) }
    }

    @Test
    fun givenUserMigrationComplete_whenAppVersionIsNewAndUserHaveScalaDB_thenReturnTrue() = runTest {
        val (arrangement, useCase) = Arrangement()
            .withUserMigrationStatus(UserMigrationStatus.Completed)
            .withNewAppVersion()
            .withUserHaveAValidDB()
            .arrange()

        val userId = UserId("userId", "domain")
        assertTrue(useCase(userId))

        verify(exactly = 1) { arrangement.globalDataStore.getUserMigrationStatus(userId.value) }
        verify(exactly = 1) { arrangement.applicationContext.getDatabasePath(userId.value) }
    }

    @Test
    fun givenUserMigrationComplete_whenAppVersionIsSameAndUserHaveScalaDB_thenReturnFalse() = runTest {
        val (arrangement, useCase) = Arrangement()
            .withUserMigrationStatus(UserMigrationStatus.Completed)
            .withSameAppVersion()
            .withUserHaveAValidDB()
            .arrange()

        val userId = UserId("userId", "domain")
        assertFalse(useCase(userId))

        verify(exactly = 1) { arrangement.globalDataStore.getUserMigrationStatus(userId.value) }
        verify(exactly = 0) { arrangement.applicationContext.getDatabasePath(userId.value) }
    }

    @Test
    fun givenUserMigrationComplete_whenAppVersionIsNewAndUserHaveNoScalaDB_thenReturnFalse() = runTest {
        val (arrangement, useCase) = Arrangement()
            .withUserMigrationStatus(UserMigrationStatus.Completed)
            .withNewAppVersion()
            .withUserDoesNotHaveScalaDB()
            .arrange()

        val userId = UserId("userId", "domain")
        assertFalse(useCase(userId))

        verify(exactly = 1) { arrangement.globalDataStore.getUserMigrationStatus(userId.value) }
        verify(exactly = 1) { arrangement.applicationContext.getDatabasePath(userId.value) }
    }

    @Test
    fun givenNull_whenGettingMigrationStatus_thenCheckForScalaDB() = runTest {
        val (arrangement, useCase) = Arrangement()
            .withUserMigrationStatus(null)
            .withUserHaveAValidDB()
            .arrange()

        val userId = UserId("userId", "domain")
        assertTrue(useCase(userId))

        verify(exactly = 1) { arrangement.globalDataStore.getUserMigrationStatus(userId.value) }
        verify(exactly = 1) { arrangement.applicationContext.getDatabasePath(userId.value) }
    }

    @Test
    fun givenNull_whenGettingMigrationStatusAndScalaDBDoesNotExists_thenCheckForScalaDB() = runTest {
        val (arrangement, useCase) = Arrangement()
            .withUserMigrationStatus(null)
            .withUserDoesNotHaveScalaDB()
            .arrange()

        val userId = UserId("userId", "domain")
        assertFalse(useCase(userId))

        verify(exactly = 1) { arrangement.globalDataStore.getUserMigrationStatus(userId.value) }
        verify(exactly = 1) { arrangement.applicationContext.getDatabasePath(userId.value) }
        coVerify(exactly = 1) { arrangement.globalDataStore.setUserMigrationStatus(userId.value, UserMigrationStatus.NoNeed) }
    }

    private class Arrangement {
        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        @MockK
        lateinit var applicationContext: Context

        @MockK
        lateinit var globalDataStore: GlobalDataStore

        val currentAppVersion = 10
        fun withUserHaveAValidDB() = apply {
            val dbFile: File = mockk()
            every { dbFile.exists() } returns true
            every { dbFile.isFile } returns true
            every { applicationContext.getDatabasePath(any()) } returns dbFile
        }

        fun withUserDoesNotHaveScalaDB() = apply {
            val dbFile: File = mockk()
            every { dbFile.exists() } returns false
            every { dbFile.isFile } returns false
            every { applicationContext.getDatabasePath(any()) } returns dbFile
        }

        fun withUserMigrationStatus(status: UserMigrationStatus?) = apply {
            every { globalDataStore.getUserMigrationStatus(any()) } returns flowOf(status)
        }

        fun withSameAppVersion() = apply {
            coEvery { globalDataStore.getUserMigrationAppVersion(any()) } returns currentAppVersion
        }

        fun withNewAppVersion() = apply {
            coEvery { globalDataStore.getUserMigrationAppVersion(any()) } returns currentAppVersion + 1
        }

        fun withCurrentAppVersion(version: Int?) = apply {
            every { }
        }

        private val useCase = ShouldTriggerMigrationForUserUserCase(
            applicationContext,
            globalDataStore,
            currentAppVersion,
            TestDispatcherProvider()
        )

        fun arrange() = this to useCase
    }
}
