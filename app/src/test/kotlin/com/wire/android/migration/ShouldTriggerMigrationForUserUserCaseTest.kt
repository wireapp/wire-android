package com.wire.android.migration

import android.content.Context
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
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

        private val useCase = ShouldTriggerMigrationForUserUserCase(
            applicationContext,
            globalDataStore,
            currentAppVersion
        )

        fun arrange() = this to useCase
    }
}
