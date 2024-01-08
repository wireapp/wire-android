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
