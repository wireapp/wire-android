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
package com.wire.android.emm

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.util.EMPTY
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ManagedConfigurationsReceiverTest {

    @Test
    fun `given ACTION_APPLICATION_RESTRICTIONS_CHANGED intent, when onReceive is called, then refresh both server and SSO configs`() =
        runTest {
            val (arrangement, receiver) = Arrangement()
                .withIntent(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)
                .arrange()

            receiver.onReceive(arrangement.context, arrangement.intent)
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.managedConfigurationsManager.refreshServerConfig() }
            coVerify(exactly = 1) {
                arrangement.managedConfigurationsReporter.reportAppliedState(
                    eq(ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey()),
                    any(),
                    any()
                )
            }
            coVerify(exactly = 1) { arrangement.managedConfigurationsManager.refreshSSOCodeConfig() }
            coVerify(exactly = 1) {
                arrangement.managedConfigurationsReporter.reportAppliedState(
                    eq(ManagedConfigurationsKeys.SSO_CODE.asKey()),
                    any(),
                    any()
                )
            }
            coVerify(exactly = 1) { arrangement.managedConfigurationsManager.refreshPersistentWebSocketConfig() }
            coVerify(exactly = 1) {
                arrangement.managedConfigurationsReporter.reportAppliedState(
                    eq(ManagedConfigurationsKeys.KEEP_WEBSOCKET_CONNECTION.asKey()),
                    any(),
                    any()
                )
            }
        }

    @Test
    fun `given ACTION_APPLICATION_RESTRICTIONS_CHANGED intent, when onReceive is called and refresh server returns an error, then notify`() =
        runTest {
            val (arrangement, receiver) = Arrangement()
                .withIntent(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)
                .withRefreshServerConfigResult(ServerConfigResult.Failure("Test error"))
                .arrange()

            receiver.onReceive(arrangement.context, arrangement.intent)
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.managedConfigurationsManager.refreshServerConfig() }
            coVerify(exactly = 1) {
                arrangement.managedConfigurationsReporter.reportErrorState(
                    eq(ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey()),
                    eq("Test error"),
                    any()
                )
            }
        }

    @Test
    fun `given ACTION_APPLICATION_RESTRICTIONS_CHANGED intent, when onReceive is called and refresh sso code returns an error, then notify`() =
        runTest {
            val (arrangement, receiver) = Arrangement()
                .withIntent(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)
                .withRefreshSSOConfigResult(SSOCodeConfigResult.Failure("Test error"))
                .arrange()

            receiver.onReceive(arrangement.context, arrangement.intent)
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.managedConfigurationsManager.refreshSSOCodeConfig() }
            coVerify(exactly = 1) {
                arrangement.managedConfigurationsReporter.reportErrorState(
                    eq(ManagedConfigurationsKeys.SSO_CODE.asKey()),
                    eq("Test error"),
                    any()
                )
            }
        }

    @Test
    fun `given ACTION_APPLICATION_RESTRICTIONS_CHANGED intent, when onReceive is called with Empty Server Config, then notify cleared`() =
        runTest {
            val (arrangement, receiver) = Arrangement()
                .withIntent(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)
                .withRefreshServerConfigResult(ServerConfigResult.Empty)
                .arrange()

            receiver.onReceive(arrangement.context, arrangement.intent)
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.managedConfigurationsManager.refreshServerConfig() }
            coVerify(exactly = 1) {
                arrangement.managedConfigurationsReporter.reportAppliedState(
                    eq(ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey()),
                    eq("Managed configuration cleared"),
                    eq(String.EMPTY)
                )
            }
        }

    @Test
    fun `given ACTION_APPLICATION_RESTRICTIONS_CHANGED intent, when onReceive is called with Empty SSO Config, then notify cleared`() =
        runTest {
            val (arrangement, receiver) = Arrangement()
                .withIntent(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)
                .withRefreshSSOConfigResult(SSOCodeConfigResult.Empty)
                .arrange()

            receiver.onReceive(arrangement.context, arrangement.intent)
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.managedConfigurationsManager.refreshSSOCodeConfig() }
            coVerify(exactly = 1) {
                arrangement.managedConfigurationsReporter.reportAppliedState(
                    eq(ManagedConfigurationsKeys.SSO_CODE.asKey()),
                    eq("Managed configuration cleared"),
                    eq(String.EMPTY)
                )
            }
        }

    @Test
    fun `given unexpected intent action, when onReceive is called, then do not refresh configurations`() =
        runTest {
            val (arrangement, receiver) = Arrangement()
                .withIntent("com.wire.android.UNEXPECTED_ACTION")
                .arrange()

            receiver.onReceive(arrangement.context, arrangement.intent)
            advanceUntilIdle()

            coVerify(exactly = 0) { arrangement.managedConfigurationsManager.refreshServerConfig() }
            coVerify(exactly = 0) { arrangement.managedConfigurationsManager.refreshSSOCodeConfig() }
        }

    @Test
    fun `given null intent action, when onReceive is called, then do not refresh configurations`() =
        runTest {
            val (arrangement, receiver) = Arrangement()
                .withIntent(null)
                .arrange()

            receiver.onReceive(arrangement.context, arrangement.intent)
            advanceUntilIdle()

            coVerify(exactly = 0) { arrangement.managedConfigurationsManager.refreshServerConfig() }
            coVerify(exactly = 0) { arrangement.managedConfigurationsManager.refreshSSOCodeConfig() }
        }

    private class Arrangement {

        val context: Context = ApplicationProvider.getApplicationContext()
        val managedConfigurationsManager: ManagedConfigurationsManager = mockk(relaxed = true)
        val managedConfigurationsReporter: ManagedConfigurationsReporter = mockk(relaxed = true)
        private val dispatchers = TestDispatcherProvider()
        private val persistentWebSocketEnforcedFlow = MutableStateFlow(false)
        lateinit var intent: Intent

        init {
            every { managedConfigurationsManager.persistentWebSocketEnforcedByMDM } returns persistentWebSocketEnforcedFlow
        }

        fun withIntent(action: String?) = apply {
            intent = if (action != null) Intent(action) else Intent()
        }

        fun withRefreshServerConfigResult(result: ServerConfigResult) = apply {
            coEvery { managedConfigurationsManager.refreshServerConfig() } returns result
        }

        fun withRefreshSSOConfigResult(result: SSOCodeConfigResult) = apply {
            coEvery { managedConfigurationsManager.refreshSSOCodeConfig() } returns result
        }

        fun withPersistentWebSocketEnforced(enforced: Boolean) = apply {
            persistentWebSocketEnforcedFlow.value = enforced
        }

        fun arrange() = this to ManagedConfigurationsReceiver(
            managedConfigurationsManager,
            managedConfigurationsReporter,
            dispatchers
        )
    }
}
