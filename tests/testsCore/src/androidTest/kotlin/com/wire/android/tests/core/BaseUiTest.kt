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

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import backendUtils.BackendClient
import backendUtils.BackendSetupHelper
import backendUtils.team.deleteTeam
import backendUtils.user.deleteUser
import backendUtils.user.removeBackendClients
import com.wire.android.tests.core.di.testModule
import com.wire.android.tests.core.pages.AllPages
import com.wire.android.tests.support.suite.AllureFailureScreenshotRule
import com.wire.android.tests.support.suite.AllureLabelsRule
import com.wire.android.tests.support.testiny.TestinySyncRule
import io.qameta.allure.kotlin.Allure
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import service.TestServiceHelper
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import kotlin.getValue

/**
 * Base class for all UI tests.
 * - Starts Koin with testModule
 * - Provides shared test helpers and default cleanup for users tracked by ClientUserManager
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

    // RuleChain order: failure screenshots run first, then this rule performs cleanup.
    private val failureScreenshotRule = AllureFailureScreenshotRule()
    private val commonCleanupRule = TestRule { base, _: Description ->
        object : Statement() {
            override fun evaluate() {
                try {
                    base.evaluate()
                } finally {
                    tearDownAfterAllureScreenshot()
                }
            }
        }
    }

    // Screenshots must run before backend cleanup changes the app state.
    @get:Rule
    val failureScreenshotAndCleanupRule: RuleChain = RuleChain
        .outerRule(commonCleanupRule)
        .around(failureScreenshotRule)

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

    protected fun trackCreatedUserForCleanup(user: ClientUser) {
        clientUserManager.appendCustomUser(user)
    }

    protected fun trackCreatedTeamOwnerForCleanup(user: ClientUser) {
        user.isTeamOwner = true
        trackCreatedUserForCleanup(user)
    }

    /**
     * Shared teardown cleanup for users created through ClientUserManager.
     *
     * What this method does:
     * 1. Removes backend clients/devices for every tracked created user.
     * 2. Deletes teams for tracked team owners.
     * 3. Optionally deletes tracked personal users when deletePersonalUsers = true.
     *
     */
    protected fun cleanupCreatedUsers(
        backendClient: BackendClient,
        clientUserManager: ClientUserManager,
        deletePersonalUsers: Boolean = false
    ) {
        // Removes backend clients/devices only. This does not delete the user.
        clientUserManager.createdUsers.forEach { user ->
            cleanupBackendClient(backendClient, user)
        }

        // Deletes tracked teams. Deleting a team also removes its members on the backend.
        clientUserManager.getAllTeamOwners().forEach { owner ->
            runCatching { owner.deleteTeam(backendClient) }
        }

        if (deletePersonalUsers) {
            // Deletes tracked personal users only when the test explicitly opts in.
            clientUserManager.createdUsers
                .filter { it.teamId.isNullOrBlank() }
                .forEach { user ->
                    runCatching { user.deleteUser(backendClient) }
                }
        }
    }

    /**
     * Shared UI test helper wiring.
     *
     * Provides common UI test fields such as `pages`, `device`, and instrumentation `context`.
     * `ClientUserManager`, `BackendSetupHelper`, and `TestServiceHelper` must share the same user pool
     * so setup, service actions, and cleanup operate on the same users.
     */
    protected val pages: AllPages by inject()
    protected lateinit var device: UiDevice
    protected lateinit var context: Context
    protected lateinit var backendClient: BackendClient
    protected lateinit var clientUserManager: ClientUserManager
    protected lateinit var backendSetupHelper: BackendSetupHelper
    protected lateinit var testServiceHelper: TestServiceHelper

    /**
     * Shared cleanup keeps personal users by default.
     * Override to true only for tests that create standalone personal users.
     */
    protected open val deletePersonalUsersAfterTest: Boolean = false

    protected fun initCommonTestHelpers(backendName: String = DEFAULT_BACKEND_NAME) {
        initCommonTestHelpers(BackendClient.loadBackend(backendName))
    }

    protected fun initCommonTestHelpers(backendClient: BackendClient) {
        context = InstrumentationRegistry.getInstrumentation().context
        this.backendClient = backendClient
        // The selected backend is passed into ClientUserManager, which stamps it onto generated users.
        clientUserManager = ClientUserManager(
            useSpecialEmail = false,
            backendClient = backendClient
        )
        testServiceHelper = TestServiceHelper(clientUserManager)
        backendSetupHelper = BackendSetupHelper(clientUserManager, testServiceHelper::addDevice)
    }

    private fun tearDownAfterAllureScreenshot() {
        tearDownCallingManager() // CallingManager cleanup for call tests.
        tearDownCommonTestHelpers() // Shared backend user/team cleanup.
    }

    private fun tearDownCommonTestHelpers() {
        if (::backendClient.isInitialized && ::clientUserManager.isInitialized) {
            // Shared cleanup for users tracked by ClientUserManager.
            cleanupCreatedUsers(
                backendClient,
                clientUserManager,
                deletePersonalUsers = deletePersonalUsersAfterTest
            )
        }
    }

    // BaseCallUiTest overrides this to clean CallingManager before common backend cleanup runs.
    protected open fun tearDownCallingManager() = Unit

    private companion object {
        const val DEFAULT_BACKEND_NAME = "STAGING"
    }
}
