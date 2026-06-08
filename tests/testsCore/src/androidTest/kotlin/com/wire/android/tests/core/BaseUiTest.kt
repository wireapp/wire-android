/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.tests.core

import backendUtils.BackendClient
import backendUtils.team.deleteTeam
import backendUtils.user.deleteUser
import backendUtils.user.removeBackendClients
import com.wire.android.tests.core.di.testModule
import org.junit.Rule
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import com.wire.android.tests.support.suite.AllureFailureScreenshotRule
import com.wire.android.tests.support.suite.AllureLabelsRule
import com.wire.android.tests.support.testiny.TestinySyncRule
import io.qameta.allure.kotlin.Allure
import user.usermanager.ClientUserManager
import user.utils.ClientUser

/**
 * Base class for all UI tests.
 * - Starts Koin with testModule
 */
abstract class BaseUiTest : KoinTest {

    // Dependency injection
    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(testModule)
    }

    // Push TestCaseId / Category / Tag into Allure labels
    @get:Rule
    val allureLabelsRule = AllureLabelsRule()

    // Screenshot ONLY for real failures
    @get:Rule
    val failureScreenshotRule = AllureFailureScreenshotRule()

    // Report each finished test to Testiny.
    @get:Rule
    val testinySyncRule = TestinySyncRule()

    protected fun step(name: String, block: () -> Unit) {
        Allure.step(name) { block() }
    }

    /**
     * Removes backend clients/devices for the given user during teardown.
     *
     * Important:
     * - This does NOT delete the user account.
     * - This does NOT delete the team.
     * - This only removes backend clients/devices for that user.
     */
    protected fun cleanupBackendClient(
        backendClient: BackendClient,
        user: ClientUser?
    ) {
        runCatching { user?.removeBackendClients(backendClient) }
    }

    /**
     * Shared teardown cleanup for users created through TeamHelper/ClientUserManager.
     *
     * What this method does:
     * 1. Removes backend clients/devices for every tracked created user.
     * 2. Deletes teams for tracked team owners.
     * 3. Optionally deletes tracked personal users when deletePersonalUsers = true.
     *
     */
    protected fun cleanupCreatedUsers(
        backendClient: BackendClient,
        usersManager: ClientUserManager,
        deletePersonalUsers: Boolean = false
    ) {
        // Removes backend clients/devices only. This does not delete the user.
        usersManager.createdUsers.forEach { user ->
            cleanupBackendClient(backendClient, user)
        }

        // Deletes tracked teams. Deleting a team also removes its members on the backend.
        usersManager.getAllTeamOwners().forEach { owner ->
            runCatching { owner.deleteTeam(backendClient) }
        }

        if (deletePersonalUsers) {
            // Deletes tracked personal users only when the test explicitly opts in.
            usersManager.createdUsers
                .filter { it.teamId.isNullOrBlank() }
                .forEach { user ->
                    runCatching { user.deleteUser(backendClient) }
                }
        }
    }
}
