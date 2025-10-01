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
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ManagedConfigurationsReceiverTest {

    @MockK
    private lateinit var managedConfigurationsManager: ManagedConfigurationsManager

    private lateinit var context: Context
    private val dispatchers = TestDispatcherProvider()

    @Before
fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `given ACTION_APPLICATION_RESTRICTIONS_CHANGED intent, when onReceive is called, then refresh both server and SSO configs`() =
        runTest {
            val receiver = ManagedConfigurationsReceiver(managedConfigurationsManager, dispatchers)
            val intent = Intent(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)

            receiver.onReceive(context, intent)
            advanceUntilIdle()

            coVerify(exactly = 1) { managedConfigurationsManager.refreshServerConfig() }
            coVerify(exactly = 1) { managedConfigurationsManager.refreshSSOCodeConfig() }
        }

    @Test
    fun `given unexpected intent action, when onReceive is called, then do not refresh configurations`() =
        runTest {
            val receiver = ManagedConfigurationsReceiver(managedConfigurationsManager, dispatchers)
            val intent = Intent("com.wire.android.UNEXPECTED_ACTION")

            receiver.onReceive(context, intent)
            advanceUntilIdle()

            coVerify(exactly = 0) { managedConfigurationsManager.refreshServerConfig() }
            coVerify(exactly = 0) { managedConfigurationsManager.refreshSSOCodeConfig() }
        }

    @Test
    fun `given null intent action, when onReceive is called, then do not refresh configurations`() =
        runTest {
            val receiver = ManagedConfigurationsReceiver(managedConfigurationsManager, dispatchers)
            val intent = Intent()

            receiver.onReceive(context, intent)
            advanceUntilIdle()

            coVerify(exactly = 0) { managedConfigurationsManager.refreshServerConfig() }
            coVerify(exactly = 0) { managedConfigurationsManager.refreshSSOCodeConfig() }
        }
}
